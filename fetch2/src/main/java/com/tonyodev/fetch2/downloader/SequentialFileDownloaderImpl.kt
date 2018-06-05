package com.tonyodev.fetch2.downloader

import com.tonyodev.fetch2.*
import com.tonyodev.fetch2.exception.FetchException
import com.tonyodev.fetch2.provider.NetworkInfoProvider
import com.tonyodev.fetch2.util.*
import java.io.*
import java.net.HttpURLConnection
import kotlin.math.ceil

class SequentialFileDownloaderImpl(private val initialDownload: Download,
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
    private var total: Long = -1L
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
        var randomAccessFileOutput: RandomAccessFile? = null
        var output: OutputStream? = null
        var input: BufferedInputStream? = null
        var response: Downloader.Response? = null
        try {
            val file = getFile()
            downloaded = initialDownload.downloaded
            if (!interrupted && !terminated) {
                val request = getRequest()
                response = downloader.execute(request, interruptMonitor)
                val isResponseSuccessful = response?.isSuccessful ?: false
                if (!interrupted && !terminated && response != null && isResponseSuccessful) {
                    total = if (response.contentLength == -1L) {
                        -1L
                    } else {
                        downloaded + response.contentLength
                    }
                    val seekPosition = if (response.code == HttpURLConnection.HTTP_PARTIAL) {
                        logger.d("FileDownloader resuming Download $download")
                        downloaded
                    } else {
                        logger.d("FileDownloader starting Download $download")
                        0L
                    }
                    output = downloader.getRequestOutputStream(request, seekPosition)
                    if (output == null) {
                        randomAccessFileOutput = RandomAccessFile(file, "rw")
                        randomAccessFileOutput.seek(seekPosition)
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
                        writeToOutput(input, randomAccessFileOutput, output, response)
                    }
                } else if (response == null && !interrupted && !terminated) {
                    throw FetchException(EMPTY_RESPONSE_BODY,
                            FetchException.Code.EMPTY_RESPONSE_BODY)
                } else if (!isResponseSuccessful && !interrupted && !terminated) {
                    throw FetchException(RESPONSE_NOT_SUCCESSFUL,
                            FetchException.Code.REQUEST_NOT_SUCCESSFUL)
                } else if (!interrupted && !terminated) {
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
                    var disconnectDetected = !networkInfoProvider.isNetworkAvailable
                    for (i in 1..10) {
                        try {
                            Thread.sleep(500)
                        } catch (e: InterruptedException) {
                            logger.e("FileDownloader", e)
                            break
                        }
                        if (!networkInfoProvider.isNetworkAvailable) {
                            disconnectDetected = true
                            break
                        }
                    }
                    if (disconnectDetected) {
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
                randomAccessFileOutput?.close()
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
            try {
                output?.close()
            } catch (e: Exception) {
                logger.e("FileDownloader", e)
            }
            terminated = true
        }
    }

    private fun writeToOutput(input: BufferedInputStream,
                              randomAccessFileOutput: RandomAccessFile?,
                              downloaderOutputStream: OutputStream?,
                              response: Downloader.Response) {
        var reportingStopTime: Long
        var downloadSpeedStopTime: Long
        var downloadedBytesPerSecond = downloaded
        val buffer = ByteArray(downloadBufferSizeBytes)
        var reportingStartTime = System.nanoTime()
        var downloadSpeedStartTime = System.nanoTime()

        var read = input.read(buffer, 0, downloadBufferSizeBytes)
        while (!interrupted && !terminated && read != -1) {
            randomAccessFileOutput?.write(buffer, 0, read)
            downloaderOutputStream?.write(buffer, 0, read)
            if (!terminated) {
                downloaded += read
                downloadInfo.downloaded = downloaded
                downloadInfo.total = total
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
                    if (progressReportingIntervalMillis > DEFAULT_DOWNLOAD_SPEED_REPORTING_INTERVAL_IN_MILLISECONDS) {
                        delegate?.saveDownloadProgress(downloadInfo)
                    }
                }

                reportingStopTime = System.nanoTime()
                val hasReportingTimeElapsed = hasIntervalTimeElapsed(reportingStartTime,
                        reportingStopTime, progressReportingIntervalMillis)

                if (hasReportingTimeElapsed) {
                    if (progressReportingIntervalMillis <= DEFAULT_DOWNLOAD_SPEED_REPORTING_INTERVAL_IN_MILLISECONDS) {
                        delegate?.saveDownloadProgress(downloadInfo)
                    }
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
        try {
            downloaderOutputStream?.flush()
        } catch (e: IOException) {
            logger.e("FileDownloader", e)
        }
        if (read == -1 && !interrupted && !terminated) {
            total = downloaded
            completedDownload = true
            downloadInfo.downloaded = downloaded
            downloadInfo.total = total
            if (!terminated) {
                if (downloader.verifyContentMD5(response.request, response.md5)) {
                    delegate?.onProgress(
                            download = downloadInfo,
                            etaInMilliSeconds = estimatedTimeRemainingInMilliseconds,
                            downloadedBytesPerSecond = getAverageDownloadedBytesPerSecond())
                    delegate?.onComplete(
                            download = downloadInfo)
                } else {
                    throw FetchException(INVALID_CONTENT_MD5, FetchException.Code.INVALID_CONTENT_MD5)
                }
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
        return Downloader.Request(
                id = initialDownload.id,
                url = initialDownload.url,
                headers = headers,
                file = initialDownload.file,
                tag = initialDownload.tag)
    }

    private fun getAverageDownloadedBytesPerSecond(): Long {
        if (averageDownloadedBytesPerSecond < 1) {
            return 0L
        }
        return ceil(averageDownloadedBytesPerSecond).toLong()
    }

    private val interruptMonitor = object : InterruptMonitor {
        override val isInterrupted: Boolean
            get() {
                return interrupted
            }
    }

}