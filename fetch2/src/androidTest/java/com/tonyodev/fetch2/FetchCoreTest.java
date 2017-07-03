package com.tonyodev.fetch2;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;

/**
 * Created by tonyofrancis on 7/1/17.
 */

@RunWith(AndroidJUnit4.class)
public class FetchTest {

    public static final String TEST_GROUP_ID = "testGroup";
    public static final String TEST_URL = "http://www.tonyofrancis.com/tonyodev/fetch/bunny.m4v";

    @Test
    public void fetchNotNull() {
        initFetch();
        Assert.assertNotNull(Fetch.getInstance());
    }

    @Test
    public void enqueue() {
        initFetch();
        Fetch fetch = Fetch.getInstance();
        Request request = getRequest();
        fetch.enqueue(request);
    }

    @Test
    public void enqueueWithCallback() throws Exception {
        initFetch();
        Fetch fetch = Fetch.getInstance();
        Request request = getRequest();
        fetch.enqueue(request, new Callback() {
            @Override
            public void onQueued(@NonNull Request request) {
            }

            @Override
            public void onFailure(@NonNull Request request, @NonNull Error error) {
                throw new RuntimeException("failed enqueue");
            }
        });
    }

    @Test
    public void enqueueList() {
        initFetch();
        Fetch fetch =  Fetch.getInstance();
        List<Request> list = getRequestList();
        fetch.enqueue(list);
    }

    @Test
    public void enqueueListWithCallback() {
        initFetch();
        Fetch fetch =  Fetch.getInstance();
        List<Request> list = getRequestList();

        fetch.enqueue(list, new Callback() {
            @Override
            public void onQueued(@NonNull Request request) {

            }

            @Override
            public void onFailure(@NonNull Request request, @NonNull Error error) {
                throw new RuntimeException("failed list enqueue");
            }
        });
    }

    @Test
    public void pause() throws Exception {
        initFetch();
        Fetch fetch = Fetch.getInstance();
        final Request request = getRequest();
        ;
        fetch.enqueue(request);
        fetch.pause(request.getId());
        fetch.query(request.getId(), new Query<RequestData>() {
            @Override
            public void onResult(@Nullable RequestData result) {

                Assert.assertNotNull(result);

                if (result.getStatus() != Status.PAUSED) {
                    throw new RuntimeException("Request status should be paused");
                }
            }
        });
    }

    @Test
    public void pauseGroup() throws Exception {
        initFetch();
        Fetch fetch = Fetch.getInstance();
        final List<Request> list = getRequestList();
        fetch.enqueue(list);
        fetch.pauseGroup(TEST_GROUP_ID);
        fetch.queryByGroupId(TEST_GROUP_ID, new Query<List<RequestData>>() {
            @Override
            public void onResult(@Nullable List<RequestData> result) {

                Assert.assertNotNull(result);
                Assert.assertEquals(list.size(),result.size());

                for (RequestData requestData : result) {

                    if (requestData.getStatus() != Status.PAUSED) {
                        throw new RuntimeException("Request status should be paused");
                    }
                }

            }
        });
    }

    @Test
    public void pauseAll() throws Exception {
        initFetch();
        Fetch fetch = Fetch.getInstance();
        final List<Request> list = getRequestList();
        fetch.enqueue(list);
        fetch.pauseAll();
        fetch.queryAll(new Query<List<RequestData>>() {
            @Override
            public void onResult(@Nullable List<RequestData> result) {

                Assert.assertNotNull(result);
                Assert.assertEquals(list.size(),result.size());

                for (RequestData requestData : result) {

                    if (requestData.getStatus() != Status.PAUSED) {
                        throw new RuntimeException("Request status should be paused");
                    }
                }

            }
        });
    }

    @Test
    public void resume() throws Exception {
        initFetch();
        Fetch fetch = Fetch.getInstance();
        final Request request = getRequest();
        fetch.enqueue(request);
        fetch.pause(request.getId());
        fetch.resume(request.getId());
        fetch.query(request.getId(), new Query<RequestData>() {
            @Override
            public void onResult(@Nullable RequestData result) {

                Assert.assertNotNull(result);

                if (result.getStatus() == Status.PAUSED) {
                    throw new RuntimeException("Request status should be resumed");
                }
            }
        });
    }

    @Test
    public void resumeGroup() throws Exception {
        initFetch();
        Fetch fetch = Fetch.getInstance();
        final List<Request> list = getRequestList();
        fetch.enqueue(list);
        fetch.pauseGroup(TEST_GROUP_ID);
        fetch.resumeGroup(TEST_GROUP_ID);
        fetch.queryByGroupId(TEST_GROUP_ID, new Query<List<RequestData>>() {
            @Override
            public void onResult(@Nullable List<RequestData> result) {

                Assert.assertNotNull(result);
                Assert.assertEquals(list.size(),result.size());

                for (RequestData requestData : result) {

                    if (requestData.getStatus() == Status.PAUSED) {
                        throw new RuntimeException("Request status should be resumed");
                    }
                }

            }
        });
    }

    @Test
    public void resumeAll() throws Exception {
        initFetch();
        Fetch fetch = Fetch.getInstance();
        final List<Request> list = getRequestList();
        fetch.enqueue(list);
        fetch.pauseAll();
        fetch.resumeAll();
        fetch.queryAll(new Query<List<RequestData>>() {
            @Override
            public void onResult(@Nullable List<RequestData> result) {

                Assert.assertNotNull(result);
                Assert.assertEquals(list.size(),result.size());

                for (RequestData requestData : result) {

                    if (requestData.getStatus() == Status.PAUSED) {
                        throw new RuntimeException("Request status should be resumed");
                    }
                }

            }
        });
    }

    @Test
    public void retry() throws Exception {
        initFetch();
        Fetch fetch = Fetch.getInstance();
        final Request request = getRequest();
        fetch.enqueue(request);
        fetch.pause(request.getId());
        fetch.retry(request.getId());
        fetch.query(request.getId(), new Query<RequestData>() {
            @Override
            public void onResult(@Nullable RequestData result) {

                Assert.assertNotNull(result);

                if (result.getStatus() == Status.PAUSED) {
                    throw new RuntimeException("Request status should be resumed");
                }
            }
        });
    }

    @Test
    public void retryGroup() throws Exception {
        initFetch();
        Fetch fetch = Fetch.getInstance();
        final List<Request> list = getRequestList();
        fetch.enqueue(list);
        fetch.pauseGroup(TEST_GROUP_ID);
        fetch.retryGroup(TEST_GROUP_ID);
        fetch.queryByGroupId(TEST_GROUP_ID, new Query<List<RequestData>>() {
            @Override
            public void onResult(@Nullable List<RequestData> result) {

                Assert.assertNotNull(result);
                Assert.assertEquals(list.size(),result.size());

                for (RequestData requestData : result) {

                    if (requestData.getStatus() == Status.PAUSED) {
                        throw new RuntimeException("Request status should be resumed");
                    }
                }

            }
        });
    }

    @Test
    public void retryAll() throws Exception {
        initFetch();
        Fetch fetch = Fetch.getInstance();
        final List<Request> list = getRequestList();
        fetch.enqueue(list);
        fetch.pauseAll();
        fetch.retryAll();
        fetch.queryAll(new Query<List<RequestData>>() {
            @Override
            public void onResult(@Nullable List<RequestData> result) {

                Assert.assertNotNull(result);
                Assert.assertEquals(list.size(),result.size());

                for (RequestData requestData : result) {

                    if (requestData.getStatus() == Status.PAUSED) {
                        throw new RuntimeException("Request status should be resumed");
                    }
                }

            }
        });
    }

    @Test
    public void cancel() throws Exception {
        initFetch();
        Fetch fetch = Fetch.getInstance();
        final Request request = getRequest();
        fetch.enqueue(request);
        fetch.cancel(request.getId());
        fetch.query(request.getId(), new Query<RequestData>() {
            @Override
            public void onResult(@Nullable RequestData result) {

                Assert.assertNotNull(result);

                if (result.getStatus() != Status.CANCELLED) {
                    throw new RuntimeException("Request status should be cancelled");
                }
            }
        });
    }

    @Test
    public void cancelGroup() throws Exception {
        initFetch();
        Fetch fetch = Fetch.getInstance();
        final List<Request> list = getRequestList();
        fetch.enqueue(list);
        fetch.cancelGroup(TEST_GROUP_ID);
        fetch.queryByGroupId(TEST_GROUP_ID, new Query<List<RequestData>>() {
            @Override
            public void onResult(@Nullable List<RequestData> result) {

                Assert.assertNotNull(result);
                Assert.assertEquals(list.size(),result.size());

                for (RequestData requestData : result) {

                    if (requestData.getStatus() != Status.CANCELLED) {
                        throw new RuntimeException("Request status should be cancelled");
                    }
                }

            }
        });
    }

    @Test
    public void cancelAll() throws Exception {
        initFetch();
        Fetch fetch = Fetch.getInstance();
        final List<Request> list = getRequestList();
        fetch.enqueue(list);
        fetch.cancelAll();
        fetch.queryAll(new Query<List<RequestData>>() {
            @Override
            public void onResult(@Nullable List<RequestData> result) {

                Assert.assertNotNull(result);
                Assert.assertEquals(list.size(),result.size());

                for (RequestData requestData : result) {

                    if (requestData.getStatus() != Status.CANCELLED) {
                        throw new RuntimeException("Request status should be cancelled");
                    }
                }

            }
        });
    }

    private void initFetch() {
        if (!Fetch.isInitialized()) {
            Context appContext = getContext();
            Fetch.init(appContext,new OkHttpClient());
        }

        Fetch.getInstance().deleteAll();
    }

    private Request getRequest() {
        Context appContext = getContext();
        Request request = new Request(TEST_URL,appContext.getCacheDir()+"/video.m4v");
        request.setGroupId(TEST_GROUP_ID);
        return request;
    }

    private List<Request> getRequestList() {
        Context appContext = getContext();
        int size = 100;
        List<Request> list = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            Request request = new Request(TEST_URL,appContext.getCacheDir()+"/video"+i+".m4v");
            request.setGroupId(TEST_GROUP_ID);
            list.add(request);
        }
        return list;
    }

    private Context getContext() {
        return InstrumentationRegistry.getTargetContext();
    }

}
