package com.tonyodev.fetch2;

import com.tonyodev.fetch2.database.DownloadInfo;
import com.tonyodev.fetch2.helper.DownloadPriorityComparator;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


public class DownloadPriorityComparatorUnitTest {

    @Test
    public void inCorrectOrder() throws Exception {
        final DownloadPriorityComparator comparator = new DownloadPriorityComparator();
        final List<DownloadInfo> downloads = new ArrayList<>();
        final DownloadInfo downloadOneNormalPriority = new DownloadInfo();
        final DownloadInfo downloadTwoNormalPriority = new DownloadInfo();

        downloadOneNormalPriority.setId(1);
        downloadOneNormalPriority.setCreated(10);
        downloadTwoNormalPriority.setId(2);
        downloadTwoNormalPriority.setCreated(5);
        downloads.add(downloadOneNormalPriority);
        downloads.add(downloadTwoNormalPriority);
        downloads.sort(comparator);

        DownloadInfo first = downloads.get(0);
        assertNotNull(first);
        assertEquals(first, downloadTwoNormalPriority);
        downloadTwoNormalPriority.setPriority(Priority.HIGH);
        downloads.sort(comparator);
        first = downloads.get(0);
        assertNotNull(first);
        assertEquals(first, downloadOneNormalPriority);

    }
}
