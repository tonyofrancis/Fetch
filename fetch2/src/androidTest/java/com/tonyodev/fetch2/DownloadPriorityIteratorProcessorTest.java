package com.tonyodev.fetch2;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.tonyodev.fetch2.database.DatabaseManager;
import com.tonyodev.fetch2.database.DatabaseManagerImpl;
import com.tonyodev.fetch2.database.DownloadDatabase;
import com.tonyodev.fetch2.database.migration.Migration;
import com.tonyodev.fetch2.downloader.DownloadManager;
import com.tonyodev.fetch2.downloader.DownloadManagerImpl;
import com.tonyodev.fetch2.helper.PriorityIteratorProcessor;
import com.tonyodev.fetch2.helper.PriorityIteratorProcessorImpl;
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

    private PriorityIteratorProcessor<Download> priorityIteratorProcessorImpl;

    @Before
    public void useAppContext() throws Exception {
        final Context appContext = InstrumentationRegistry.getTargetContext();
        final HandlerThread handlerThread = new HandlerThread("test");
        handlerThread.start();
        final Handler handler = new Handler(handlerThread.getLooper());
        final String namespace = "fetch2DatabaseTest";
        final FetchLogger fetchLogger = new FetchLogger(true, namespace);
        final Migration[] migrations = DownloadDatabase.getMigrations();
        final DatabaseManager databaseManager = new DatabaseManagerImpl(appContext, namespace,
                true, fetchLogger, migrations);
        final Downloader client = FetchDefaults.getDefaultDownloader();
        final long progessInterval = FetchDefaults.DEFAULT_PROGRESS_REPORTING_INTERVAL_IN_MILLISECONDS;
        final int concurrentLimit = FetchDefaults.DEFAULT_CONCURRENT_LIMIT;
        final int bufferSize = FetchDefaults.DEFAULT_DOWNLOAD_BUFFER_SIZE_BYTES;
        final NetworkInfoProvider networkInfoProvider = new NetworkInfoProvider(appContext);
        final boolean retryOnNetworkGain = false;
        final DownloadManager downloadManager = new DownloadManagerImpl(client, concurrentLimit,
                progessInterval, bufferSize, fetchLogger, networkInfoProvider, retryOnNetworkGain);
        priorityIteratorProcessorImpl = new PriorityIteratorProcessorImpl(
                handler,
                new DownloadProvider(databaseManager),
                downloadManager,
                new NetworkInfoProvider(appContext),
                fetchLogger);
    }

    @Test
    public void pause() throws Exception {
        priorityIteratorProcessorImpl.stop();
        priorityIteratorProcessorImpl.start();
        priorityIteratorProcessorImpl.pause();
        assertTrue(priorityIteratorProcessorImpl.isPaused());
    }

    @Test
    public void resume() throws Exception {
        priorityIteratorProcessorImpl.stop();
        priorityIteratorProcessorImpl.start();
        priorityIteratorProcessorImpl.pause();
        priorityIteratorProcessorImpl.resume();
        assertFalse(priorityIteratorProcessorImpl.isPaused());
    }

    @Test
    public void start() throws Exception {
        priorityIteratorProcessorImpl.stop();
        priorityIteratorProcessorImpl.start();
        assertFalse(priorityIteratorProcessorImpl.isStopped());
    }

    @Test
    public void stop() throws Exception {
        priorityIteratorProcessorImpl.stop();
        priorityIteratorProcessorImpl.start();
        priorityIteratorProcessorImpl.stop();
        assertTrue(priorityIteratorProcessorImpl.isStopped());
    }

    @After
    public void cleanup() throws Exception {
        priorityIteratorProcessorImpl.stop();
    }

}
