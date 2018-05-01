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

class ChunkFileDownloaderImpl(private val initialDownload: Download,
                              private val downloader: Downloader,
                              private val progressReportingIntervalMillis: Long,
                              private val downloadBufferSizeBytes: Int,
                              private val logger: Logger,
                              private val networkInfoProvider: NetworkInfoProvider,
                              private val retryOnNetworkGain: Boolean,
                              private val fileChunkTempDir: String,
                              private val fileChunkLimit: Int) : FileDownloader {

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

    private val downloadedLock = Object()

    private var total = 0L

    private var averageDownloadedBytesPerSecond = 0.0

    private val movingAverageCalculator = AverageCalculator(5)

    private var estimatedTimeRemainingInMilliseconds: Long = -1

    private var chunkExecutorService: ExecutorService? = null

    override fun run() {
        var openingResponse: Downloader.Response? = null
        var output: OutputStream? = null
        var randomAccessFile: RandomAccessFile? = null
        try {
            val openingRequest = getOpeningRequest()
            openingResponse = downloader.execute(openingRequest)
            if (!interrupted && !terminated && openingResponse?.isSuccessful == true) {
                total = openingResponse.contentLength
                if (total > 0) {
                    val fileChunks = getFileChunkList(openingResponse.code)
                    val chunkDownloadsList = fileChunks.filter { !it.completed }
                    if (!interrupted && !terminated) {
                        delegate?.onStarted(
                                download = downloadInfo,
                                etaInMilliseconds = estimatedTimeRemainingInMilliseconds,
                                downloadedBytesPerSecond = getAverageDownloadedBytesPerSecond())
                        if (chunkDownloadsList.isNotEmpty()) {
                            chunkExecutorService = Executors.newFixedThreadPool(chunkDownloadsList.size)
                        }
                        downloadChunks(chunkDownloadsList)
                        waitAndPerformProgressReporting(chunkDownloadsList)
                        if (!interrupted && !terminated) {
                            val downloadedBytesSum = fileChunks.asSequence().map { it.downloaded }.sum()
                            if (downloadedBytesSum == total) {
                                downloadInfo.downloaded = downloadedBytesSum
                                downloadInfo.total = total
                                output = downloader.getRequestOutputStream(openingRequest, 0)
                                if (output == null) {
                                    randomAccessFile = RandomAccessFile(getFile(downloadInfo.file), "rw")
                                    randomAccessFile.seek(0)
                                }
                                for (fileChunk in fileChunks) {
                                    mergeChunk(fileChunk, output, randomAccessFile)
                                }
                                if (!interrupted && !terminated) {
                                    for (fileChunk in fileChunks) {
                                        val file = File(fileChunk.file)
                                        file.delete()
                                    }
                                    completedDownload = true
                                    if (!terminated) {
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
                        downloadInfo.downloaded = downloaded
                        downloadInfo.total = total
                        delegate?.saveDownloadProgress(downloadInfo)
                        if (!completedDownload && !terminated) {
                            delegate?.onProgress(
                                    download = downloadInfo,
                                    etaInMilliSeconds = estimatedTimeRemainingInMilliseconds,
                                    downloadedBytesPerSecond = getAverageDownloadedBytesPerSecond())
                        }
                        val failedChunk = fileChunks.firstOrNull { it.status == DownloadingStatus.ERROR }
                        if (failedChunk != null) {
                            throw failedChunk.errorException!!
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
            chunkExecutorService?.shutdown()
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

    private fun getOpeningRequest(): Downloader.Request {
        val headers = initialDownload.headers.toMutableMap()
        headers["Range"] = "bytes=0-"
        return Downloader.Request(
                id = initialDownload.id,
                url = initialDownload.url,
                headers = headers,
                file = initialDownload.file,
                tag = initialDownload.tag)
    }

    private fun getRequestForFileChunk(fileChunk: FileChuck): Downloader.Request {
        val headers = initialDownload.headers.toMutableMap()
        headers["Range"] = "bytes=${fileChunk.startBytes + fileChunk.downloaded}-"
        return Downloader.Request(
                id = initialDownload.id,
                url = initialDownload.url,
                headers = headers,
                file = initialDownload.file,
                tag = initialDownload.tag)
    }

    private fun getFileChunkList(openingResponseCode: Int): List<FileChuck> {
        return if (openingResponseCode == HttpURLConnection.HTTP_PARTIAL) {
            val fileChunkInfo = getChuckInfo()
            var counterBytes = 0L
            val fileChunks = mutableListOf<FileChuck>()
            for (position in 1..fileChunkInfo.chunks) {
                val startBytes = counterBytes
                val endBytes = if (fileChunkInfo.chunks == position) {
                    total
                } else {
                    counterBytes + fileChunkInfo.bytesPerChunk
                }
                counterBytes = endBytes
                val fileChunk = FileChuck(
                        id = downloadInfo.id,
                        position = position,
                        startBytes = startBytes,
                        endBytes = endBytes,
                        file = getFileForChunk(downloadInfo.id, position))
                fileChunks.add(fileChunk)
            }
            //TODO: get this information from a database
            fileChunks.forEach {
                val file = File(it.file)
                it.downloaded = file.length()
                downloaded += it.downloaded
                if (it.startBytes + it.downloaded == it.endBytes) {
                    it.status = DownloadingStatus.COMPLETED
                }
            }
            fileChunks
        } else {
            val singleFileChunk = FileChuck(
                    id = downloadInfo.id,
                    position = 1,
                    startBytes = 0,
                    endBytes = total,
                    file = getFileForChunk(downloadInfo.id, 1))
            //TODO: get this information from a database
            val file = File(singleFileChunk.file)
            singleFileChunk.downloaded = file.length()
            if (singleFileChunk.startBytes + singleFileChunk.downloaded == singleFileChunk.endBytes) {
                singleFileChunk.status = DownloadingStatus.COMPLETED
            }
            downloaded += singleFileChunk.downloaded
            listOf(singleFileChunk)
        }
    }

    private fun getChuckInfo(): FileChunkInfo {
        return if (fileChunkLimit == DEFAULT_FILE_CHUNK_LIMIT) {
            val fileSizeInMb = total.toFloat() / (1024 * 1024).toFloat()
            val fileSizeInGb = total.toFloat() / (1024 * 1024 * 1024).toFloat()
            when {
                fileSizeInGb >= 1 -> {
                    val chunks = 4
                    val bytesPerChunk = ceil((total.toFloat() / chunks.toFloat())).toLong()
                    FileChunkInfo(chunks, bytesPerChunk)
                }
                fileSizeInMb >= 1 -> {
                    val chunks = 2
                    val bytesPerChunk = ceil((total.toFloat() / chunks.toFloat())).toLong()
                    FileChunkInfo(chunks, bytesPerChunk)
                }
                else -> FileChunkInfo(1, total)
            }
        } else {
            val bytesPerChunk = ceil((total.toFloat() / fileChunkLimit.toFloat())).toLong()
            return FileChunkInfo(fileChunkLimit, bytesPerChunk)
        }
    }

    private fun getFileForChunk(id: Int, position: Int): String {
        return "$fileChunkTempDir/$id/$id.$position.tmp"
    }

    private fun getAverageDownloadedBytesPerSecond(): Long {
        if (averageDownloadedBytesPerSecond < 1) {
            return 0L
        }
        return ceil(averageDownloadedBytesPerSecond).toLong()
    }

    private fun waitAndPerformProgressReporting(chunkDownloadsList: List<FileChuck>) {
        var reportingStopTime: Long
        var downloadSpeedStopTime: Long
        var downloadedBytesPerSecond = downloaded
        var reportingStartTime = System.nanoTime()
        var downloadSpeedStartTime = System.nanoTime()
        while (chunksDownloading(chunkDownloadsList) && !interrupted && !terminated) {
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

    private fun mergeChunk(fileChunk: FileChuck,
                           outputStream: OutputStream?,
                           randomAccessFile: RandomAccessFile?) {
        val chunkFile = getFile(fileChunk.file)
        //TODO:GET INPUT STREAM FROM DOWNLOADER
        val chunkInputStream = BufferedInputStream(FileInputStream(chunkFile))
        try {
            val buffer = ByteArray(downloadBufferSizeBytes)
            var read = chunkInputStream.read(buffer, 0, downloadBufferSizeBytes)
            while (read != -1) {
                outputStream?.write(buffer, 0, read)
                randomAccessFile?.write(buffer, 0, read)
                read = chunkInputStream.read(buffer, 0, downloadBufferSizeBytes)
            }
        } catch (e: Exception) {
            logger.e("FileDownloader", e)
            throw Exception("chunk_merge_failed") //TODO: CATCH THIS IN MAIN CATCH
        } finally {
            try {
                chunkInputStream.close()
            } catch (e: Exception) {
                logger.e("FileDownloader", e)
            }
        }
    }

    private fun downloadChunks(fileChunks: List<FileChuck>) {
        for (downloadChunk in fileChunks) {
            if (!interrupted && !terminated) {
                downloadChunk.status = DownloadingStatus.DOWNLOADING
                chunkExecutorService?.execute({
                    val downloadRequest = getRequestForFileChunk(downloadChunk)
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
                            val buffer = ByteArray(downloadBufferSizeBytes)
                            var read: Int = downloadResponse.byteStream?.read(buffer, 0, downloadBufferSizeBytes)
                                    ?: -1
                            var remainderBytes: Long = downloadChunk.endBytes - (downloadChunk.startBytes + downloadChunk.downloaded)
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
                            }
                            if (remainderBytes == 0L) {
                                downloadChunk.status = DownloadingStatus.COMPLETED
                            } else {
                                downloadChunk.status = DownloadingStatus.QUEUED
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
                        downloadChunk.status = DownloadingStatus.ERROR
                        downloadChunk.errorException = e
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
                    }
                })
            }
        }
    }

    private fun addToTotalDownloaded(read: Int) {
        synchronized(downloadedLock) {
            downloaded += read
        }
    }

    private fun chunksDownloading(chunkDownloadsList: List<FileChuck>): Boolean {
        if (!interrupted && !terminated) {
            for (chunk in chunkDownloadsList) {
                if (chunk.status == DownloadingStatus.DOWNLOADING) {
                    return true
                }
            }
        }
        return false
    }

    private fun getFile(filePath: String): File {
        val file = File(filePath)
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

    data class FileChunkInfo(val chunks: Int, val bytesPerChunk: Long)

    data class FileChuck(val id: Int = 0,
                         val position: Int = 0,
                         val startBytes: Long = 0L,
                         val endBytes: Long = 0L,
                         var downloaded: Long = 0L,
                         var file: String,
                         var status: DownloadingStatus = DownloadingStatus.QUEUED,
                         var errorException: Throwable? = null) {

        val completed: Boolean
            get() {
                return status == DownloadingStatus.COMPLETED
            }
    }

    enum class DownloadingStatus {
        QUEUED,
        DOWNLOADING,
        COMPLETED,
        ERROR;
    }

}