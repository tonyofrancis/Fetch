package com.tonyodev.fetch2core

import java.io.Closeable
import java.io.IOException

/**
 * Class that can be used to wrap an INPUT Stream of file input resource.
 * */
abstract class InputResourceWrapper : Closeable {

    /** read bytes from resource
     * @param byteArray data
     * @param offSet read offset
     * @param length data length
     * */
    @Throws(IOException::class)
    abstract fun read(byteArray: ByteArray, offSet: Int = 0, length: Int = byteArray.size): Int

    /** Set read offset position
     * @param offset read offset position
     * */
    @Throws(IOException::class)
    abstract fun setReadOffset(offset: Long)

}