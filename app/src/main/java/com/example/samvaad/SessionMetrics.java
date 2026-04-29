package com.example.samvaad;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.PropertyName;
import java.util.ArrayList;
import java.util.List;

/**
 * SessionMetrics — The master document for an interview session.
 * Refactored for professional nesting and architectural alignment.
 */
public class SessionMetrics implements Parcelable {
    
    @DocumentId
    public String firestoreId;
    
    // Core Audit Fields
    public String status; // "IN_PROGRESS" or "COMPLETED"
    public String scenarioTitle;
    public String targetRole;
    public String sessionGoal;
    public String sessionMode; 
    public boolean chaosEnabled;
    public long durationSeconds;
    
    @com.google.firebase.firestore.Exclude
    public java.util.Date timestamp;

    // The Telemetry Map (Professional Nesting)
    public Telemetry telemetry;

    // Exact transcription
    public List<QnAPair> transcript;

    // Media & Visuals
    public ArrayList<Float> amplitudeTimeline;
    public String videoFilePath;

    // Primary Scores
    public float overallScore;
    public float clarityScore;
    public float paceScore;

    // AI Analysis
    public LlmFeedback llmFeedback;

    public static class Telemetry implements Parcelable {
        public float avgWpm;
        public int silenceCount;
        public int fillerWordCount;
        public int chaosDistractionCount;
        public long recoveryTimeMs;
        public float postureStability;
        public int totalFaceChecks;
        public int successfulFaceChecks;

        public Telemetry() {}

        protected Telemetry(Parcel in) {
            avgWpm = in.readFloat();
            silenceCount = in.readInt();
            fillerWordCount = in.readInt();
            chaosDistractionCount = in.readInt();
            recoveryTimeMs = in.readLong();
            postureStability = in.readFloat();
            totalFaceChecks = in.readInt();
            successfulFaceChecks = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeFloat(avgWpm);
            dest.writeInt(silenceCount);
            dest.writeInt(fillerWordCount);
            dest.writeInt(chaosDistractionCount);
            dest.writeLong(recoveryTimeMs);
            dest.writeFloat(postureStability);
            dest.writeInt(totalFaceChecks);
            dest.writeInt(successfulFaceChecks);
        }

        @Override
        public int describeContents() { return 0; }

        public static final Creator<Telemetry> CREATOR = new Creator<Telemetry>() {
            @Override
            public Telemetry createFromParcel(Parcel in) { return new Telemetry(in); }
            @Override
            public Telemetry[] newArray(int size) { return new Telemetry[size]; }
        };
    }

    public SessionMetrics() {
        amplitudeTimeline = new ArrayList<>();
        transcript = new ArrayList<>();
        telemetry = new Telemetry();
    }

    @com.google.firebase.firestore.PropertyName("timestamp")
    public java.util.Date getTimestamp() { return timestamp; }

    @com.google.firebase.firestore.PropertyName("timestamp")
    public void setTimestamp(Object value) {
        if (value instanceof Long) {
            this.timestamp = new java.util.Date((Long) value);
        } else if (value instanceof com.google.firebase.Timestamp) {
            this.timestamp = ((com.google.firebase.Timestamp) value).toDate();
        } else if (value instanceof java.util.Date) {
            this.timestamp = (java.util.Date) value;
        }
    }

    protected SessionMetrics(Parcel in) {
        firestoreId = in.readString();
        status = in.readString();
        scenarioTitle = in.readString();
        targetRole = in.readString();
        sessionGoal = in.readString();
        sessionMode = in.readString();
        chaosEnabled = in.readByte() != 0;
        durationSeconds = in.readLong();
        
        long time = in.readLong();
        timestamp = time == -1 ? null : new java.util.Date(time);
        
        telemetry = in.readParcelable(Telemetry.class.getClassLoader());
        
        transcript = new ArrayList<>();
        in.readTypedList(transcript, QnAPair.CREATOR);
        
        amplitudeTimeline = new ArrayList<>();
        in.readList(amplitudeTimeline, Float.class.getClassLoader());
        
        videoFilePath = in.readString();
        overallScore = in.readFloat();
        clarityScore = in.readFloat();
        paceScore = in.readFloat();
        llmFeedback = in.readParcelable(LlmFeedback.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(firestoreId);
        dest.writeString(status);
        dest.writeString(scenarioTitle);
        dest.writeString(targetRole);
        dest.writeString(sessionGoal);
        dest.writeString(sessionMode);
        dest.writeByte((byte) (chaosEnabled ? 1 : 0));
        dest.writeLong(durationSeconds);
        dest.writeLong(timestamp != null ? timestamp.getTime() : -1);
        dest.writeParcelable(telemetry, flags);
        dest.writeTypedList(transcript);
        dest.writeList(amplitudeTimeline);
        dest.writeString(videoFilePath);
        dest.writeFloat(overallScore);
        dest.writeFloat(clarityScore);
        dest.writeFloat(paceScore);
        dest.writeParcelable(llmFeedback, flags);
    }

    @Override
    public int describeContents() { return 0; }

    public static final Creator<SessionMetrics> CREATOR = new Creator<SessionMetrics>() {
        @Override
        public SessionMetrics createFromParcel(Parcel in) { return new SessionMetrics(in); }
        @Override
        public SessionMetrics[] newArray(int size) { return new SessionMetrics[size]; }
    };
}
