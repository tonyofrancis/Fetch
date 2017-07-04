package com.tonyodev.fetch2;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.tonyodev.fetch2.database.Database;
import com.tonyodev.fetch2.database.DatabaseManager;
import com.tonyodev.fetch2.database.DatabaseRow;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by tonyofrancis on 7/3/17.
 */

@RunWith(AndroidJUnit4.class)
public class DatabaseManagerTest {

    public static final String TEST_GROUP_ID = "testGroup";
    public static final String TEST_URL = "http://www.tonyofrancis.com/tonyodev/fetchCore/bunny.m4v";
    public static final String TEST_FILE_PATH = "/storage/fetch/bunny.m4v";

    private Database database;

    @Before
    public void setUp() throws Exception {
        Context context = InstrumentationRegistry.getTargetContext();
        database = new DatabaseManager(context);
    }

    @Test
    public void databaseNotNull() throws Exception {
        Assert.assertNotNull(database);
    }

    @Test
    public void getWriteDatabase() throws Exception {
        Assert.assertNotNull(database.getWriteOnlyDatabase());
    }

    @Test
    public void getReadDatabase() throws Exception {
        Assert.assertNotNull(database.getReadOnlyDatabase());
    }

    @Test
    public void insertSingle() throws Exception {
        clearDatabase();
        final DatabaseRow databaseRow = getRow();
        database.insert(databaseRow);
    }

    @Test
    public void insertList() throws Exception {
        clearDatabase();
        final List<DatabaseRow> list = getRowList();
        database.insert(list);
    }

    @Test
    public void removeSingle() throws Exception {
        clearDatabase();
        final DatabaseRow databaseRow = getRow();
        database.insert(databaseRow);
        database.remove(databaseRow.getId());
        DatabaseRow row = database.query(databaseRow.getId());
        Assert.assertNull(row);
    }

    @Test
    public void removeGroup() throws Exception {
        clearDatabase();
        List<DatabaseRow> rows = getRowList();
        long[] ids = new long[rows.size()];

        for (int i = 0; i < ids.length; i++) {
            ids[i] = rows.get(i).getId();
        }

        database.insert(rows);
        database.remove(ids);
        List<DatabaseRow> list = database.query(ids);
        Assert.assertTrue(list.size() == 0);
    }

    @Test
    public void removeAll() throws Exception {
        clearDatabase();
        List<DatabaseRow> rows = getRowList();
        database.insert(rows);
        database.removeAll();
        List<DatabaseRow> list = database.query();
        Assert.assertTrue(list.size() == 0);
    }

    @Test
    public void contains() throws Exception {
        clearDatabase();
        final DatabaseRow row = getRow();
        database.insert(row);
        boolean contains = database.contains(row.getId());
        Assert.assertTrue(contains);
    }

    @Test
    public void containsNot() throws Exception {
        clearDatabase();
        DatabaseRow row = getRow();
        boolean contains = database.contains(row.getId());
        Assert.assertFalse(contains);
    }

    @Test
    public void queryByStatus() throws Exception {
        clearDatabase();
        List<DatabaseRow> rows = getRowList();

        for (DatabaseRow row : rows) {
            row.setStatus(Status.CANCELLED.getValue());
        }
        database.insert(rows);
        List<DatabaseRow> list = database.queryByStatus(Status.CANCELLED.getValue());
        Assert.assertNotNull(list);
        Assert.assertEquals(rows.size(),list.size());
    }

    @Test
    public void query() {
        clearDatabase();
        List<DatabaseRow> rows = getRowList();
        database.insert(rows);
        List<DatabaseRow> list = database.query();
        Assert.assertNotNull(list);
        Assert.assertEquals(rows.size(),list.size());
    }

    @Test
    public void queryIds() throws Exception {
        clearDatabase();
        List<DatabaseRow> rows = getRowList();
        long[] ids = new long[rows.size()];

        for (int i = 0; i < ids.length; i++) {
            ids[i] = rows.get(i).getId();
        }

        database.insert(rows);
        List<DatabaseRow> list = database.query(ids);
        Assert.assertNotNull(list);
        Assert.assertEquals(rows.size(),list.size());
    }

    @Test
    public void queryByGroupId() throws Exception {
        clearDatabase();
        DatabaseRow row = getRow();

        database.insert(row);
        List<DatabaseRow> list = database.queryByGroupId(TEST_GROUP_ID);
        Assert.assertNotNull(list);
        Assert.assertEquals(1,list.size());
    }

    @Test
    public void queryGroupByStatusId() throws Exception {
        clearDatabase();
        List<DatabaseRow> rows = getRowList();

        for (DatabaseRow row : rows) {
            row.setStatus(Status.CANCELLED.getValue());
        }

        database.insert(rows);
        List<DatabaseRow> list = database.queryGroupByStatusId(TEST_GROUP_ID,Status.CANCELLED.getValue());
        Assert.assertNotNull(list);
        Assert.assertEquals(rows.size(),list.size());
    }

    @Test
    public void querySingle() {
        clearDatabase();
        DatabaseRow row = getRow();
        database.insert(row);
        DatabaseRow databaseRow = database.query(row.getId());
        Assert.assertNotNull(databaseRow);
    }

    @Test
    public void updateDownloadedBytes() throws Exception {
        clearDatabase();
        final DatabaseRow row = getRow();
        database.insert(row);
        database.updateDownloadedBytes(row.getId(),100L);
        DatabaseRow databaseRow = database.query(row.getId());
        Assert.assertNotNull(databaseRow);
        Assert.assertEquals(100L,databaseRow.getDownloadedBytes());
    }

    @Test
    public void setDownloadedBytesAndTotalBytes() throws Exception {
        clearDatabase();
        final DatabaseRow row = getRow();
        database.insert(row);
        database.setDownloadedBytesAndTotalBytes(row.getId(),50L,100L);
        DatabaseRow databaseRow = database.query(row.getId());
        Assert.assertNotNull(databaseRow);
        Assert.assertEquals(50L,databaseRow.getDownloadedBytes());
        Assert.assertEquals(100L,databaseRow.getTotalBytes());
    }

    @Test
    public void setStatusAndError() throws Exception {
        clearDatabase();
        final DatabaseRow row = getRow();
        database.insert(row);
        database.setStatusAndError(row.getId(),Status.REMOVED.getValue(),Error.SERVER_ERROR.getValue());
        DatabaseRow databaseRow = database.query(row.getId());
        Assert.assertNotNull(databaseRow);
        Assert.assertEquals(Status.REMOVED.getValue(),databaseRow.getStatus());
        Assert.assertEquals(Error.SERVER_ERROR.getValue(),databaseRow.getError());
    }

    private void clearDatabase() {
        database.removeAll();
    }

    private DatabaseRow getRow() {
        return new DatabaseRow(1,TEST_URL,TEST_FILE_PATH,Status.QUEUED.getValue(),
                0L,0L,Error.NONE.getValue(),new HashMap<String,String>(),TEST_GROUP_ID);
    }

    private List<DatabaseRow> getRowList() {
        List<DatabaseRow> list = new ArrayList<>();
        int size = 20;

        for (int i = 0; i < size; i++) {
            list.add(new DatabaseRow(i, TEST_URL, TEST_FILE_PATH + i, Status.QUEUED.getValue(),
                    0L, 0L, Error.NONE.getValue(), new HashMap<String, String>(), TEST_GROUP_ID));
        }

        return list;
    }
}
