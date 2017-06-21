package com.tonyodev.fetch2;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.util.ArrayMap;
import android.support.v4.util.ArraySet;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;


public final class Fetch extends FetchCore {

    private static ConcurrentHashMap<String,Fetch> pool = new ConcurrentHashMap<>();

    private final String name;
    private final DatabaseManager databaseManager;
    private final DownloadManager downloadManager;
    private final Handler mainHandler;
    private final ExecutorService executor;
    private final Set<WeakReference<FetchListener>> listeners;
    private volatile boolean isDisposed;

    @NonNull
    public static Fetch getDefaultInstance(@NonNull Context context) {

        String defaultName = FetchHelper.getDefaultDatabaseName();
        if (pool.containsKey(defaultName)) {
            return pool.get(defaultName);
        }

        Fetch fetch = new Builder(context).build();
        pool.put(defaultName,fetch);
        return fetch;
    }

    public static class Builder {
        private String name;
        private OkHttpClient client;
        private Context context;

        public Builder(@NonNull Context context) {
            this(context,FetchHelper.getDefaultDatabaseName());
        }

        public Builder(@NonNull Context context, @NonNull String name) {
            FetchHelper.throwIfContextIsNull(context);
            FetchHelper.throwIfFetchNameIsNullOrEmpty(name);
            this.name = name;
            this.context = context.getApplicationContext();
            this.client = NetworkUtils.okHttpClient();
        }

        @NonNull
        public Builder name(@NonNull String name) {
            FetchHelper.throwIfFetchNameIsNullOrEmpty(name);
            this.name = name;
            return this;
        }

        @NonNull
        public Builder client(@NonNull OkHttpClient client) {
            FetchHelper.throwIfClientIsNull(client);
            this.client = client;
            return this;
        }

        @NonNull
        public Fetch build() {

            if (pool.containsKey(name)) {
                return pool.get(name);
            }

            Fetch fetch = new Fetch(this);
            pool.put(name,fetch);
            return fetch;
        }
    }

    private Fetch(Builder builder) {
        this.isDisposed = false;
        this.listeners = new ArraySet<>();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.executor = Executors.newSingleThreadExecutor();

        this.name = builder.name;
        this.databaseManager = DatabaseManager.newInstance(builder.context.getApplicationContext(),name);
        this.downloadManager = DownloadManager.newInstance(builder.context.getApplicationContext(),databaseManager,
                builder.client,getDownloadListener(),actionProcessor);
    }

    private final ActionProcessor<Runnable> actionProcessor = new ActionProcessor<Runnable>() {

        private final ConcurrentLinkedQueue<Runnable> queue = new ConcurrentLinkedQueue<>();

        @Override
        public synchronized void queueAction(Runnable action) {
            boolean wasEmpty = queue.isEmpty();

            queue.add(action);

            if (wasEmpty) {
                processNext();
            }
        }

        @Override
        public synchronized void processNext() {
            if(!executor.isShutdown() && !queue.isEmpty()) {
                executor.execute(queue.remove());
            }
        }

        @Override
        public void clearQueue() {
            queue.clear();
        }
    };


    private synchronized void postOnMain(Runnable action) {
        mainHandler.post(action);
    }

    public void download(@NonNull final Request request) {
        FetchHelper.throwIfDisposed(this);
        FetchHelper.throwIfRequestIsNull(request);

        actionProcessor.queueAction(new Runnable() {
            @Override
            public void run() {

                databaseManager.executeTransaction(new AbstractTransaction<Boolean>() {
                    @Override
                    public void onPreExecute() {
                    }

                    @Override
                    public void onExecute(Database database) {
                        Boolean inserted = database.insert(request.getId(),request.getUrl(),request.getAbsoluteFilePath(),request.getGroupId());
                        setValue(inserted);
                    }

                    @Override
                    public void onPostExecute() {
                        if (getValue()) {
                            downloadManager.resume(request.getId());
                        }
                    }
                });
            }
        });
    }

    public void download(@NonNull final Request request, @NonNull final Callback callback) {
        FetchHelper.throwIfDisposed(this);
        FetchHelper.throwIfRequestIsNull(request);
        FetchHelper.throwIfCallbackIsNull(callback);

        actionProcessor.queueAction(new Runnable() {
            @Override
            public void run() {

                databaseManager.executeTransaction(new AbstractTransaction<Boolean>() {
                    @Override
                    public void onPreExecute() {
                    }

                    @Override
                    public void onExecute(Database database) {

                        boolean inserted = database.insert(request.getId(), request.getUrl(), request.getAbsoluteFilePath(),request.getGroupId());
                        setValue(inserted);
                    }

                    @Override
                    public void onPostExecute() {

                        if (getValue()){
                            postOnMain(new Runnable() {
                                @Override
                                public void run() {
                                    callback.onQueued(request);
                                }
                            });

                            downloadManager.resume(request.getId());
                        }else {
                            postOnMain(new Runnable() {
                                @Override
                                public void run() {
                                    callback.onFailure(request,Error.UNKNOWN);
                                }
                            });
                        }
                    }
                });
            }
        });
    }

    public void download(@NonNull final List<Request> requests) {
        FetchHelper.throwIfDisposed(this);
        FetchHelper.throwIfRequestListIsNull(requests);

        actionProcessor.queueAction(new Runnable() {
            @Override
            public void run() {
                databaseManager.executeTransaction(new AbstractTransaction<List<Long>>() {
                    @Override
                    public void onPreExecute() {
                    }

                    @Override
                    public void onExecute(Database database) {

                        List<Long> ids = new ArrayList<>();

                        for (Request request : requests) {
                            if(request != null && database.insert(request.getId(),request.getUrl(),request.getAbsoluteFilePath(),request.getGroupId())) {
                                ids.add(request.getId());
                            }
                        }

                        setValue(ids);
                    }

                    @Override
                    public void onPostExecute() {
                        for (Long id : getValue()) {
                            downloadManager.resume(id);
                        }
                    }
                });
            }
        });
    }

    public void download(@NonNull final List<Request> requests, @NonNull final Callback callback) {
        FetchHelper.throwIfDisposed(this);
        FetchHelper.throwIfRequestListIsNull(requests);
        FetchHelper.throwIfCallbackIsNull(callback);

        actionProcessor.queueAction(new Runnable() {
            @Override
            public void run() {

                databaseManager.executeTransaction(new AbstractTransaction<Map<Request,Boolean>>() {
                    @Override
                    public void onPreExecute() {

                    }

                    @Override
                    public void onExecute(Database database) {

                        Map<Request,Boolean> map = new ArrayMap<>();

                        for (final Request request : requests) {
                            if(request != null) {
                                boolean inserted = database.insert(request.getId(), request.getUrl(), request.getAbsoluteFilePath(),request.getGroupId());
                                map.put(request, inserted);
                            }
                        }
                        setValue(map);
                    }

                    @Override
                    public void onPostExecute() {

                        Set<Request> requests = getValue().keySet();

                        for (final Request request : requests) {

                            if (getValue().get(request)) {

                                downloadManager.resume(request.getId());

                                postOnMain(new Runnable() {
                                    @Override
                                    public void run() {
                                        callback.onQueued(request);
                                    }
                                });

                            } else {
                                postOnMain(new Runnable() {
                                    @Override
                                    public void run() {
                                        callback.onFailure(request,Error.UNKNOWN);
                                    }
                                });
                            }
                        }
                    }
                });
            }
        });
    }

    public void pause(final long id) {
        FetchHelper.throwIfDisposed(this);

        actionProcessor.queueAction(new Runnable() {
            @Override
            public void run() {
                downloadManager.pause(id);
            }
        });
    }

    public void pause(final String groupId) {
        FetchHelper.throwIfDisposed(this);
        FetchHelper.throwIfGroupIDIsNull(groupId);

        actionProcessor.queueAction(new Runnable() {
            @Override
            public void run() {
                downloadManager.pause(groupId);
            }
        });
    }
    
    public void pauseAll() {
        FetchHelper.throwIfDisposed(this);
        actionProcessor.queueAction(new Runnable() {
            @Override
            public void run() {
                downloadManager.pauseAll();
            }
        });
    }

    public void resume(final long id) {
        FetchHelper.throwIfDisposed(this);

        actionProcessor.queueAction(new Runnable() {
            @Override
            public void run() {
                downloadManager.resume(id);
            }
        });
    }

    public void resume(final String groupId) {
        FetchHelper.throwIfDisposed(this);
        FetchHelper.throwIfGroupIDIsNull(groupId);

        actionProcessor.queueAction(new Runnable() {
            @Override
            public void run() {
                downloadManager.resume(groupId);
            }
        });
    }

    public void resumeAll() {
        FetchHelper.throwIfDisposed(this);
        actionProcessor.queueAction(new Runnable() {
            @Override
            public void run() {
                downloadManager.resumeAll();
            }
        });
    }

    public void retry(final long id) {
        FetchHelper.throwIfDisposed(this);

        actionProcessor.queueAction(new Runnable() {
            @Override
            public void run() {
                downloadManager.retry(id);
            }
        });
    }

    public void retry(final String groupId) {
        FetchHelper.throwIfDisposed(this);
        FetchHelper.throwIfGroupIDIsNull(groupId);

        actionProcessor.queueAction(new Runnable() {
            @Override
            public void run() {
                downloadManager.retryGroup(groupId);
            }
        });
    }

    public void cancel(final long id) {
        FetchHelper.throwIfDisposed(this);


        actionProcessor.queueAction(new Runnable() {
            @Override
            public void run() {
                downloadManager.cancel(id);
            }
        });
    }

    public void cancel(final String groupId) {
        FetchHelper.throwIfDisposed(this);
        FetchHelper.throwIfGroupIDIsNull(groupId);

        actionProcessor.queueAction(new Runnable() {
            @Override
            public void run() {
                downloadManager.cancelGroup(groupId);
            }
        });

    }

    public void cancelAll() {
        FetchHelper.throwIfDisposed(this);
        actionProcessor.queueAction(new Runnable() {
            @Override
            public void run() {
                downloadManager.cancelAll();
            }
        });
    }

    public void remove(final long id) {
        FetchHelper.throwIfDisposed(this);

        actionProcessor.queueAction(new Runnable() {
            @Override
            public void run() {
                downloadManager.remove(id);
            }
        });
    }

    public void remove(final String groupId) {
        FetchHelper.throwIfDisposed(this);
        FetchHelper.throwIfGroupIDIsNull(groupId);

        actionProcessor.queueAction(new Runnable() {
            @Override
            public void run() {
                downloadManager.removeGroup(groupId);
            }
        });
    }

    public void removeAll() {
        FetchHelper.throwIfDisposed(this);
        actionProcessor.queueAction(new Runnable() {
            @Override
            public void run() {
                downloadManager.removeAll();
            }
        });
    }

    public void delete(final long id) {
        FetchHelper.throwIfDisposed(this);
        actionProcessor.queueAction(new Runnable() {
            @Override
            public void run() {
                databaseManager.executeTransaction(new AbstractTransaction<RequestData>() {
                    @Override
                    public void onPreExecute() {
                    }

                    @Override
                    public void onExecute(Database database) {
                        RequestData requestData = database.query(id);
                        setValue(requestData);
                    }

                    @Override
                    public void onPostExecute() {
                        if (getValue() != null) {
                            downloadManager.remove(id);
                            File file = new File(getValue().getAbsoluteFilePath());

                            if (file.exists()) {
                                file.delete();
                            }
                        }
                    }
                });
            }
        });

    }

    public void delete(final String groupId) {
        FetchHelper.throwIfDisposed(this);
        FetchHelper.throwIfGroupIDIsNull(groupId);

        actionProcessor.queueAction(new Runnable() {
            @Override
            public void run() {
                databaseManager.executeTransaction(new AbstractTransaction<List<RequestData>>() {
                    @Override
                    public void onPreExecute() {
                    }

                    @Override
                    public void onExecute(Database database) {
                        List<RequestData> requestList = database.queryByGroupId(groupId);
                        setValue(requestList);
                    }

                    @Override
                    public void onPostExecute() {
                        downloadManager.removeGroup(groupId);

                        if (getValue() != null) {
                            for (RequestData requestData : getValue()) {
                                File file = new File(requestData.getAbsoluteFilePath());

                                if (file.exists()) {
                                    file.delete();
                                }
                            }
                        }
                    }
                });
            }
        });

    }

    public void deleteAll() {
        FetchHelper.throwIfDisposed(this);
        actionProcessor.queueAction(new Runnable() {
            @Override
            public void run() {
                databaseManager.executeTransaction(new AbstractTransaction<List<RequestData>>() {

                    @Override
                    public void onPreExecute() {

                    }

                    @Override
                    public void onExecute(Database database) {
                        List<RequestData> result = database.query();
                        setValue(result);
                    }

                    @Override
                    public void onPostExecute() {
                        downloadManager.removeAll();

                        if (getValue() != null) {
                            for (RequestData requestData : getValue()) {
                                File file = new File(requestData.getAbsoluteFilePath());

                                if (file.exists()) {
                                    file.delete();
                                }
                            }
                        }
                    }
                });
            }
        });
    }

    public void query(final long id, @NonNull final Query<RequestData> query) {
        FetchHelper.throwIfDisposed(this);
        FetchHelper.throwIfQueryIsNull(query);

        actionProcessor.queueAction(new Runnable() {
            @Override
            public void run() {

                databaseManager.executeTransaction(new Transaction() {
                    @Override
                    public void onPreExecute() {

                    }

                    @Override
                    public void onExecute(Database database) {
                        final RequestData requestData = database.query(id);
                        postOnMain(new Runnable() {
                            @Override
                            public void run() {
                                query.onResult(requestData);
                            }
                        });
                    }

                    @Override
                    public void onPostExecute() {

                    }
                });
            }
        });
    }

    public void query(@NonNull final List<Long> ids, @NonNull final Query<List<RequestData>> query) {
        FetchHelper.throwIfDisposed(this);
        FetchHelper.throwIfQueryIsNull(query);
        FetchHelper.throwIfIdListIsNull(ids);

        actionProcessor.queueAction(new Runnable() {
            @Override
            public void run() {

                databaseManager.executeTransaction(new Transaction() {

                    @Override
                    public void onPreExecute() {

                    }

                    @Override
                    public void onExecute(Database database) {

                        final List<RequestData> results = database.query(FetchHelper.createIdArray(ids));
                        postOnMain(new Runnable() {
                            @Override
                            public void run() {
                                query.onResult(results);
                            }
                        });
                    }

                    @Override
                    public void onPostExecute() {

                    }
                });
            }
        });
    }

    public void queryAll(@NonNull final Query<List<RequestData>> query) {
        FetchHelper.throwIfDisposed(this);
        FetchHelper.throwIfQueryIsNull(query);

        actionProcessor.queueAction(new Runnable() {
            @Override
            public void run() {
                databaseManager.executeTransaction(new Transaction() {

                    @Override
                    public void onPreExecute() {

                    }

                    @Override
                    public void onExecute(Database database) {
                        final List<RequestData> result = database.query();
                        postOnMain(new Runnable() {
                            @Override
                            public void run() {
                                query.onResult(result);
                            }
                        });
                    }

                    @Override
                    public void onPostExecute() {

                    }
                });
            }
        });
    }

    public void queryByStatus(@NonNull final Status status,@NonNull final Query<List<RequestData>> query) {
        FetchHelper.throwIfDisposed(this);
        FetchHelper.throwIfQueryIsNull(query);
        FetchHelper.throwIfStatusIsNull(status);

        actionProcessor.queueAction(new Runnable() {
            @Override
            public void run() {
                databaseManager.executeTransaction(new Transaction() {

                    @Override
                    public void onPreExecute() {

                    }

                    @Override
                    public void onExecute(Database database) {
                        final List<RequestData> result = database.queryByStatus(status.getValue());
                        postOnMain(new Runnable() {
                            @Override
                            public void run() {
                                query.onResult(result);
                            }
                        });
                    }

                    @Override
                    public void onPostExecute() {

                    }
                });
            }
        });
    }

    public void queryByGroupId(@NonNull final String groupId,@NonNull final Query<List<RequestData>> query) {
        FetchHelper.throwIfDisposed(this);
        FetchHelper.throwIfQueryIsNull(query);
        FetchHelper.throwIfGroupIDIsNull(groupId);

        actionProcessor.queueAction(new Runnable() {
            @Override
            public void run() {
                databaseManager.executeTransaction(new Transaction() {
                    @Override
                    public void onPreExecute() {

                    }

                    @Override
                    public void onExecute(Database database) {
                        final List<RequestData> result = database.queryByGroupId(groupId);
                        postOnMain(new Runnable() {
                            @Override
                            public void run() {
                                query.onResult(result);
                            }
                        });
                    }

                    @Override
                    public void onPostExecute() {

                    }
                });
            }
        });
    }

    public void queryGroupByStatusId(final @NonNull String groupId,final @NonNull Status status, final @NonNull Query<List<RequestData>> query) {
        FetchHelper.throwIfDisposed(this);
        FetchHelper.throwIfGroupIDIsNull(groupId);
        FetchHelper.throwIfStatusIsNull(status);
        FetchHelper.throwIfQueryIsNull(query);

        actionProcessor.queueAction(new Runnable() {
            @Override
            public void run() {
                databaseManager.executeTransaction(new Transaction() {
                    @Override
                    public void onPreExecute() {

                    }

                    @Override
                    public void onExecute(Database database) {
                        final List<RequestData> result = database.queryGroupByStatusId(groupId,status.getValue());
                        postOnMain(new Runnable() {
                            @Override
                            public void run() {
                                query.onResult(result);
                            }
                        });
                    }

                    @Override
                    public void onPostExecute() {

                    }
                });
            }
        });
    }

    public void queryContains(final long id, @NonNull final Query<Boolean> query) {
        FetchHelper.throwIfDisposed(this);
        FetchHelper.throwIfQueryIsNull(query);

        actionProcessor.queueAction(new Runnable() {
            @Override
            public void run() {

                databaseManager.executeTransaction(new Transaction() {

                    @Override
                    public void onPreExecute() {

                    }

                    @Override
                    public void onExecute(Database database) {
                        final boolean found = database.contains(id);
                        postOnMain(new Runnable() {
                            @Override
                            public void run() {
                                query.onResult(found);
                            }
                        });
                    }

                    @Override
                    public void onPostExecute() {

                    }
                });
            }
        });
    }

    public synchronized void addListener(@NonNull FetchListener fetchListener) {
        FetchHelper.throwIfDisposed(this);

        if(fetchListener != null && !containsListener(fetchListener)) {
            fetchListener.onAttach(this);
            listeners.add(new WeakReference<>(fetchListener));
        }
    }

    private boolean containsListener(FetchListener fetchListener) {
        Iterator<WeakReference<FetchListener>> iterator = listeners.iterator();
        WeakReference<FetchListener> ref;

        while (iterator.hasNext()) {
            ref = iterator.next();

            if (ref.get() != null && ref.get() == fetchListener){
                return true;
            }
        }

        return false;
    }

    public synchronized void removeListener(@NonNull FetchListener fetchListener) {
        FetchHelper.throwIfDisposed(this);

        if (fetchListener != null) {

            Iterator<WeakReference<FetchListener>> iterator = listeners.iterator();
            WeakReference<FetchListener> ref;

            while (iterator.hasNext()) {
                ref = iterator.next();

                if (ref.get() != null && ref.get() == fetchListener){
                    iterator.remove();
                    fetchListener.onDetach(this);
                    break;
                }
            }
        }
    }

    public synchronized void removeListeners() {
        FetchHelper.throwIfDisposed(this);

        Iterator<WeakReference<FetchListener>> iterator = listeners.iterator();
        WeakReference<FetchListener> ref;

        while(iterator.hasNext()) {
            ref = iterator.next();
            iterator.remove();

            if (ref.get() != null) {
                ref.get().onDetach(this);
            }
        }
    }

    @NonNull
    public String getName() {
        return this.name;
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public synchronized void dispose() {
        if(!isDisposed) {
            removeListeners();
            executor.shutdown();
            actionProcessor.clearQueue();
            downloadManager.dispose();
            databaseManager.dispose();
            isDisposed = true;
            pool.remove(getName());
        }
    }

    @Override
    public boolean isDisposed() {
        return isDisposed;
    }

    private DownloadListener getDownloadListener() {
        return new DownloadListener() {
            @Override
            public void onComplete(final long id,final int progress,final long downloadedBytes,final long totalBytes) {
                postOnMain(new Runnable() {
                    @Override
                    public void run() {
                        for (WeakReference<FetchListener> ref : listeners) {
                            if (ref.get() != null) {
                                ref.get().onComplete(id,progress,downloadedBytes,totalBytes);
                            }
                        }
                    }
                });
            }

            @Override
            public void onError(final long id,@NonNull final  Error error,final int progress,final long downloadedBytes,final long totalBytes) {
                postOnMain(new Runnable() {
                    @Override
                    public void run() {
                        for (WeakReference<FetchListener> ref : listeners) {
                            if(ref.get() != null) {
                                ref.get().onError(id,error,progress,downloadedBytes,totalBytes);
                            }
                        }
                    }
                });
            }

            @Override
            public void onProgress(final long id,final int progress,final long downloadedBytes,final long totalBytes) {
                postOnMain(new Runnable() {
                    @Override
                    public void run() {
                        for (WeakReference<FetchListener> ref : listeners) {
                            if(ref.get() != null) {
                                ref.get().onProgress(id,progress,downloadedBytes,totalBytes);
                            }
                        }
                    }
                });
            }

            @Override
            public void onPause(final long id,final int progress,final long downloadedBytes,final long totalBytes) {
                postOnMain(new Runnable() {
                    @Override
                    public void run() {
                        for (WeakReference<FetchListener> ref : listeners) {
                            if(ref.get() != null) {
                                ref.get().onPause(id,progress,downloadedBytes,totalBytes);
                            }
                        }
                    }
                });
            }

            @Override
            public void onCancelled(final long id,final int progress,final long downloadedBytes,final long totalBytes) {
                postOnMain(new Runnable() {
                    @Override
                    public void run() {
                        for (WeakReference<FetchListener> ref : listeners) {
                            if(ref.get() != null) {
                                ref.get().onCancelled(id,progress,downloadedBytes,totalBytes);
                            }
                        }
                    }
                });
            }

            @Override
            public void onRemoved(final long id,final int progress,final long downloadedBytes,final long totalBytes) {
                postOnMain(new Runnable() {
                    @Override
                    public void run() {
                        for (WeakReference<FetchListener> ref : listeners) {
                            if(ref.get() != null) {
                                ref.get().onRemoved(id,progress,downloadedBytes,totalBytes);
                            }
                        }
                    }
                });
            }
        };
    }
}