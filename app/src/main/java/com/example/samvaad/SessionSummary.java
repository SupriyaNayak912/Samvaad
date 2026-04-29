package com.example.samvaad;

import java.util.List;

/**
 * SessionSummary — the JSON contract containing everything Gemini needs
 * to generate personalized interview feedback.
 *
 * It contains the explicit text of what the user said (transcript) plus the mathematical
 * scores calculated on-device.
 */
public class SessionSummary {

    public String uid;
    public String scenarioTitle;
    public String targetRole;       // e.g. "Software Developer", "Product Manager"
    public String sessionGoal;      // e.g. "Reduce fillers", "Build confidence"
    public String sessionMode;      // "vault" or "smart"
    public boolean chaosEnabled;

    // Scores (computed on-device by ScoringEngine before sending)
    public float overallScore;
    public float paceScore;
    public float clarityScore;
    public float resilienceScore;
    public float presenceScore;
    public String behavioralProfile; // "composed", "overcompensating", "avoidant"

    // The transcription record of the exact Q&A
    public List<QnAPair> transcript;
    public String masterTranscript; // High-fidelity full session transcript from Whisper


    // Key raw metrics for context
    public float avgWpm;
    public int fillerWordCount;
    public int silenceCount;
    public int chaosDistractionCount;
    public long durationSeconds;

    /** Convenience factory — builds a SessionSummary from metrics + scores. */
    public static SessionSummary from(SessionMetrics metrics, ScoreResult scores, String uid) {
        SessionSummary s = new SessionSummary();
        s.uid                    = uid;
        s.scenarioTitle          = metrics.scenarioTitle;
        s.targetRole             = metrics.targetRole;
        s.sessionGoal            = metrics.sessionGoal;
        s.sessionMode            = metrics.sessionMode;
        s.chaosEnabled           = metrics.chaosEnabled;
        s.overallScore           = scores.overallScore;
        s.paceScore              = scores.paceScore;
        s.clarityScore           = scores.clarityScore;
        s.resilienceScore        = scores.resilienceScore;
        s.presenceScore          = scores.presenceScore;
        s.behavioralProfile      = scores.behavioralProfile;
        s.transcript             = metrics.transcript;
        s.masterTranscript       = (metrics.llmFeedback != null) ? metrics.llmFeedback.getSummary() : ""; // Stub check, will be set explicitly
        s.avgWpm                 = metrics.telemetry.avgWpm;
        s.fillerWordCount        = metrics.telemetry.fillerWordCount;
        s.silenceCount           = metrics.telemetry.silenceCount;
        s.chaosDistractionCount  = metrics.telemetry.chaosDistractionCount;
        s.durationSeconds        = metrics.durationSeconds;
        return s;
    }
}
