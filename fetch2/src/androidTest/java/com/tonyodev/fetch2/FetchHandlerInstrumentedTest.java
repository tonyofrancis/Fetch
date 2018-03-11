package com.tonyodev.fetch2;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.tonyodev.fetch2.database.DatabaseManager;
import com.tonyodev.fetch2.database.DatabaseManagerImpl;
import com.tonyodev.fetch2.database.DownloadDatabase;
import com.tonyodev.fetch2.database.DownloadInfo;
import com.tonyodev.fetch2.database.migration.Migration;
import com.tonyodev.fetch2.downloader.DownloadManager;
import com.tonyodev.fetch2.downloader.DownloadManagerImpl;
import com.tonyodev.fetch2.fetch.FetchHandler;
import com.tonyodev.fetch2.fetch.FetchHandlerImpl;
import com.tonyodev.fetch2.helper.DownloadInfoUpdater;
import com.tonyodev.fetch2.helper.PriorityListProcessor;
import com.tonyodev.fetch2.helper.PriorityListProcessorImpl;
import com.tonyodev.fetch2.provider.DownloadProvider;
import com.tonyodev.fetch2.provider.ListenerProvider;
import com.tonyodev.fetch2.provider.NetworkInfoProvider;
import com.tonyodev.fetch2.util.FetchDefaults;
import com.tonyodev.fetch2.util.FetchTypeConverterExtensions;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class FetchHandlerInstrumentedTest {

    private Context appContext;
    private FetchHandler fetchHandler;
    private DatabaseManager databaseManager;
    private PriorityListProcessor<Download> priorityListProcessorImpl;

    @Before
    public void useAppContext() throws Exception {
        appContext = InstrumentationRegistry.getTargetContext();
        final HandlerThread handlerThread = new HandlerThread("test");
        handlerThread.start();
        final Handler handler = new Handler(handlerThread.getLooper());
        final String namespace = "fetch2DatabaseTest";
        final FetchLogger fetchLogger = new FetchLogger(true, namespace);
        final Boolean autoStart = true;
        final Migration[] migrations = DownloadDatabase.getMigrations();
        databaseManager = new DatabaseManagerImpl(appContext, namespace,
                true, fetchLogger, migrations);
        final Downloader client = FetchDefaults.getDefaultDownloader();
        final long progessInterval = FetchDefaults.DEFAULT_PROGRESS_REPORTING_INTERVAL_IN_MILLISECONDS;
        final int concurrentLimit = FetchDefaults.DEFAULT_CONCURRENT_LIMIT;
        final int bufferSize = FetchDefaults.DEFAULT_DOWNLOAD_BUFFER_SIZE_BYTES;
        final NetworkInfoProvider networkInfoProvider = new NetworkInfoProvider(appContext);
        final boolean retryOnNetworkGain = false;
        final ListenerProvider listenerProvider = new ListenerProvider();
        final Handler uiHandler = new Handler(Looper.getMainLooper());
        final DownloadInfoUpdater downloadInfoUpdater = new DownloadInfoUpdater(databaseManager);
        final DownloadManager downloadManager = new DownloadManagerImpl(client, concurrentLimit,
                progessInterval, bufferSize, fetchLogger, networkInfoProvider, retryOnNetworkGain,
                listenerProvider, uiHandler, downloadInfoUpdater);
        priorityListProcessorImpl = new PriorityListProcessorImpl(
                handler,
                new DownloadProvider(databaseManager),
                downloadManager,
                new NetworkInfoProvider(appContext),
                fetchLogger);
        fetchHandler = new FetchHandlerImpl(namespace, databaseManager, downloadManager,
                priorityListProcessorImpl, listenerProvider, handler, fetchLogger, autoStart);
    }

    @Test
    public void enqueue() throws Exception {
        fetchHandler.deleteAll();
        final Request request = getTestRequest();
        final Download download = fetchHandler.enqueue(request);
        assertNotNull(download);
        assertEquals(request.getId(), download.getId());
    }

    @Test
    public void enqueueMulti() throws Exception {
        fetchHandler.deleteAll();
        final String url = "http://www.example.com/test.txt";
        final String dir = appContext.getFilesDir() + "/testFolder/";
        final List<Request> requestList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            final String file = dir + "test" + i + ".txt";
            final Request request = new Request(url, file);
            requestList.add(request);
        }
        final List<Download> downloads = fetchHandler.enqueue(requestList);
        assertNotNull(downloads);
        for (Download download : downloads) {
            assertNotNull(download);
        }
    }

    @Test
    public void pauseWithId() throws Exception {
        fetchHandler.deleteAll();
        final Request request = getTestRequest();
        final Download download = fetchHandler.enqueue(request);
        assertNotNull(download);
        final List<Download> downloads = fetchHandler.pause(new int[]{request.getId()});
        assertEquals(1, downloads.size());
        final Download pausedDownload = downloads.get(0);
        assertNotNull(pausedDownload);
        assertEquals(Status.PAUSED, pausedDownload.getStatus());
    }

    @Test
    public void pauseGroup() throws Exception {
        fetchHandler.deleteAll();
        final Request request = getTestRequest();
        final Request request2 = getTestRequest();
        final int groupId = 4;
        request.setGroupId(groupId);
        request2.setGroupId(groupId);
        final List<Request> requestList = new ArrayList<>();
        requestList.add(request);
        requestList.add(request2);
        fetchHandler.enqueue(requestList);

        final List<Download> downloads = fetchHandler.pausedGroup(groupId);
        assertNotNull(downloads);
        assertEquals(2, downloads.size());

        for (Download download : downloads) {
            assertNotNull(download);
            assertEquals(groupId, download.getGroup());
            assertEquals(Status.PAUSED, download.getStatus());
        }
    }

    @Test
    public void freeze() throws Exception {
        fetchHandler.deleteAll();
        final int size = 4;
        List<Request> requestList = getTestRequestList(size);
        final List<Download> downloads = fetchHandler.enqueue(requestList);
        assertNotNull(downloads);
        assertEquals(size, downloads.size());
        fetchHandler.freeze();
        assertTrue(priorityListProcessorImpl.isPaused());
    }

    @Test
    public void resumeWithId() throws Exception {
        fetchHandler.deleteAll();
        final Request request = getTestRequest();
        final Download download = fetchHandler.enqueue(request);
        assertNotNull(download);
        final List<Download> downloads = fetchHandler.resume(new int[]{request.getId()});
        assertEquals(1, downloads.size());
        final Download resumedDownload = downloads.get(0);
        assertNotNull(resumedDownload);
        assertEquals(Status.QUEUED, resumedDownload.getStatus());
    }

    @Test
    public void resumeGroup() throws Exception {
        fetchHandler.deleteAll();
        final Request request = getTestRequest();
        final Request request2 = getTestRequest();
        final int groupId = 4;
        request.setGroupId(groupId);
        request2.setGroupId(groupId);
        final List<Request> requestList = new ArrayList<>();
        requestList.add(request);
        requestList.add(request2);
        fetchHandler.enqueue(requestList);

        final List<Download> downloads = fetchHandler.resumeGroup(groupId);
        assertNotNull(downloads);
        assertEquals(0, downloads.size());

    }

    @Test
    public void unfreeze() throws Exception {
        fetchHandler.deleteAll();
        final int size = 4;
        List<Request> requestList = getTestRequestList(size);
        fetchHandler.enqueue(requestList);
        fetchHandler.unfreeze();
        assertFalse(priorityListProcessorImpl.isPaused());
    }

    @Test
    public void removeWithId() throws Exception {
        fetchHandler.deleteAll();
        final Request request = getTestRequest();
        final Download download = fetchHandler.enqueue(request);
        assertNotNull(download);
        final List<Download> downloads = fetchHandler.remove(new int[]{request.getId()});
        assertEquals(1, downloads.size());
        final Download removedDownload = downloads.get(0);
        assertNotNull(removedDownload);
        assertEquals(Status.REMOVED, removedDownload.getStatus());
        final Download download1 = databaseManager.get(download.getId());
        assertNull(download1);
    }

    @Test
    public void removeGroup() throws Exception {
        fetchHandler.deleteAll();
        final Request request = getTestRequest();
        final Request request2 = getTestRequest();
        final int groupId = 4;
        request.setGroupId(groupId);
        request2.setGroupId(groupId);
        final List<Request> requestList = new ArrayList<>();
        requestList.add(request);
        requestList.add(request2);
        fetchHandler.enqueue(requestList);

        final List<Download> downloads = fetchHandler.removeGroup(groupId);
        assertNotNull(downloads);
        assertEquals(2, downloads.size());

        for (Download download : downloads) {
            assertNotNull(download);
            assertEquals(groupId, download.getGroup());
            assertEquals(Status.REMOVED, download.getStatus());
        }
        final List<Integer> idList = new ArrayList<>();
        idList.add(request.getId());
        idList.add(request2.getId());
        final List<DownloadInfo> downloads1 = databaseManager.get(idList);
        assertNotNull(downloads1);
        for (DownloadInfo downloadInfo : downloads1) {
            assertNull(downloadInfo);
        }
    }

    @Test
    public void removeAll() throws Exception {
        fetchHandler.deleteAll();
        final int size = 4;
        List<Request> requestList = getTestRequestList(size);
        fetchHandler.enqueue(requestList);
        List<Download> removedDownloadsList = fetchHandler.removeAll();
        assertNotNull(removedDownloadsList);
        assertEquals(size, removedDownloadsList.size());

        for (Download download : removedDownloadsList) {
            assertNotNull(download);
            assertEquals(Status.REMOVED, download.getStatus());
        }

        final List<DownloadInfo> downloads1 = databaseManager.get();
        assertNotNull(downloads1);
        for (DownloadInfo downloadInfo : downloads1) {
            assertNull(downloadInfo);
        }
    }

    @Test
    public void deleteWithId() throws Exception {
        fetchHandler.deleteAll();
        final Request request = getTestRequest();
        final Download download = fetchHandler.enqueue(request);
        assertNotNull(download);
        final List<Download> downloads = fetchHandler.delete(new int[]{request.getId()});
        assertEquals(1, downloads.size());
        final Download deletedDownload = downloads.get(0);
        assertNotNull(deletedDownload);
        assertEquals(Status.DELETED, deletedDownload.getStatus());
        final Download download1 = databaseManager.get(download.getId());
        assertNull(download1);
    }

    @Test
    public void deleteGroup() throws Exception {
        fetchHandler.deleteAll();
        final Request request = getTestRequest();
        final Request request2 = getTestRequest();
        final int groupId = 4;
        request.setGroupId(groupId);
        request2.setGroupId(groupId);
        final List<Request> requestList = new ArrayList<>();
        requestList.add(request);
        requestList.add(request2);
        fetchHandler.enqueue(requestList);

        final List<Download> downloads = fetchHandler.deleteGroup(groupId);
        assertNotNull(downloads);
        assertEquals(2, downloads.size());

        for (Download download : downloads) {
            assertNotNull(download);
            assertEquals(groupId, download.getGroup());
            assertEquals(Status.DELETED, download.getStatus());
        }
        final List<Integer> idList = new ArrayList<>();
        idList.add(request.getId());
        idList.add(request2.getId());
        final List<DownloadInfo> downloads1 = databaseManager.get(idList);
        assertNotNull(downloads1);
        for (DownloadInfo downloadInfo : downloads1) {
            assertNull(downloadInfo);
        }
    }

    @Test
    public void deleteAll() throws Exception {
        fetchHandler.deleteAll();
        final int size = 4;
        List<Request> requestList = getTestRequestList(size);
        fetchHandler.enqueue(requestList);
        List<Download> deletedDownloadsList = fetchHandler.deleteAll();
        assertNotNull(deletedDownloadsList);
        assertEquals(size, deletedDownloadsList.size());

        for (Download download : deletedDownloadsList) {
            assertNotNull(download);
            assertEquals(Status.DELETED, download.getStatus());
        }

        final List<DownloadInfo> downloads1 = databaseManager.get();
        assertNotNull(downloads1);
        for (DownloadInfo downloadInfo : downloads1) {
            assertNull(downloadInfo);
        }
    }

    @Test
    public void cancelledWithId() throws Exception {
        fetchHandler.deleteAll();
        final Request request = getTestRequest();
        final Download download = fetchHandler.enqueue(request);
        assertNotNull(download);
        final List<Download> downloads = fetchHandler.cancel(new int[]{request.getId()});
        assertEquals(1, downloads.size());
        final Download cancelledDownload = downloads.get(0);
        assertNotNull(cancelledDownload);
        assertEquals(Status.CANCELLED, cancelledDownload.getStatus());
    }

    @Test
    public void cancelledGroup() throws Exception {
        fetchHandler.deleteAll();
        final Request request = getTestRequest();
        final Request request2 = getTestRequest();
        final int groupId = 4;
        request.setGroupId(groupId);
        request2.setGroupId(groupId);
        final List<Request> requestList = new ArrayList<>();
        requestList.add(request);
        requestList.add(request2);
        fetchHandler.enqueue(requestList);

        final List<Download> downloads = fetchHandler.cancelGroup(groupId);
        assertNotNull(downloads);
        assertEquals(2, downloads.size());

        for (Download download : downloads) {
            assertNotNull(download);
            assertEquals(groupId, download.getGroup());
            assertEquals(Status.CANCELLED, download.getStatus());
        }
    }

    @Test
    public void cancelledAll() throws Exception {
        fetchHandler.deleteAll();
        final int size = 4;
        List<Request> requestList = getTestRequestList(size);
        fetchHandler.enqueue(requestList);
        List<Download> cancelledDownloadsList = fetchHandler.cancelAll();
        assertNotNull(cancelledDownloadsList);
        assertEquals(size, cancelledDownloadsList.size());

        for (Download download : cancelledDownloadsList) {
            assertNotNull(download);
            assertEquals(Status.CANCELLED, download.getStatus());
        }
    }

    @Test
    public void retry() throws Exception {
        fetchHandler.deleteAll();
        final Request request = getTestRequest();
        final Download download = fetchHandler.enqueue(request);
        assertNotNull(download);
        assertEquals(request.getId(), download.getId());
        final DownloadInfo downloadInfo = FetchTypeConverterExtensions.toDownloadInfo(download);
        downloadInfo.setStatus(Status.FAILED);
        databaseManager.update(downloadInfo);
        final List<Download> queuedDownloads = fetchHandler.retry(new int[]{request.getId()});
        assertNotNull(queuedDownloads);
        assertEquals(1, queuedDownloads.size());
        final Download queuedDownload = queuedDownloads.get(0);
        assertNotNull(queuedDownload);
        assertEquals(download.getId(), queuedDownload.getId());
        assertEquals(Status.QUEUED, queuedDownload.getStatus());
    }

    @Test
    public void updateRequest() throws Exception {
        fetchHandler.deleteAll();
        final Request request = getTestRequest();
        final Download download = fetchHandler.enqueue(request);
        assertNotNull(download);
        assertEquals(request.getId(), download.getId());

        final int groupId = 1245;
        final Priority priority = Priority.LOW;
        final RequestInfo requestInfo = new RequestInfo();
        requestInfo.setGroupId(groupId);
        requestInfo.setPriority(priority);
        fetchHandler.updateRequest(download.getId(), requestInfo);

        final Download downloadInfo = fetchHandler.getDownload(download.getId());
        assertNotNull(downloadInfo);
        assertEquals(download.getId(), downloadInfo.getId());
        assertEquals(groupId, downloadInfo.getGroup());
        assertEquals(priority, downloadInfo.getPriority());
    }

    @Test
    public void updateSetGlobalNetworkType() throws Exception {
        final NetworkType networkType = NetworkType.WIFI_ONLY;
        fetchHandler.setGlobalNetworkType(networkType);
        assertEquals(networkType, priorityListProcessorImpl.getGlobalNetworkType());

        fetchHandler.setGlobalNetworkType(NetworkType.GLOBAL_OFF);
    }

    @Test
    public void get() throws Exception {
        fetchHandler.deleteAll();
        final String url = "http://www.example.com/test.txt";
        final String dir = appContext.getFilesDir() + "/testFolder/";
        final List<Request> requestList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            final String file = dir + "test" + i + ".txt";
            final Request request = new Request(url, file);
            requestList.add(request);
        }
        final List<Download> downloadInfoList = fetchHandler.enqueue(requestList);
        final List<Download> queryList = fetchHandler.getDownloads();
        assertNotNull(downloadInfoList);
        assertNotNull(queryList);
        assertEquals(downloadInfoList.size(), queryList.size());
    }


    @Test
    public void getId() throws Exception {
        fetchHandler.deleteAll();
        final Request request = getTestRequest();
        final Download downloadInfo = fetchHandler.enqueue(request);
        assertNotNull(downloadInfo);
        final Download query = fetchHandler.getDownload(downloadInfo.getId());
        assertNotNull(query);
        assertEquals(downloadInfo.getId(), query.getId());
    }

    @Test
    public void getIds() throws Exception {
        fetchHandler.deleteAll();
        final String url = "http://www.example.com/test.txt";
        final String dir = appContext.getFilesDir() + "/testFolder/";
        final List<Request> requestList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            final String file = dir + "test" + i + ".txt";
            final Request request = new Request(url, file);
            requestList.add(request);
        }
        final List<Download> downloadInfoList = fetchHandler.enqueue(requestList);
        final List<Integer> ids = new ArrayList<>();
        for(Request request : requestList) {
            ids.add(request.getId());
        }
        final List<Download> queryList = fetchHandler.getDownloads(ids);
        assertNotNull(queryList);
        assertEquals(downloadInfoList.size(), queryList.size());
        for (Download downloadInfo : queryList) {
            assertNotNull(downloadInfo);
            assertTrue(ids.contains(downloadInfo.getId()));
        }
    }

    @Test
    public void getStatus() throws Exception {
        fetchHandler.deleteAll();
        final String url = "http://www.example.com/test.txt";
        final String dir = appContext.getFilesDir() + "/testFolder/";
        final List<Request> requestList = new ArrayList<>();
        final Status status = Status.QUEUED;
        for (int i = 0; i < 10; i++) {
            final String file = dir + "test" + i + ".txt";
            final Request request = new Request(url, file);
            requestList.add(request);
        }
        final List<Download> downloadInfoList = fetchHandler.enqueue(requestList);
        final List<Download> queryList = fetchHandler.getDownloadsWithStatus(status);
        assertNotNull(queryList);
        assertEquals(downloadInfoList.size(), queryList.size());
        for (Download download : queryList) {
            assertTrue(downloadInfoList.contains(download));
            assertEquals(status, download.getStatus());
        }
    }


    @Test
    public void getGroup() throws Exception {
        fetchHandler.deleteAll();
        final String url = "http://www.example.com/test.txt";
        final String dir = appContext.getFilesDir() + "/testFolder/";
        final List<Request> requestList = new ArrayList<>();
        final int group = 10;
        for (int i = 0; i < 10; i++) {
            final String file = dir + "test" + i + ".txt";
            final Request request = new Request(url, file);
            request.setGroupId(group);
            requestList.add(request);
        }
        final List<Download> downloadInfoList = fetchHandler.enqueue(requestList);
        final List<Download> queryList = fetchHandler.getDownloadsInGroup(group);
        assertNotNull(queryList);
        assertEquals(downloadInfoList.size(), queryList.size());
        for (Download download : queryList) {
            assertTrue(downloadInfoList.contains(download));
            assertEquals(group, download.getGroup());
        }
    }

    @Test
    public void addListener() throws Exception {
        final AbstractFetchListener abstractFetchListener = new AbstractFetchListener() {

        };
        fetchHandler.addListener(abstractFetchListener);
        final boolean hasListener = fetchHandler.getFetchListenerProvider()
                .getListeners().contains(abstractFetchListener);
        assertTrue(hasListener);
    }

    @Test
    public void removeListener() throws Exception {
        final AbstractFetchListener abstractFetchListener = new AbstractFetchListener() {

        };
        fetchHandler.addListener(abstractFetchListener);
        final boolean hasListener = fetchHandler.getFetchListenerProvider()
                .getListeners().contains(abstractFetchListener);
        assertTrue(hasListener);
        fetchHandler.removeListener(abstractFetchListener);
        final boolean doesNotHaveListener = fetchHandler.getFetchListenerProvider()
                .getListeners().contains(abstractFetchListener);
        assertFalse(doesNotHaveListener);
    }

    @After
    public void cleanUp() throws Exception {
        databaseManager.deleteAll();
        fetchHandler.close();
    }

    public Request getTestRequest() {
        final String url = "http://www.example.com/test.txt";
        final String file = appContext.getFilesDir() + "/testFolder/test" + System.nanoTime() + ".txt";
        return new Request(url, file);
    }

    public List<Request> getTestRequestList(int size) {
        final ArrayList<Request> requests = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            final Request request = getTestRequest();
            requests.add(request);
        }
        return requests;
    }


}
