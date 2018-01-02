package com.tonyodev.fetch2

/**
 * Enumeration which contains the different network types
 * a download can have and be downloaded over.
 * */
enum class NetworkType(val value: Int) {

    /** Indicates a network type that is not bounded. This is used by Fetch
     * to disable the global network type option allowing each download to indicate
     * their own network type they can download on.*/
    GLOBAL_OFF(-1),

    /** Indicates that a download can be downloaded over mobile or wifi networks.*/
    ALL(0),

    /** Indicates that a download can be downloaded only on wifi networks.*/
    WIFI_ONLY(1);

    companion object {

        @JvmStatic
        fun valueOf(value: Int): NetworkType {
            return when (value) {
                -1 -> GLOBAL_OFF
                0 -> ALL
                1 -> WIFI_ONLY
                else -> ALL
            }
        }

    }

}