package com.tonyodev.fetch2.database;

import com.tonyodev.fetch2.RequestData;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by tonyofrancis on 7/3/17.
 */

public final class DatabaseRowConverter {

    private DatabaseRowConverter() {}

    public static RequestData toRequestData(DatabaseRow row) {
        RequestData requestData  = null;
        if (row != null) {
            requestData = new RequestData(row.getUrl(),row.getAbsoluteFilePath(),row.getStatus()
                    ,row.getError(),row.getDownloadedBytes(),row.getTotalBytes(),
                    row.getHeaders(),row.getGroupId());
        }
        return requestData;
    }

    public static List<RequestData> toRequestDataList(List<DatabaseRow> rows) {
        List<RequestData> list = new ArrayList<>(rows.size());
        for (DatabaseRow row : rows) {
            list.add(toRequestData(row));
        }
        return list;
    }
}
