package com.tonyodev.fetch2.downloader

import com.tonyodev.fetch2.*
import com.tonyodev.fetch2.exception.FetchException
import com.tonyodev.fetch2.helper.FileDownloaderDelegate
import com.tonyodev.fetch2.provider.NetworkInfoProvider
import com.tonyodev.fetch2.util.*
import com.tonyodev.fetch2core.*
import java.io.*
import java.net.HttpURLConnection
import kotlin.math.ceil

class SequentialFileDownloaderImpl(private val initialDownload: Download,
                                   private val downloader: Downloader<*, *>,
                                   private val progressReportingIntervalMillis: Long,
                                   private val logger: Logger,
                                   private val networkInfoProvider: NetworkInfoProvider,
                                   private val retryOnNetworkGain: Boolean,
                                   private val hashCheckingEnabled: Boolean,
                                   private val storageResolver: StorageResolver,
                                   private val preAllocateFileOnCreation: Boolean) : FileDownloader {

    @Volatile
    override var interrupted = false
        set(value) {
            (delegate as? FileDownloaderDelegate)?.interrupted = value
            field = value
        }
    @Volatile
    override var terminated = false
        set(value) {
            (delegate as? FileDownloaderDelegate)?.interrupted = value
            field = value
        }

    override val completedDownload: Boolean
        get() {
            return isDownloadComplete()
        }

    override var delegate: FileDownloader.Delegate? = null
    @Volatile
    private var total: Long = -1L
    @Volatile
    private var totalUnknown = false
    @Volatile
    private var downloaded: Long = 0
    private var estimatedTimeRemainingInMilliseconds: Long = -1
    private val downloadInfo by lazy { initialDownload.toDownloadInfo(delegate!!.getNewDownloadInfoInstance()) }
    private var averageDownloadedBytesPerSecond = 0.0
    private val movingAverageCalculator = AverageCalculator(5)
    private val downloadBlock = {
        val downloadBlock = DownloadBlockInfo()
        downloadBlock.blockPosition = 1
        downloadBlock.downloadId = initialDownload.id
        downloadBlock
    }()
    private val totalDownloadBlocks = 1

    override val download: Download
        get () {
            downloadInfo.downloaded = downloaded
            downloadInfo.total = total
            return downloadInfo
        }

    override fun run() {
        var outputResourceWrapper: OutputResourceWrapper? = null
        var input: BufferedInputStream? = null
        var response: Downloader.Response? = null
        try {
            downloaded = initialDownload.downloaded
            total = initialDownload.total
            downloadInfo.downloaded = downloaded
            downloadInfo.total = total
            if (!interrupted && !terminated) {
                val request = getRequest()
                response = downloader.execute(request, interruptMonitor)
                if (response != null) {
                    setIsTotalUnknown(response)
                }
                val isResponseSuccessful = response?.isSuccessful ?: false
                if (!interrupted && !terminated && response != null && isResponseSuccessful) {
                    downloaded = if (response.code == HttpURLConnection.HTTP_PARTIAL || response.acceptsRanges) {
                        initialDownload.downloaded
                    } else {
                        0
                    }
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
                    downloadInfo.downloaded = downloaded
                    downloadInfo.total = total
                    if (!storageResolver.fileExists(request.file)) {
                        storageResolver.createFile(request.file, initialDownload.enqueueAction == EnqueueAction.INCREMENT_FILE_NAME)
                    }
                    if (preAllocateFileOnCreation) {
                        storageResolver.preAllocateFile(request.file, downloadInfo.total)
                    }
                    outputResourceWrapper = storageResolver.getRequestOutputResourceWrapper(request)
                    outputResourceWrapper.setWriteOffset(seekPosition)
                    if (!interrupted && !terminated) {
                        val bufferSize = downloader.getRequestBufferSize(request)
                        input = BufferedInputStream(response.byteStream, bufferSize)
                        downloadInfo.downloaded = downloaded
                        downloadInfo.total = total
                        downloadBlock.downloadedBytes = downloaded
                        downloadBlock.startByte = seekPosition
                        downloadBlock.endByte = total
                        if (!terminated && !interrupted) {
                            downloadInfo.etaInMilliSeconds = -1
                            downloadInfo.downloadedBytesPerSecond = -1
                            delegate?.onStarted(
                                    download = downloadInfo,
                                    downloadBlocks = listOf(downloadBlock),
                                    totalBlocks = totalDownloadBlocks)
                            delegate?.onDownloadBlockUpdated(downloadInfo, downloadBlock, totalDownloadBlocks)
                        }
                        writeToOutput(input, outputResourceWrapper, bufferSize)
                    }
                } else if (response == null && !interrupted && !terminated && !isDownloadComplete()) {
                    throw FetchException(EMPTY_RESPONSE_BODY)
                } else if (!isResponseSuccessful && !interrupted && !isDownloadComplete()) {
                    throw FetchException(RESPONSE_NOT_SUCCESSFUL)
                } else if (!interrupted && !terminated && downloaded < total && !isDownloadComplete()) {
                    throw FetchException(UNKNOWN_ERROR)
                }
            }
            if (!isDownloadComplete() && !terminated && !interrupted) {
                downloadInfo.downloaded = downloaded
                downloadInfo.total = total
                downloadBlock.downloadedBytes = downloaded
                downloadBlock.endByte = total
                if (!terminated && !interrupted) {
                    delegate?.saveDownloadProgress(downloadInfo)
                    delegate?.onDownloadBlockUpdated(downloadInfo, downloadBlock, totalDownloadBlocks)
                    downloadInfo.etaInMilliSeconds = estimatedTimeRemainingInMilliseconds
                    downloadInfo.downloadedBytesPerSecond = getAverageDownloadedBytesPerSecond()
                    delegate?.onProgress(
                            download = downloadInfo,
                            etaInMilliSeconds = downloadInfo.etaInMilliSeconds,
                            downloadedBytesPerSecond = downloadInfo.downloadedBytesPerSecond)
                }
            } else if (isDownloadComplete() && response != null) {
                verifyDownloadCompletion(response)
            }
        } catch (e: Exception) {
            if (!interrupted && !terminated) {
                logger.e("FileDownloader download:$download", e)
                var error = getErrorFromThrowable(e)
                error.throwable = e
                if (response != null) {
                    error.httpResponse = copyDownloadResponseNoStream(response)
                }
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
                downloadBlock.downloadedBytes = downloaded
                downloadBlock.endByte = total
                if (!terminated && !interrupted) {
                    downloadInfo.etaInMilliSeconds = -1
                    downloadInfo.downloadedBytesPerSecond = -1
                    delegate?.onError(download = downloadInfo, error = error, throwable = e)
                }
            }
        } finally {
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
                outputResourceWrapper?.close()
            } catch (e: Exception) {
                logger.e("FileDownloader", e)
            }
            terminated = true
        }
    }

    private fun isDownloadComplete(): Boolean {
        return ((downloaded > 0 && total > 0) || totalUnknown) && (downloaded >= total)
    }

    private fun setIsTotalUnknown(response: Downloader.Response) {
        if (response.isSuccessful && response.contentLength == -1L) {
            totalUnknown = true
        }
    }

    private fun writeToOutput(input: BufferedInputStream,
                              outputResourceWrapper: OutputResourceWrapper?,
                              bufferSize: Int) {
        var reportingStopTime: Long
        var downloadSpeedStopTime: Long
        var downloadedBytesPerSecond = downloaded
        val buffer = ByteArray(bufferSize)
        var reportingStartTime = System.nanoTime()
        var downloadSpeedStartTime = System.nanoTime()
        var read = input.read(buffer, 0, bufferSize)
        while (!interrupted && !terminated && read != -1) {
            outputResourceWrapper?.write(buffer, 0, read)
            if (!terminated && !interrupted) {
                downloaded += read
                downloadInfo.downloaded = downloaded
                downloadInfo.total = total
                downloadBlock.downloadedBytes = downloaded
                downloadBlock.endByte = total
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
                    downloadBlock.downloadedBytes = downloaded
                    if (!terminated && !interrupted) {
                        delegate?.saveDownloadProgress(downloadInfo)
                        delegate?.onDownloadBlockUpdated(downloadInfo, downloadBlock, totalDownloadBlocks)
                        downloadInfo.etaInMilliSeconds = estimatedTimeRemainingInMilliseconds
                        downloadInfo.downloadedBytesPerSecond = getAverageDownloadedBytesPerSecond()
                        delegate?.onProgress(
                                download = downloadInfo,
                                etaInMilliSeconds = downloadInfo.etaInMilliSeconds,
                                downloadedBytesPerSecond = downloadInfo.downloadedBytesPerSecond)
                    }
                    reportingStartTime = System.nanoTime()
                }
                if (downloadSpeedCheckTimeElapsed) {
                    downloadSpeedStartTime = System.nanoTime()
                }
                read = input.read(buffer, 0, bufferSize)
            }
        }
        outputResourceWrapper?.flush()
    }

    private fun verifyDownloadCompletion(response: Downloader.Response) {
        if (!interrupted && !terminated && isDownloadComplete()) {
            total = downloaded
            downloadInfo.downloaded = downloaded
            downloadInfo.total = total
            downloadBlock.downloadedBytes = downloaded
            downloadBlock.endByte = total
            if (hashCheckingEnabled) {
                if (downloader.verifyContentHash(response.request, response.hash)) {
                    if (!terminated && !interrupted) {
                        delegate?.saveDownloadProgress(downloadInfo)
                        delegate?.onDownloadBlockUpdated(downloadInfo, downloadBlock, totalDownloadBlocks)
                        downloadInfo.etaInMilliSeconds = estimatedTimeRemainingInMilliseconds
                        downloadInfo.downloadedBytesPerSecond = getAverageDownloadedBytesPerSecond()
                        delegate?.onProgress(
                                download = downloadInfo,
                                etaInMilliSeconds = downloadInfo.etaInMilliSeconds,
                                downloadedBytesPerSecond = downloadInfo.downloadedBytesPerSecond)
                        downloadInfo.etaInMilliSeconds = -1
                        downloadInfo.downloadedBytesPerSecond = -1
                        delegate?.onComplete(
                                download = downloadInfo)
                    }
                } else {
                    throw FetchException(INVALID_CONTENT_HASH)
                }
            } else {
                if (!terminated && !interrupted) {
                    delegate?.saveDownloadProgress(downloadInfo)
                    delegate?.onDownloadBlockUpdated(downloadInfo, downloadBlock, totalDownloadBlocks)
                    downloadInfo.etaInMilliSeconds = estimatedTimeRemainingInMilliseconds
                    downloadInfo.downloadedBytesPerSecond = getAverageDownloadedBytesPerSecond()
                    delegate?.onProgress(
                            download = downloadInfo,
                            etaInMilliSeconds = downloadInfo.etaInMilliSeconds,
                            downloadedBytesPerSecond = downloadInfo.downloadedBytesPerSecond)
                    downloadInfo.etaInMilliSeconds = -1
                    downloadInfo.downloadedBytesPerSecond = -1
                    delegate?.onComplete(
                            download = downloadInfo)
                }
            }
        }
    }

    private fun getRequest(): Downloader.ServerRequest {
        val headers = initialDownload.headers.toMutableMap()
        headers["Range"] = "bytes=$downloaded-"
        return Downloader.ServerRequest(
                id = initialDownload.id,
                url = initialDownload.url,
                headers = headers,
                file = initialDownload.file,
                fileUri = getFileUri(initialDownload.file),
                tag = initialDownload.tag,
                identifier = initialDownload.identifier,
                requestMethod = GET_REQUEST_METHOD,
                extras = initialDownload.extras,
                redirected = false,
                redirectUrl = "",
                segment = 1)
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