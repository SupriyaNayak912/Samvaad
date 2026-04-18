package com.example.samvaad;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface SessionMetricsDao {
    @Insert
    long insertSession(SessionMetrics session);

    @Update
    void updateSession(SessionMetrics session);

    @Delete
    void deleteSession(SessionMetrics session);

    @Query("SELECT * FROM session_metrics ORDER BY timestamp DESC")
    List<SessionMetrics> getAllSessions();

    @Query("SELECT * FROM session_metrics WHERE id = :sessionId LIMIT 1")
    SessionMetrics getSessionById(int sessionId);
}
