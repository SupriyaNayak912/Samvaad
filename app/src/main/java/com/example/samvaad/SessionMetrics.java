package com.example.samvaad;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.PropertyName;
import java.util.ArrayList;
import java.util.List;

public class SessionMetrics implements Parcelable {
    public int id;
    
    @DocumentId
    public String firestoreId;
    
    // Core telemetry
    public float avgWpm;
    public int chaosDistractionCount;
    public long recoveryTimeMs;
    
    @PropertyName("stability")
    public float postureStability;

    public int fillerWordCount;
    public int silenceCount;
    
    // Face detection tracking
    public int totalFaceChecks;
    public int successfulFaceChecks;

    // Session Context
    public String scenarioTitle;
    public String targetRole;
    public String sessionGoal;
    public String sessionMode; // "vault" or "smart"
    public boolean chaosEnabled;
    public long timestamp;
    public long durationSeconds;
    
    // Exact transcription
    public List<QnAPair> transcript;

    // Legacy/Media fields
    public ArrayList<Float> amplitudeTimeline;
    public String videoFilePath;

    // Scores (Legacy fields - mostly unused now as we use ScoreResult, but preserved for DB/Parcel compat if needed)
    public float overallScore;
    public float clarityScore;
    public float paceScore;

    // AI Analysis
    public LlmFeedback llmFeedback;

    public SessionMetrics() {
        amplitudeTimeline = new ArrayList<>();
        transcript = new ArrayList<>();
    }

    protected SessionMetrics(Parcel in) {
        id = in.readInt();
        avgWpm = in.readFloat();
        chaosDistractionCount = in.readInt();
        recoveryTimeMs = in.readLong();
        postureStability = in.readFloat();
        fillerWordCount = in.readInt();
        silenceCount = in.readInt();
        totalFaceChecks = in.readInt();
        successfulFaceChecks = in.readInt();
        scenarioTitle = in.readString();
        targetRole = in.readString();
        sessionGoal = in.readString();
        sessionMode = in.readString();
        chaosEnabled = in.readByte() != 0;
        timestamp = in.readLong();
        durationSeconds = in.readLong();
        
        transcript = new ArrayList<>();
        in.readTypedList(transcript, QnAPair.CREATOR);
        
        amplitudeTimeline = new ArrayList<>();
        in.readList(amplitudeTimeline, Float.class.getClassLoader());
        
        videoFilePath = in.readString();
        overallScore = in.readFloat();
        clarityScore = in.readFloat();
        paceScore = in.readFloat();
        llmFeedback = in.readParcelable(LlmFeedback.class.getClassLoader());
        firestoreId = in.readString();
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
        dest.writeLong(silenceCount); // Changed to writeLong recently? Wait, it's int.
        dest.writeInt(totalFaceChecks);
        dest.writeInt(successfulFaceChecks);
        dest.writeString(scenarioTitle);
        dest.writeString(targetRole);
        dest.writeString(sessionGoal);
        dest.writeString(sessionMode);
        dest.writeByte((byte) (chaosEnabled ? 1 : 0));
        dest.writeLong(timestamp);
        dest.writeLong(durationSeconds);
        dest.writeTypedList(transcript);
        dest.writeList(amplitudeTimeline);
        dest.writeString(videoFilePath);
        dest.writeFloat(overallScore);
        dest.writeFloat(clarityScore);
        dest.writeFloat(paceScore);
        dest.writeParcelable(llmFeedback, flags);
        dest.writeString(firestoreId);
    }
}
