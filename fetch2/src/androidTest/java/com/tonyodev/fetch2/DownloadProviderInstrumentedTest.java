package com.tonyodev.fetch2;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.tonyodev.fetch2.database.DatabaseManager;
import com.tonyodev.fetch2.database.DatabaseManagerImpl;
import com.tonyodev.fetch2.database.DownloadDatabase;
import com.tonyodev.fetch2.database.DownloadInfo;
import com.tonyodev.fetch2.database.migration.Migration;
import com.tonyodev.fetch2.provider.DownloadProvider;
import com.tonyodev.fetch2.util.FetchTypeConverterExtensions;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


@RunWith(AndroidJUnit4.class)
public class DownloadProviderInstrumentedTest {

    private DatabaseManager databaseManager;
    private Context appContext;
    private DownloadProvider downloadProvider;

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
        downloadProvider = new DownloadProvider(databaseManager);
    }

    @After
    public void cleanup() throws Exception {
        databaseManager.deleteAll();
        databaseManager.close();
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
        final List<Download> queryList = downloadProvider.getDownloads();
        assertNotNull(queryList);
        assertEquals(downloadInfoList.size(), queryList.size());
    }


    @Test
    public void getId() throws Exception {
        databaseManager.deleteAll();
        final Request request = getTestRequest();
        final DownloadInfo downloadInfo = FetchTypeConverterExtensions.toDownloadInfo(request);
        databaseManager.insert(downloadInfo);
        final Download query = downloadProvider.getDownload(downloadInfo.getId());
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
        for(DownloadInfo downloadInfo : downloadInfoList) {
            ids.add(downloadInfo.getId());
        }
        final List<Download> queryList = downloadProvider.getDownloads(ids);
        assertNotNull(queryList);
        assertEquals(downloadInfoList.size(), queryList.size());
        for (Download download : queryList) {
            assertNotNull(download);
            assertTrue(ids.contains(download.getId()));
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
        final List<Download> queryList = downloadProvider.getByStatus(status);
        assertNotNull(queryList);
        assertEquals(downloadInfoList.size(), queryList.size());
        for (Download download : queryList) {
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
        final List<Download> queryList = downloadProvider.getByGroup(group);
        assertNotNull(queryList);
        assertEquals(downloadInfoList.size(), queryList.size());
        for (Download download : queryList) {
            assertEquals(group, download.getGroup());
        }
    }

    public Request getTestRequest() {
        final String url = "http://www.example.com/test.txt";
        final String file = appContext.getFilesDir() + "/testFolder/test" + System.nanoTime() + ".txt";
        return new Request(url, file);
    }

}