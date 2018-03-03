package com.tonyodev.fetch2.helper

import android.os.Handler
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Logger

class PriorityIteratorProcessorHandlerImpl constructor(private val handler: Handler,
                                                       private val priorityIteratorProcessor: PriorityIteratorProcessor<Download>,
                                                       private val logger: Logger)
    : PriorityIteratorProcessorHandler {

    override fun runProcessor() {
        try {
            handler.post {
                if (priorityIteratorProcessor.isStopped) {
                    priorityIteratorProcessor.start()
                }
            }
        } catch (e: Exception) {
            logger.e("PriorityIteratorProcessorHandler", e)
        }
    }

}