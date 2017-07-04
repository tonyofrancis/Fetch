package com.tonyodev.fetch2;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Created by tonyofrancis on 7/4/17.
 */
@RunWith(AndroidJUnit4.class)
public class RequestTest {

    public static final String TEST_GROUP_ID = "testGroup";
    public static final String TEST_URL = "http://www.tonyofrancis.com/tonyodev/fetchCore/bunny.m4v";
    public static final String TEST_FILE_PATH = "/storage/fetch/bunny.m4v";


    private Request request;

    @Before
    public void setUp() throws Exception {
        request = new Request(TEST_URL,TEST_FILE_PATH);
        request.setGroupId(TEST_GROUP_ID);
    }

    @Test
    public void requestNull() throws Exception {
        Assert.assertNotNull(request);
    }

    @Test
    public void urlString() throws Exception {
        Assert.assertEquals(TEST_URL,request.getUrl());
    }


    @Test
    public void filePathString() throws Exception {
        Assert.assertEquals(TEST_FILE_PATH,request.getAbsoluteFilePath());
    }

    @Test
    public void groupId() throws Exception {
        Assert.assertEquals(TEST_GROUP_ID,request.getGroupId());
    }

    @Test
    public void headers() throws Exception {
        request.putHeader("testHeader","novalue");
        Assert.assertTrue(request.getHeaders().containsKey("testHeader"));
        Assert.assertEquals(request.getHeaders().get("testHeader"),"novalue");
    }

    @Test
    public void equals() throws Exception {
        Request testRequest = new Request(TEST_URL,TEST_FILE_PATH);
        testRequest.setGroupId("otherRequest");
        Assert.assertEquals(testRequest,request);
    }
}
