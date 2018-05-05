package com.tonyodev.fetch2.downloader

import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Downloader
import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.Logger
import com.tonyodev.fetch2.exception.FetchException
import com.tonyodev.fetch2.getErrorFromThrowable
import com.tonyodev.fetch2.provider.NetworkInfoProvider
import com.tonyodev.fetch2.util.*
import java.io.*
import java.net.HttpURLConnection
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.ceil

class InlineParallelFileDownloaderImpl(private val initialDownload: Download,
                                       private val downloader: Downloader,
                                       private val progressReportingIntervalMillis: Long,
                                       private val downloadBufferSizeBytes: Int,
                                       private val logger: Logger,
                                       private val networkInfoProvider: NetworkInfoProvider,
                                       private val retryOnNetworkGain: Boolean,
                                       private val fileChunkTempDir: String) : FileDownloader {

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

    private var throwable: Throwable? = null

    private var fileChunks = emptyList<FileChuck>()

    private var outputStream: OutputStream? = null

    private var randomAccessFileOutput: RandomAccessFile? = null

    override fun run() {
        var openingResponse: Downloader.Response? = null
        try {
            val openingRequest = getRequestForDownload(initialDownload)
            openingResponse = downloader.execute(openingRequest)
            if (!interrupted && !terminated && openingResponse?.isSuccessful == true) {
                total = openingResponse.contentLength
                if (total > 0) {
                    fileChunks = getFileChunkList(openingResponse.code, openingRequest)
                    try {
                        downloader.disconnect(openingResponse)
                    } catch (e: Exception) {
                        logger.e("FileDownloader", e)
                    }
                    val chunkDownloadsList = fileChunks.filter { !it.isDownloaded }
                    if (!interrupted && !terminated) {
                        downloadInfo.downloaded = downloaded
                        downloadInfo.total = total
                        delegate?.onStarted(
                                download = downloadInfo,
                                etaInMilliseconds = estimatedTimeRemainingInMilliseconds,
                                downloadedBytesPerSecond = getAverageDownloadedBytesPerSecond())
                        if (chunkDownloadsList.isNotEmpty()) {
                            executorService = Executors.newFixedThreadPool(chunkDownloadsList.size)
                        }
                        downloadChunks(openingRequest, chunkDownloadsList)
                        waitAndPerformProgressReporting()
                        downloadInfo.downloaded = downloaded
                        downloadInfo.total = total
                        if (!interrupted && !terminated) {
                            throwExceptionIfFound()
                            completedDownload = true
                            delegate?.onProgress(
                                    download = downloadInfo,
                                    etaInMilliSeconds = estimatedTimeRemainingInMilliseconds,
                                    downloadedBytesPerSecond = getAverageDownloadedBytesPerSecond())
                            delegate?.onComplete(
                                    download = downloadInfo)
                        }
                        delegate?.saveDownloadProgress(downloadInfo)
                        if (!completedDownload && !terminated) {
                            delegate?.onProgress(
                                    download = downloadInfo,
                                    etaInMilliSeconds = estimatedTimeRemainingInMilliseconds,
                                    downloadedBytesPerSecond = getAverageDownloadedBytesPerSecond())
                        }
                    }
                } else {
                    throw FetchException(EMPTY_RESPONSE_BODY,
                            FetchException.Code.EMPTY_RESPONSE_BODY)
                }
            } else if (openingResponse == null && !interrupted && !terminated) {
                throw FetchException(EMPTY_RESPONSE_BODY,
                        FetchException.Code.EMPTY_RESPONSE_BODY)
            } else if (openingResponse?.isSuccessful == false && !interrupted && !terminated) {
                throw FetchException(RESPONSE_NOT_SUCCESSFUL,
                        FetchException.Code.REQUEST_NOT_SUCCESSFUL)
            } else if (!interrupted && !terminated) {
                throw FetchException(UNKNOWN_ERROR,
                        FetchException.Code.UNKNOWN)
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
                randomAccessFileOutput?.close()
            } catch (e: Exception) {
                logger.e("FileDownloader", e)
            }
            try {
                outputStream?.close()
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
            if (completedDownload && !terminated && !interrupted) {
                deleteAllTempFiles()
            }
            terminated = true
        }
    }

    private fun getFileChunkList(openingResponseCode: Int, request: Downloader.Request): List<FileChuck> {
        val file = getFile(downloadInfo.file)
        if (!file.exists()) {
            deleteAllTempFiles()
        }
        val previousChunkSize = getPreviousChunkCount(downloadInfo.id)
        return if (openingResponseCode == HttpURLConnection.HTTP_PARTIAL) {
            val fileChunkInfo = getChuckInfo(request)
            if (previousChunkSize != fileChunkInfo.chunkCount) {
                deleteAllTempFiles()
            }
            saveCurrentChunkCount(downloadInfo.id, fileChunkInfo.chunkCount)
            var counterBytes = 0L
            val fileChunks = mutableListOf<FileChuck>()
            for (position in 1..fileChunkInfo.chunkCount) {
                if (!interrupted && !terminated) {
                    val startBytes = counterBytes
                    val endBytes = if (fileChunkInfo.chunkCount == position) {
                        total
                    } else {
                        counterBytes + fileChunkInfo.bytesPerFileChunk
                    }
                    counterBytes = endBytes
                    val fileChunk = FileChuck(
                            id = downloadInfo.id,
                            position = position,
                            startBytes = startBytes,
                            endBytes = endBytes,
                            downloaded = getSavedDownloadedInfo(downloadInfo.id, position)
                    )
                    downloaded += fileChunk.downloaded
                    fileChunks.add(fileChunk)
                } else {
                    break
                }
            }
            fileChunks
        } else {
            if (previousChunkSize != 1) {
                deleteAllTempFiles()
            }
            saveCurrentChunkCount(downloadInfo.id, 1)
            val fileChunk = FileChuck(
                    id = downloadInfo.id,
                    position = 1,
                    startBytes = 0,
                    endBytes = total,
                    downloaded = getSavedDownloadedInfo(downloadInfo.id, 1))
            downloaded += fileChunk.downloaded
            listOf(fileChunk)
        }
    }

    private fun getPreviousChunkCount(id: Int): Int {
        var chunkCount = -1
        try {
            if (!terminated && !interrupted) {
                chunkCount = getTextFromFile(getMetaFilePath(id))?.toInt() ?: -1
            }
        } catch (e: Exception) {
        }
        return chunkCount
    }

    private fun saveCurrentChunkCount(id: Int, chunkCount: Int) {
        try {
            if (!terminated && !interrupted) {
                writeTextToFile(getMetaFilePath(id), chunkCount.toString())
            }
        } catch (e: Exception) {
        }
    }

    private fun getMetaFilePath(id: Int): String {
        return "$fileChunkTempDir/$id.meta.txt"
    }

    private fun getChuckInfo(request: Downloader.Request): FileChunkInfo {
        val fileChunkSize = downloader.getFileChunkSize(request, total)
                ?: DEFAULT_FILE_CHUNK_NO_LIMIT_SET
        return if (fileChunkSize == DEFAULT_FILE_CHUNK_NO_LIMIT_SET) {
            val fileSizeInMb = total.toFloat() / 1024F * 1024F
            val fileSizeInGb = total.toFloat() / 1024F * 1024F * 1024F
            when {
                fileSizeInGb >= 1F -> {
                    val chunks = 4
                    val bytesPerChunk = ceil((total.toFloat() / chunks.toFloat())).toLong()
                    FileChunkInfo(chunks, bytesPerChunk)
                }
                fileSizeInMb >= 1F -> {
                    val chunks = 2
                    val bytesPerChunk = ceil((total.toFloat() / chunks.toFloat())).toLong()
                    FileChunkInfo(chunks, bytesPerChunk)
                }
                else -> FileChunkInfo(1, total)
            }
        } else {
            val bytesPerChunk = ceil((total.toFloat() / fileChunkSize.toFloat())).toLong()
            return FileChunkInfo(fileChunkSize, bytesPerChunk)
        }
    }

    private fun getDownloadedInfoFilePath(id: Int, position: Int): String {
        return "$fileChunkTempDir/$id.$position.txt"
    }

    private fun deleteTempFile(id: Int, position: Int) {
        try {
            val textFile = getFile(getDownloadedInfoFilePath(id, position))
            if (textFile.exists()) {
                textFile.delete()
            }
        } catch (e: Exception) {
        }
    }

    private fun deleteMetaFile(id: Int) {
        try {
            val textFile = getFile(getMetaFilePath(id))
            if (textFile.exists()) {
                textFile.delete()
            }
        } catch (e: Exception) {
        }
    }

    private fun getSavedDownloadedInfo(id: Int, position: Int): Long {
        var downloaded = 0L
        try {
            if (!terminated && !interrupted) {
                downloaded = getTextFromFile(getDownloadedInfoFilePath(id, position))?.toLong() ?: 0L
            }
        } catch (e: Exception) {
        }
        return downloaded
    }

    private fun saveDownloadedInfo(id: Int, position: Int, downloaded: Long) {
        try {
            writeTextToFile(getDownloadedInfoFilePath(id, position), downloaded.toString())
        } catch (e: Exception) {

        }
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

    private fun downloadChunks(request: Downloader.Request, chunksDownloadsList: List<FileChuck>) {
        actionsCounter = 0
        actionsTotal = chunksDownloadsList.size
        outputStream = downloader.getRequestOutputStream(request, 0)
        if (outputStream == null) {
            randomAccessFileOutput = RandomAccessFile(downloadInfo.file, "rw")
            randomAccessFileOutput?.seek(0)
        }
        for (downloadChunk in chunksDownloadsList) {
            if (!interrupted && !terminated) {
                executorService?.execute({
                    val downloadRequest = getRequestForDownload(downloadInfo, downloadChunk.startBytes + downloadChunk.downloaded)
                    var downloadResponse: Downloader.Response? = null
                    try {
                        downloadResponse = downloader.execute(downloadRequest)
                        if (!terminated && !interrupted && downloadResponse?.isSuccessful == true) {
                            var reportingStopTime: Long
                            val buffer = ByteArray(downloadBufferSizeBytes)
                            var read: Int = downloadResponse.byteStream?.read(buffer, 0, downloadBufferSizeBytes)
                                    ?: -1
                            var remainderBytes: Long = downloadChunk.endBytes - (downloadChunk.startBytes + downloadChunk.downloaded)
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
                                seekPosition = downloadChunk.startBytes + downloadChunk.downloaded
                                synchronized(lock) {
                                    if (!interrupted && !terminated) {
                                        val outputStream = outputStream
                                        if (outputStream != null) {
                                            downloader.seekOutputStreamToPosition(request, outputStream, seekPosition)
                                            outputStream.write(buffer, 0, streamBytes)
                                        } else {
                                            randomAccessFileOutput?.seek(seekPosition)
                                            randomAccessFileOutput?.write(buffer, 0, streamBytes)
                                        }
                                        downloadChunk.downloaded += streamBytes
                                        downloaded += streamBytes
                                    }
                                }
                                if (!interrupted && !terminated) {
                                    reportingStopTime = System.nanoTime()
                                    val hasReportingTimeElapsed = hasIntervalTimeElapsed(reportingStartTime,
                                            reportingStopTime, DEFAULT_DOWNLOAD_SPEED_REPORTING_INTERVAL_IN_MILLISECONDS)
                                    if (hasReportingTimeElapsed) {
                                        saveDownloadedInfo(downloadChunk.id, downloadChunk.position, downloadChunk.downloaded)
                                        reportingStartTime = System.nanoTime()
                                    }
                                    if (read != -1) {
                                        read = downloadResponse.byteStream?.read(buffer, 0, downloadBufferSizeBytes) ?: -1
                                        remainderBytes = downloadChunk.endBytes - (downloadChunk.startBytes + downloadChunk.downloaded)
                                    }
                                }
                            }
                        } else if (downloadResponse == null && !interrupted && !terminated) {
                            throw FetchException(EMPTY_RESPONSE_BODY,
                                    FetchException.Code.EMPTY_RESPONSE_BODY)
                        } else if (downloadResponse?.isSuccessful == false && !interrupted && !terminated) {
                            throw FetchException(RESPONSE_NOT_SUCCESSFUL,
                                    FetchException.Code.REQUEST_NOT_SUCCESSFUL)
                        } else if (!interrupted && !terminated) {
                            throw FetchException(UNKNOWN_ERROR,
                                    FetchException.Code.UNKNOWN)
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
                })
            }
        }
    }

    private fun deleteAllTempFiles() {
        try {
            for (fileChunk in fileChunks) {
                if (!interrupted && !terminated) {
                    deleteTempFile(fileChunk.id, fileChunk.position)
                } else {
                    break
                }
            }
            if (!interrupted && !terminated) {
                deleteMetaFile(downloadInfo.id)
            }
        } catch (e: Exception) {
        }
    }

    private fun incrementActionCompletedCount() {
        synchronized(lock) {
            actionsCounter += 1
        }
    }

    private fun throwExceptionIfFound() {
        val exception = throwable
        if (exception != null) {
            throw exception
        }
    }

    data class FileChunkInfo(val chunkCount: Int, val bytesPerFileChunk: Long)

    data class FileChuck(val id: Int = 0,
                         val position: Int = 0,
                         val startBytes: Long = 0L,
                         val endBytes: Long = 0L,
                         var downloaded: Long = 0L) {

        val isDownloaded: Boolean
            get() {
                return startBytes + downloaded == endBytes
            }
    }

}