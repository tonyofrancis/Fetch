package com.tonyodev.fetch2;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.tonyodev.fetch2.database.FetchDatabaseManager;
import com.tonyodev.fetch2.database.FetchDatabaseManagerImpl;
import com.tonyodev.fetch2.database.DownloadDatabase;
import com.tonyodev.fetch2.database.DownloadInfo;
import com.tonyodev.fetch2.database.migration.Migration;
import com.tonyodev.fetch2.fetch.LiveSettings;
import com.tonyodev.fetch2.util.FetchTypeConverterExtensions;
import com.tonyodev.fetch2core.DefaultStorageResolver;
import com.tonyodev.fetch2core.FetchCoreUtils;
import com.tonyodev.fetch2core.FetchLogger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import kotlin.Pair;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class DatabaseInstrumentedTest {

    private FetchDatabaseManager fetchDatabaseManager;
    private Context appContext;

    @Before
    public void useAppContext() throws Exception {
        // Context of the app under test.
        appContext = InstrumentationRegistry.getTargetContext();
        assertEquals("com.tonyodev.fetch2.test", appContext.getPackageName());
        final String namespace = "fetch2DatabaseTest";
        final Migration[] migrations = DownloadDatabase.getMigrations();
        final LiveSettings liveSettings = new LiveSettings(namespace);
        FetchLogger fetchLogger = new FetchLogger(true, namespace);
        DefaultStorageResolver defaultStorageResolver = new DefaultStorageResolver(appContext, FetchCoreUtils.getFileTempDir(appContext));
        fetchDatabaseManager = new FetchDatabaseManagerImpl(appContext, namespace, fetchLogger, migrations, liveSettings, false,
                defaultStorageResolver);
    }

    @After
    public void cleanup() throws Exception {
        fetchDatabaseManager.deleteAll();
        fetchDatabaseManager.close();
    }

    @Test
    public void clear() throws Exception {
        fetchDatabaseManager.deleteAll();
        final String url = "http://www.example.com/test.txt";
        final String dir = appContext.getFilesDir() + "/testFolder/";
        final List<Request> requestList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            final String file = dir + "test" + i + ".txt";
            final Request request = new Request(url, file);
            requestList.add(request);
        }
        final List<DownloadInfo> downloadInfoList = new ArrayList<>();
        for (Request request : requestList) {
            final DownloadInfo downloadInfo = FetchTypeConverterExtensions.toDownloadInfo(request, new DownloadInfo());
            downloadInfoList.add(downloadInfo);
        }
        fetchDatabaseManager.insert(downloadInfoList);
        fetchDatabaseManager.deleteAll();
        final List<DownloadInfo> downloads = fetchDatabaseManager.get();
        assertEquals(0, downloads.size());
    }

    @Test
    public void insertSingle() throws Exception {
        fetchDatabaseManager.deleteAll();
        final Request request = getTestRequest();
        final DownloadInfo downloadInfo = FetchTypeConverterExtensions.toDownloadInfo(request, new DownloadInfo());
        final Pair<DownloadInfo, Boolean> pair = fetchDatabaseManager.insert(downloadInfo);
        assertTrue(pair.getSecond());
    }


    @Test
    public void multiInsert() throws Exception {
        fetchDatabaseManager.deleteAll();
        final String url = "http://www.example.com/test.txt";
        final String dir = appContext.getFilesDir() + "/testFolder/";
        final List<Request> requestList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            final String file = dir + "test" + i + ".txt";
            final Request request = new Request(url, file);
            requestList.add(request);
        }
        final List<DownloadInfo> downloadInfoList = new ArrayList<>();
        for (Request request : requestList) {
            final DownloadInfo downloadInfo = FetchTypeConverterExtensions.toDownloadInfo(request, new DownloadInfo());
            downloadInfoList.add(downloadInfo);
        }
        final List<Pair<DownloadInfo, Boolean>> insertedList = fetchDatabaseManager.insert(downloadInfoList);
        for (Pair<DownloadInfo, Boolean> downloadInfoBooleanPair : insertedList) {
            assertTrue(downloadInfoBooleanPair.getSecond());
        }
    }

    @Test
    public void deleteSingle() throws Exception {
        fetchDatabaseManager.deleteAll();
        final Request request = getTestRequest();
        final DownloadInfo downloadInfo = FetchTypeConverterExtensions.toDownloadInfo(request, new DownloadInfo());
        fetchDatabaseManager.insert(downloadInfo);
        fetchDatabaseManager.delete(downloadInfo);
        final Download query = fetchDatabaseManager.get(downloadInfo.getId());
        assertNull(query);
    }

    @Test
    public void deleteMulti() throws Exception {
        fetchDatabaseManager.deleteAll();
        final String url = "http://www.example.com/test.txt";
        final String dir = appContext.getFilesDir() + "/testFolder/";
        final List<Request> requestList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            final String file = dir + "test" + i + ".txt";
            final Request request = new Request(url, file);
            requestList.add(request);
        }
        final List<DownloadInfo> downloadInfoList = new ArrayList<>();
        for (Request request : requestList) {
            final DownloadInfo downloadInfo = FetchTypeConverterExtensions.toDownloadInfo(request, new DownloadInfo());
            downloadInfoList.add(downloadInfo);
        }
        fetchDatabaseManager.insert(downloadInfoList);
        fetchDatabaseManager.delete(downloadInfoList);
        final List<Integer> ids = new ArrayList<>();
        for (DownloadInfo downloadInfo : downloadInfoList) {
            ids.add(downloadInfo.getId());
        }
        final List<DownloadInfo> queryList = fetchDatabaseManager.get(ids);
        assertEquals(0, queryList.size());
    }

    @Test
    public void update() throws Exception {
        fetchDatabaseManager.deleteAll();
        final Request request = getTestRequest();
        final File file = FetchCoreUtils.getFile(request.getFile());
        final DownloadInfo downloadInfo = FetchTypeConverterExtensions.toDownloadInfo(request, new DownloadInfo());
        fetchDatabaseManager.insert(downloadInfo);
        final int groupId = 2;
        final Priority priority = Priority.HIGH;
        final Status status = Status.QUEUED;
        downloadInfo.setGroup(groupId);
        downloadInfo.setPriority(priority);
        downloadInfo.setStatus(status);
        fetchDatabaseManager.update(downloadInfo);
        final Download query = fetchDatabaseManager.get(downloadInfo.getId());
        assertNotNull(query);
        assertEquals(groupId, query.getGroup());
        assertEquals(priority, query.getPriority());
        assertEquals(status, query.getStatus());
    }

    @Test
    public void updateMulti() throws Exception {
        fetchDatabaseManager.deleteAll();
        final String url = "http://www.example.com/test.txt";
        final String dir = appContext.getFilesDir() + "/testFolder/";
        final List<Request> requestList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            final String file = dir + "test" + i + ".txt";
            final Request request = new Request(url, file);
            requestList.add(request);
        }
        final List<DownloadInfo> downloadInfoList = new ArrayList<>();
        for (Request request : requestList) {
            final DownloadInfo downloadInfo = FetchTypeConverterExtensions.toDownloadInfo(request, new DownloadInfo());
            downloadInfoList.add(downloadInfo);
        }
        fetchDatabaseManager.insert(downloadInfoList);
        final int groupId = 2;
        final Priority priority = Priority.HIGH;
        final Status status = Status.QUEUED;
        for (int i = 0; i < downloadInfoList.size(); i++) {
            final DownloadInfo downloadInfo = downloadInfoList.get(i);
            downloadInfo.setGroup(groupId);
            downloadInfo.setPriority(priority);
            downloadInfo.setStatus(status);
        }
        fetchDatabaseManager.update(downloadInfoList);
        final List<Integer> ids = new ArrayList<>();
        for (DownloadInfo downloadInfo : downloadInfoList) {
            ids.add(downloadInfo.getId());
        }
        final List<DownloadInfo> queryList = fetchDatabaseManager.get(ids);
        assertNotNull(queryList);
        assertEquals(ids.size(), queryList.size());
        for (DownloadInfo downloadInfo : queryList) {
            assertEquals(groupId, downloadInfo.getGroup());
            assertEquals(priority, downloadInfo.getPriority());
            assertEquals(status, downloadInfo.getStatus());
        }

    }

    @Test
    public void get() throws Exception {
        fetchDatabaseManager.deleteAll();
        final String url = "http://www.example.com/test.txt";
        final String dir = appContext.getFilesDir() + "/testFolder/";
        final List<Request> requestList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            final String file = dir + "test" + i + ".txt";
            final Request request = new Request(url, file);
            requestList.add(request);
        }
        final List<DownloadInfo> downloadInfoList = new ArrayList<>();
        for (Request request : requestList) {
            final DownloadInfo downloadInfo = FetchTypeConverterExtensions.toDownloadInfo(request, new DownloadInfo());
            downloadInfoList.add(downloadInfo);
        }
        fetchDatabaseManager.insert(downloadInfoList);
        final List<DownloadInfo> queryList = fetchDatabaseManager.get();
        assertNotNull(queryList);
        assertEquals(downloadInfoList.size(), queryList.size());
    }


    @Test
    public void getId() throws Exception {
        fetchDatabaseManager.deleteAll();
        final Request request = getTestRequest();
        final DownloadInfo downloadInfo = FetchTypeConverterExtensions.toDownloadInfo(request, new DownloadInfo());
        fetchDatabaseManager.insert(downloadInfo);
        final Download query = fetchDatabaseManager.get(downloadInfo.getId());
        assertNotNull(query);
        assertEquals(downloadInfo.getId(), query.getId());
    }

    @Test
    public void getIds() throws Exception {
        fetchDatabaseManager.deleteAll();
        final String url = "http://www.example.com/test.txt";
        final String dir = appContext.getFilesDir() + "/testFolder/";
        final List<Request> requestList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            final String file = dir + "test" + i + ".txt";
            final Request request = new Request(url, file);
            requestList.add(request);
        }
        final List<DownloadInfo> downloadInfoList = new ArrayList<>();
        for (Request request : requestList) {
            final DownloadInfo downloadInfo = FetchTypeConverterExtensions.toDownloadInfo(request, new DownloadInfo());
            downloadInfoList.add(downloadInfo);
        }
        fetchDatabaseManager.insert(downloadInfoList);
        final List<Integer> ids = new ArrayList<>();
        for (DownloadInfo downloadInfo : downloadInfoList) {
            ids.add(downloadInfo.getId());
        }
        final List<DownloadInfo> queryList = fetchDatabaseManager.get(ids);
        assertNotNull(queryList);
        assertEquals(downloadInfoList.size(), queryList.size());
        for (DownloadInfo downloadInfo : queryList) {
            assertNotNull(downloadInfo);
            assertTrue(ids.contains(downloadInfo.getId()));
        }
    }

    @Test
    public void getStatus() throws Exception {
        fetchDatabaseManager.deleteAll();
        final String url = "http://www.example.com/test.txt";
        final String dir = appContext.getFilesDir() + "/testFolder/";
        final List<Request> requestList = new ArrayList<>();
        final Status status = Status.QUEUED;
        for (int i = 0; i < 10; i++) {
            final String file = dir + "test" + i + ".txt";
            final Request request = new Request(url, file);
            requestList.add(request);
        }
        final List<DownloadInfo> downloadInfoList = new ArrayList<>();
        for (Request request : requestList) {
            final DownloadInfo downloadInfo = FetchTypeConverterExtensions.toDownloadInfo(request, new DownloadInfo());
            downloadInfoList.add(downloadInfo);
        }
        for (DownloadInfo downloadInfo : downloadInfoList) {
            downloadInfo.setStatus(status);
        }
        fetchDatabaseManager.insert(downloadInfoList);
        final List<DownloadInfo> queryList = fetchDatabaseManager.getByStatus(status);
        assertNotNull(queryList);
        assertEquals(downloadInfoList.size(), queryList.size());
        for (DownloadInfo download : queryList) {
            assertTrue(downloadInfoList.contains(download));
            assertEquals(status, download.getStatus());
        }
    }


    @Test
    public void getGroup() throws Exception {
        fetchDatabaseManager.deleteAll();
        final String url = "http://www.example.com/test.txt";
        final String dir = appContext.getFilesDir() + "/testFolder/";
        final List<Request> requestList = new ArrayList<>();
        final int group = 10;
        for (int i = 0; i < 10; i++) {
            final String file = dir + "test" + i + ".txt";
            final Request request = new Request(url, file);
            requestList.add(request);
        }
        final List<DownloadInfo> downloadInfoList = new ArrayList<>();
        for (Request request : requestList) {
            final DownloadInfo downloadInfo = FetchTypeConverterExtensions.toDownloadInfo(request, new DownloadInfo());
            downloadInfoList.add(downloadInfo);
        }
        for (DownloadInfo downloadInfo : downloadInfoList) {
            downloadInfo.setGroup(group);
        }
        fetchDatabaseManager.insert(downloadInfoList);
        final List<DownloadInfo> queryList = fetchDatabaseManager.getByGroup(group);
        assertNotNull(queryList);
        assertEquals(downloadInfoList.size(), queryList.size());
        for (DownloadInfo download : queryList) {
            assertTrue(downloadInfoList.contains(download));
            assertEquals(group, download.getGroup());
        }
    }

    @Test
    public void databaseVerification() throws Exception {
        fetchDatabaseManager.deleteAll();
        final List<Request> requests = getTestRequestList(20);
        final List<DownloadInfo> downloadInfoList = new ArrayList<>(20);
        for (Request request : requests) {
            final DownloadInfo downloadInfo = FetchTypeConverterExtensions.toDownloadInfo(request, new DownloadInfo());
            downloadInfo.setStatus(Status.DOWNLOADING);
            downloadInfoList.add(downloadInfo);
            final File file = FetchCoreUtils.getFile(request.getFile());
        }
        fetchDatabaseManager.insert(downloadInfoList);
        int size = fetchDatabaseManager.get().size();
        assertEquals(20, size);
        fetchDatabaseManager.sanitizeOnFirstEntry();
        final List<DownloadInfo> downloads = fetchDatabaseManager.get();
        for (DownloadInfo download : downloads) {
            assertNotNull(download);
            assertEquals(Status.QUEUED, download.getStatus());
        }
    }

    @Test
    public void getGroupWithStatus() throws Exception {
        fetchDatabaseManager.deleteAll();
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


        final List<DownloadInfo> downloadInfoList = new ArrayList<>();
        for (Request request : requestList) {
            final DownloadInfo downloadInfo = FetchTypeConverterExtensions.toDownloadInfo(request, new DownloadInfo());
            downloadInfoList.add(downloadInfo);
        }
        for (int i = 0; i <= 4; i++) {
            downloadInfoList.get(i).setStatus(Status.QUEUED);
        }
        for (int i = 5; i < 10; i++) {
            downloadInfoList.get(i).setStatus(Status.DOWNLOADING);
        }

        fetchDatabaseManager.insert(downloadInfoList);
        List<Status> statuses = new ArrayList<>();
        statuses.add(Status.QUEUED);
        statuses.add(Status.DOWNLOADING);
        final List<DownloadInfo> queryList = fetchDatabaseManager.getDownloadsInGroupWithStatus(group, statuses);

        assertNotNull(queryList);
        assertEquals(downloadInfoList.size(), queryList.size());
        int queuedCount = 0;
        int downloadingCount = 0;
        for (DownloadInfo download : queryList) {
            assertTrue(downloadInfoList.contains(download));
            if (download.getStatus() == Status.QUEUED) {
                queuedCount++;
            } else if (download.getStatus() == Status.DOWNLOADING) {
                downloadingCount++;
            }
        }
        assertEquals(5, queuedCount);
        assertEquals(5, downloadingCount);
    }

    @Test
    public void fileUpdate() throws Exception {
        fetchDatabaseManager.deleteAll();
        final Request request = getTestRequest();
        final DownloadInfo downloadInfo = FetchTypeConverterExtensions.toDownloadInfo(request, new DownloadInfo());
        final Pair<DownloadInfo, Boolean> pair = fetchDatabaseManager.insert(downloadInfo);
        assertTrue(pair.getSecond());
        final long downloaded = 2000;
        final long total = Long.MAX_VALUE;
        final Status status = Status.DOWNLOADING;
        downloadInfo.setDownloaded(downloaded);
        downloadInfo.setTotal(total);
        downloadInfo.setStatus(status);
        fetchDatabaseManager.updateFileBytesInfoAndStatusOnly(downloadInfo);
        final DownloadInfo downloadInfo1 = fetchDatabaseManager.get(downloadInfo.getId());
        assertNotNull(downloadInfo1);
        assertEquals(downloaded, downloadInfo1.getDownloaded());
        assertEquals(total, downloadInfo1.getTotal());
        assertEquals(status, downloadInfo1.getStatus());
    }

    @Test
    public void closed() throws Exception {
        assertFalse(fetchDatabaseManager.isClosed());
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