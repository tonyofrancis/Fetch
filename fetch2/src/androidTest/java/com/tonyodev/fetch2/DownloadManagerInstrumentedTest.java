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
import com.tonyodev.fetch2.downloader.DownloadManagerCoordinator;
import com.tonyodev.fetch2.fetch.ListenerCoordinator;
import com.tonyodev.fetch2.fetch.LiveSettings;
import com.tonyodev.fetch2.helper.DownloadInfoUpdater;
import com.tonyodev.fetch2.provider.NetworkInfoProvider;
import com.tonyodev.fetch2.util.FetchDefaults;
import com.tonyodev.fetch2.util.FetchTypeConverterExtensions;
import com.tonyodev.fetch2core.Downloader;
import com.tonyodev.fetch2core.FetchCoreDefaults;
import com.tonyodev.fetch2core.FetchCoreUtils;
import com.tonyodev.fetch2core.FetchLogger;
import com.tonyodev.fetch2core.FileServerDownloader;
import com.tonyodev.fetch2core.HandlerWrapper;

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
        final LiveSettings liveSettings = new LiveSettings(namespace);
        databaseManager = new DatabaseManagerImpl(appContext, namespace, migrations, liveSettings, false);
        final Downloader client = FetchDefaults.getDefaultDownloader();
        final FileServerDownloader serverDownloader = FetchDefaults.getDefaultFileServerDownloader();
        final long progessInterval = FetchCoreDefaults.DEFAULT_PROGRESS_REPORTING_INTERVAL_IN_MILLISECONDS;
        final int concurrentLimit = FetchDefaults.DEFAULT_CONCURRENT_LIMIT;
        final NetworkInfoProvider networkInfoProvider = new NetworkInfoProvider(appContext);
        final boolean retryOnNetworkGain = false;
        final Handler uiHandler = new Handler(Looper.getMainLooper());
        final DownloadInfoUpdater downloadInfoUpdater = new DownloadInfoUpdater(databaseManager);
        final String tempDir = FetchCoreUtils.getFileTempDir(appContext);
        final DownloadManagerCoordinator downloadManagerCoordinator = new DownloadManagerCoordinator(namespace);
        final ListenerCoordinator listenerCoordinator = new ListenerCoordinator(namespace);
        downloadManager = new DownloadManagerImpl(client, concurrentLimit,
                progessInterval, fetchLogger, networkInfoProvider, retryOnNetworkGain,
                downloadInfoUpdater, tempDir, downloadManagerCoordinator,
                listenerCoordinator, serverDownloader, false, uiHandler);
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