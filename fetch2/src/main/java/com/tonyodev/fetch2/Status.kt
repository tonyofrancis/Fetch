package com.tonyodev.fetch2

enum class Status private constructor(val value: Int) {
    ERROR(-1), CANCELLED(0), QUEUED(1), DOWNLOADING(2), PAUSED(3), COMPLETED(4), REMOVED(5), INVALID(6);

    override fun toString(): String {
        return "Status: " + value
    }

    companion object {

        fun valueOf(status: Int): Status {
            when (status) {
                -1 -> return ERROR
                0 -> return CANCELLED
                1 -> return QUEUED
                2 -> return DOWNLOADING
                3 -> return PAUSED
                4 -> return COMPLETED
                5 -> return REMOVED
                else -> return INVALID
            }
        }
    }
}