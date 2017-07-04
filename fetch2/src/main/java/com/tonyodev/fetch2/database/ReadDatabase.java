package com.tonyodev.fetch2.database;

import java.io.Closeable;
import java.util.List;

/**
 * Created by tonyofrancis on 6/29/17.
 */

public interface ReadDatabase extends Closeable {
    boolean contains(long id);
    List<DatabaseRow> queryByStatus(int status);
    DatabaseRow query(final long id);
    List<DatabaseRow> query();
    List<DatabaseRow> query(long[] ids);
    List<DatabaseRow> queryByGroupId(String groupId);
    List<DatabaseRow> queryGroupByStatusId(String groupId, int status);
}
