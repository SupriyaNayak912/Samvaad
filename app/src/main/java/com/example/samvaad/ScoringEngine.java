package com.example.samvaad;

/**
 * ScoringEngine — stateless, mathematically grounded scoring utility.
 *
 * All four pillars use bounded functions (no score can go below 0 or above 100).
 * No hardcoded "penalty points" — scores decay smoothly based on deviation.
 *
 * The Samvaad Readiness Index (SRI) is the equal-weighted average of all 4 pillars.
 */
public class ScoringEngine {

    private ScoringEngine() {} // Utility class — no instantiation

    /**
     * Main entry point. Takes a completed SessionMetrics and returns a full ScoreResult.
     */
    public static ScoreResult calculate(SessionMetrics metrics) {
        SessionMetrics.Telemetry t = metrics.telemetry;
        if (t == null) t = new SessionMetrics.Telemetry(); // Safe fallback

        float pace       = calculatePaceScore(t.avgWpm);
        float clarity    = calculateClarityScore(t.fillerWordCount, t.avgWpm, metrics.durationSeconds);
        float resilience = calculateResilienceScore(t.recoveryTimeMs, t.chaosDistractionCount);
        float presence   = calculatePresenceScore(
                t.totalFaceChecks > 0
                        ? (float) t.successfulFaceChecks / t.totalFaceChecks
                        : t.postureStability,
                t.postureStability
        );
        return new ScoreResult(pace, clarity, resilience, presence);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PILLAR 1: PACE SCORE (Bell Curve)
    //
    // Ideal range: 130–150 WPM.
    // Within range → 100. Outside range → smooth exponential decay.
    // Formula: 100 × e^(-0.002 × deviation²)
    //
    // Examples:
    //   140 WPM  → deviation=0  → score ≈ 100
    //   120 WPM  → deviation=10 → score ≈ 82
    //   200 WPM  → deviation=50 → score ≈ 7
    // ─────────────────────────────────────────────────────────────────────────
    static float calculatePaceScore(float avgWpm) {
        if (avgWpm <= 0) return 50f; // No speech data — neutral score
        float deviation = 0;
        if (avgWpm < 130) deviation = 130 - avgWpm;
        else if (avgWpm > 150) deviation = avgWpm - 150;
        return (float) (100.0 * Math.exp(-0.002 * deviation * deviation));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PILLAR 2: CLARITY SCORE (Filler Density)
    //
    // Estimates total words from WPM × duration, then calculates filler rate.
    // Formula: max(0, 100 - (fillerRate × 8))
    //
    // Examples:
    //   0 fillers out of 300 words  → rate=0%   → score=100
    //   5 fillers out of 300 words  → rate=1.6% → score≈87
    //   15 fillers out of 300 words → rate=5%   → score=60
    // ─────────────────────────────────────────────────────────────────────────
    static float calculateClarityScore(int fillerWordCount, float avgWpm, long durationSeconds) {
        if (durationSeconds <= 0 || avgWpm <= 0) {
            // Fallback: just use raw filler count as a rough penalty
            return Math.max(0, 100 - (fillerWordCount * 5f));
        }
        float durationMinutes = durationSeconds / 60f;
        float estimatedTotalWords = avgWpm * durationMinutes;
        if (estimatedTotalWords < 1) estimatedTotalWords = 1;
        float fillerRate = (fillerWordCount / estimatedTotalWords) * 100f;
        return Math.max(0, 100f - (fillerRate * 8f));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PILLAR 3: RESILIENCE SCORE (Exponential Decay + Distraction Penalty)
    //
    // Measures how quickly you recovered after a chaos distraction.
    // No chaos → score based on natural composure (defaults to 85 as no test happened).
    // Formula: base = 100 × e^(-0.00015 × recoveryMs)
    //          penalty = chaosCount × 8
    //          score = max(0, base - penalty)
    //
    // Examples:
    //   Recovered in 1s, 1 chaos  → base=86, penalty=8  → score≈78
    //   Recovered in 3s, 2 chaos  → base=64, penalty=16 → score≈48
    //   Never recovered           → base≈0,  penalty=+   → score=0
    // ─────────────────────────────────────────────────────────────────────────
    static float calculateResilienceScore(long recoveryTimeMs, int chaosDistractionCount) {
        if (chaosDistractionCount == 0) return 85f; // No chaos — neutral/positive default
        float base = (float) (100.0 * Math.exp(-0.00015 * recoveryTimeMs));
        float penalty = chaosDistractionCount * 8f;
        return Math.max(0, base - penalty);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PILLAR 4: PRESENCE SCORE (Composite Eye Contact + Posture)
    //
    // facePresenceRatio: % of camera frames where your face was detected (0.0–1.0)
    // postureStability:  accelerometer-derived posture score (0.0–1.0)
    // Formula: (faceRatio × 70) + (posture × 30) → out of 100
    //
    // Examples:
    //   Face 90%, posture 0.85 → (63) + (25.5) → score≈88.5
    //   Face 50%, posture 0.50 → (35) + (15)   → score=50
    //   Face not detected, posture 0 → score=0
    // ─────────────────────────────────────────────────────────────────────────
    static float calculatePresenceScore(float facePresenceRatio, float postureStability) {
        // Guard against invalid sensor data
        if (facePresenceRatio < 0 || facePresenceRatio > 1) facePresenceRatio = 0.5f;
        if (postureStability < 0  || postureStability > 1)  postureStability  = 0.5f;
        return (facePresenceRatio * 70f) + (postureStability * 30f);
    }
}
