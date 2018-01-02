package com.tonyodev.fetch2

/**
 * An immutable object which contains a current snapshot of all the information
 * about a specific download managed by Fetch.
 * */
interface Download {

    /** Used to identify a download. This id also matches the id of the request that started
     * the download.*/
    val id: Int

    /** The Fetch namespace this download belongs to.*/
    val namespace: String

    /** The url where the file will be downloaded from.*/
    val url: String

    /** The file eg(/files/download.txt) where the file will be
     * downloaded to and saved on disk.*/
    val file: String

    /** The group id this download belongs to.*/
    val group: Int

    /** The download Priority of this download.
     * @see com.tonyodev.fetch2.Priority
     * */
    val priority: Priority

    /** The headers used by the downloader to send header information to
     * the server about a request.*/
    val headers: Map<String, String>

    /** The amount of bytes downloaded thus far and saved to the file.*/
    val downloaded: Long

    /** The file size of a download in bytes. This field could return -1 if the server
     * did not readily provide the Content-Length when the connection was established.*/
    val total: Long

    /** The current status of a download.
     *  @see com.tonyodev.fetch2.Status
     *  */
    val status: Status

    /** If the download encountered an error, the download status will be Status.Failed and
     *  this field will provide the specific error when possible.
     *  Otherwise the default non-error value is Error.NONE.
     *  @see com.tonyodev.fetch2.Error
     *  */
    val error: Error

    /** The network type this download is allowed to download on.
     * @see com.tonyodev.fetch2.NetworkType
     * */
    val networkType: NetworkType

    /** The download progress thus far eg(95 indicating 95% completed). If the total field of
     * this object has a value of -1, this field will also return -1 indicating that the server
     * did not readily provide the Content-Length and that the progress is undetermined.*/
    val progress: Int

    /** The timestamp when this download was created.*/
    val created: Long

    /** The request information used to create this download.*/
    val request: Request

}