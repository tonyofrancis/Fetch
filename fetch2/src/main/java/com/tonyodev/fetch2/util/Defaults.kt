@file:JvmName("FetchDefaults")

package com.tonyodev.fetch2.util

import com.tonyodev.fetch2.*


const val DEFAULT_TAG = "fetch2"
const val DEFAULT_GROUP_ID = 0
const val DEFAULT_PROGRESS_REPORTING_INTERVAL_IN_MILLISECONDS = 2_000L
const val DEFAULT_DOWNLOAD_SPEED_REPORTING_INTERVAL_IN_MILLISECONDS = 1_000L
const val DEFAULT_CONCURRENT_LIMIT = 1
const val EMPTY_JSON_OBJECT_STRING = "{}"
const val DEFAULT_LOGGING_ENABLED = false
const val DEFAULT_IN_MEMORY_DATABASE_ENABLED = false
const val PRIORITY_QUEUE_INTERVAL_IN_MILLISECONDS = 500L
const val DEFAULT_DOWNLOAD_BUFFER_SIZE_BYTES = 8192
const val DEFAULT_AUTO_START = true
const val DEFAULT_RETRY_ON_NETWORK_GAIN = false
val defaultEmptyHeaderMap = mapOf<String, String>()
val defaultNetworkType = NetworkType.ALL
val defaultGlobalNetworkType = NetworkType.GLOBAL_OFF
val defaultPriority = Priority.NORMAL
val defaultNoError = Error.NONE
val defaultStatus = Status.NONE
val defaultDownloader: Downloader = HttpUrlConnectionDownloader()
val defaultLogger: Logger = FetchLogger(DEFAULT_LOGGING_ENABLED, DEFAULT_TAG)
