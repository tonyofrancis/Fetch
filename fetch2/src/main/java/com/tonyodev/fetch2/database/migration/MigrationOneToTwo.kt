package com.tonyodev.fetch2.database.migration

import android.arch.persistence.db.SupportSQLiteDatabase
import com.tonyodev.fetch2.database.DownloadDatabase


class MigrationOneToTwo: Migration(1, 2) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE '${DownloadDatabase.TABLE_NAME}' "
                + "ADD COLUMN '${DownloadDatabase.COLUMN_TAG}' TEXT NULL DEFAULT NULL")
    }

}