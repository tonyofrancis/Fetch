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
import com.tonyodev.fetch2.database.FetchDatabaseManagerWrapper;
import com.tonyodev.fetch2.database.migration.Migration;
import com.tonyodev.fetch2.downloader.DownloadManager;
import com.tonyodev.fetch2.downloader.DownloadManagerImpl;
import com.tonyodev.fetch2.downloader.DownloadManagerCoordinator;
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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class DownloadPriorityIteratorProcessorTest {

    private PriorityListProcessor<Download> priorityListProcessorImpl;

    @Before
    public void useAppContext() throws Exception {
        final Context appContext = InstrumentationRegistry.getTargetContext();
        final HandlerThread handlerThread = new HandlerThread("test");
        handlerThread.start();
        final String namespace = "fetch2DatabaseTest";
        final FetchLogger fetchLogger = new FetchLogger(true, namespace);
        final Migration[] migrations = DownloadDatabase.getMigrations();
        final LiveSettings liveSettings = new LiveSettings(namespace);
        DefaultStorageResolver defaultStorageResolver = new DefaultStorageResolver(appContext, FetchCoreUtils.getFileTempDir(appContext));
        final FetchDatabaseManager fetchDatabaseManager = new FetchDatabaseManagerImpl(appContext, namespace, fetchLogger, migrations, liveSettings,
                false, defaultStorageResolver);
        final FetchDatabaseManagerWrapper databaseManagerWrapper = new FetchDatabaseManagerWrapper(fetchDatabaseManager);
        final Downloader client = FetchDefaults.getDefaultDownloader();
        final FileServerDownloader serverDownloader = FetchDefaults.getDefaultFileServerDownloader();
        final long progessInterval = FetchCoreDefaults.DEFAULT_PROGRESS_REPORTING_INTERVAL_IN_MILLISECONDS;
        final int concurrentLimit = FetchDefaults.DEFAULT_CONCURRENT_LIMIT;
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
                new HandlerWrapper(namespace, null),
                new DownloadProvider(databaseManagerWrapper),
                downloadManager,
                new NetworkInfoProvider(appContext, null),
                fetchLogger,
                listenerCoordinator,
                concurrentLimit,
                appContext,
                namespace,
                PrioritySort.ASC);
    }

    @Test
    public void pause() throws Exception {
        priorityListProcessorImpl.stop();
        priorityListProcessorImpl.start();
        priorityListProcessorImpl.pause();
        assertTrue(priorityListProcessorImpl.isPaused());
    }

    @Test
    public void resume() throws Exception {
        priorityListProcessorImpl.stop();
        priorityListProcessorImpl.start();
        priorityListProcessorImpl.pause();
        priorityListProcessorImpl.resume();
        assertFalse(priorityListProcessorImpl.isPaused());
    }

    @Test
    public void start() throws Exception {
        priorityListProcessorImpl.stop();
        priorityListProcessorImpl.start();
        assertFalse(priorityListProcessorImpl.isStopped());
    }

    @Test
    public void stop() throws Exception {
        priorityListProcessorImpl.stop();
        priorityListProcessorImpl.start();
        priorityListProcessorImpl.stop();
        assertTrue(priorityListProcessorImpl.isStopped());
    }

    @After
    public void cleanup() throws Exception {
        priorityListProcessorImpl.stop();
    }

}
