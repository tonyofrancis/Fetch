@file:JvmName("FetchIntent")

package com.tonyodev.fetch2

const val ACTION_QUEUE_BACKOFF_RESET = "com.tonyodev.fetch2.action.QUEUE_BACKOFF_RESET"

const val ACTION_TYPE_INVALID = -1
const val ACTION_TYPE_PAUSE = 0
const val ACTION_TYPE_RESUME = 1
const val ACTION_TYPE_DELETE = 2
const val ACTION_TYPE_CANCEL = 4
const val ACTION_TYPE_RETRY = 5
const val ACTION_TYPE_PAUSE_ALL = 6
const val ACTION_TYPE_RESUME_ALL = 7
const val ACTION_TYPE_CANCEL_ALL = 8
const val ACTION_TYPE_DELETE_ALL = 9
const val ACTION_TYPE_RETRY_ALL = 10

const val DOWNLOAD_ID_INVALID = -1
const val NOTIFICATION_GROUP_ID_INVALID = -1
const val NOTIFICATION_ID_INVALID = -1

const val EXTRA_NAMESPACE = "com.tonyodev.fetch2.extra.NAMESPACE"
const val EXTRA_DOWNLOAD_ID = "com.tonyodev.fetch2.extra.DOWNLOAD_ID"
const val EXTRA_DOWNLOAD_NOTIFICATIONS = "con.tonyodev.fetch2.extra.DOWNLOAD_NOTIFICATIONS"
const val EXTRA_NOTIFICATION_ID = "com.tonyodev.fetch2.extra.NOTIFICATION_ID"
const val EXTRA_NOTIFICATION_GROUP_ID = "com.tonyodev.fetch2.extra.NOTIFICATION_GROUP_ID"
const val EXTRA_ACTION_TYPE = "com.tonyodev.fetch2.extra.ACTION_TYPE"
const val EXTRA_GROUP_ACTION = "com.tonyodev.fetch2.extra.GROUP_ACTION"
