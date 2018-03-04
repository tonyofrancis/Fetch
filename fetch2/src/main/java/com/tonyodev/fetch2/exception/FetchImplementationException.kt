package com.tonyodev.fetch2.exception

class FetchImplementationException constructor(message: String,
                                               code: Code = Code.NONE) : FetchException(message, code)