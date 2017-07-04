package com.tonyodev.fetch2.core;

import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import com.tonyodev.fetch2.Error;
import com.tonyodev.fetch2.Request;
import com.tonyodev.fetch2.RequestData;
import com.tonyodev.fetch2.Status;
import com.tonyodev.fetch2.callback.Callback;
import com.tonyodev.fetch2.callback.Query;
import com.tonyodev.fetch2.database.Database;
import com.tonyodev.fetch2.database.DatabaseException;
import com.tonyodev.fetch2.database.DatabaseManager;
import com.tonyodev.fetch2.database.DatabaseRow;
import com.tonyodev.fetch2.database.DatabaseRowConverter;
import com.tonyodev.fetch2.download.DownloadListener;
import com.tonyodev.fetch2.download.DownloadManager;
import com.tonyodev.fetch2.download.Downloadable;
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

    private final Downloadable downloadable;
    private final Database database;
    private final DownloadListener downloadListener;
    private final Handler mainHandler;

    public FetchCore(Context context, OkHttpClient client, DownloadListener downloadListener) {
        this.database = new DatabaseManager(context);
        this.downloadable = new DownloadManager(context,client,downloadListener,database);
        this.downloadListener = downloadListener;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void enqueue(Request request) {
        List<Request> requests = new ArrayList<>(1);
        requests.add(request);
        enqueue(requests);
    }

    @Override
    public void enqueue(final Request request, final Callback callback) {
        List<Request> requests = new ArrayList<>(1);
        requests.add(request);
        enqueue(requests,callback);
    }

    @Override
    public void enqueue(List<Request> requests) {
        insertAndQueue(requests,null);
    }

    @Override
    public void enqueue(final List<Request> requests, final Callback callback) {
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
            final Error err = error;
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    for (Request request : requests) {
                        callback.onFailure(request,err);
                    }
                }
            });
        }
    }

    private void insertAndQueue(final List<Request> requests, final Callback callback) {
        final List<DatabaseRow> rows = new ArrayList<>(requests.size());

        for (Request request : requests) {
            DatabaseRow row = DatabaseRow.newInstance(request.getId(),request.getUrl(),request.getAbsoluteFilePath(),request.getGroupId());
            rows.add(row);
        }

        database.insert(rows);

        if (callback != null) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    for (Request request : requests) {
                        callback.onQueued(request);
                    }
                }
            });
        }

        for (DatabaseRow row : rows) {
            downloadable.resume(row.getId());
        }
    }

    @Override
    public void pause(long... ids) {
        List<DatabaseRow> rows = database.query(ids);
        if (rows != null) {
            List<DatabaseRow> list = new ArrayList<>(ids.length);
            for (DatabaseRow row : rows) {
                if (row != null) {
                    list.add(row);
                }
            }
            pause(list);
        }
    }

    @Override
    public void pauseGroup(String groupId) {
        pause(database.queryByGroupId(groupId));
    }

    @Override
    public void pauseAll() {
        pause(database.query());
    }

    private void pause(List<DatabaseRow> list) {
        if (list != null) {
            for (int i = 0; i < list.size(); i++) {
                DatabaseRow databaseRow = list.get(i);
                if (databaseRow != null && canPause(Status.valueOf(databaseRow.getStatus()))) {
                    final long id = databaseRow.getId();
                    downloadable.pause(databaseRow.getId());
                    database.setStatusAndError(id, Status.PAUSED.getValue(), Error.NONE.getValue());
                    databaseRow = database.query(id);
                    int progress = Utils.calculateProgress(databaseRow.getDownloadedBytes(),databaseRow.getTotalBytes());
                    downloadListener.onPaused(databaseRow.getId(),progress,databaseRow.getDownloadedBytes(),databaseRow.getTotalBytes());
                }
            }
        }
    }

    @Override
    public void resume(long... ids) {
        List<DatabaseRow> rows = database.query(ids);
        if (rows != null) {
            List<DatabaseRow> list = new ArrayList<>(ids.length);
            for (DatabaseRow row : rows) {
                if (row != null) {
                    list.add(row);
                }
            }
            resume(list);
        }
    }

    @Override
    public void resumeGroup(String groupId) {
        resume(database.queryByGroupId(groupId));
    }

    @Override
    public void resumeAll() {
        resume(database.query());
    }

    private void resume(List<DatabaseRow> list) {
        if (list != null) {
            for (int i = 0; i < list.size(); i++) {
                DatabaseRow databaseRow = list.get(i);
                if (databaseRow != null && canResume(Status.valueOf(databaseRow.getStatus()))) {
                    final long id = databaseRow.getId();
                    database.setStatusAndError(id, QUEUED.getValue(),Error.NONE.getValue());
                    downloadable.resume(id);
                }
            }
        }
    }

    @Override
    public void retry(long... ids) {
        resume(ids);
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
    public void cancel(long... ids) {
        List<DatabaseRow> rows= database.query(ids);
        if (rows != null) {
            List<DatabaseRow> list = new ArrayList<>(ids.length);
            for (DatabaseRow row : rows) {
                if (row != null) {
                    list.add(row);
                }
            }
            cancel(list);
        }
    }

    @Override
    public void cancelGroup(String groupId) {
        cancel(database.queryByGroupId(groupId));
    }

    @Override
    public void cancelAll() {
        cancel(database.query());
    }

    private void cancel(List<DatabaseRow> list) {
        if (list != null) {
            for (int i = 0; i < list.size(); i++) {
                DatabaseRow databaseRow = list.get(i);
                if (databaseRow != null && canCancel(Status.valueOf(databaseRow.getStatus()))) {
                    final long id = databaseRow.getId();
                    downloadable.pause(databaseRow.getId());
                    database.setStatusAndError(id,Status.CANCELLED.getValue(),Error.NONE.getValue());
                    databaseRow = database.query(id);
                    int progress = Utils.calculateProgress(databaseRow.getDownloadedBytes(),databaseRow.getTotalBytes());
                    downloadListener.onCancelled(id,progress,databaseRow.getDownloadedBytes(),databaseRow.getTotalBytes());
                }
            }
        }
    }

    @Override
    public void remove(long... ids) {
        List<DatabaseRow> rows = database.query(ids);
        if (rows != null) {
            List<DatabaseRow> list = new ArrayList<>(ids.length);
            for (DatabaseRow row : rows) {
                if (row != null) {
                    list.add(row);
                }
            }
            remove(list);
        }
    }

    @Override
    public void removeGroup(String groupId) {
        remove(database.queryByGroupId(groupId));
    }

    @Override
    public void removeAll() {
        remove(database.query());
    }

    private void remove(List<DatabaseRow> list) {
        if (list != null) {
            for (int i = 0; i < list.size(); i++) {
                DatabaseRow databaseRow = list.get(i);
                if (databaseRow != null) {
                    final long id = databaseRow.getId();
                    downloadable.pause(databaseRow.getId());
                    databaseRow = database.query(id);
                    database.remove(id);
                    int progress = Utils.calculateProgress(databaseRow.getDownloadedBytes(),databaseRow.getTotalBytes());
                    downloadListener.onRemoved(id,progress,databaseRow.getDownloadedBytes(),databaseRow.getTotalBytes());
                }
            }
        }
    }

    @Override
    public void delete(long... ids) {
        List<DatabaseRow> rows = database.query(ids);
        if (rows != null) {
            List<DatabaseRow> list = new ArrayList<>(ids.length);
            for (DatabaseRow row : rows) {
                if (row != null) {
                    list.add(row);
                }
            }
            remove(list);
            deleteRowFiles(list);
        }
    }

    @Override
    public void deleteGroup(final String groupId) {
        List<DatabaseRow> list = database.queryByGroupId(groupId);
        if (list != null) {
            remove(list);
            deleteRowFiles(list);
        }
    }

    @Override
    public void deleteAll() {
        List<DatabaseRow> list = database.query();
        if (list != null) {
            remove(list);
            deleteRowFiles(list);
        }
    }

    private void deleteRowFiles(List<DatabaseRow> list) {
        for (DatabaseRow databaseRow : list) {
            File file = new File(databaseRow.getAbsoluteFilePath());
            if (file.exists()) {
                file.delete();
            }
        }
    }

    @Override
    public void query(long id, final Query<RequestData> query) {
        final RequestData requestData = DatabaseRowConverter.toRequestData(database.query(id));
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                query.onResult(requestData);
            }
        });
    }

    @Override
    public void query(List<Long> ids, final Query<List<RequestData>> query) {
        final List<RequestData> results = DatabaseRowConverter.toRequestDataList(database.query(Utils.createIdArray(ids)));
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                query.onResult(results);
            }
        });
    }

    @Override
    public void queryAll(final Query<List<RequestData>> query) {
        final List<RequestData> result = DatabaseRowConverter.toRequestDataList(database.query());
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                query.onResult(result);
            }
        });
    }

    @Override
    public void queryByStatus(Status status, final Query<List<RequestData>> query) {
        final List<RequestData> result = DatabaseRowConverter.toRequestDataList(database.queryByStatus(status.getValue()));
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                query.onResult(result);
            }
        });
    }

    @Override
    public void queryByGroupId(String groupId, final Query<List<RequestData>> query) {
        final List<RequestData> result = DatabaseRowConverter.toRequestDataList(database.queryByGroupId(groupId));
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                query.onResult(result);
            }
        });
    }

    @Override
    public void queryGroupByStatusId(String groupId, Status status, final Query<List<RequestData>> query) {
        final List<RequestData> result = DatabaseRowConverter.toRequestDataList(database.queryGroupByStatusId(groupId,status.getValue()));
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                query.onResult(result);
            }
        });
    }

    @Override
    public void contains(long id, final @NonNull Query<Boolean> query) {
        final boolean found = database.contains(id);
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                query.onResult(found);
            }
        });
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
}
