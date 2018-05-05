package com.tonyodev.fetch2.database.migration

import android.arch.persistence.room.migration.Migration


abstract class Migration constructor(startVersion: Int, endVersion: Int)
    : Migration(startVersion, endVersion)