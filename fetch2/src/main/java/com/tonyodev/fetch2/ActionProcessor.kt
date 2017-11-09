package com.tonyodev.fetch2

/**
 * Created by tonyofrancis on 6/11/17.
 */

internal interface ActionProcessor<T> {

    fun queueAction(action: T)

    fun processNext()

    fun clearQueue()
}
