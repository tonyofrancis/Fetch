package com.tonyodev.fetch2.downloader

import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2core.Downloader
import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2core.Logger
import com.tonyodev.fetch2.exception.FetchException
import com.tonyodev.fetch2.getErrorFromThrowable
import com.tonyodev.fetch2.provider.NetworkInfoProvider
import com.tonyodev.fetch2.util.*
import com.tonyodev.fetch2core.*
import java.io.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.ceil

class ParallelFileDownloaderImpl(private val initialDownload: Download,
                                 private val downloader: Downloader,
                                 private val progressReportingIntervalMillis: Long,
                                 private val downloadBufferSizeBytes: Int,
                                 private val logger: Logger,
                                 private val networkInfoProvider: NetworkInfoProvider,
                                 private val retryOnNetworkGain: Boolean,
                                 private val fileTempDir: String,
                                 private val md5CheckingEnabled: Boolean) : FileDownloader {

    @Volatile
    override var interrupted = false

    @Volatile
    override var terminated = false

    @Volatile
    override var completedDownload = false

    override var delegate: FileDownloader.Delegate? = null

    private var downloadInfo = initialDownload.toDownloadInfo()

    override val download: Download
        get () {
            downloadInfo.downloaded = downloaded
            downloadInfo.total = total
            return downloadInfo
        }

    @Volatile
    private var downloaded = 0L

    private var total = -1L

    private var averageDownloadedBytesPerSecond = 0.0

    private val movingAverageCalculator = AverageCalculator(5)

    private var estimatedTimeRemainingInMilliseconds: Long = -1

    private var executorService: ExecutorService? = null

    @Volatile
    private var actionsCounter = 0

    private var actionsTotal = 0

    private val lock = Object()

    @Volatile
    private var throwable: Throwable? = null

    private var fileSlices = emptyList<FileSlice>()

    private var outputResourceWrapper: OutputResourceWrapper? = null

    private var totalDownloadBlocks = 0

    override fun run() {
        var openingResponse: Downloader.Response? = null
        try {
            val openingRequest = if (downloader.getHeadRequestMethodSupported(getRequestForDownload(initialDownload))) {
                getRequestForDownload(initialDownload, HEAD_REQUEST_METHOD)
            } else {
                getRequestForDownload(initialDownload)
            }
            openingResponse = downloader.execute(openingRequest, interruptMonitor)
            if (!interrupted && !terminated && openingResponse?.isSuccessful == true) {
                total = openingResponse.contentLength
                if (total > 0) {
                    fileSlices = getFileSliceList(openingResponse.acceptsRanges, openingRequest)
                    totalDownloadBlocks = fileSlices.size
                    try {
                        downloader.disconnect(openingResponse)
                    } catch (e: Exception) {
                        logger.e("FileDownloader", e)
                    }
                    val sliceFileDownloadsList = fileSlices.filter { !it.isDownloaded }
                    if (!interrupted && !terminated) {
                        downloadInfo.downloaded = downloaded
                        downloadInfo.total = total
                        delegate?.onStarted(
                                download = downloadInfo,
                                etaInMilliseconds = estimatedTimeRemainingInMilliseconds,
                                downloadedBytesPerSecond = getAverageDownloadedBytesPerSecond())
                        fileSlices.forEach {
                            val downloadBlock = DownloadBlockInfo()
                            downloadBlock.downloadId = it.id
                            downloadBlock.blockPosition = it.position
                            downloadBlock.downloadedBytes = it.downloaded
                            downloadBlock.startByte = it.startBytes
                            downloadBlock.endByte = it.endBytes
                            delegate?.onDownloadBlockUpdated(downloadInfo, downloadBlock, totalDownloadBlocks)
                        }
                        if (sliceFileDownloadsList.isNotEmpty()) {
                            executorService = Executors.newFixedThreadPool(sliceFileDownloadsList.size)
                        }
                        downloadSliceFiles(openingRequest, sliceFileDownloadsList)
                        waitAndPerformProgressReporting()
                        downloadInfo.downloaded = downloaded
                        downloadInfo.total = total
                        if (!interrupted && !terminated) {
                            var fileSlicesTotal = 0L
                            fileSlices.forEach {
                                fileSlicesTotal += it.downloaded
                            }
                            if (fileSlicesTotal != total) {
                                throwable = FetchException(DOWNLOAD_INCOMPLETE)
                            }
                            throwExceptionIfFound()
                            completedDownload = true
                            if (md5CheckingEnabled) {
                                if (downloader.verifyContentMD5(openingResponse.request, openingResponse.md5)) {
                                    delegate?.onProgress(
                                            download = downloadInfo,
                                            etaInMilliSeconds = estimatedTimeRemainingInMilliseconds,
                                            downloadedBytesPerSecond = getAverageDownloadedBytesPerSecond())
                                    delegate?.onComplete(
                                            download = downloadInfo)
                                } else {
                                    throw FetchException(INVALID_CONTENT_MD5)
                                }
                            } else {
                                delegate?.onProgress(
                                        download = downloadInfo,
                                        etaInMilliSeconds = estimatedTimeRemainingInMilliseconds,
                                        downloadedBytesPerSecond = getAverageDownloadedBytesPerSecond())
                                delegate?.onComplete(
                                        download = downloadInfo)
                            }
                        }
                        if (!completedDownload) {
                            downloadInfo.downloaded = downloaded
                            downloadInfo.total = total
                            if (!terminated) {
                                delegate?.saveDownloadProgress(downloadInfo)
                                delegate?.onProgress(
                                        download = downloadInfo,
                                        etaInMilliSeconds = estimatedTimeRemainingInMilliseconds,
                                        downloadedBytesPerSecond = getAverageDownloadedBytesPerSecond())
                            }
                        }
                    }
                } else {
                    throw FetchException(EMPTY_RESPONSE_BODY)
                }
            } else if (openingResponse == null && !interrupted && !terminated) {
                throw FetchException(EMPTY_RESPONSE_BODY)
            } else if (openingResponse?.isSuccessful == false && !interrupted && !terminated) {
                throw FetchException(RESPONSE_NOT_SUCCESSFUL)
            } else if (!interrupted && !terminated) {
                throw FetchException(UNKNOWN_ERROR)
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
                executorService?.shutdown()
            } catch (e: Exception) {
                logger.e("FileDownloader", e)
            }
            try {
                outputResourceWrapper?.close()
            } catch (e: Exception) {
                logger.e("FileDownloader", e)
            }
            if (openingResponse != null) {
                try {
                    downloader.disconnect(openingResponse)
                } catch (e: Exception) {
                    logger.e("FileDownloader", e)
                }
            }
            terminated = true
        }
    }

    private fun getFileSliceList(acceptsRanges: Boolean, request: Downloader.ServerRequest): List<FileSlice> {
        val file = getFile(downloadInfo.file)
        if (!file.exists()) {
            deleteAllTempFiles()
        }
        val previousSliceSize = getPreviousSliceCount(downloadInfo.id, fileTempDir)
        return if (acceptsRanges) {
            val fileSliceInfo = getChuckInfo(request)
            if (previousSliceSize != fileSliceInfo.slicingCount) {
                deleteAllTempFiles()
            }
            saveCurrentSliceCount(downloadInfo.id, fileSliceInfo.slicingCount, fileTempDir)
            var counterBytes = 0L
            val fileSlices = mutableListOf<FileSlice>()
            for (position in 1..fileSliceInfo.slicingCount) {
                if (!interrupted && !terminated) {
                    val startBytes = counterBytes
                    val endBytes = if (fileSliceInfo.slicingCount == position) {
                        total
                    } else {
                        counterBytes + fileSliceInfo.bytesPerFileSlice
                    }
                    counterBytes = endBytes
                    val fileSlice = FileSlice(
                            id = downloadInfo.id,
                            position = position,
                            startBytes = startBytes,
                            endBytes = endBytes,
                            downloaded = getSavedDownloadedInfo(downloadInfo.id, position, fileTempDir)
                    )
                    downloaded += fileSlice.downloaded
                    fileSlices.add(fileSlice)
                } else {
                    break
                }
            }
            fileSlices
        } else {
            if (previousSliceSize != 1) {
                deleteAllTempFiles()
            }
            saveCurrentSliceCount(downloadInfo.id, 1, fileTempDir)
            val fileSlice = FileSlice(
                    id = downloadInfo.id,
                    position = 1,
                    startBytes = 0,
                    endBytes = total,
                    downloaded = getSavedDownloadedInfo(downloadInfo.id, 1, fileTempDir))
            downloaded += fileSlice.downloaded
            listOf(fileSlice)
        }
    }

    private fun getChuckInfo(request: Downloader.ServerRequest): FileSliceInfo {
        val fileSliceSize = downloader.getFileSlicingCount(request, total)
                ?: DEFAULT_FILE_SLICE_NO_LIMIT_SET
        return getFileSliceInfo(fileSliceSize, total)
    }

    private fun getAverageDownloadedBytesPerSecond(): Long {
        if (averageDownloadedBytesPerSecond < 1) {
            return 0L
        }
        return ceil(averageDownloadedBytesPerSecond).toLong()
    }

    private fun waitAndPerformProgressReporting() {
        var reportingStopTime: Long
        var downloadSpeedStopTime: Long
        var downloadedBytesPerSecond = downloaded
        var reportingStartTime = System.nanoTime()
        var downloadSpeedStartTime = System.nanoTime()
        while (actionsCounter != actionsTotal && !interrupted && !terminated) {
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
        }
    }

    private fun downloadSliceFiles(request: Downloader.ServerRequest, fileSlicesDownloadsList: List<FileSlice>) {
        actionsCounter = 0
        actionsTotal = fileSlicesDownloadsList.size
        outputResourceWrapper = downloader.getRequestOutputResourceWrapper(request)
        if (outputResourceWrapper == null) {
            outputResourceWrapper = object : OutputResourceWrapper() {

                private val randomAccessFile = RandomAccessFile(downloadInfo.file, "rw")

                init {
                    randomAccessFile.seek(0)
                }

                override fun write(byteArray: ByteArray, offSet: Int, length: Int) {
                    randomAccessFile.write(byteArray, offSet, length)
                }

                override fun setWriteOffset(offset: Long) {
                    randomAccessFile.seek(offset)
                }

                override fun flush() {

                }

                override fun close() {
                    randomAccessFile.close()
                }
            }
        }
        for (fileSlice in fileSlicesDownloadsList) {
            if (!interrupted && !terminated) {
                executorService?.execute {
                    val downloadBlock = DownloadBlockInfo()
                    downloadBlock.downloadId = fileSlice.id
                    downloadBlock.blockPosition = fileSlice.position
                    downloadBlock.downloadedBytes = fileSlice.downloaded
                    downloadBlock.startByte = fileSlice.startBytes
                    downloadBlock.endByte = fileSlice.endBytes
                    val downloadRequest = getRequestForDownload(downloadInfo, fileSlice.startBytes + fileSlice.downloaded)
                    var downloadResponse: Downloader.Response? = null
                    try {
                        downloadResponse = downloader.execute(downloadRequest, interruptMonitor)
                        if (!terminated && !interrupted && downloadResponse?.isSuccessful == true) {
                            var reportingStopTime: Long
                            val buffer = ByteArray(downloadBufferSizeBytes)
                            var read: Int = downloadResponse.byteStream?.read(buffer, 0, downloadBufferSizeBytes)
                                    ?: -1
                            var remainderBytes: Long = fileSlice.endBytes - (fileSlice.startBytes + fileSlice.downloaded)
                            var reportingStartTime = System.nanoTime()
                            var streamBytes: Int
                            var seekPosition: Long
                            while (remainderBytes > 0L && read != -1 && !interrupted && !terminated) {
                                streamBytes = if (read <= remainderBytes) {
                                    read
                                } else {
                                    read = -1
                                    remainderBytes.toInt()
                                }
                                seekPosition = fileSlice.startBytes + fileSlice.downloaded
                                synchronized(lock) {
                                    if (!interrupted && !terminated) {
                                        outputResourceWrapper?.setWriteOffset(seekPosition)
                                        outputResourceWrapper?.write(buffer, 0, streamBytes)
                                        fileSlice.downloaded += streamBytes
                                        downloaded += streamBytes
                                    }
                                }
                                if (!interrupted && !terminated) {
                                    reportingStopTime = System.nanoTime()
                                    val hasReportingTimeElapsed = hasIntervalTimeElapsed(reportingStartTime,
                                            reportingStopTime, DEFAULT_DOWNLOAD_SPEED_REPORTING_INTERVAL_IN_MILLISECONDS)
                                    if (hasReportingTimeElapsed) {
                                        saveDownloadedInfo(fileSlice.id, fileSlice.position, fileSlice.downloaded, fileTempDir)
                                        downloadBlock.downloadedBytes = fileSlice.downloaded
                                        delegate?.onDownloadBlockUpdated(downloadInfo, downloadBlock, totalDownloadBlocks)
                                        reportingStartTime = System.nanoTime()
                                    }
                                    if (read != -1) {
                                        read = downloadResponse.byteStream?.read(buffer, 0, downloadBufferSizeBytes) ?: -1
                                        remainderBytes = fileSlice.endBytes - (fileSlice.startBytes + fileSlice.downloaded)
                                    }
                                }
                            }
                            saveDownloadedInfo(fileSlice.id, fileSlice.position, fileSlice.downloaded, fileTempDir)
                            downloadBlock.downloadedBytes = fileSlice.downloaded
                            delegate?.onDownloadBlockUpdated(downloadInfo, downloadBlock, totalDownloadBlocks)
                        } else if (downloadResponse == null && !interrupted && !terminated) {
                            throw FetchException(EMPTY_RESPONSE_BODY)
                        } else if (downloadResponse?.isSuccessful == false && !interrupted && !terminated) {
                            throw FetchException(RESPONSE_NOT_SUCCESSFUL)
                        } else if (!interrupted && !terminated) {
                            throw FetchException(UNKNOWN_ERROR)
                        }
                    } catch (e: Exception) {
                        throwable = e
                    } finally {
                        try {
                            if (downloadResponse != null) {
                                downloader.disconnect(downloadResponse)
                            }
                        } catch (e: Exception) {
                            logger.e("FileDownloader", e)
                        }
                        incrementActionCompletedCount()
                    }
                }
            }
        }
    }

    private fun deleteAllTempFiles() {
        try {
            for (fileSlice in fileSlices) {
                deleteTempFile(fileSlice.id, fileSlice.position, fileTempDir)
            }
            deleteMetaFile(downloadInfo.id, fileTempDir)
        } catch (e: Exception) {
        }
    }

    private fun incrementActionCompletedCount() {
        synchronized(lock) {
            actionsCounter += 1
        }
    }

    private val interruptMonitor = object : InterruptMonitor {
        override val isInterrupted: Boolean
            get() {
                return interrupted
            }
    }

    private fun throwExceptionIfFound() {
        val exception = throwable
        if (exception != null) {
            throw exception
        }
    }

}