package com.tonyodev.fetch2;

import android.arch.persistence.room.*;
import android.arch.persistence.room.Query;

import java.util.List;


/**
 * Created by tonyofrancis on 6/14/17.
 */

@Dao
public interface RequestInfoDao {

 @Insert
 long insert(RequestInfo requestInfo);

 @Insert
 List<Long> insert(List<RequestInfo> requestInfoList);

 @Query("SELECT * FROM requestInfos WHERE status = :status")
 List<RequestInfo> queryByStatus(int status);

 @Query("SELECT * FROM requestInfos WHERE groupId = :groupId")
 List<RequestInfo> queryByGroupId(String groupId);

 @Query("SELECT * FROM requestInfos WHERE id = :id LIMIT 1")
 RequestInfo query(long id);

 @Query("SELECT * FROM requestInfos")
 List<RequestInfo>query();

 @Query("SELECT * FROM requestInfos WHERE id IN(:ids)")
 List<RequestInfo>query(long[] ids);

 @Query("UPDATE requestInfos SET downloadedBytes = :downloadedBytes WHERE id = :id")
 void updateDownloadedBytes(long id,long downloadedBytes);

 @Query("UPDATE requestInfos SET downloadedBytes = :downloadedBytes, totalBytes = :totalBytes WHERE id = :id")
 void setDownloadedBytesAndTotalBytes(long id,long downloadedBytes,long totalBytes);

 @Query("UPDATE requestInfos SET status = :status, error = :error WHERE id = :id")
 void setStatusAndError(long id,int status,int error);

 @Query("DELETE FROM requestInfos WHERE id = :id")
 void remove(long id);

 @Query("DELETE FROM requestInfos")
 void deleteAll();
}
