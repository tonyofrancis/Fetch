package com.tonyodev.fetch2;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.tonyodev.fetch2.core.FetchCore;
import com.tonyodev.fetch2.download.DownloadListener;
import com.tonyodev.fetch2.util.NetworkUtils;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import okhttp3.OkHttpClient;

/**
 * Created by tonyofrancis on 7/1/17.
 */

@RunWith(AndroidJUnit4.class)
public class FetchCoreTest {

    public static final String TEST_GROUP_ID = "testGroup";
    public static final String TEST_URL = "http://www.tonyofrancis.com/tonyodev/fetchCore/bunny.m4v";

    private FetchCore fetchCore;


    @Before
    public void setUp() throws Exception {

        OkHttpClient client = NetworkUtils.okHttpClient();
        Context context = getContext();

        fetchCore = new FetchCore(context, client, new DownloadListener() {
            @Override
            public void onComplete(long id, int progress, long downloadedBytes, long totalBytes) {

            }

            @Override
            public void onError(long id, @NonNull Error error, int progress, long downloadedBytes, long totalBytes) {

            }

            @Override
            public void onProgress(long id, int progress, long downloadedBytes, long totalBytes) {

            }

            @Override
            public void onPaused(long id, int progress, long downloadedBytes, long totalBytes) {

            }

            @Override
            public void onCancelled(long id, int progress, long downloadedBytes, long totalBytes) {

            }

            @Override
            public void onRemoved(long id, int progress, long downloadedBytes, long totalBytes) {

            }
        });

        fetchCore.deleteAll();
    }

    @Test
    public void fetchCoreNotNull() {
        Assert.assertNotNull(fetchCore);
    }

    @Test
    public void enqueue() {
        clearDatabase();
        Request request = getRequest();
        fetchCore.enqueue(request);
    }

    @Test
    public void enqueueWithCallback() throws Exception {
        clearDatabase();
        Request request = getRequest();
        fetchCore.enqueue(request, new Callback() {
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
        clearDatabase();
        List<Request> list = getRequestList();
        fetchCore.enqueue(list);
    }

    @Test
    public void enqueueListWithCallback() {
        clearDatabase();
        List<Request> list = getRequestList();
        fetchCore.enqueue(list, new Callback() {
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
        clearDatabase();
        final Request request = getRequest();
        fetchCore.enqueue(request);
        fetchCore.pause(request.getId());
        fetchCore.query(request.getId(), new Query<RequestData>() {
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
        clearDatabase();
        final List<Request> list = getRequestList();
        fetchCore.enqueue(list);
        fetchCore.pauseGroup(TEST_GROUP_ID);
        fetchCore.queryByGroupId(TEST_GROUP_ID, new Query<List<RequestData>>() {
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
        clearDatabase();
        final List<Request> list = getRequestList();
        fetchCore.enqueue(list);
        fetchCore.pauseAll();
        fetchCore.queryAll(new Query<List<RequestData>>() {
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
        clearDatabase();
        final Request request = getRequest();
        fetchCore.enqueue(request);
        fetchCore.pause(request.getId());
        fetchCore.resume(request.getId());
        fetchCore.query(request.getId(), new Query<RequestData>() {
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
        clearDatabase();
        final List<Request> list = getRequestList();
        fetchCore.enqueue(list);
        fetchCore.pauseGroup(TEST_GROUP_ID);
        fetchCore.resumeGroup(TEST_GROUP_ID);
        fetchCore.queryByGroupId(TEST_GROUP_ID, new Query<List<RequestData>>() {
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
        clearDatabase();
        final List<Request> list = getRequestList();
        fetchCore.enqueue(list);
        fetchCore.pauseAll();
        fetchCore.resumeAll();
        fetchCore.queryAll(new Query<List<RequestData>>() {
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
        clearDatabase();
        final Request request = getRequest();
        fetchCore.enqueue(request);
        fetchCore.pause(request.getId());
        fetchCore.retry(request.getId());
        fetchCore.query(request.getId(), new Query<RequestData>() {
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
        clearDatabase();
        final List<Request> list = getRequestList();
        fetchCore.enqueue(list);
        fetchCore.pauseGroup(TEST_GROUP_ID);
        fetchCore.retryGroup(TEST_GROUP_ID);
        fetchCore.queryByGroupId(TEST_GROUP_ID, new Query<List<RequestData>>() {
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
        clearDatabase();
        final List<Request> list = getRequestList();
        fetchCore.enqueue(list);
        fetchCore.pauseAll();
        fetchCore.retryAll();
        fetchCore.queryAll(new Query<List<RequestData>>() {
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
        clearDatabase();
        final Request request = getRequest();
        fetchCore.enqueue(request);
        fetchCore.cancel(request.getId());
        fetchCore.query(request.getId(), new Query<RequestData>() {
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
        clearDatabase();
        final List<Request> list = getRequestList();
        fetchCore.enqueue(list);
        fetchCore.cancelGroup(TEST_GROUP_ID);
        fetchCore.queryByGroupId(TEST_GROUP_ID, new Query<List<RequestData>>() {
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
        clearDatabase();
        final List<Request> list = getRequestList();
        fetchCore.enqueue(list);
        fetchCore.cancelAll();
        fetchCore.queryAll(new Query<List<RequestData>>() {
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
    public void remove() throws Exception {
        clearDatabase();
        final Request request = getRequest();
        fetchCore.enqueue(request);
        fetchCore.remove(request.getId());
        fetchCore.query(request.getId(), new Query<RequestData>() {
            @Override
            public void onResult(@Nullable RequestData result) {
                Assert.assertNull(result);
            }
        });
    }

    @Test
    public void removeGroup() throws Exception {
        clearDatabase();
        final List<Request> list = getRequestList();
        fetchCore.enqueue(list);
        fetchCore.removeGroup(TEST_GROUP_ID);
        fetchCore.queryByGroupId(TEST_GROUP_ID, new Query<List<RequestData>>() {
            @Override
            public void onResult(@Nullable List<RequestData> result) {

                if (result != null && result.size() > 0) {
                    throw new RuntimeException("Requests were not removed");
                }
            }
        });
    }

    @Test
    public void removeAll() throws Exception {
        clearDatabase();
        final List<Request> list = getRequestList();
        fetchCore.enqueue(list);
        fetchCore.removeAll();
        fetchCore.queryAll(new Query<List<RequestData>>() {
            @Override
            public void onResult(@Nullable List<RequestData> result) {

                if (result != null && result.size() > 0) {
                    throw new RuntimeException("Requests were not removed");
                }
            }
        });
    }

    @Test
    public void delete() throws Exception {
        clearDatabase();
        final Request request = getRequest();
        fetchCore.enqueue(request);
        fetchCore.delete(request.getId());
        fetchCore.query(request.getId(), new Query<RequestData>() {
            @Override
            public void onResult(@Nullable RequestData result) {
                Assert.assertNull(result);
            }
        });

        Assert.assertFalse(fileExist(request.getAbsoluteFilePath()));
    }

    @Test
    public void deleteGroup() throws Exception {
        clearDatabase();
        final List<Request> list = getRequestList();
        fetchCore.enqueue(list);
        fetchCore.deleteGroup(TEST_GROUP_ID);
        fetchCore.queryByGroupId(TEST_GROUP_ID, new Query<List<RequestData>>() {
            @Override
            public void onResult(@Nullable List<RequestData> result) {

                if (result != null && result.size() > 0) {
                    throw new RuntimeException("Requests were not removed");
                }
            }
        });

        for (Request request : list) {
            Assert.assertFalse(fileExist(request.getAbsoluteFilePath()));
        }
    }

    @Test
    public void deleteAll() throws Exception {
        clearDatabase();
        final List<Request> list = getRequestList();
        fetchCore.enqueue(list);
        fetchCore.deleteAll();
        fetchCore.queryAll(new Query<List<RequestData>>() {
            @Override
            public void onResult(@Nullable List<RequestData> result) {

                if (result != null && result.size() > 0) {
                    throw new RuntimeException("Requests were not removed");
                }
            }
        });

        for (Request request : list) {
            Assert.assertFalse(fileExist(request.getAbsoluteFilePath()));
        }
    }

    @Test
    public void query() throws Exception {
        clearDatabase();
        final Request request = getRequest();
        fetchCore.enqueue(request);
        fetchCore.query(request.getId(), new Query<RequestData>() {
            @Override
            public void onResult(@Nullable RequestData result) {
                Assert.assertNotNull(result);
            }
        });
    }

    @Test
    public void queryList() throws Exception {
        clearDatabase();
        final List<Request> list = getRequestList();
        List<Long> ids = new ArrayList<>();

        for (Request request : list) {
            ids.add(request.getId());
        }

        fetchCore.enqueue(list);
        fetchCore.query(ids, new Query<List<RequestData>>() {
            @Override
            public void onResult(@Nullable List<RequestData> result) {
                Assert.assertNotNull(result);
                Assert.assertEquals(list.size(),result.size());
                Set<Request> set = new HashSet<>(list);
                for (RequestData requestData : result) {
                    Assert.assertTrue(set.contains(requestData.getRequest()));
                }
            }
        });
    }

    @Test
    public void queryAll() throws Exception {
        clearDatabase();
        final List<Request> list = getRequestList();
        fetchCore.enqueue(list);
        fetchCore.queryAll(new Query<List<RequestData>>() {
            @Override
            public void onResult(@Nullable List<RequestData> result) {
                Assert.assertNotNull(result);
                Assert.assertEquals(list.size(),result.size());
                Set<Request> set = new HashSet<>(list);
                for (RequestData requestData : result) {
                    Assert.assertTrue(set.contains(requestData.getRequest()));
                }
            }
        });
    }

    @Test
    public void queryByStatus() throws Exception {
        clearDatabase();
        final List<Request> list = getRequestList();
        final List<Long> pausedIdList = new ArrayList<>();

        for (int i = 0; i < list.size(); i += 2) {
            pausedIdList.add(list.get(i).getId());
        }

        fetchCore.enqueue(list);

        for (Long id : pausedIdList) {
            fetchCore.pause(id);
        }

        fetchCore.queryByStatus(Status.PAUSED, new Query<List<RequestData>>() {
            @Override
            public void onResult(@Nullable List<RequestData> result) {
                Assert.assertNotNull(result);
                Assert.assertEquals(pausedIdList.size(),result.size());

                Set<Long> setId = new HashSet<>(pausedIdList);

                for (RequestData requestData : result) {
                    Assert.assertTrue(setId.contains(requestData.getId()));
                }
            }
        });
    }

    @Test
    public void queryByGroupId() throws Exception {
        clearDatabase();
        final List<Request> list = getRequestList();
        fetchCore.enqueue(list);
        fetchCore.queryByGroupId(TEST_GROUP_ID, new Query<List<RequestData>>() {
            @Override
            public void onResult(@Nullable List<RequestData> result) {
                Assert.assertNotNull(result);
                Assert.assertEquals(list.size(),result.size());
                Set<Request> requests = new HashSet<>(list);

                for (RequestData requestData : result) {
                    Assert.assertTrue(requests.contains(requestData.getRequest()));
                }
            }
        });
    }

    @Test
    public void queryGroupByStatusId() throws Exception {
        clearDatabase();
        final List<Request> list = getRequestList();
        fetchCore.enqueue(list);
        fetchCore.cancelGroup(TEST_GROUP_ID);
        fetchCore.queryGroupByStatusId(TEST_GROUP_ID, Status.CANCELLED, new Query<List<RequestData>>() {
            @Override
            public void onResult(@Nullable List<RequestData> result) {
                Assert.assertNotNull(result);
                Assert.assertEquals(list.size(),result.size());
                Set<Request> requests = new HashSet<>(list);
                for (RequestData requestData : result) {
                    Assert.assertTrue(requests.contains(requestData.getRequest()));
                }
            }
        });
    }

    @Test
    public void contains() {
        clearDatabase();
        Request request = getRequest();
        fetchCore.enqueue(request);
        fetchCore.pause(request.getId());
        fetchCore.contains(request.getId(), new Query<Boolean>() {
            @Override
            public void onResult(@Nullable Boolean result) {
                Assert.assertNotNull(result);
                Assert.assertTrue(result);
            }
        });
    }

    @Test
    public void containsNot() {
        clearDatabase();
        Request request = getRequest();
        fetchCore.enqueue(request);
        fetchCore.pause(request.getId());
        fetchCore.delete(request.getId());
        fetchCore.contains(request.getId(), new Query<Boolean>() {
            @Override
            public void onResult(@Nullable Boolean result) {
                Assert.assertNotNull(result);
                Assert.assertFalse(result);
            }
        });
    }

    private boolean fileExist(String file) {
        return new File(file).exists();
    }

    private Request getRequest() {
        Context appContext = getContext();
        Request request = new Request(TEST_URL,appContext.getCacheDir()+"/video.m4v");
        request.setGroupId(TEST_GROUP_ID);
        return request;
    }

    private List<Request> getRequestList() {
        Context appContext = getContext();
        int size = 10;
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

    private void clearDatabase() {
        fetchCore.deleteAll();
    }
}
