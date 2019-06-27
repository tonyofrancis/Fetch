package com.tonyodev.fetch2;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.tonyodev.fetch2.database.FetchDatabaseManager;
import com.tonyodev.fetch2.database.FetchDatabaseManagerImpl;
import com.tonyodev.fetch2.database.DownloadDatabase;
import com.tonyodev.fetch2.database.DownloadInfo;
import com.tonyodev.fetch2.database.FetchDatabaseManagerWrapper;
import com.tonyodev.fetch2.database.migration.Migration;
import com.tonyodev.fetch2.downloader.DownloadManager;
import com.tonyodev.fetch2.downloader.DownloadManagerImpl;
import com.tonyodev.fetch2.downloader.DownloadManagerCoordinator;
import com.tonyodev.fetch2.fetch.FetchHandler;
import com.tonyodev.fetch2.fetch.FetchHandlerImpl;
import com.tonyodev.fetch2.fetch.LiveSettings;
import com.tonyodev.fetch2.provider.GroupInfoProvider;
import com.tonyodev.fetch2core.DefaultStorageResolver;
import com.tonyodev.fetch2core.Downloader;
import com.tonyodev.fetch2core.FetchCoreDefaults;
import com.tonyodev.fetch2core.FetchCoreUtils;
import com.tonyodev.fetch2core.FetchLogger;
import com.tonyodev.fetch2core.FileServerDownloader;
import com.tonyodev.fetch2core.HandlerWrapper;
import com.tonyodev.fetch2.fetch.ListenerCoordinator;
import com.tonyodev.fetch2.helper.DownloadInfoUpdater;
import com.tonyodev.fetch2.helper.PriorityListProcessor;
import com.tonyodev.fetch2.helper.PriorityListProcessorImpl;
import com.tonyodev.fetch2.provider.DownloadProvider;
import com.tonyodev.fetch2.provider.NetworkInfoProvider;
import com.tonyodev.fetch2.util.FetchDefaults;
import com.tonyodev.fetch2.util.FetchTypeConverterExtensions;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import kotlin.Pair;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class FetchHandlerInstrumentedTest {

    private Context appContext;
    private FetchHandler fetchHandler;
    private FetchDatabaseManager fetchDatabaseManager;
    private PriorityListProcessor<Download> priorityListProcessorImpl;

    @Before
    public void useAppContext() throws Exception {
        appContext = InstrumentationRegistry.getTargetContext();
        final HandlerThread handlerThread = new HandlerThread("test");
        handlerThread.start();
        final String namespace = "fetch2DatabaseTest";
        final FetchLogger fetchLogger = new FetchLogger(true, namespace);
        final Boolean autoStart = true;
        final Migration[] migrations = DownloadDatabase.getMigrations();
        final LiveSettings liveSettings = new LiveSettings(namespace);
        DefaultStorageResolver defaultStorageResolver = new DefaultStorageResolver(appContext, FetchCoreUtils.getFileTempDir(appContext));
        fetchDatabaseManager = new FetchDatabaseManagerImpl(appContext, namespace, fetchLogger, migrations, liveSettings, false, defaultStorageResolver);
        final FetchDatabaseManagerWrapper databaseManagerWrapper = new FetchDatabaseManagerWrapper(fetchDatabaseManager);
        final int concurrentLimit = FetchDefaults.DEFAULT_CONCURRENT_LIMIT;
        final HandlerWrapper handlerWrapper = new HandlerWrapper(namespace, null);
        final Downloader client = FetchDefaults.getDefaultDownloader();
        final FileServerDownloader serverClient = FetchDefaults.getDefaultFileServerDownloader();
        final FileServerDownloader serverDownloader = FetchDefaults.getDefaultFileServerDownloader();
        final long progessInterval = FetchCoreDefaults.DEFAULT_PROGRESS_REPORTING_INTERVAL_IN_MILLISECONDS;
        final NetworkInfoProvider networkInfoProvider = new NetworkInfoProvider(appContext, null);
        final boolean retryOnNetworkGain = false;
        final Handler uiHandler = new Handler(Looper.getMainLooper());
        final DownloadInfoUpdater downloadInfoUpdater = new DownloadInfoUpdater(databaseManagerWrapper);
        final String tempDir = FetchCoreUtils.getFileTempDir(appContext);
        final DownloadManagerCoordinator downloadManagerCoordinator = new DownloadManagerCoordinator(namespace);
        final DownloadProvider downloadProvider = new DownloadProvider(databaseManagerWrapper);
        final GroupInfoProvider groupInfoProvider = new GroupInfoProvider(namespace, downloadProvider);
        final ListenerCoordinator listenerCoordinator = new ListenerCoordinator(namespace, groupInfoProvider, downloadProvider, uiHandler);
        final DefaultStorageResolver storageResolver = new DefaultStorageResolver(appContext, tempDir);
        final DownloadManager downloadManager = new DownloadManagerImpl(client, concurrentLimit,
                progessInterval, fetchLogger, networkInfoProvider, retryOnNetworkGain,
                downloadInfoUpdater, downloadManagerCoordinator,
                listenerCoordinator, serverDownloader, false, storageResolver,
                appContext, namespace, groupInfoProvider, FetchDefaults.DEFAULT_GLOBAL_AUTO_RETRY_ATTEMPTS, false);
        priorityListProcessorImpl = new PriorityListProcessorImpl(
                handlerWrapper,
                new DownloadProvider(databaseManagerWrapper),
                downloadManager,
                new NetworkInfoProvider(appContext , null),
                fetchLogger,
                listenerCoordinator,
                concurrentLimit,
                appContext,
                namespace,
                PrioritySort.ASC);
        fetchHandler = new FetchHandlerImpl(namespace, databaseManagerWrapper, downloadManager,
                priorityListProcessorImpl, fetchLogger, autoStart,
                client, serverClient, listenerCoordinator, uiHandler, storageResolver, null,
                groupInfoProvider, PrioritySort.ASC, FetchDefaults.DEFAULT_CREATE_FILE_ON_ENQUEUE);
    }

    @Test
    public void enqueue() throws Exception {
        fetchHandler.deleteAll();
        final Request request = getTestRequest();
        final Pair<Download, Error> download = fetchHandler.enqueue(request);
        assertNotNull(download);
        assertEquals(request.getId(), download.getFirst().getId());
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
        final List<Pair<Download, Error>> downloads = fetchHandler.enqueue(requestList);
        assertNotNull(downloads);
    }

    @Test
    public void pauseWithId() throws Exception {
        fetchHandler.deleteAll();
        final Request request = getTestRequest();
        final Pair<Download, Error> download = fetchHandler.enqueue(request);
        assertNotNull(download);
        final List<Download> downloads = fetchHandler.pause(getIdList(request.getId()));
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
        final List<Pair<Download, Error>> downloads = fetchHandler.enqueue(requestList);
        assertNotNull(downloads);
        assertEquals(size, downloads.size());
        fetchHandler.freeze();
        assertTrue(priorityListProcessorImpl.isPaused());
    }

    @Test
    public void resumeWithId() throws Exception {
        fetchHandler.deleteAll();
        final Request request = getTestRequest();
        final Pair<Download, Error> download = fetchHandler.enqueue(request);
        assertNotNull(download);
        final List<Download> downloads = fetchHandler.resume(getIdList(request.getId()));
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
        assertEquals(2, downloads.size());

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
        final Pair<Download, Error> download = fetchHandler.enqueue(request);
        assertNotNull(download);
        final List<Download> downloads = fetchHandler.remove(getIdList(request.getId()));
        assertEquals(1, downloads.size());
        final Download removedDownload = downloads.get(0);
        assertNotNull(removedDownload);
        assertEquals(Status.REMOVED, removedDownload.getStatus());
        final Download download1 = fetchDatabaseManager.get(download.getFirst().getId());
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
        final List<DownloadInfo> downloads1 = fetchDatabaseManager.get(idList);
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

        final List<DownloadInfo> downloads1 = fetchDatabaseManager.get();
        assertNotNull(downloads1);
        for (DownloadInfo downloadInfo : downloads1) {
            assertNull(downloadInfo);
        }
    }

    @Test
    public void deleteWithId() throws Exception {
        fetchHandler.deleteAll();
        final Request request = getTestRequest();
        final Pair<Download, Error> download = fetchHandler.enqueue(request);
        assertNotNull(download);
        final List<Download> downloads = fetchHandler.delete(getIdList(request.getId()));
        assertEquals(1, downloads.size());
        final Download deletedDownload = downloads.get(0);
        assertNotNull(deletedDownload);
        assertEquals(Status.DELETED, deletedDownload.getStatus());
        final Download download1 = fetchDatabaseManager.get(download.getFirst().getId());
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
        final List<DownloadInfo> downloads1 = fetchDatabaseManager.get(idList);
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

        final List<DownloadInfo> downloads1 = fetchDatabaseManager.get();
        assertNotNull(downloads1);
        for (DownloadInfo downloadInfo : downloads1) {
            assertNull(downloadInfo);
        }
    }

    @Test
    public void cancelledWithId() throws Exception {
        fetchHandler.deleteAll();
        final Request request = getTestRequest();
        final Pair<Download, Error> download = fetchHandler.enqueue(request);
        assertNotNull(download);
        final List<Download> downloads = fetchHandler.cancel(getIdList(request.getId()));
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
        final Pair<Download, Error> download = fetchHandler.enqueue(request);
        assertNotNull(download);
        assertEquals(request.getId(), download.getFirst().getId());
        final DownloadInfo downloadInfo = FetchTypeConverterExtensions.toDownloadInfo(download.getFirst(), new DownloadInfo());
        downloadInfo.setStatus(Status.FAILED);
        fetchDatabaseManager.update(downloadInfo);
        final List<Download> queuedDownloads = fetchHandler.retry(getIdList(request.getId()));
        assertNotNull(queuedDownloads);
        assertEquals(1, queuedDownloads.size());
        final Download queuedDownload = queuedDownloads.get(0);
        assertNotNull(queuedDownload);
        assertEquals(download.getFirst().getId(), queuedDownload.getId());
        assertEquals(Status.QUEUED, queuedDownload.getStatus());
    }

    @Test
    public void updateRequest() throws Exception {
        fetchHandler.deleteAll();
        final Request request = getTestRequest();
        final Pair<Download, Error> download = fetchHandler.enqueue(request);
        assertNotNull(download);
        assertEquals(request.getId(), download.getFirst().getId());

        final int groupId = 1245;
        final Priority priority = Priority.LOW;
        final Request request1 = new Request(request.getUrl(), request.getFile());
        request1.setGroupId(groupId);
        request1.setPriority(priority);
        fetchHandler.updateRequest(download.getFirst().getId(), request1);

        final Download downloadInfo = fetchHandler.getDownload(download.getFirst().getId());
        assertNotNull(downloadInfo);
        assertEquals(download.getFirst().getId(), downloadInfo.getId());
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
        final List<Pair<Download, Error>> downloadInfoList = fetchHandler.enqueue(requestList);
        final List<Download> queryList = fetchHandler.getDownloads();
        assertNotNull(downloadInfoList);
        assertNotNull(queryList);
        assertEquals(downloadInfoList.size(), queryList.size());
    }


    @Test
    public void getId() throws Exception {
        fetchHandler.deleteAll();
        final Request request = getTestRequest();
        final Pair<Download, Error> downloadInfo = fetchHandler.enqueue(request);
        assertNotNull(downloadInfo);
        final Download query = fetchHandler.getDownload(downloadInfo.getFirst().getId());
        assertNotNull(query);
        assertEquals(downloadInfo.getFirst().getId(), query.getId());
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
        final List<Pair<Download, Error>> downloadInfoList = fetchHandler.enqueue(requestList);
        final List<Integer> ids = new ArrayList<>();
        for (Request request : requestList) {
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

    @After
    public void cleanUp() throws Exception {
        fetchDatabaseManager.deleteAll();
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

    public List<Integer> getIdList(int id) {
        final List<Integer> idList = new ArrayList<>();
        idList.add(id);
        return idList;
    }


}
