package com.example.samvaad;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "user_activity_logs")
public class UserActivityLog {
    @PrimaryKey(autoGenerate = true)
    public int log_id;
    
    public String firebase_uid;
    public long timestamp;
    public String screen_name;
    public String event_type;
    public int duration_ms;

    public UserActivityLog(String firebase_uid, long timestamp, String screen_name, String event_type, int duration_ms) {
        this.firebase_uid = firebase_uid;
        this.timestamp = timestamp;
        this.screen_name = screen_name;
        this.event_type = event_type;
        this.duration_ms = duration_ms;
    }
}