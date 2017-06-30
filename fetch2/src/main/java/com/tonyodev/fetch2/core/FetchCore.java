package com.tonyodev.fetch2.core;

import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;
import android.support.annotation.NonNull;

import com.tonyodev.fetch2.Callback;
import com.tonyodev.fetch2.Error;
import com.tonyodev.fetch2.Query;
import com.tonyodev.fetch2.Request;
import com.tonyodev.fetch2.RequestData;
import com.tonyodev.fetch2.Status;
import com.tonyodev.fetch2.database.Database;
import com.tonyodev.fetch2.database.DatabaseException;
import com.tonyodev.fetch2.database.DatabaseManager;
import com.tonyodev.fetch2.database.DatabaseRow;
import com.tonyodev.fetch2.database.ReadDatabase;
import com.tonyodev.fetch2.database.Transaction;
import com.tonyodev.fetch2.download.DownloadManager;
import com.tonyodev.fetch2.download.DownloadListener;
import com.tonyodev.fetch2.util.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;

import static com.tonyodev.fetch2.Status.QUEUED;


/**
 * Created by tonyofrancis on 6/28/17.
 */

public final class FetchCore implements Fetchable {

    private final DownloadManager downloadManager;
    private final DatabaseManager databaseManager;
    private final DownloadListener downloadListener;
    private final ReadDatabase readDatabase;

    public FetchCore(Context context, OkHttpClient client, DownloadListener downloadListener) {
        this.databaseManager = new DatabaseManager(context);
        this.downloadManager = new DownloadManager(context,client,downloadListener,databaseManager);
        this.downloadListener = downloadListener;
        this.readDatabase = databaseManager.getReadDatabase();
    }

    @Override
    public void enqueue(Request request) {
        insertAndQueue(request,null);
    }

    @Override
    public void enqueue(Request request, Callback callback) {
        Error error = Error.NONE;
        try {
            insertAndQueue(request,callback);
        }catch (DatabaseException e) {
            error = Error.REQUEST_NOT_FOUND_IN_DATABASE;
        }
        catch (SQLiteConstraintException e) {
            error = Error.REQUEST_ALREADY_EXIST;
        }
        if(error.getValue() != Error.NONE.getValue()) {
            callback.onFailure(request,error);
        }
    }

    private void insertAndQueue(Request request, Callback callback) {
        long id = request.getId();
        final DatabaseRow row = DatabaseRow.newInstance(request.getId(),request.getUrl(),request.getAbsoluteFilePath(),request.getGroupId());
        databaseManager.executeTransaction(new Transaction() {
            @Override
            public void onExecute(Database database) {
                database.insert(row);
            }
        });

        if (callback != null) {
            callback.onQueued(request);
        }
        downloadManager.resume(id);
    }

    @Override
    public void enqueue(List<Request> requests) {
        insertAndQueue(requests,null);
    }

    @Override
    public void enqueue(List<Request> requests, Callback callback) {
        Error error = Error.NONE;
        try {
            insertAndQueue(requests, callback);
        }catch (DatabaseException e) {
            error = Error.REQUEST_NOT_FOUND_IN_DATABASE;
        }
        catch (SQLiteConstraintException e) {
            error = Error.REQUEST_ALREADY_EXIST;
        }
        if(error.getValue() != Error.NONE.getValue()) {
            for (final Request request : requests) {
                callback.onFailure(request, error);
            }
        }
    }

    private void insertAndQueue(List<Request> requests, Callback callback) {
        final List<DatabaseRow> rows = new ArrayList<>(requests.size());

        for (Request request : requests) {
            DatabaseRow row = DatabaseRow.newInstance(request.getId(),request.getUrl(),request.getAbsoluteFilePath(),request.getGroupId());
            rows.add(row);
        }

        databaseManager.executeTransaction(new Transaction() {
            @Override
            public void onExecute(Database database) {
                database.insert(rows);
            }
        });

        if (callback != null) {
            for (Request request : requests) {
                callback.onQueued(request);
            }
        }

        for (DatabaseRow row : rows) {
            downloadManager.resume(row.getId());
        }
    }

    @Override
    public void pause(long id) {
        RequestData requestData = readDatabase.query(id);
        if (requestData != null) {
            List<RequestData> list = new ArrayList<>(1);
            list.add(requestData);
            pause(list);
        }
    }

    @Override
    public void pauseGroup(String groupId) {
        pause(readDatabase.queryByGroupId(groupId));
    }

    @Override
    public void pauseAll() {
        pause(readDatabase.query());
    }

    private void pause(List<RequestData> list) {
        if (list != null) {
            for (int i = 0; i < list.size(); i++) {
                RequestData requestData = list.get(i);
                if (requestData != null && canPause(requestData.getStatus())) {
                    final long id = requestData.getId();
                    downloadManager.pause(requestData.getId());
                    databaseManager.executeTransaction(new Transaction() {
                        @Override
                        public void onExecute(Database database) {
                            database.setStatusAndError(id, Status.PAUSED.getValue(), Error.NONE.getValue());
                        }
                    });
                    requestData = readDatabase.query(id);
                    downloadListener.onPaused(requestData.getId(),requestData.getProgress(),requestData.getDownloadedBytes(),requestData.getTotalBytes());
                }
            }
        }
    }

    @Override
    public void resume(long id) {
        RequestData requestData = readDatabase.query(id);
        if (requestData != null) {
            List<RequestData> list = new ArrayList<>(1);
            list.add(requestData);
            resume(list);
        }
    }

    @Override
    public void resumeGroup(String groupId) {
        resume(readDatabase.queryByGroupId(groupId));
    }

    @Override
    public void resumeAll() {
        resume(readDatabase.query());
    }

    private void resume(List<RequestData> list) {
        if (list != null) {
            for (int i = 0; i < list.size(); i++) {
                RequestData requestData = list.get(i);
                if (requestData != null && canResume(requestData.getStatus())) {
                    final long id = requestData.getId();
                    databaseManager.executeTransaction(new Transaction() {
                        @Override
                        public void onExecute(Database database) {
                            database.setStatusAndError(id, QUEUED.getValue(),Error.NONE.getValue());
                        }
                    });
                    downloadManager.resume(id);
                }
            }
        }
    }

    @Override
    public void retry(long id) {
        resume(id);
    }

    @Override
    public void retryGroup(String id) {
        resumeGroup(id);
    }

    @Override
    public void retryAll() {
        resumeAll();
    }

    @Override
    public void cancel(long id) {
        RequestData requestData = readDatabase.query(id);
        if (requestData != null) {
            List<RequestData> list = new ArrayList<>(1);
            list.add(requestData);
            resume(list);
        }
    }

    @Override
    public void cancelGroup(String groupId) {
        cancel(readDatabase.queryByGroupId(groupId));
    }

    @Override
    public void cancelAll() {
        cancel(readDatabase.query());
    }

    private void cancel(List<RequestData> list) {
        if (list != null) {
            for (int i = 0; i < list.size(); i++) {
                RequestData requestData = list.get(i);
                final long id = requestData.getId();
                if (requestData != null && canCancel(requestData.getStatus())) {
                    downloadManager.pause(requestData.getId());
                    databaseManager.executeTransaction(new Transaction() {
                        @Override
                        public void onExecute(Database database) {
                            database.setStatusAndError(id,Status.CANCELLED.getValue(),Error.NONE.getValue());
                        }
                    });
                    requestData = readDatabase.query(id);
                    downloadListener.onCancelled(id,requestData.getProgress(),requestData.getDownloadedBytes(),requestData.getTotalBytes());
                }
            }
        }
    }

    @Override
    public void remove(long id) {
        RequestData requestData = readDatabase.query(id);
        if (requestData != null) {
            List<RequestData> list = new ArrayList<>(1);
            list.add(requestData);
            resume(list);
        }
    }

    @Override
    public void removeGroup(String groupId) {
        remove(readDatabase.queryByGroupId(groupId));
    }

    @Override
    public void removeAll() {
        remove(readDatabase.query());
    }

    private void remove(List<RequestData> list) {
        if (list != null) {
            for (int i = 0; i < list.size(); i++) {
                RequestData requestData = list.get(i);
                final long id = requestData.getId();
                if (requestData != null) {
                    downloadManager.pause(requestData.getId());
                    requestData = readDatabase.query(id);
                    databaseManager.executeTransaction(new Transaction() {
                        @Override
                        public void onExecute(Database database) {
                            database.remove(id);
                        }
                    });
                    downloadListener.onRemoved(id,requestData.getProgress(),requestData.getDownloadedBytes(),requestData.getTotalBytes());
                }
            }
        }
    }

    @Override
    public void delete(long id) {
        RequestData requestData = readDatabase.query(id);
        if (requestData != null) {
            List<RequestData> list = new ArrayList<>(1);
            list.add(requestData);
            remove(list);
            deleteFile(requestData.getAbsoluteFilePath());
        }
    }

    @Override
    public void deleteGroup(final String groupId) {
        List<RequestData> list = readDatabase.queryByGroupId(groupId);
        if (list != null) {
            remove(list);
            for (RequestData requestData : list) {
                deleteFile(requestData.getAbsoluteFilePath());
            }
        }
    }

    @Override
    public void deleteAll() {
        List<RequestData> list = readDatabase.query();
        if (list != null) {
            remove(list);
            for (RequestData requestData : list) {
                deleteFile(requestData.getAbsoluteFilePath());
            }
        }
    }

    @Override
    public void query(long id, Query<RequestData> query) {
        final RequestData requestData = readDatabase.query(id);
        query.onResult(requestData);
    }

    @Override
    public void query(List<Long> ids, Query<List<RequestData>> query) {
        final List<RequestData> results = readDatabase.query(Utils.createIdArray(ids));
        query.onResult(results);
    }

    @Override
    public void queryAll(Query<List<RequestData>> query) {
        final List<RequestData> result = readDatabase.query();
        query.onResult(result);
    }

    @Override
    public void queryByStatus(Status status, Query<List<RequestData>> query) {
        final List<RequestData> result = readDatabase.queryByStatus(status.getValue());
        query.onResult(result);
    }

    @Override
    public void queryByGroupId(String groupId, Query<List<RequestData>> query) {
        final List<RequestData> result = readDatabase.queryByGroupId(groupId);
        query.onResult(result);
    }

    @Override
    public void queryGroupByStatusId(String groupId, Status status, Query<List<RequestData>> query) {
        final List<RequestData> result = readDatabase.queryGroupByStatusId(groupId,status.getValue());
        query.onResult(result);
    }

    @Override
    public void contains(long id, @NonNull Query<Boolean> query) {
        final boolean found = readDatabase.contains(id);
        query.onResult(found);
    }

    private boolean canPause(Status currentStatus) {
        switch (currentStatus) {
            case DOWNLOADING:
            case QUEUED:
                return true;
        }
        return false;
    }

    private boolean canResume(Status currentStatus) {
        switch (currentStatus) {
            case PAUSED:
            case QUEUED:
            case ERROR:
            case CANCELLED:
                return true;
        }
        return false;
    }

    private boolean canCancel(Status currentStatus) {
        switch (currentStatus) {
            case PAUSED:
            case QUEUED:
            case ERROR:
            case DOWNLOADING:
                return true;
        }
        return false;
    }

    private void deleteFile(String file) {
        File file1 = new File(file);
        if (file1.exists()) {
            file1.delete();
        }
    }
}
