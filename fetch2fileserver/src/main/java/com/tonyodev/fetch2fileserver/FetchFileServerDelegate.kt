package com.tonyodev.fetch2fileserver

import com.tonyodev.fetch2core.Extras
import com.tonyodev.fetch2core.FileResource
import com.tonyodev.fetch2core.InputResourceWrapper
import com.tonyodev.fetch2core.InterruptMonitor
import com.tonyodev.fetch2core.server.FileRequest
import com.tonyodev.fetch2core.server.FileResourceTransporterWriter

/**
 * Delegate that can be attached to a Fetch File Server instance to take certain actions
 * on a FileRequested by the client.
 * */
interface FetchFileServerDelegate {

    /** Called when a client is successfully connected to the Fetch File Server and
     * the request has been authorized.
     * @param sessionId sessionId
     * @param fileRequest File request by the client
     * */
    fun onClientConnected(sessionId: String, fileRequest: FileRequest)

    /**
     * Called when a client provides custom data that the file server device can used or act on.
     * @param sessionId sessionId
     * @param extras Custom data extras
     * @param fileRequest File request by the client
     * */
    fun onClientDidProvideExtras(sessionId: String, extras: Extras, fileRequest: FileRequest)

    /** Called when a client disconnects from the Fetch File Server.
     * @param sessionId sessionId
     * @param fileRequest File request by the client
     * */
    fun onClientDisconnected(sessionId: String, fileRequest: FileRequest)

    /** Called when the Fetch File Server needs to provide the client the requested file input stream
     * wrapped in a InputResourceWrapper.
     * If null is returned, Fetch File Server will provide the InputResourceWrapper. Use this method if you need
     * to provide a custom InputResourceWrapper. For example an encrypted input stream. This method is called on a background thread.
     * @param sessionId sessionId
     * @param fileRequest File request by the client
     * @param fileResource Resource File that Fetch file Server will provide the client.
     * @param fileOffset The offset reading will begin from. Use this value to seek to the right
     * offset position. Note: Not seeking to the right position will cause the server to send
     * invalid data to the client.
     * @return file InputResourceWrapper. Can be null.
     * */
    fun getFileInputResourceWrapper(sessionId: String, fileRequest: FileRequest, fileResource: FileResource, fileOffset: Long): InputResourceWrapper?

    /** Called if the client requested a custom request that the Fetch File Server cannot
     * serve. Use this callback to provide a custom response. This method is called on a background thread.
     * @param sessionId sessionId
     * @param fileRequest File request by the client
     * @param fileResourceTransporterWriter Writer used to transport byte data to the requesting client.
     * @param interruptMonitor used this object to monitor interruption.
     * A request may have been cancelled by the server because it is shutting down or the client
     * has closed the connection.
     * **/
    fun onCustomRequest(sessionId: String, fileRequest: FileRequest,
                        fileResourceTransporterWriter: FileResourceTransporterWriter, interruptMonitor: InterruptMonitor)

}