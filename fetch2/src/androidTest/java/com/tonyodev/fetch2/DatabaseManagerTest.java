package com.tonyodev.fetch2;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.tonyodev.fetch2.database.DatabaseManager;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Created by tonyofrancis on 7/3/17.
 */

@RunWith(AndroidJUnit4.class)
public class DatabaseManagerTest {

    private DatabaseManager databaseManager;

    @Before
    public void setUp() throws Exception {
        Context context = InstrumentationRegistry.getTargetContext();
        databaseManager = new DatabaseManager(context);
    }

    @Test
    public void databaseNotNull() throws Exception {
        Assert.assertNotNull(databaseManager);
    }
}
