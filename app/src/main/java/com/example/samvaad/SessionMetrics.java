package com.example.samvaad;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.ArrayList;

public class SessionMetrics implements Parcelable {
    public int id;
    public float avgWpm;
    public int chaosDistractionCount;
    public long recoveryTimeMs;
    public float postureStability;
    public int fillerWordCount;
    // For the Line Chart timeline
    public ArrayList<Float> amplitudeTimeline;
    public String videoFilePath;
    
    public String scenarioTitle;
    public float overallScore;
    public float clarityScore;
    public float paceScore;
    public long timestamp;
    public long durationSeconds;


    public SessionMetrics() {
        amplitudeTimeline = new ArrayList<>();
    }

    protected SessionMetrics(Parcel in) {
        id = in.readInt();
        avgWpm = in.readFloat();
        chaosDistractionCount = in.readInt();
        recoveryTimeMs = in.readLong();
        postureStability = in.readFloat();
        fillerWordCount = in.readInt();
        amplitudeTimeline = new ArrayList<>();
        in.readList(amplitudeTimeline, Float.class.getClassLoader());
        videoFilePath = in.readString();
        scenarioTitle = in.readString();
        overallScore = in.readFloat();
        clarityScore = in.readFloat();
        paceScore = in.readFloat();
        timestamp = in.readLong();
        durationSeconds = in.readLong();
    }

    public static final Creator<SessionMetrics> CREATOR = new Creator<SessionMetrics>() {
        @Override
        public SessionMetrics createFromParcel(Parcel in) {
            return new SessionMetrics(in);
        }

        @Override
        public SessionMetrics[] newArray(int size) {
            return new SessionMetrics[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeFloat(avgWpm);
        dest.writeInt(chaosDistractionCount);
        dest.writeLong(recoveryTimeMs);
        dest.writeFloat(postureStability);
        dest.writeInt(fillerWordCount);
        dest.writeList(amplitudeTimeline);
        dest.writeString(videoFilePath);
        dest.writeString(scenarioTitle);
        dest.writeFloat(overallScore);
        dest.writeFloat(clarityScore);
        dest.writeFloat(paceScore);
        dest.writeLong(timestamp);
        dest.writeLong(durationSeconds);
    }
}
