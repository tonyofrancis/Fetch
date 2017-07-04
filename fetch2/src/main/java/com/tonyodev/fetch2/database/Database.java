package com.tonyodev.fetch2.database;

/**
 * Created by tonyofrancis on 6/11/17.
 */

public interface Database extends ReadDatabase, WriteDatabase {
    ReadDatabase getReadOnlyDatabase();
    WriteDatabase getWriteOnlyDatabase();
}
