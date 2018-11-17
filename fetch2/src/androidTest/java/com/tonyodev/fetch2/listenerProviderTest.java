package com.tonyodev.fetch2;

import android.support.test.runner.AndroidJUnit4;

import com.tonyodev.fetch2.fetch.ListenerCoordinator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertNotNull;


@RunWith(AndroidJUnit4.class)
public class listenerProviderTest {

    private ListenerCoordinator listenerProvider;

    @Before
    public void setUp() throws Exception {
        listenerProvider = new ListenerCoordinator("listenerprovider");
    }

    @Test
    public void hasMainListener() throws Exception {
        assertNotNull(listenerProvider.getMainListener());
    }

    @After
    public void cleanUp() throws Exception {

    }
}