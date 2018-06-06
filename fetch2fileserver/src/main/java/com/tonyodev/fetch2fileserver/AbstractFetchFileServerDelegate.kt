package com.tonyodev.fetch2fileserver

import com.tonyodev.fetch2core.InterruptMonitor
import com.tonyodev.fetch2fileserver.transporter.FileRequest
import com.tonyodev.fetch2fileserver.transporter.FileResourceTransporterWriter
import java.io.InputStream

/**
 * Delegate that can be attached to a Fetch File Server instance to take certain actions
 * on a FileRequested by the client.
 * */
abstract class AbstractFetchFileServerDelegate : FetchFileServerDelegate {

    /** Called when a client is successfully connected to the Fetch File Server and
     * the request has been authorized.
     * @param client Client identifier
     * @param fileRequest File request by the client
     * */
    override fun onClientConnected(client: String, fileRequest: FileRequest) {

    }

    /**
     * Called when a client provides custom data that the file server device can used or act on.
     * @param client Client identifier
     * @param customData Custom data
     * @param fileRequest File request by the client
     * */
    override fun onClientDidProvideCustomData(client: String, customData: String, fileRequest: FileRequest) {

    }

    /** Called when a client disconnects from the Fetch File Server.
     * @param client Client identifier
     * */
    override fun onClientDisconnected(client: String) {

    }

    /** Called when the Fetch File Server needs to provide the client the requested file input stream.
     * If null is returned, Fetch File Server will provide the InputSteam. Use this method if you need
     * to provide a custom InputStream. For example an encrypted input stream.
     * @param fileResource Resource File that Fetch file Server will provide the client.
     * @param fileOffset The offset reading will begin from. Use this value to seek to the right
     * offset position. Note: Not seeking to the right position will cause the server to send
     * invalid data to the client.
     * @return file input stream. Can be null.
     * */
    override fun getFileInputStream(fileResource: FileResource, fileOffset: Long): InputStream? {
        return null
    }

    /** Called if the client requested a custom request that the Fetch File Server cannot
     * serve. Use this callback to provide a custom response.
     * @param client Client identifier
     * @param fileRequest File request by the client
     * @param fileResourceTransporterWriter Writer used to transport byte data to the requesting client.
     * @param interruptMonitor used this object to monitor interruption.
     * A request may have been cancelled by the server because it is shutting down or the client
     * has closed the connection.
     * **/
    override fun onCustomRequest(client: String, fileRequest: FileRequest,
                                 fileResourceTransporterWriter: FileResourceTransporterWriter, interruptMonitor: InterruptMonitor) {

    }

}