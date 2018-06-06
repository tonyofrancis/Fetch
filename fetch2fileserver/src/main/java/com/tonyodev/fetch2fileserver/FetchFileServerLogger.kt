package com.tonyodev.fetch2fileserver

import com.tonyodev.fetch2.FetchLogger

/** Fetch File Server Default Logger*/
open class FetchFileServerLogger(enableLogging: Boolean = true,
                                 tag: String = "FetchFileServerLogger") : FetchLogger(enableLogging, tag)