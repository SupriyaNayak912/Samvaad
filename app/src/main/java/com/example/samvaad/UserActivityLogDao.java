package com.example.samvaad;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface UserActivityLogDao {
    @Insert
    void insertLog(UserActivityLog log);

    @Query("SELECT * FROM user_activity_logs WHERE firebase_uid = :uid ORDER BY timestamp DESC")
    List<UserActivityLog> getLogsForUser(String uid);

    @Query("SELECT * FROM user_activity_logs ORDER BY timestamp DESC LIMIT 10")
    List<UserActivityLog> getRecentLogs();
}