package com.tonyodev.fetch2.database;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;


/**
 * Created by tonyofrancis on 6/14/17.
 */

@Dao
public interface FetchDao {

 @Insert
 long insert(DatabaseRow databaseRow);

 @Insert
 List<Long> insert(List<DatabaseRow> databaseRowList);

 @Query("SELECT * FROM fetch WHERE status = :status")
 List<DatabaseRow> queryByStatus(int status);

 @Query("SELECT * FROM fetch WHERE groupId = :groupId")
 List<DatabaseRow> queryByGroupId(String groupId);

 @Query("SELECT * FROM fetch WHERE groupId = :groupId AND status = :status")
 List<DatabaseRow> queryGroupByStatusId(String groupId, int status);

 @Query("SELECT * FROM fetch WHERE id = :id LIMIT 1")
 DatabaseRow query(long id);

 @Query("SELECT * FROM fetch")
 List<DatabaseRow>query();

 @Query("SELECT * FROM fetch WHERE id IN(:ids)")
 List<DatabaseRow>query(long[] ids);

 @Query("UPDATE fetch SET downloadedBytes = :downloadedBytes WHERE id = :id")
 void updateDownloadedBytes(long id,long downloadedBytes);

 @Query("UPDATE fetch SET downloadedBytes = :downloadedBytes, totalBytes = :totalBytes WHERE id = :id")
 void setDownloadedBytesAndTotalBytes(long id,long downloadedBytes,long totalBytes);

 @Query("UPDATE fetch SET status = :status, error = :error WHERE id = :id")
 void setStatusAndError(long id,int status,int error);

 @Query("DELETE FROM fetch WHERE id = :id")
 int remove(long id);

 @Query("DELETE FROM fetch")
 void deleteAll();
}
