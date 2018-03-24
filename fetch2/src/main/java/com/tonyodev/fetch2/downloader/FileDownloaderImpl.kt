package com.tonyodev.fetch2.downloader

import com.tonyodev.fetch2.*
import com.tonyodev.fetch2.exception.FetchException
import com.tonyodev.fetch2.provider.NetworkInfoProvider
import com.tonyodev.fetch2.util.*
import java.io.BufferedInputStream
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import kotlin.math.ceil

class FileDownloaderImpl(private val initialDownload: Download,
                         private val downloader: Downloader,
                         private val progressReportingIntervalMillis: Long,
                         private val downloadBufferSizeBytes: Int,
                         private val logger: Logger,
                         private val networkInfoProvider: NetworkInfoProvider,
                         private val retryOnNetworkGain: Boolean) : FileDownloader {

    @Volatile
    override var interrupted = false
    @Volatile
    override var terminated = false
    @Volatile
    override var completedDownload = false
    override var delegate: FileDownloader.Delegate? = null
    private var total: Long = 0
    private var downloaded: Long = 0
    private var estimatedTimeRemainingInMilliseconds: Long = -1
    private var downloadInfo = initialDownload.toDownloadInfo()
    private var averageDownloadedBytesPerSecond = 0.0
    private val movingAverageCalculator = AverageCalculator(5)

    override val download: Download
        get () {
            downloadInfo.downloaded = downloaded
            downloadInfo.total = total
            return downloadInfo
        }

    override fun run() {
        var output: RandomAccessFile? = null
        var input: BufferedInputStream? = null
        var response: Downloader.Response? = null
        try {
            val file = getFile()
            downloaded = file.length()
            if (!interrupted && !terminated) {
                response = downloader.execute(getRequest())
                val isResponseSuccessful = response?.isSuccessful ?: false
                if (!interrupted && !terminated && response != null && isResponseSuccessful) {
                    total = if (response.contentLength == (-1).toLong()) {
                        -1
                    } else {
                        downloaded + response.contentLength
                    }
                    output = RandomAccessFile(file, "rw")
                    if (response.code == HttpURLConnection.HTTP_PARTIAL) {
                        output.seek(downloaded)
                        logger.d("FileDownloader resuming Download $download")
                    } else {
                        output.seek(0)
                        logger.d("FileDownloader starting Download $download")
                    }
                    if (!interrupted && !terminated) {
                        input = BufferedInputStream(response.byteStream, downloadBufferSizeBytes)
                        downloadInfo.downloaded = downloaded
                        downloadInfo.total = total
                        if (!terminated) {
                            delegate?.onStarted(
                                    download = downloadInfo,
                                    etaInMilliseconds = estimatedTimeRemainingInMilliseconds,
                                    downloadedBytesPerSecond = getAverageDownloadedBytesPerSecond())
                        }
                        writeToOutput(input, output)
                    }
                } else if (response == null && !interrupted && !terminated) {
                    throw FetchException(EMPTY_RESPONSE_BODY,
                            FetchException.Code.EMPTY_RESPONSE_BODY)
                } else if (!isResponseSuccessful && !interrupted && !terminated) {
                    throw FetchException(RESPONSE_NOT_SUCCESSFUL,
                            FetchException.Code.REQUEST_NOT_SUCCESSFUL)
                } else if(!interrupted && !terminated) {
                    throw FetchException(UNKNOWN_ERROR,
                            FetchException.Code.UNKNOWN)
                }
            }
            if (!completedDownload) {
                downloadInfo.downloaded = downloaded
                downloadInfo.total = total
                if (!terminated) {
                    delegate?.onProgress(
                            download = downloadInfo,
                            etaInMilliSeconds = estimatedTimeRemainingInMilliseconds,
                            downloadedBytesPerSecond = getAverageDownloadedBytesPerSecond())
                }
            }
        } catch (e: Exception) {
            if (!interrupted && !terminated) {
                logger.e("FileDownloader", e)
                var error = getErrorFromThrowable(e)
                error.throwable = e
                if (retryOnNetworkGain) {
                    try {
                        Thread.sleep(5000)
                    } catch (e: InterruptedException) {
                        logger.e("FileDownloader", e)
                    }
                    if (!networkInfoProvider.isNetworkAvailable) {
                        error = Error.NO_NETWORK_CONNECTION
                    }
                }
                downloadInfo.downloaded = downloaded
                downloadInfo.total = total
                downloadInfo.error = error
                if (!terminated) {
                    delegate?.onError(download = downloadInfo)
                }
            }
        } finally {
            try {
                output?.close()
            } catch (e: Exception) {
                logger.e("FileDownloader", e)
            }
            try {
                input?.close()
            } catch (e: Exception) {
                logger.e("FileDownloader", e)
            }
            if (response != null) {
                try {
                    downloader.disconnect(response)
                } catch (e: Exception) {
                    logger.e("FileDownloader", e)
                }
            }
            terminated = true
        }
    }

    private fun writeToOutput(input: BufferedInputStream, output: RandomAccessFile) {
        var reportingStopTime: Long
        var downloadSpeedStopTime: Long
        var downloadedBytesPerSecond = downloaded
        val buffer = ByteArray(downloadBufferSizeBytes)
        var reportingStartTime = System.nanoTime()
        var downloadSpeedStartTime = System.nanoTime()

        var read = input.read(buffer, 0, downloadBufferSizeBytes)
        while (!interrupted && !terminated && read != -1) {
            output.write(buffer, 0, read)
            if (!terminated) {
                downloaded += read
                downloadSpeedStopTime = System.nanoTime()
                val downloadSpeedCheckTimeElapsed = hasIntervalTimeElapsed(downloadSpeedStartTime,
                        downloadSpeedStopTime, DEFAULT_DOWNLOAD_SPEED_REPORTING_INTERVAL_IN_MILLISECONDS)

                if (downloadSpeedCheckTimeElapsed) {
                    downloadedBytesPerSecond = downloaded - downloadedBytesPerSecond
                    movingAverageCalculator.add(downloadedBytesPerSecond.toDouble())
                    averageDownloadedBytesPerSecond =
                            movingAverageCalculator.getMovingAverageWithWeightOnRecentValues()
                    estimatedTimeRemainingInMilliseconds = calculateEstimatedTimeRemainingInMilliseconds(
                            downloadedBytes = downloaded,
                            totalBytes = total,
                            downloadedBytesPerSecond = getAverageDownloadedBytesPerSecond())
                    downloadedBytesPerSecond = downloaded
                }

                reportingStopTime = System.nanoTime()
                val hasReportingTimeElapsed = hasIntervalTimeElapsed(reportingStartTime,
                        reportingStopTime, progressReportingIntervalMillis)

                if (hasReportingTimeElapsed) {
                    downloadInfo.downloaded = downloaded
                    downloadInfo.total = total
                    if (!terminated) {
                        delegate?.onProgress(
                                download = downloadInfo,
                                etaInMilliSeconds = estimatedTimeRemainingInMilliseconds,
                                downloadedBytesPerSecond = getAverageDownloadedBytesPerSecond())
                    }
                    reportingStartTime = System.nanoTime()
                }

                if (downloadSpeedCheckTimeElapsed) {
                    downloadSpeedStartTime = System.nanoTime()
                }
                read = input.read(buffer, 0, downloadBufferSizeBytes)
            }
        }
        if (read == -1 && !interrupted && !terminated) {
            total = downloaded
            completedDownload = true
            downloadInfo.downloaded = downloaded
            downloadInfo.total = total
            if (!terminated) {
                delegate?.onComplete(
                        download = downloadInfo)
            }
        }
    }

    private fun getFile(): File {
        val file = File(initialDownload.file)
        if (!file.exists()) {
            if (file.parentFile != null && !file.parentFile.exists()) {
                if (file.parentFile.mkdirs()) {
                    file.createNewFile()
                    logger.d("FileDownloader download file ${file.absolutePath} created")
                }
            } else {
                file.createNewFile()
                logger.d("FileDownloader download file ${file.absolutePath} created")
            }
        }
        return file
    }

    private fun getRequest(): Downloader.Request {
        val headers = initialDownload.headers.toMutableMap()
        headers["Range"] = "bytes=$downloaded-"
        return Downloader.Request(initialDownload.url, headers)
    }

    private fun getAverageDownloadedBytesPerSecond(): Long {
        if (averageDownloadedBytesPerSecond < 1) {
            return 0L
        }
        return ceil(averageDownloadedBytesPerSecond).toLong()
    }

}