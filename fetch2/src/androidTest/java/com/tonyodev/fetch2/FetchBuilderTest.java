package com.tonyodev.fetch2;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.tonyodev.fetch2core.FetchLogger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;


@RunWith(AndroidJUnit4.class)
public class FetchBuilderTest {

    private Context appContext;

    @Before
    public void useAppContext() throws Exception {
        // Context of the app under test.
        appContext = InstrumentationRegistry.getTargetContext();

    }

    @Test
    public void hasCorrectFetchPreferences() throws Exception {
        final String namespace = "_fetch_";
        final int bufferSize = 1;
        final boolean loggingEnabled = true;
        final long progressInterval = 10;
        final int concurrentLimit = 2000;
        final NetworkType networkType = NetworkType.WIFI_ONLY;
        final FetchLogger logger = new FetchLogger();
        final HttpUrlConnectionDownloader downloader = new HttpUrlConnectionDownloader();
        final boolean enableInMemoryDatabase = true;
        final FetchConfiguration.Builder builder =
                new FetchConfiguration.Builder(appContext)
                        .setNamespace(namespace)
                        .enableLogging(loggingEnabled)
                        .setProgressReportingInterval(progressInterval)
                        .setGlobalNetworkType(networkType)
                        .setDownloadConcurrentLimit(concurrentLimit)
                        .setLogger(logger)
                        .setHttpDownloader(downloader);
        final FetchConfiguration prefs = builder.build();
        assertEquals(namespace, prefs.getNamespace());
        assertEquals(loggingEnabled, prefs.getLoggingEnabled());
        assertEquals(progressInterval, prefs.getProgressReportingIntervalMillis());
        assertEquals(concurrentLimit, prefs.getConcurrentLimit());
        assertEquals(networkType, prefs.getGlobalNetworkType());
        assertEquals(logger, prefs.getLogger());
        assertEquals(downloader, prefs.getHttpDownloader());
    }

    @After
    public void cleanup() throws Exception {

    }

}
