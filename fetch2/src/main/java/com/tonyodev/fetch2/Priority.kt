package com.tonyodev.fetch2

/**
 * Enumeration which contains the different priority level
 * a download can have.
 * */
enum class Priority(val value: Int) {

    /** Highest priority level a download can have.*/
    HIGH(1),

    /** Normal priority level a download can have. This is the default priority
     * for newly created downloads.*/
    NORMAL(0),

    /** Lowest priority level a download can have.*/
    LOW(-1);

    companion object {

        @JvmStatic
        fun valueOf(value: Int): Priority {
            return when (value) {
                1 -> HIGH
                0 -> NORMAL
                -1 -> LOW
                else -> NORMAL
            }
        }

    }

}