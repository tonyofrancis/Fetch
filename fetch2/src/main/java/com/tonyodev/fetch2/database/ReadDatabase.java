package com.tonyodev.fetch2.database;

import com.tonyodev.fetch2.RequestData;

import java.io.Closeable;
import java.util.List;

/**
 * Created by tonyofrancis on 6/29/17.
 */

public interface ReadDatabase extends Closeable {
    boolean contains(long id);
    List<RequestData> queryByStatus(int status);
    RequestData query(final long id);
    List<RequestData> query();
    List<RequestData> query(long[] ids);
    List<RequestData> queryByGroupId(String groupId);
    List<RequestData> queryGroupByStatusId(String groupId,int status);
}
