package com.tonyodev.fetch2;

import android.support.test.runner.AndroidJUnit4;

import com.tonyodev.fetch2.util.Utils;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;

/**
 * Created by tonyofrancis on 7/4/17.
 */
@RunWith(AndroidJUnit4.class)
public class RequestDataTest {

    public static final String TEST_GROUP_ID = "testGroup";
    public static final String TEST_URL = "http://www.tonyofrancis.com/tonyodev/fetchCore/bunny.m4v";
    public static final String TEST_FILE_PATH = "/storage/fetch/bunny.m4v";

    private RequestData requestData;

    @Before
    public void setUp() throws Exception {
        Request request = new Request(TEST_URL,TEST_FILE_PATH,new HashMap<String, String>());
        request.setGroupId(TEST_GROUP_ID);
        int progress = Utils.calculateProgress(0L,0L);
        requestData = new RequestData(request,Status.QUEUED,Error.NONE,0L,0L,progress);
    }

    @Test
    public void requestDataNotNull() throws Exception {
        Assert.assertNotNull(requestData);
    }

    @Test
    public void url() throws Exception {
        Assert.assertEquals(requestData.getUrl(),TEST_URL);
    }

    @Test
    public void filePath() throws Exception {
        Assert.assertEquals(requestData.getAbsoluteFilePath(),TEST_FILE_PATH);
    }

    @Test
    public void groupId() throws Exception {
        Assert.assertEquals(requestData.getGroupId(),TEST_GROUP_ID);
    }

    @Test
    public void status() throws Exception {
        Assert.assertEquals(Status.QUEUED,requestData.getStatus());
    }

    @Test
    public void error() throws Exception {
        Assert.assertEquals(Error.NONE,requestData.getError());
    }

    @Test
    public void downloadedBytes() throws Exception {
        Assert.assertEquals(0L,requestData.getDownloadedBytes());
    }

    @Test
    public void totalBytes() throws Exception {
     Assert.assertEquals(0L,requestData.getTotalBytes());
    }

    @Test
    public void headers() throws Exception {
        Assert.assertNotNull(requestData.getHeaders());
    }
}
