package com.tonyodev.fetch2;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.tonyodev.fetch2.database.DatabaseManager;
import com.tonyodev.fetch2.database.DatabaseManagerImpl;
import com.tonyodev.fetch2.database.DownloadDatabase;
import com.tonyodev.fetch2.database.DownloadInfo;
import com.tonyodev.fetch2.database.migration.Migration;
import com.tonyodev.fetch2.util.FetchDatabaseExtensions;
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
public class DatabaseInstrumentedTest {

    private DatabaseManager databaseManager;
    private Context appContext;

    @Before
    public void useAppContext() throws Exception {
        // Context of the app under test.
        appContext = InstrumentationRegistry.getTargetContext();
        assertEquals("com.tonyodev.fetch2.test", appContext.getPackageName());
        final String namespace = "fetch2DatabaseTest";
        final Migration[] migrations = DownloadDatabase.getMigrations();
        FetchLogger fetchLogger = new FetchLogger(true, namespace);
        databaseManager = new DatabaseManagerImpl(appContext, namespace,
                true, fetchLogger, migrations);
    }

    @After
    public void cleanup() throws Exception {
        databaseManager.deleteAll();
        databaseManager.close();
    }

    @Test
    public void clear() throws Exception {
        databaseManager.deleteAll();
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
            final DownloadInfo downloadInfo = FetchTypeConverterExtensions.toDownloadInfo(request);
            downloadInfoList.add(downloadInfo);
        }
        databaseManager.insert(downloadInfoList);
        databaseManager.deleteAll();
        final List<DownloadInfo> downloads = databaseManager.get();
        assertEquals(0, downloads.size());
    }

    @Test
    public void insertSingle() throws Exception {
        databaseManager.deleteAll();
        final Request request = getTestRequest();
        final DownloadInfo downloadInfo = FetchTypeConverterExtensions.toDownloadInfo(request);
        final Pair<DownloadInfo, Boolean> pair = databaseManager.insert(downloadInfo);
        assertTrue(pair.getSecond());
    }


    @Test
    public void multiInsert() throws Exception {
        databaseManager.deleteAll();
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
            final DownloadInfo downloadInfo = FetchTypeConverterExtensions.toDownloadInfo(request);
            downloadInfoList.add(downloadInfo);
        }
        final List<Pair<DownloadInfo, Boolean>> insertedList = databaseManager.insert(downloadInfoList);
        for (Pair<DownloadInfo, Boolean> downloadInfoBooleanPair : insertedList) {
            assertTrue(downloadInfoBooleanPair.getSecond());
        }
    }

    @Test
    public void deleteSingle() throws Exception {
        databaseManager.deleteAll();
        final Request request = getTestRequest();
        final DownloadInfo downloadInfo = FetchTypeConverterExtensions.toDownloadInfo(request);
        databaseManager.insert(downloadInfo);
        databaseManager.delete(downloadInfo);
        final Download query = databaseManager.get(downloadInfo.getId());
        assertNull(query);
    }

    @Test
    public void deleteMulti() throws Exception {
        databaseManager.deleteAll();
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
            final DownloadInfo downloadInfo = FetchTypeConverterExtensions.toDownloadInfo(request);
            downloadInfoList.add(downloadInfo);
        }
        databaseManager.insert(downloadInfoList);
        databaseManager.delete(downloadInfoList);
        final List<Integer> ids = new ArrayList<>();
        for (DownloadInfo downloadInfo : downloadInfoList) {
            ids.add(downloadInfo.getId());
        }
        final List<DownloadInfo> queryList = databaseManager.get(ids);
        assertEquals(0, queryList.size());
    }

    @Test
    public void update() throws Exception {
        databaseManager.deleteAll();
        final Request request = getTestRequest();
        final DownloadInfo downloadInfo = FetchTypeConverterExtensions.toDownloadInfo(request);
        databaseManager.insert(downloadInfo);
        final int groupId = 2;
        final Priority priority = Priority.HIGH;
        final Status status = Status.QUEUED;
        downloadInfo.setGroup(groupId);
        downloadInfo.setPriority(priority);
        downloadInfo.setStatus(status);
        databaseManager.update(downloadInfo);
        final Download query = databaseManager.get(downloadInfo.getId());
        assertNotNull(query);
        assertEquals(groupId, query.getGroup());
        assertEquals(priority, query.getPriority());
        assertEquals(status, query.getStatus());
    }

    @Test
    public void updateMulti() throws Exception {
        databaseManager.deleteAll();
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
            final DownloadInfo downloadInfo = FetchTypeConverterExtensions.toDownloadInfo(request);
            downloadInfoList.add(downloadInfo);
        }
        databaseManager.insert(downloadInfoList);
        final int groupId = 2;
        final Priority priority = Priority.HIGH;
        final Status status = Status.QUEUED;
        for (int i = 0; i < downloadInfoList.size(); i++) {
            final DownloadInfo downloadInfo = downloadInfoList.get(i);
            downloadInfo.setGroup(groupId);
            downloadInfo.setPriority(priority);
            downloadInfo.setStatus(status);
        }
        databaseManager.update(downloadInfoList);
        final List<Integer> ids = new ArrayList<>();
        for (DownloadInfo downloadInfo : downloadInfoList) {
            ids.add(downloadInfo.getId());
        }
        final List<DownloadInfo> queryList = databaseManager.get(ids);
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
        databaseManager.deleteAll();
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
            final DownloadInfo downloadInfo = FetchTypeConverterExtensions.toDownloadInfo(request);
            downloadInfoList.add(downloadInfo);
        }
        databaseManager.insert(downloadInfoList);
        final List<DownloadInfo> queryList = databaseManager.get();
        assertNotNull(queryList);
        assertEquals(downloadInfoList.size(), queryList.size());
    }


    @Test
    public void getId() throws Exception {
        databaseManager.deleteAll();
        final Request request = getTestRequest();
        final DownloadInfo downloadInfo = FetchTypeConverterExtensions.toDownloadInfo(request);
        databaseManager.insert(downloadInfo);
        final Download query = databaseManager.get(downloadInfo.getId());
        assertNotNull(query);
        assertEquals(downloadInfo.getId(), query.getId());
    }

    @Test
    public void getIds() throws Exception {
        databaseManager.deleteAll();
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
            final DownloadInfo downloadInfo = FetchTypeConverterExtensions.toDownloadInfo(request);
            downloadInfoList.add(downloadInfo);
        }
        databaseManager.insert(downloadInfoList);
        final List<Integer> ids = new ArrayList<>();
        for (DownloadInfo downloadInfo : downloadInfoList) {
            ids.add(downloadInfo.getId());
        }
        final List<DownloadInfo> queryList = databaseManager.get(ids);
        assertNotNull(queryList);
        assertEquals(downloadInfoList.size(), queryList.size());
        for (DownloadInfo downloadInfo : queryList) {
            assertNotNull(downloadInfo);
            assertTrue(ids.contains(downloadInfo.getId()));
        }
    }

    @Test
    public void getStatus() throws Exception {
        databaseManager.deleteAll();
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
            final DownloadInfo downloadInfo = FetchTypeConverterExtensions.toDownloadInfo(request);
            downloadInfoList.add(downloadInfo);
        }
        for (DownloadInfo downloadInfo : downloadInfoList) {
            downloadInfo.setStatus(status);
        }
        databaseManager.insert(downloadInfoList);
        final List<DownloadInfo> queryList = databaseManager.getByStatus(status);
        assertNotNull(queryList);
        assertEquals(downloadInfoList.size(), queryList.size());
        for (DownloadInfo download : queryList) {
            assertTrue(downloadInfoList.contains(download));
            assertEquals(status, download.getStatus());
        }
    }


    @Test
    public void getGroup() throws Exception {
        databaseManager.deleteAll();
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
            final DownloadInfo downloadInfo = FetchTypeConverterExtensions.toDownloadInfo(request);
            downloadInfoList.add(downloadInfo);
        }
        for (DownloadInfo downloadInfo : downloadInfoList) {
            downloadInfo.setGroup(group);
        }
        databaseManager.insert(downloadInfoList);
        final List<DownloadInfo> queryList = databaseManager.getByGroup(group);
        assertNotNull(queryList);
        assertEquals(downloadInfoList.size(), queryList.size());
        for (DownloadInfo download : queryList) {
            assertTrue(downloadInfoList.contains(download));
            assertEquals(group, download.getGroup());
        }
    }

    @Test
    public void databaseVerification() throws Exception {
        databaseManager.deleteAll();
        final List<Request> requests = getTestRequestList(20);
        final List<DownloadInfo> downloadInfoList = new ArrayList<>(20);
        for (Request request : requests) {
            final DownloadInfo downloadInfo = FetchTypeConverterExtensions.toDownloadInfo(request);
            downloadInfo.setStatus(Status.DOWNLOADING);
            downloadInfoList.add(downloadInfo);
        }
        databaseManager.insert(downloadInfoList);
        int size = databaseManager.get().size();
        assertEquals(20, size);
        FetchDatabaseExtensions.verifyDatabase(databaseManager);
        final List<DownloadInfo> downloads = databaseManager.get();
        for (DownloadInfo download : downloads) {
            assertNotNull(download);
            assertEquals(Status.QUEUED, download.getStatus());
        }
    }

    @Test
    public void getGroupWithStatus() throws Exception {
        databaseManager.deleteAll();
        final String url = "http://www.example.com/test.txt";
        final String dir = appContext.getFilesDir() + "/testFolder/";
        final List<Request> requestList = new ArrayList<>();
        final Status status = Status.QUEUED;
        final int group = 10;
        for (int i = 0; i < 10; i++) {
            final String file = dir + "test" + i + ".txt";
            final Request request = new Request(url, file);
            request.setGroupId(group);
            requestList.add(request);
        }
        final List<DownloadInfo> downloadInfoList = new ArrayList<>();
        for (Request request : requestList) {
            final DownloadInfo downloadInfo = FetchTypeConverterExtensions.toDownloadInfo(request);
            downloadInfoList.add(downloadInfo);
        }
        for (DownloadInfo downloadInfo : downloadInfoList) {
            downloadInfo.setStatus(status);
        }
        databaseManager.insert(downloadInfoList);
        final List<DownloadInfo> queryList = databaseManager.getDownloadsInGroupWithStatus(group, status);
        assertNotNull(queryList);
        assertEquals(downloadInfoList.size(), queryList.size());
        for (DownloadInfo download : queryList) {
            assertTrue(downloadInfoList.contains(download));
            assertEquals(status, download.getStatus());
        }
    }

    @Test
    public void fileUpdate() throws Exception {
        databaseManager.deleteAll();
        final Request request = getTestRequest();
        final DownloadInfo downloadInfo = FetchTypeConverterExtensions.toDownloadInfo(request);
        final Pair<DownloadInfo, Boolean> pair = databaseManager.insert(downloadInfo);
        assertTrue(pair.getSecond());
        final long downloaded = 2000;
        final long total = Long.MAX_VALUE;
        final Status status = Status.DOWNLOADING;
        downloadInfo.setDownloaded(downloaded);
        downloadInfo.setTotal(total);
        downloadInfo.setStatus(status);
        databaseManager.updateFileBytesInfoAndStatusOnly(downloadInfo);
        final DownloadInfo downloadInfo1 = databaseManager.get(downloadInfo.getId());
        assertNotNull(downloadInfo1);
        assertEquals(downloaded, downloadInfo1.getDownloaded());
        assertEquals(total, downloadInfo1.getTotal());
        assertEquals(status, downloadInfo1.getStatus());
    }

    @Test
    public void closed() throws Exception {
        assertFalse(databaseManager.isClosed());
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