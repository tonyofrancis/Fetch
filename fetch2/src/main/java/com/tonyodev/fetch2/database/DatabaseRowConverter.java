package com.tonyodev.fetch2.database;

import com.tonyodev.fetch2.Error;
import com.tonyodev.fetch2.Request;
import com.tonyodev.fetch2.RequestData;
import com.tonyodev.fetch2.Status;
import com.tonyodev.fetch2.util.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by tonyofrancis on 7/3/17.
 */

public final class DatabaseRowConverter {

    private DatabaseRowConverter() {}

    public static RequestData toRequestData(DatabaseRow row) {
        RequestData requestData = null;
        if (row != null) {
            Request request = new Request(row.getUrl(),row.getAbsoluteFilePath(),row.getHeaders());
            request.setGroupId(row.getGroupId());
            int progress = Utils.calculateProgress(row.getDownloadedBytes(),row.getTotalBytes());

            requestData = new RequestData(request,Status.valueOf(row.getStatus()),Error.valueOf(row.getError()),
                    row.getDownloadedBytes(),row.getTotalBytes(),progress);
        }
        return requestData;
    }

    public static List<RequestData> toRequestDataList(List<DatabaseRow> rows) {
        List<RequestData> list = new ArrayList<>();
        for (DatabaseRow row : rows) {
            RequestData requestData = toRequestData(row);
            if (requestData != null) {
                list.add(requestData);
            }
        }
        return list;
    }
}
