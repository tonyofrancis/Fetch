package com.tonyodev.fetch2.core;

import com.tonyodev.fetch2.callback.Callback;
import com.tonyodev.fetch2.callback.Query;
import com.tonyodev.fetch2.Request;
import com.tonyodev.fetch2.RequestData;
import com.tonyodev.fetch2.Status;

import java.util.List;

/**
 * Created by tonyofrancis on 6/29/17.
 */

public interface Fetchable {

    //ACTION METHODS
    void enqueue(Request request);
    void enqueue(Request request,Callback callback);
    void enqueue(List<Request> requests);
    void enqueue(List<Request> requests, Callback callback);
    void pause(long id);
    void pauseGroup(String id);
    void pauseAll();
    void resume(long id);
    void resumeGroup(String id);
    void resumeAll();
    void retry(long id);
    void retryGroup(String id);
    void retryAll();
    void cancel(long id);
    void cancelGroup(String id);
    void cancelAll();
    void remove(long id);
    void removeGroup(String id);
    void removeAll();
    void delete(long id);
    void deleteGroup(String id);
    void deleteAll();

    //QUERY METHODS
    void query(long id, Query<RequestData> query);
    void query(List<Long> ids, Query<List<RequestData>> query);
    void queryAll(Query<List<RequestData>> query);
    void queryByStatus(Status status, final Query<List<RequestData>> query);
    void queryByGroupId(String groupId, final Query<List<RequestData>> query);
    void queryGroupByStatusId(String groupId, Status status, Query<List<RequestData>> query);
    void contains(long id, Query<Boolean> query);
}
