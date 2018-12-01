package com.tonyodev.fetch2core

import com.tonyodev.fetch2core.server.FetchFileResourceTransporter
import com.tonyodev.fetch2core.server.FileRequest
import java.net.InetSocketAddress

interface FileServerDownloader : Downloader<FetchFileResourceTransporter, FileServerDownloader.TransporterRequest> {

    /** Fetch a Catalog List of File Resources from a Fetch File Server
     * @param serverRequest the server request
     * @return list of File Resources
     * */
    fun getFetchFileServerCatalog(serverRequest: Downloader.ServerRequest): List<FileResource>

    /** Class used to hold configuration settings for a FetchFileServer Transporter connection.*/
    open class TransporterRequest {

        var inetSocketAddress = InetSocketAddress(0)
        var fileRequest = FileRequest()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as TransporterRequest
            if (inetSocketAddress != other.inetSocketAddress) return false
            if (fileRequest != other.fileRequest) return false
            return true
        }

        override fun hashCode(): Int {
            var result = inetSocketAddress.hashCode()
            result = 31 * result + fileRequest.hashCode()
            return result
        }

        override fun toString(): String {
            return "TransporterRequest(inetSocketAddress=$inetSocketAddress, fileRequest=$fileRequest)"
        }

    }

}