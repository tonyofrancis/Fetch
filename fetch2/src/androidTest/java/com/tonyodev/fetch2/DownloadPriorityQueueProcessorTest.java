package com.tonyodev.fetch2;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.tonyodev.fetch2.database.DatabaseManager;
import com.tonyodev.fetch2.database.DatabaseManagerImpl;
import com.tonyodev.fetch2.downloader.DownloadManager;
import com.tonyodev.fetch2.downloader.DownloadManagerImpl;
import com.tonyodev.fetch2.helper.PriorityQueueProcessor;
import com.tonyodev.fetch2.helper.PriorityQueueProcessorImpl;
import com.tonyodev.fetch2.provider.DownloadProvider;
import com.tonyodev.fetch2.provider.NetworkProvider;
import com.tonyodev.fetch2.util.FetchDefaults;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class DownloadPriorityQueueProcessorTest {

    private PriorityQueueProcessor<Download> priorityQueueProcessorImpl;

    @Before
    public void useAppContext() throws Exception {
        final Context appContext = InstrumentationRegistry.getTargetContext();
        final HandlerThread handlerThread = new HandlerThread("test");
        handlerThread.start();
        final Handler handler = new Handler(handlerThread.getLooper());
        final String namespace = "fetch2DatabaseTest";
        final FetchLogger fetchLogger = new FetchLogger(true, namespace);
        final DatabaseManager databaseManager = new DatabaseManagerImpl(appContext, namespace,
                true, fetchLogger);
        final Downloader client = FetchDefaults.getDefaultDownloader();
        final long progessInterval = FetchDefaults.DEFAULT_PROGRESS_REPORTING_INTERVAL_IN_MILLISECONDS;
        final int concurrentLimit = FetchDefaults.DEFAULT_CONCURRENT_LIMIT;
        final int bufferSize = FetchDefaults.DEFAULT_DOWNLOAD_BUFFER_SIZE_BYTES;
        final DownloadManager downloadManager = new DownloadManagerImpl(client, concurrentLimit,
                progessInterval, bufferSize, fetchLogger);
        priorityQueueProcessorImpl = new PriorityQueueProcessorImpl(
                handler,
                new DownloadProvider(databaseManager),
                downloadManager,
                new NetworkProvider(appContext),
                fetchLogger);
    }

    @Test
    public void pause() throws Exception {
        priorityQueueProcessorImpl.stop();
        priorityQueueProcessorImpl.start();
        priorityQueueProcessorImpl.pause();
        assertTrue(priorityQueueProcessorImpl.isPaused());
    }

    @Test
    public void resume() throws Exception {
        priorityQueueProcessorImpl.stop();
        priorityQueueProcessorImpl.start();
        priorityQueueProcessorImpl.pause();
        priorityQueueProcessorImpl.resume();
        assertFalse(priorityQueueProcessorImpl.isPaused());
    }

    @Test
    public void start() throws Exception {
        priorityQueueProcessorImpl.stop();
        priorityQueueProcessorImpl.start();
        assertFalse(priorityQueueProcessorImpl.isStopped());
    }

    @Test
    public void stop() throws Exception {
        priorityQueueProcessorImpl.stop();
        priorityQueueProcessorImpl.start();
        priorityQueueProcessorImpl.stop();
        assertTrue(priorityQueueProcessorImpl.isStopped());
    }

    @After
    public void cleanup() throws Exception {
        priorityQueueProcessorImpl.stop();
    }

}
