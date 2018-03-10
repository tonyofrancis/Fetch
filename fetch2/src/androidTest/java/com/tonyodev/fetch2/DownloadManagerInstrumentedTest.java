package com.tonyodev.fetch2;

import android.content.Context;
import android.os.Handler;
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
import com.tonyodev.fetch2.helper.DownloadInfoUpdater;
import com.tonyodev.fetch2.provider.ListenerProvider;
import com.tonyodev.fetch2.provider.NetworkInfoProvider;
import com.tonyodev.fetch2.util.FetchDefaults;
import com.tonyodev.fetch2.util.FetchTypeConverterExtensions;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


@RunWith(AndroidJUnit4.class)
public class DownloadManagerInstrumentedTest {

    private DownloadManager downloadManager;
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
        final Downloader client = FetchDefaults.getDefaultDownloader();
        final long progessInterval = FetchDefaults.DEFAULT_PROGRESS_REPORTING_INTERVAL_IN_MILLISECONDS;
        final int concurrentLimit = FetchDefaults.DEFAULT_CONCURRENT_LIMIT;
        final int bufferSize = FetchDefaults.DEFAULT_DOWNLOAD_BUFFER_SIZE_BYTES;
        final NetworkInfoProvider networkInfoProvider = new NetworkInfoProvider(appContext);
        final boolean retryOnNetworkGain = false;
        final ListenerProvider listenerProvider = new ListenerProvider();
        final Handler uiHandler = new Handler(Looper.getMainLooper());
        final DownloadInfoUpdater downloadInfoUpdater = new DownloadInfoUpdater(databaseManager);
        downloadManager = new DownloadManagerImpl(client, concurrentLimit,
                progessInterval, bufferSize, fetchLogger, networkInfoProvider, retryOnNetworkGain,
                listenerProvider, uiHandler, downloadInfoUpdater);
    }

    @After
    public void cleanUp() throws Exception {
        downloadManager.close();
        assertTrue(downloadManager.isClosed());
        databaseManager.close();
        assertTrue(databaseManager.isClosed());
    }

    @Test
    public void start() throws Exception {
        final Request request = getTestRequest();
        final DownloadInfo downloadInfo = FetchTypeConverterExtensions.toDownloadInfo(request);
        final Boolean started = downloadManager.start(downloadInfo);
        downloadManager.cancel(downloadInfo.getId());
        deleteTestFile(downloadInfo.getFile());
        assertEquals(started, true);
    }

    @Test
    public void cancel() throws Exception {
        final Request request = getTestRequest();
        final DownloadInfo downloadInfo = FetchTypeConverterExtensions.toDownloadInfo(request);
        downloadManager.start(downloadInfo);
        final Boolean cancelled = downloadManager.cancel(downloadInfo.getId());
        deleteTestFile(downloadInfo.getFile());
        assertEquals(cancelled, true);
    }

    @Test
    public void cancelAll() throws Exception {
        final Request request = getTestRequest();
        final DownloadInfo downloadInfo = FetchTypeConverterExtensions.toDownloadInfo(request);
        downloadManager.start(downloadInfo);
        downloadManager.cancelAll();
        deleteTestFile(downloadInfo.getFile());
        assertEquals(downloadManager.getActiveDownloadCount(), 0);
    }

    @Test
    public void contains() throws Exception {
        final Request request = getTestRequest();
        final DownloadInfo downloadInfo = FetchTypeConverterExtensions.toDownloadInfo(request);
        downloadManager.start(downloadInfo);
        final Boolean contains = downloadManager.contains(downloadInfo.getId());
        downloadManager.cancel(downloadInfo.getId());
        deleteTestFile(downloadInfo.getFile());
        assertEquals(contains, true);
    }

    @Test
    public void canAccommodateNewDownload() throws Exception {
        assertEquals(downloadManager.canAccommodateNewDownload(), true);
    }

    public Request getTestRequest() {
        final String url = "http://download.blender.org/peach/bigbuckbunny_movies/BigBuckBunny_320x180.mp4";
        final String file = appContext.getFilesDir() + "/testFolder/bunny" +
                System.nanoTime() + " .mp4";
        return new Request(url, file);
    }

    public void deleteTestFile(String fileString) {
        final File file = new File(fileString);
        if (file.exists()) {
            file.delete();
        }
    }
}