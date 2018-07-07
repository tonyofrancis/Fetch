package com.tonyodev.fetch2core

import java.io.Closeable
import java.io.IOException

/**
 * Class that can be used to wrap an output Stream of file output resource.
 * */
abstract class OutputResourceWrapper : Closeable {

    /** Write bytes to resource
     * @param byteArray data
     * @param offSet write offset
     * @param length data length
     * */
    @Throws(IOException::class)
    abstract fun write(byteArray: ByteArray, offSet: Int = 0, length: Int = byteArray.size)

    /** Set write offset position
     * @param offset write offset position
     * */
    @Throws(IOException::class)
    abstract fun setWriteOffset(offset: Long)

    /** Flush resource buffer*/
    @Throws(IOException::class)
    abstract fun flush()

}