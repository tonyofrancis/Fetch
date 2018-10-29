@file:JvmName("FetchIntent")

package com.tonyodev.fetch2

const val ACTION_NOTIFICATION_ACTION = "com.tonyodev.fetch2.action.NOTIFICATION_ACTION"
const val ACTION_NOTIFICATION_CHECK = "com.tonyodev.fetch2.action.NOTIFICATION_CHECK"
const val ACTION_QUEUE_BACKOFF_RESET = "com.tonyodev.fetch2.action.QUEUE_BACKOFF_RESET"

const val ACTION_TYPE_INVALID = -1
const val ACTION_TYPE_PAUSE = 0
const val ACTION_TYPE_RESUME = 1
const val ACTION_TYPE_DELETE = 2
const val ACTION_TYPE_CANCEL = 4

const val DOWNLOAD_ID_INVALID = -1

const val EXTRA_NAMESPACE = "com.tonyodev.fetch2.extra.NAMESPACE"
const val EXTRA_DOWNLOAD_ID = "com.tonyodev.fetch2.extra.DOWNLOAD_ID"
const val EXTRA_ACTION_TYPE = "com.tonyodev.fetch2.extra.ACTION_TYPE"
const val EXTRA_DOWNLOAD_STATUS = "com.tonyodev.fetch2.extra.DOWNLOAD_STATUS"
