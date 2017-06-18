package com.tonyodev.fetch2;

import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.Database;

/**
 * Created by tonyofrancis on 6/14/17.
 */

@Database(entities = {RequestInfo.class},version = 1,exportSchema = false)
public abstract class FetchDatabase extends RoomDatabase {
    public abstract RequestInfoDao requestInfoDao();
}
