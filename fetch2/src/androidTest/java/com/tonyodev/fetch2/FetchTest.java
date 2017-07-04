package com.tonyodev.fetch2;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.tonyodev.fetch2.listener.FetchListener;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by tonyofrancis on 7/3/17.
 */
@RunWith(AndroidJUnit4.class)
public class FetchTest {

    private Fetch fetch;

    @Before
    public void setUp() throws Exception {
        if (!Fetch.isInitialized()) {
            Fetch.init(getContext());
        }
        fetch = Fetch.getInstance();
    }

    @Test
    public void fetchNotNull() throws Exception {
        Assert.assertNotNull(fetch);
    }

    @Test
    public void fetchIsInit() throws Exception {
        Assert.assertTrue(Fetch.isInitialized());
    }

    @Test
    public void addListener() throws Exception {
        clearListeners();
        FetchListener listener = getListener();
        fetch.addListener(listener);
        List<FetchListener> listenerList = fetch.getListeners();
        Set<FetchListener> set = new HashSet<>(listenerList);
        Assert.assertTrue(set.contains(listener));
    }

    @Test
    public void removeListener() throws Exception {
        clearListeners();
        FetchListener listener = getListener();
        fetch.addListener(listener);
        fetch.removeListener(listener);
        Assert.assertEquals(0,fetch.getListeners().size());
    }

    @Test
    public void removeListeners() throws Exception {
        clearListeners();
        fetch.addListener(getListener());
        fetch.addListener(getListener());
        fetch.removeListeners();
        Assert.assertEquals(0,fetch.getListeners().size());
    }

    @Test
    public void fetchGetListener() throws Exception {
        clearListeners();
        fetch.addListener(getListener());
        fetch.addListener(getListener());
        List<FetchListener> listeners = fetch.getListeners();

        Assert.assertNotNull(listeners);
        Assert.assertEquals(2,listeners.size());
    }

    private Context getContext() {
        return InstrumentationRegistry.getTargetContext();
    }


    private void clearListeners() {
        fetch.removeListeners();
    }

    private FetchListener getListener() {
        return new FetchListener() {
            @Override
            public void onAttach(@NonNull Fetch fetch) {

            }

            @Override
            public void onDetach(@NonNull Fetch fetch) {

            }

            @Override
            public void onComplete(long id, int progress, long downloadedBytes, long totalBytes) {

            }

            @Override
            public void onError(long id, @NonNull Error error, int progress, long downloadedBytes, long totalBytes) {

            }

            @Override
            public void onProgress(long id, int progress, long downloadedBytes, long totalBytes) {

            }

            @Override
            public void onPaused(long id, int progress, long downloadedBytes, long totalBytes) {

            }

            @Override
            public void onCancelled(long id, int progress, long downloadedBytes, long totalBytes) {

            }

            @Override
            public void onRemoved(long id, int progress, long downloadedBytes, long totalBytes) {

            }
        };
    }
}
