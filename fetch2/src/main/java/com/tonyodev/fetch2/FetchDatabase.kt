package com.tonyodev.fetch2

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase

/**
 * Created by tonyofrancis on 6/14/17.
 */

@Database(entities = arrayOf(RequestInfo::class), version = 1, exportSchema = false)
abstract class FetchDatabase : RoomDatabase() {
    abstract fun requestInfoDao(): RequestInfoDao
}
