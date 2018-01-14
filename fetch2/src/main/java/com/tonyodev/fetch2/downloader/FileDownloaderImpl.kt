package com.tonyodev.fetch2.downloader

import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Downloader
import com.tonyodev.fetch2.Logger
import com.tonyodev.fetch2.exception.FetchException
import com.tonyodev.fetch2.getErrorFromMessage
import com.tonyodev.fetch2.util.*
import java.io.BufferedInputStream
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import kotlin.math.ceil

open class FileDownloaderImpl(val initialDownload: Download,
                              val downloader: Downloader,
                              val progressReportingIntervalMillis: Long,
                              val downloadBufferSizeBytes: Int,
                              val logger: Logger) : FileDownloader {

    @Volatile
    override var interrupted = false
    @Volatile
    override var terminated = false
    @Volatile
    override var completedDownload = false
    override var delegate: FileDownloader.Delegate? = null
    var totalInternal: Long = 0
    var downloadedInternal: Long = 0
    var estimatedTimeRemainingInMillisecondsInternal: Long = -1
    var downloadInfoInternal = initialDownload.toDownloadInfo()
    var averageDownloadedBytesPerSecondInternal = 0.0
    val movingAverageCalculatorInternal = AverageCalculator(5)

    override val download: Download
        get () {
            downloadInfoInternal.downloaded = downloadedInternal
            downloadInfoInternal.total = totalInternal
            return downloadInfoInternal
        }

    override fun run() {
        var output: RandomAccessFile? = null
        var input: BufferedInputStream? = null
        var response: Downloader.Response? = null
        try {
            val file = getFileInternal()
            downloadedInternal = file.length()
            if (!interrupted) {
                response = downloader.execute(getRequestInternal())
                val isResponseSuccessful = response?.isSuccessful ?: false
                if (!interrupted && response != null && isResponseSuccessful) {
                    totalInternal = if (response.contentLength == (-1).toLong()) {
                        -1
                    } else {
                        downloadedInternal + response.contentLength
                    }
                    output = RandomAccessFile(file, "rw")
                    if (response.code == HttpURLConnection.HTTP_PARTIAL) {
                        output.seek(downloadedInternal)
                        logger.d("FileDownloader resuming Download $download")
                    } else {
                        output.seek(0)
                        logger.d("FileDownloader starting Download $download")
                    }
                    if (!interrupted) {
                        input = BufferedInputStream(response.byteStream, downloadBufferSizeBytes)
                        downloadInfoInternal.downloaded = downloadedInternal
                        downloadInfoInternal.total = totalInternal
                        delegate?.onStarted(
                                download = downloadInfoInternal,
                                etaInMilliseconds = estimatedTimeRemainingInMillisecondsInternal,
                                downloadedBytesPerSecond = getAverageDownloadedBytesPerSecond())
                        writeToOutputInternal(input, output)
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
                downloadInfoInternal.downloaded = downloadedInternal
                downloadInfoInternal.total = totalInternal
                delegate?.onProgress(
                        download = downloadInfoInternal,
                        etaInMilliSeconds = estimatedTimeRemainingInMillisecondsInternal,
                        downloadedBytesPerSecond = getAverageDownloadedBytesPerSecond())
            }
        } catch (e: Exception) {
            if (!interrupted) {
                logger.e("FileDownloader", e)
                val error = getErrorFromMessage(e.message)
                downloadInfoInternal.downloaded = downloadedInternal
                downloadInfoInternal.total = totalInternal
                downloadInfoInternal.error = error
                delegate?.onError(download = downloadInfoInternal)
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

    open fun writeToOutputInternal(input: BufferedInputStream, output: RandomAccessFile) {
        var reportingStopTime: Long
        var downloadSpeedStopTime: Long
        var downloadedBytesPerSecond = downloadedInternal
        val buffer = ByteArray(downloadBufferSizeBytes)
        var reportingStartTime = System.nanoTime()
        var downloadSpeedStartTime = System.nanoTime()

        var read = input.read(buffer, 0, downloadBufferSizeBytes)
        while (!interrupted && read != -1) {
            output.write(buffer, 0, read)
            downloadedInternal += read

            downloadSpeedStopTime = System.nanoTime()
            val downloadSpeedCheckTimeElapsed = hasIntervalTimeElapsed(downloadSpeedStartTime,
                    downloadSpeedStopTime, DEFAULT_DOWNLOAD_SPEED_REPORTING_INTERVAL_IN_MILLISECONDS)

            if (downloadSpeedCheckTimeElapsed) {
                downloadedBytesPerSecond = downloadedInternal - downloadedBytesPerSecond
                movingAverageCalculatorInternal.add(downloadedBytesPerSecond.toDouble())
                averageDownloadedBytesPerSecondInternal =
                        movingAverageCalculatorInternal.getMovingAverageWithWeightOnRecentValues()
                estimatedTimeRemainingInMillisecondsInternal = calculateEstimatedTimeRemainingInMilliseconds(
                        downloadedBytes = downloadedInternal,
                        totalBytes = totalInternal,
                        downloadedBytesPerSecond = getAverageDownloadedBytesPerSecond())
                downloadedBytesPerSecond = downloadedInternal
            }

            reportingStopTime = System.nanoTime()
            val hasReportingTimeElapsed = hasIntervalTimeElapsed(reportingStartTime,
                    reportingStopTime, progressReportingIntervalMillis)

            if (hasReportingTimeElapsed) {
                downloadInfoInternal.downloaded = downloadedInternal
                downloadInfoInternal.total = totalInternal
                delegate?.onProgress(
                        download = downloadInfoInternal,
                        etaInMilliSeconds = estimatedTimeRemainingInMillisecondsInternal,
                        downloadedBytesPerSecond = getAverageDownloadedBytesPerSecond())
                reportingStartTime = System.nanoTime()
            }

            if (downloadSpeedCheckTimeElapsed) {
                downloadSpeedStartTime = System.nanoTime()
            }
            read = input.read(buffer, 0, downloadBufferSizeBytes)
        }
        if (read == -1 && !interrupted) {
            totalInternal = downloadedInternal
            completedDownload = true
            downloadInfoInternal.downloaded = downloadedInternal
            downloadInfoInternal.total = totalInternal
            delegate?.onComplete(
                    download = downloadInfoInternal)
        }
    }

    open fun getFileInternal(): File {
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

    open fun getRequestInternal(): Downloader.Request {
        val headers = initialDownload.headers.toMutableMap()
        headers.put("Range", "bytes=$downloadedInternal-")
        return Downloader.Request(initialDownload.url, headers)
    }

    fun getAverageDownloadedBytesPerSecond(): Long {
        if (averageDownloadedBytesPerSecondInternal < 1) {
            return 0L
        }
        return ceil(averageDownloadedBytesPerSecondInternal).toLong()
    }

}