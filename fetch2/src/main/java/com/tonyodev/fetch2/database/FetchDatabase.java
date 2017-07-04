package com.tonyodev.fetch2.database;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

/**
 * Created by tonyofrancis on 6/14/17.
 */

@Database(entities = {DatabaseRow.class},version = 1,exportSchema = false)
public abstract class FetchDatabase extends RoomDatabase {
    public abstract FetchDao fetchDao();
}
