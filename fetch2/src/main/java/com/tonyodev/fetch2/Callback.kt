package com.tonyodev.fetch2

interface Callback {
    fun onQueued(request: Request)
    fun onFailure(request: Request, error: Error)
}
