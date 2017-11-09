package com.tonyodev.fetch2

/**
 * Created by tonyofrancis on 6/11/17.
 */

internal interface Transaction {
    fun onPreExecute()
    fun onExecute(database: Database)
    fun onPostExecute()
}
