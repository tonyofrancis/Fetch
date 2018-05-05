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

class ParallelFileDownloaderImpl(private val initialDownload: Download,
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
            downloadInfo.downloaded = getProgressDownloaded()
            downloadInfo.total = total
            return downloadInfo
        }

    private var downloaded = 0L

    private val downloadedLock = Object()

    private var total = -1L

    private var averageDownloadedBytesPerSecond = 0.0

    private val movingAverageCalculator = AverageCalculator(5)

    private var estimatedTimeRemainingInMilliseconds: Long = -1

    private var executorService: ExecutorService? = null

    private var phase = Phase.IDLE

    private var actionsCounter = 0

    private val actionsLock = Object()

    private var actionsTotal = 0

    private val mergeLock = Object()

    private var mergeCounter = 0

    @Volatile
    private var mergeBytesWritten = 0L

    @Volatile
    private var tempFilesDeleted = 0L

    private var throwable: Throwable? = null

    private var fileChunks = emptyList<FileChuck>()

    override fun run() {
        var openingResponse: Downloader.Response? = null
        var output: OutputStream? = null
        var randomAccessFile: RandomAccessFile? = null
        try {
            val openingRequest = getRequestForDownload(initialDownload)
            openingResponse = downloader.execute(openingRequest)
            if (!interrupted && !terminated && openingResponse?.isSuccessful == true) {
                total = openingResponse.contentLength
                if (total > 0) {
                    fileChunks = getFileChunkList(openingResponse.code, openingRequest)
                    val chunkDownloadsList = fileChunks.filter { !it.isDownloaded }
                    if (!interrupted && !terminated) {
                        downloadInfo.downloaded = getProgressDownloaded()
                        downloadInfo.total = total
                        delegate?.onStarted(
                                download = downloadInfo,
                                etaInMilliseconds = estimatedTimeRemainingInMilliseconds,
                                downloadedBytesPerSecond = getAverageDownloadedBytesPerSecond())
                        executorService = Executors.newFixedThreadPool(chunkDownloadsList.size + 1)
                        downloadChunks(chunkDownloadsList)
                        waitAndPerformProgressReporting()
                        if (!interrupted && !terminated) {
                            throwExceptionIfFound()
                            if (downloaded == total) {
                                output = downloader.getRequestOutputStream(openingRequest, 0)
                                if (output == null) {
                                    randomAccessFile = RandomAccessFile(getFile(downloadInfo.file), "rw")
                                    randomAccessFile.seek(0)
                                }
                                if (!interrupted && !terminated) {
                                    phase = Phase.DOWNLOADING
                                    downloadInfo.downloaded = getProgressDownloaded()
                                    delegate?.onProgress(
                                            download = downloadInfo,
                                            etaInMilliSeconds = estimatedTimeRemainingInMilliseconds,
                                            downloadedBytesPerSecond = getAverageDownloadedBytesPerSecond())
                                    phase = Phase.IDLE
                                    mergeAllChunks(output, randomAccessFile)
                                    waitAndPerformProgressReporting()
                                    if (!interrupted && !terminated) {
                                        throwExceptionIfFound()
                                        val allFileChunksMerged = mergeCounter == fileChunks.size
                                        if (allFileChunksMerged) {
                                            deleteAllTempFiles()
                                            waitAndPerformProgressReporting()
                                            if (!interrupted && !terminated) {
                                                throwExceptionIfFound()
                                                downloadInfo.downloaded = total
                                                downloadInfo.total = total
                                                phase = Phase.COMPLETED
                                                completedDownload = true
                                                delegate?.onProgress(
                                                        download = downloadInfo,
                                                        etaInMilliSeconds = estimatedTimeRemainingInMilliseconds,
                                                        downloadedBytesPerSecond = getAverageDownloadedBytesPerSecond())
                                                delegate?.onComplete(
                                                        download = downloadInfo)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        downloadInfo.downloaded = getProgressDownloaded()
                        delegate?.saveDownloadProgress(downloadInfo)
                        if (!completedDownload && !terminated) {
                            delegate?.onProgress(
                                    download = downloadInfo,
                                    etaInMilliSeconds = estimatedTimeRemainingInMilliseconds,
                                    downloadedBytesPerSecond = getAverageDownloadedBytesPerSecond())
                        }
                        if (!terminated) {
                            throwExceptionIfFound()
                        }
                    }
                    phase = Phase.IDLE
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
                downloadInfo.downloaded = getProgressDownloaded()
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
            if (openingResponse != null) {
                try {
                    downloader.disconnect(openingResponse)
                } catch (e: Exception) {
                    logger.e("FileDownloader", e)
                }
            }
            try {
                output?.close()
            } catch (e: Exception) {
                logger.e("FileDownloader", e)
            }
            try {
                randomAccessFile?.close()
            } catch (e: Exception) {
                logger.e("FileDownloader", e)
            }
            terminated = true
        }
    }

    private fun getProgressDownloaded(): Long {
        return when (phase) {
            Phase.DOWNLOADING -> {
                val actualProgress = calculateProgress(downloaded, total)
                val eightyNinePercentOfTotal = (0.89F * total.toFloat())
                val downloaded = (actualProgress.toFloat() / 100.toFloat()) * eightyNinePercentOfTotal
                downloaded.toLong()
            }
            Phase.MERGING -> {
                val actualProgress = calculateProgress(mergeBytesWritten, total)
                val tenPercentOfTotal = (0.1F * total.toFloat())
                val eightyNinePercentOfTotal = (0.89F * total.toFloat())
                val merged = (actualProgress.toFloat() / 100.toFloat()) * tenPercentOfTotal
                (eightyNinePercentOfTotal + merged).toLong()
            }
            Phase.DELETING_TEMP_FILES -> {
                val actualProgress = calculateProgress(tempFilesDeleted, fileChunks.size.toLong())
                val tenPercentOfTotal = (0.1F * total.toFloat())
                val eightyNinePercentOfTotal = (0.89F * total.toFloat())
                val onePercentTotal = (0.01F * total.toFloat())
                val deleted = (actualProgress.toFloat() / 100.toFloat()) * onePercentTotal
                (eightyNinePercentOfTotal + tenPercentOfTotal + deleted).toLong()
            }
            Phase.COMPLETED -> {
                total
            }
            Phase.IDLE -> {
                -1L
            }
        }
    }

    private fun getFileChunkList(openingResponseCode: Int, request: Downloader.Request): List<FileChuck> {
        return if (openingResponseCode == HttpURLConnection.HTTP_PARTIAL) {
            val fileChunkInfo = getChuckInfo(request)
            var counterBytes = 0L
            val fileChunks = mutableListOf<FileChuck>()
            for (position in 1..fileChunkInfo.chunkCount) {
                val startBytes = counterBytes
                val endBytes = if (fileChunkInfo.chunkCount == position) {
                    total
                } else {
                    counterBytes + fileChunkInfo.bytesPerChunk
                }
                counterBytes = endBytes
                val fileChunk = FileChuck(
                        id = downloadInfo.id,
                        position = position,
                        file = getBytesDataFileForChunk(downloadInfo.id, position),
                        startBytes = startBytes,
                        endBytes = endBytes,
                        downloaded = getSavedDownloadedForFileChunk(downloadInfo.id, position)
                )
                downloaded += fileChunk.downloaded
                fileChunks.add(fileChunk)
            }
            fileChunks
        } else {
            val fileChunk = FileChuck(
                    id = downloadInfo.id,
                    position = 1,
                    startBytes = 0,
                    file = getBytesDataFileForChunk(downloadInfo.id, 1),
                    endBytes = total,
                    downloaded = getSavedDownloadedForFileChunk(downloadInfo.id, 1))
            downloaded += fileChunk.downloaded
            listOf(fileChunk)
        }
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

    private fun getBytesDataFileForChunk(id: Int, position: Int): String {
        return "$fileChunkTempDir/$id/$id.$position.tmp"
    }

    private fun getDownloadedInfoFileForChunk(id: Int, position: Int): String {
        return "${getBytesDataFileForChunk(id, position)}.txt"
    }

    private fun deleteTempDirForId(id: Int) {
        try {
            val file = getFile("$fileChunkTempDir/$id")
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
        }
    }

    private fun deleteTempInFilesForFileChunk(fileChunk: FileChuck) {
        try {
            val file = getFile(fileChunk.file)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
        }
        try {
            val text = getFile(getDownloadedInfoFileForChunk(fileChunk.id, fileChunk.position))
            if (text.exists()) {
                text.delete()
            }
        } catch (e: Exception) {
        }
    }

    private fun getSavedDownloadedForFileChunk(id: Int, position: Int): Long {
        var downloaded = 0L
        val file = getFile(getDownloadedInfoFileForChunk(id, position))
        if (file.exists() && !interrupted && !terminated) {
            val bufferedReader = BufferedReader(FileReader(file))
            try {
                val string: String? = bufferedReader.readLine()
                downloaded = string?.toLong() ?: 0L
            } catch (e: Exception) {
                logger.e("FileDownloader", e)
            } finally {
                try {
                    bufferedReader.close()
                } catch (e: Exception) {
                    logger.e("FileDownloader", e)
                }
            }
        }
        return downloaded
    }

    private fun saveDownloadedInfoForFileChunk(fileChunk: FileChuck, downloaded: Long) {
        val file = getFile(getDownloadedInfoFileForChunk(fileChunk.id, fileChunk.position))
        if (file.exists() && !interrupted && !terminated) {
            val bufferedWriter = BufferedWriter(FileWriter(file))
            try {
                bufferedWriter.write(downloaded.toString())
            } catch (e: Exception) {
                logger.e("FileDownloader", e)
            } finally {
                try {
                    bufferedWriter.close()
                } catch (e: Exception) {
                    logger.e("FileDownloader", e)
                }
            }
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
        var downloadedBytesPerSecond = getProgressDownloaded()
        var reportingStartTime = System.nanoTime()
        var downloadSpeedStartTime = System.nanoTime()
        while (chunksMergingDownloadingOrDeleting() && !interrupted && !terminated) {
            downloadInfo.downloaded = getProgressDownloaded()
            downloadInfo.total = total
            downloadSpeedStopTime = System.nanoTime()
            val downloadSpeedCheckTimeElapsed = hasIntervalTimeElapsed(downloadSpeedStartTime,
                    downloadSpeedStopTime, DEFAULT_DOWNLOAD_SPEED_REPORTING_INTERVAL_IN_MILLISECONDS)

            if (downloadSpeedCheckTimeElapsed) {
                downloadedBytesPerSecond = getProgressDownloaded() - downloadedBytesPerSecond
                movingAverageCalculator.add(downloadedBytesPerSecond.toDouble())
                averageDownloadedBytesPerSecond =
                        movingAverageCalculator.getMovingAverageWithWeightOnRecentValues()
                estimatedTimeRemainingInMilliseconds = calculateEstimatedTimeRemainingInMilliseconds(
                        downloadedBytes = getProgressDownloaded(),
                        totalBytes = total,
                        downloadedBytesPerSecond = getAverageDownloadedBytesPerSecond())
                downloadedBytesPerSecond = getProgressDownloaded()
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

    private fun mergeAllChunks(output: OutputStream?, randomAccessFile: RandomAccessFile?) {
        actionsCounter = 0
        actionsTotal = 1
        phase = Phase.MERGING
        executorService?.execute({
            try {
                for (fileChunk in fileChunks) {
                    if (!interrupted && !terminated) {
                        mergeChunk(fileChunk, output, randomAccessFile)
                    } else {
                        break
                    }
                }
            } catch (e: Exception) {
                throwable = e
            } finally {
                incrementActionCompleted()
            }
        })
    }

    private fun mergeChunk(fileChunk: FileChuck,
                           outputStream: OutputStream?,
                           randomAccessFile: RandomAccessFile?) {
        val chunkFile = getFile(fileChunk.file)
        val request = getRequestForDownload(downloadInfo, fileChunk.startBytes + fileChunk.downloaded)
        val inputStream = downloader.getRequestInputStream(request, 0)
        var inputRandomAccessFile: RandomAccessFile? = null
        if (inputStream == null) {
            inputRandomAccessFile = RandomAccessFile(chunkFile, "r")
        }
        try {
            val buffer = ByteArray(downloadBufferSizeBytes)
            var read = inputStream?.read(buffer, 0, downloadBufferSizeBytes)
                    ?: (inputRandomAccessFile?.read(buffer, 0, downloadBufferSizeBytes) ?: -1)
            while (read != -1 && !interrupted && !terminated) {
                outputStream?.write(buffer, 0, read)
                randomAccessFile?.write(buffer, 0, read)
                mergeBytesWritten += read
                read = inputStream?.read(buffer, 0, downloadBufferSizeBytes)
                        ?: (inputRandomAccessFile?.read(buffer, 0, downloadBufferSizeBytes) ?: -1)
            }
            incrementMergeCompleted()
        } catch (e: Exception) {
            throw e
        } finally {
            try {
                inputStream?.close()
            } catch (e: Exception) {
                logger.e("FileDownloader", e)
            }
            try {
                inputRandomAccessFile?.close()
            } catch (e: Exception) {
                logger.e("FileDownloader", e)
            }
        }
    }

    private fun downloadChunks(chunksDownloadsList: List<FileChuck>) {
        actionsCounter = 0
        actionsTotal = chunksDownloadsList.size
        phase = Phase.DOWNLOADING
        for (downloadChunk in chunksDownloadsList) {
            if (!interrupted && !terminated) {
                executorService?.execute({
                    val downloadRequest = getRequestForDownload(downloadInfo, downloadChunk.startBytes + downloadChunk.downloaded)
                    var downloadResponse: Downloader.Response? = null
                    var outputStream: OutputStream? = null
                    var randomAccessFileOutput: RandomAccessFile? = null
                    try {
                        downloadResponse = downloader.execute(downloadRequest)
                        if (!terminated && !interrupted && downloadResponse?.isSuccessful == true) {
                            val file = getFile(downloadChunk.file)
                            val seekPosition = if (downloadResponse.code == HttpURLConnection.HTTP_PARTIAL) {
                                downloadChunk.downloaded
                            } else {
                                0
                            }
                            outputStream = downloader.getRequestOutputStream(downloadRequest, seekPosition)
                            if (outputStream == null) {
                                randomAccessFileOutput = RandomAccessFile(file, "rw")
                                randomAccessFileOutput.seek(seekPosition)
                            }
                            var reportingStopTime: Long
                            val buffer = ByteArray(downloadBufferSizeBytes)
                            var read: Int = downloadResponse.byteStream?.read(buffer, 0, downloadBufferSizeBytes)
                                    ?: -1
                            var remainderBytes: Long = downloadChunk.endBytes - (downloadChunk.startBytes + downloadChunk.downloaded)
                            var reportingStartTime = System.nanoTime()
                            while (remainderBytes > 0L && read != -1 && !interrupted && !terminated) {
                                if (read <= remainderBytes) {
                                    randomAccessFileOutput?.write(buffer, 0, read)
                                    outputStream?.write(buffer, 0, read)
                                    downloadChunk.downloaded += read
                                    addToTotalDownloaded(read)
                                    read = downloadResponse.byteStream?.read(buffer, 0, downloadBufferSizeBytes) ?: -1
                                    remainderBytes = downloadChunk.endBytes - (downloadChunk.startBytes + downloadChunk.downloaded)
                                } else {
                                    randomAccessFileOutput?.write(buffer, 0, remainderBytes.toInt())
                                    outputStream?.write(buffer, 0, remainderBytes.toInt())
                                    downloadChunk.downloaded += remainderBytes
                                    addToTotalDownloaded(remainderBytes.toInt())
                                    read = -1
                                }
                                reportingStopTime = System.nanoTime()
                                val hasReportingTimeElapsed = hasIntervalTimeElapsed(reportingStartTime,
                                        reportingStopTime, DEFAULT_DOWNLOAD_SPEED_REPORTING_INTERVAL_IN_MILLISECONDS)
                                if (hasReportingTimeElapsed) {
                                    saveDownloadedInfoForFileChunk(downloadChunk, downloadChunk.downloaded)
                                    reportingStartTime = System.nanoTime()
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
                        incrementActionCompleted()
                    }
                })
            }
        }
    }

    private fun deleteAllTempFiles() {
        actionsCounter = 0
        actionsTotal = 1
        phase = Phase.DELETING_TEMP_FILES
        executorService?.execute({
            try {
                for (fileChunk in fileChunks) {
                    if (!interrupted && !terminated) {
                        deleteTempInFilesForFileChunk(fileChunk)
                        tempFilesDeleted += 1
                    } else {
                        break
                    }
                }
                if (!interrupted && !terminated) {
                    deleteTempDirForId(initialDownload.id)
                }
            } catch (e: Exception) {
                throwable = e
            } finally {
                incrementActionCompleted()
            }
        })
    }

    private fun addToTotalDownloaded(read: Int) {
        synchronized(downloadedLock) {
            downloaded += read
        }
    }

    private fun incrementActionCompleted() {
        synchronized(actionsLock) {
            actionsCounter += 1
            if (actionsCounter == actionsTotal) {
                phase = Phase.IDLE
            }
        }
    }

    private fun incrementMergeCompleted() {
        synchronized(mergeLock) {
            mergeCounter += 1
        }
    }

    private fun chunksMergingDownloadingOrDeleting(): Boolean {
        return !interrupted && !terminated && (phase == Phase.DOWNLOADING || phase == Phase.MERGING
                || phase == Phase.DELETING_TEMP_FILES)
    }

    private fun throwExceptionIfFound() {
        val exception = throwable
        if (exception != null) {
            throw exception
        }
    }

    data class FileChunkInfo(val chunkCount: Int, val bytesPerChunk: Long)

    data class FileChuck(val id: Int = 0,
                         val position: Int = 0,
                         val file: String,
                         val startBytes: Long = 0L,
                         val endBytes: Long = 0L,
                         var downloaded: Long = 0L) {

        val isDownloaded: Boolean
            get() {
                return startBytes + downloaded == endBytes
            }
    }

    enum class Phase {
        DOWNLOADING,
        MERGING,
        DELETING_TEMP_FILES,
        COMPLETED,
        IDLE;
    }

}