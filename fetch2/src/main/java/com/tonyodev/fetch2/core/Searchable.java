package com.tonyodev.fetch2.core;

import com.tonyodev.fetch2.Query;
import com.tonyodev.fetch2.RequestData;
import com.tonyodev.fetch2.Status;

import java.util.List;

/**
 * Created by tonyofrancis on 6/29/17.
 */

public interface Searchable {
    void query(long id, Query<RequestData> query);
    void query(List<Long> ids, Query<List<RequestData>> query);
    void queryAll(Query<List<RequestData>> query);
    void queryByStatus(Status status, final Query<List<RequestData>> query);
    void queryByGroupId(String groupId, final Query<List<RequestData>> query);
    void queryGroupByStatusId(String groupId, Status status, Query<List<RequestData>> query);
    void contains(long id, Query<Boolean> query);
}
