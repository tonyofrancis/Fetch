package com.tonyodev.fetch2.database.migration

import android.arch.persistence.db.SupportSQLiteDatabase
import com.tonyodev.fetch2.database.DownloadDatabase
import com.tonyodev.fetch2.util.DEFAULT_UNIQUE_IDENTIFIER

class MigrationThreeToFour : Migration(3, 4) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE '${DownloadDatabase.TABLE_NAME}' "
                + "ADD COLUMN '${DownloadDatabase.COLUMN_IDENTIFIER}' INTEGER NOT NULL DEFAULT $DEFAULT_UNIQUE_IDENTIFIER")
    }

}