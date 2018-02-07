package com.tonyodev.fetch2.downloader

import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Downloader
import com.tonyodev.fetch2.Logger
import com.tonyodev.fetch2.exception.FetchException
import com.tonyodev.fetch2.getErrorFromMessage
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
                         private val networkInfoProvider: NetworkInfoProvider) : FileDownloader {

    @Volatile
    override var interrupted = false
    @Volatile
    override var terminated = false
    @Volatile
    override var completedDownload = false
    override var delegate: FileDownloader.Delegate? = null
    private var totalBytes: Long = 0
    private var downloadedBytes: Long = 0
    private var estimatedTimeRemainingInMilliseconds: Long = -1
    private var downloadInfo = initialDownload.toDownloadInfo()
    private var averageDownloadedBytesPerSecond = 0.0
    private val movingAverageCalculator = AverageCalculator(5)
    private var isResponseSuccessful = false

    override val download: Download
        get () {
            downloadInfo.downloaded = downloadedBytes
            downloadInfo.total = totalBytes
            return downloadInfo
        }

    override fun run() {
        var output: RandomAccessFile? = null
        var input: BufferedInputStream? = null
        var response: Downloader.Response? = null
        try {
            val file = getFile()
            downloadedBytes = file.length()
            if (!interrupted) {
                response = downloader.execute(getRequest())
                isResponseSuccessful = response?.isSuccessful ?: false
                if (!interrupted && response != null && isResponseSuccessful) {
                    totalBytes = if (response.contentLength == (-1).toLong()) {
                        -1
                    } else {
                        downloadedBytes + response.contentLength
                    }
                    output = RandomAccessFile(file, "rw")
                    if (response.code == HttpURLConnection.HTTP_PARTIAL) {
                        output.seek(downloadedBytes)
                        logger.d("FileDownloader resuming Download $download")
                    } else {
                        output.seek(0)
                        logger.d("FileDownloader starting Download $download")
                    }
                    if (!interrupted) {
                        input = BufferedInputStream(response.byteStream, downloadBufferSizeBytes)
                        downloadInfo.downloaded = downloadedBytes
                        downloadInfo.total = totalBytes
                        delegate?.onStarted(
                                download = downloadInfo,
                                etaInMilliseconds = estimatedTimeRemainingInMilliseconds,
                                downloadedBytesPerSecond = getAverageDownloadedBytesPerSecond())
                        writeToOutput(input, output)
                    }
                } else if (response == null) {
                    throw FetchException(EMPTY_RESPONSE_BODY,
                            FetchException.Code.EMPTY_RESPONSE_BODY)
                } else if (!isResponseSuccessful) {
                    throw FetchException(RESPONSE_NOT_SUCCESSFUL,
                            FetchException.Code.REQUEST_NOT_SUCCESSFUL)
                } else {
                    throw FetchException(UNKNOWN_ERROR,
                            FetchException.Code.UNKNOWN)
                }
            }
            if (!completedDownload) {
                downloadInfo.downloaded = downloadedBytes
                downloadInfo.total = totalBytes
                delegate?.onProgress(
                        download = downloadInfo,
                        etaInMilliSeconds = estimatedTimeRemainingInMilliseconds,
                        downloadedBytesPerSecond = getAverageDownloadedBytesPerSecond())
            }
        } catch (e: Exception) {
            if (!interrupted) {
                logger.e("FileDownloader", e)
                val error = if (networkConnectionWasLost()) {
                    getErrorFromMessage(NETWORK_CONNECTION_LOST)
                } else {
                    getErrorFromMessage(e.message)
                }
                downloadInfo.downloaded = downloadedBytes
                downloadInfo.total = totalBytes
                downloadInfo.error = error
                delegate?.onError(download = downloadInfo)
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
        var downloadedBytesPerSecond = downloadedBytes
        val buffer = ByteArray(downloadBufferSizeBytes)
        var reportingStartTime = System.nanoTime()
        var downloadSpeedStartTime = System.nanoTime()

        var read = input.read(buffer, 0, downloadBufferSizeBytes)
        while (!interrupted && read != -1) {
            output.write(buffer, 0, read)
            downloadedBytes += read

            downloadSpeedStopTime = System.nanoTime()
            val downloadSpeedCheckTimeElapsed = hasIntervalTimeElapsed(downloadSpeedStartTime,
                    downloadSpeedStopTime, DEFAULT_DOWNLOAD_SPEED_REPORTING_INTERVAL_IN_MILLISECONDS)

            if (downloadSpeedCheckTimeElapsed) {
                downloadedBytesPerSecond = downloadedBytes - downloadedBytesPerSecond
                movingAverageCalculator.add(downloadedBytesPerSecond.toDouble())
                averageDownloadedBytesPerSecond =
                        movingAverageCalculator.getMovingAverageWithWeightOnRecentValues()
                estimatedTimeRemainingInMilliseconds = calculateEstimatedTimeRemainingInMilliseconds(
                        downloadedBytes = downloadedBytes,
                        totalBytes = totalBytes,
                        downloadedBytesPerSecond = getAverageDownloadedBytesPerSecond())
                downloadedBytesPerSecond = downloadedBytes
            }

            reportingStopTime = System.nanoTime()
            val hasReportingTimeElapsed = hasIntervalTimeElapsed(reportingStartTime,
                    reportingStopTime, progressReportingIntervalMillis)

            if (hasReportingTimeElapsed) {
                downloadInfo.downloaded = downloadedBytes
                downloadInfo.total = totalBytes
                delegate?.onProgress(
                        download = downloadInfo,
                        etaInMilliSeconds = estimatedTimeRemainingInMilliseconds,
                        downloadedBytesPerSecond = getAverageDownloadedBytesPerSecond())
                reportingStartTime = System.nanoTime()
            }

            if (downloadSpeedCheckTimeElapsed) {
                downloadSpeedStartTime = System.nanoTime()
            }
            read = input.read(buffer, 0, downloadBufferSizeBytes)
        }
        if (read == -1 && !interrupted) {
            totalBytes = downloadedBytes
            completedDownload = true
            downloadInfo.downloaded = downloadedBytes
            downloadInfo.total = totalBytes
            delegate?.onComplete(
                    download = downloadInfo)
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
        headers["Range"] = "bytes=$downloadedBytes-"
        return Downloader.Request(initialDownload.url, headers)
    }

    private fun getAverageDownloadedBytesPerSecond(): Long {
        if (averageDownloadedBytesPerSecond < 1) {
            return 0L
        }
        return ceil(averageDownloadedBytesPerSecond).toLong()
    }

    private fun networkConnectionWasLost(): Boolean {
        return !networkInfoProvider.isConnected && isResponseSuccessful
    }

}