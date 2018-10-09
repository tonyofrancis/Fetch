@file:JvmName("FetchDefaults")

package com.tonyodev.fetch2.util

import com.tonyodev.fetch2.*
import com.tonyodev.fetch2.NetworkType
import com.tonyodev.fetch2.Priority
import com.tonyodev.fetch2.Status
import com.tonyodev.fetch2core.*

const val DEFAULT_GROUP_ID = 0
const val DEFAULT_UNIQUE_IDENTIFIER = 0L
const val DEFAULT_DOWNLOAD_SPEED_REPORTING_INTERVAL_IN_MILLISECONDS = 1_000L
const val DEFAULT_CONCURRENT_LIMIT = 1
const val EMPTY_JSON_OBJECT_STRING = "{}"
const val DEFAULT_PRIORITY_QUEUE_INTERVAL_IN_MILLISECONDS = 500L
const val DEFAULT_AUTO_START = true
const val DEFAULT_RETRY_ON_NETWORK_GAIN = true
const val DEFAULT_FILE_SLICE_NO_LIMIT_SET = -1
const val DEFAULT_INSTANCE_NAMESPACE = "LibGlobalFetchLib"
const val DEFAULT_HASH_CHECK_ENABLED = false
const val DEFAULT_FILE_EXIST_CHECKS = true
const val DEFAULT_ENABLE_LISTENER_NOTIFY_ON_ATTACHED = false
const val DEFAULT_ENABLE_LISTENER_NOTIFY_ON_REQUEST_UPDATED = true
const val DEFAULT_ENABLE_LISTENER_AUTOSTART_ON_ATTACHED = false
const val DEFAULT_DOWNLOAD_ON_ENQUEUE = true
val defaultNetworkType = NetworkType.ALL
val defaultGlobalNetworkType = NetworkType.GLOBAL_OFF
val defaultPriority = Priority.NORMAL
val defaultNoError = Error.NONE
val defaultStatus = Status.NONE
val defaultEnqueueAction = EnqueueAction.UPDATE_ACCORDINGLY
val defaultDownloader: Downloader = HttpUrlConnectionDownloader()
val defaultFileServerDownloader: FileServerDownloader = FetchFileServerDownloader()
