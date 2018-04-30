package com.tonyodev.fetch2.downloader

import android.util.Log
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Downloader
import com.tonyodev.fetch2.Logger
import com.tonyodev.fetch2.provider.NetworkInfoProvider
import com.tonyodev.fetch2.util.calculateProgress
import com.tonyodev.fetch2.util.toDownloadInfo
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.RandomAccessFile
import java.util.concurrent.Executors
import kotlin.math.ceil

class ChunkFileDownloaderImpl(private val initialDownload: Download,
                              private val downloader: Downloader,
                              private val progressReportingIntervalMillis: Long,
                              private val downloadBufferSizeBytes: Int,
                              private val logger: Logger,
                              private val networkInfoProvider: NetworkInfoProvider,
                              private val retryOnNetworkGain: Boolean,
                              private val chuckLimit: Int,
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
            downloadInfo.downloaded = 0
            downloadInfo.total = 0
            return downloadInfo
        }

    @Volatile
    private var downloaded = 0L
    private var fileLength = 0L
    private val downloadedLock = Object()
    @Volatile
    private var doneCount = 0

    override fun run() {
        var response: Downloader.Response? = null
        try {
            val request = getRequest()
            response = downloader.execute(request)
            if (response != null && response.isSuccessful) {
                fileLength = response.contentLength
                if (response.contentLength > 0) {
                    val fileLength = response.contentLength
                    val info = getChucksForFileLength(fileLength)
                    var mainStartBytes = 0L
                    val readFileChunks = mutableListOf<ReadFileChuck>()
                    for (i in 1..info.first) {
                        val startBytes = mainStartBytes
                        val endBytes = if (info.first == i) {
                            fileLength
                        } else {
                            mainStartBytes + info.second
                        }
                        mainStartBytes = endBytes
                        val fileChunk = ReadFileChuck(
                                id = initialDownload.id,
                                position = i,
                                startBytes = startBytes,
                                endBytes = endBytes,
                                file = getFileForChunk(initialDownload.id, i))
                        readFileChunks.add(fileChunk)
                    }

                    readFileChunks.forEach {
                        val file = File(it.file)
                        it.downloaded = file.length()
                        downloaded += it.downloaded
                    }

                    val downloadsList = readFileChunks.filter { !it.done }
                    val executors = Executors.newFixedThreadPool(downloadsList.size)
                    downloadsList.forEach {
                        executors.execute({
                            val headers = initialDownload.headers.toMutableMap()
                            headers["Range"] = "bytes=${it.startBytes + it.downloaded}-"
                            val rq = Downloader.Request(
                                    id = initialDownload.id,
                                    url = initialDownload.url,
                                    headers = headers,
                                    file = initialDownload.file,
                                    tag = initialDownload.tag)
                            val rp = downloader.execute(rq)
                            if (rp?.byteStream != null) {
                                try {
                                    val randomAccessFileOutput = RandomAccessFile(getFile(it.file), "rw")
                                    randomAccessFileOutput.seek(it.downloaded)
                                    val buffer = ByteArray(downloadBufferSizeBytes)
                                    var read = rp.byteStream.read(buffer, 0, downloadBufferSizeBytes)
                                    var remainderBytes: Long = it.endBytes - (it.startBytes + it.downloaded)
                                    while (remainderBytes > 0L && read != -1) {
                                        if (read <= remainderBytes) {
                                            randomAccessFileOutput.write(buffer, 0, read)
                                            it.downloaded += read
                                            addToDownloaded(read)
                                            read = rp.byteStream.read(buffer, 0, downloadBufferSizeBytes)
                                            remainderBytes = it.endBytes - (it.startBytes + it.downloaded)
                                        } else {
                                            randomAccessFileOutput.write(buffer, 0, remainderBytes.toInt())
                                            it.downloaded += remainderBytes
                                            addToDownloaded(remainderBytes.toInt())
                                            read = -1
                                        }
                                    }
                                    randomAccessFileOutput.close()
                                    downloader.disconnect(response)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                } finally {
                                    incrementDoneCount()
                                }
                            }
                        })
                    }

                    while (doneCount != downloadsList.size && !interrupted) {

                    }

                    //TODO: MERGE ALL THE FILES TOGETHER INTO ONE FILE

                    val allDownloaded = readFileChunks.asSequence().map { it.downloaded }.sum()
                    if (allDownloaded == fileLength) {
                        readFileChunks.forEach {
                            val inputStream = BufferedInputStream(FileInputStream(getFile(it.file)))
                            val output = RandomAccessFile(getFile(initialDownload.file), "rw")
                            output.seek(it.startBytes)
                            val buffer = ByteArray(downloadBufferSizeBytes)
                            var read = inputStream.read(buffer, 0, downloadBufferSizeBytes)
                            while (read != -1) {
                                output.write(buffer, 0, read)
                                read = inputStream.read(buffer, 0, downloadBufferSizeBytes)
                            }
                            output.close()
                            inputStream.close()
                        }
                    }


                    Log.d("m", mainStartBytes.toString())


                } else {
                    //TODO: THROW EXCEPTION
                }
            } else {
                //TODO: THROW EXCEPTION
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            //close response here
            if (response != null) {
                try {
                    downloader.disconnect(response)
                } catch (e: Exception) {
                    //TODO: FAILED TO CLOSE RESPONSE
                }
            }
            terminated = true
            completedDownload = true

        }
    }

    private fun addToDownloaded(read: Int) {
        synchronized(downloadedLock) {
            downloaded += read
        }
    }

    private fun incrementDoneCount() {
        synchronized(downloadedLock) {
            doneCount += 1
        }
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

    private fun getChucksForFileLength(fileLengthBytes: Long): Pair<Int, Long> {
        val megabytes = fileLengthBytes.toFloat() / (1024 * 1024).toFloat()
        val gb = fileLengthBytes.toFloat() / (1024 * 1024 * 1024).toFloat()
        return when {
            gb >= 1 -> {
                val c = ceil(gb / 100.toFloat()).toInt()
                val b = 1e8.toLong()
                Pair(c, b)
            }
            megabytes >= 1 -> {
                val c = ceil(megabytes / 10.toFloat()).toInt()
                val b = 1e7.toLong()
                Pair(c, b)
            }
            else -> Pair(1, fileLengthBytes)
        }
    }

    private fun getRequest(): Downloader.Request {
        val headers = initialDownload.headers.toMutableMap()
        headers["Range"] = "bytes=0-"
        return Downloader.Request(
                id = initialDownload.id,
                url = initialDownload.url,
                headers = headers,
                file = initialDownload.file,
                tag = initialDownload.tag)
    }

    private fun getFileForChunk(id: Int, position: Int): String {
        return "$fileChunkTempDir/$id.$position.tmp"
    }

    data class ReadFileChuck(val id: Int = 0,
                             val position: Int = 0,
                             val startBytes: Long = 0L,
                             val endBytes: Long = 0L,
                             @Volatile
                             var downloaded: Long = 0L,
                             var file: String) {

        val done: Boolean
            get() {
                return downloaded == ((endBytes + 1) - startBytes)
            }
    }

}