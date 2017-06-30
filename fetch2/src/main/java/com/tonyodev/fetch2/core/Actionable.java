package com.tonyodev.fetch2.core;

import com.tonyodev.fetch2.Callback;
import com.tonyodev.fetch2.Request;

import java.util.List;

/**
 * Created by tonyofrancis on 6/29/17.
 */

public interface Actionable {
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

}
