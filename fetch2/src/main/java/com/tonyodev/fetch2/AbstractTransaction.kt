package com.tonyodev.fetch2

/**
 * Created by tonyofrancis on 6/11/17.
 */

internal abstract class AbstractTransaction<T> : Transaction {

    var value: T? = null
}
