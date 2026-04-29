package com.example.samvaad;

import java.util.List;

/**
 * Immutable result object returned by ScoringEngine.
 * Represents the full Samvaad Readiness Index (SRI) breakdown.
 */
public class ScoreResult {
    public final float paceScore;
    public final float clarityScore;
    public final float resilienceScore;
    public final float presenceScore;
    public final float overallScore; // The SRI — equal 25% weight per pillar

    // Behavioral signals derived from the raw metrics
    public final String behavioralProfile; // "composed", "overcompensating", "avoidant"
    public final String primaryInsight;    // One-line actionable tip for the stats screen

    public ScoreResult(float pace, float clarity, float resilience, float presence) {
        this.paceScore        = clamp(pace);
        this.clarityScore     = clamp(clarity);
        this.resilienceScore  = clamp(resilience);
        this.presenceScore    = clamp(presence);
        this.overallScore     = clamp((pace + clarity + resilience + presence) / 4f);
        this.behavioralProfile = deriveBehavioralProfile(pace, clarity, resilience, presence);
        this.primaryInsight    = deriveInsight(paceScore, clarityScore, resilienceScore, presenceScore);
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(100f, value));
    }

    /**
     * Derives a behavioral archetype based on the pattern of scores.
     * These are the three behavioral fingerprints of anxiety:
     *   - avoidant:         face away, long pauses, low presence
     *   - overcompensating: speaking too fast, very short pauses, high WPM
     *   - composed:         balanced across all pillars
     */
    private static String deriveBehavioralProfile(float pace, float clarity,
                                                   float resilience, float presence) {
        if (pace < 60 && presence < 60) return "avoidant";
        if (pace < 65 && clarity < 65)  return "overcompensating";
        return "composed";
    }

    /**
     * Returns the single most impactful piece of feedback based on the weakest pillar.
     */
    private static String deriveInsight(float pace, float clarity,
                                         float resilience, float presence) {
        float min = Math.min(Math.min(pace, clarity), Math.min(resilience, presence));
        if (min == pace)        return "Your speaking pace drifted outside the ideal range. Try the 3-second breathing reset between thoughts.";
        if (min == clarity)     return "Filler words were the main friction point. Pause silently instead of saying 'um' or 'uh'.";
        if (min == resilience)  return "Recovery from pressure events was slow. Practice the 'acknowledge and pivot' technique for curveballs.";
        return "Maintaining eye contact and frame presence weakens under pressure. Anchor your gaze to the camera lens.";
    }
}
