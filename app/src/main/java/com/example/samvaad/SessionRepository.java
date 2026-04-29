package com.example.samvaad;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class SessionRepository {

    private static final String COLLECTION_SESSIONS = "sessions";
    private static final String TAG = "SessionRepository";

    public interface SessionCallback {
        void onSuccess(String id);
        void onFailure(Exception e);
    }

    public interface ListCallback {
        void onSuccess(List<SessionMetrics> sessions);
        void onFailure(Exception e);
    }

    public interface SingleSessionCallback {
        void onSuccess(SessionMetrics session);
        void onFailure(Exception e);
    }

    public interface UserCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public static void startNewSession(SessionMetrics metrics, SessionCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onFailure(new Exception("User not authenticated"));
            return;
        }

        metrics.timestamp = new java.util.Date();
        metrics.status = "IN_PROGRESS";
        // Ensure core context is ready for the 'Immediate Save' logic
        if (metrics.scenarioTitle == null) metrics.scenarioTitle = "General Scenario";
        if (metrics.targetRole == null) metrics.targetRole = "General Practice";
        if (metrics.telemetry == null) metrics.telemetry = new SessionMetrics.Telemetry();

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .collection(COLLECTION_SESSIONS)
                .add(metrics)
                .addOnSuccessListener(documentReference -> {
                    String sessionId = documentReference.getId();
                    metrics.firestoreId = sessionId;
                    // Update document with ID for consistency
                    documentReference.update("firestoreId", sessionId);
                    Log.d(TAG, "Session initialized: " + sessionId);
                    callback.onSuccess(sessionId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error initializing session", e);
                    callback.onFailure(e);
                });
    }

    public static void saveSession(SessionMetrics metrics, SessionCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onFailure(new Exception("User not authenticated"));
            return;
        }

        metrics.status = "COMPLETED";
        if (metrics.timestamp == null) metrics.timestamp = new java.util.Date();

        com.google.firebase.firestore.DocumentReference docRef;
        if (metrics.firestoreId != null && !metrics.firestoreId.isEmpty()) {
            docRef = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user.getUid())
                    .collection(COLLECTION_SESSIONS)
                    .document(metrics.firestoreId);
        } else {
            docRef = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user.getUid())
                    .collection(COLLECTION_SESSIONS)
                    .document();
            metrics.firestoreId = docRef.getId();
        }

        docRef.set(metrics)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Session finalized: " + metrics.firestoreId);
                    incrementHeatmap();
                    updateGlobalStats(metrics);
                    callback.onSuccess(metrics.firestoreId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error finalizing session", e);
                    callback.onFailure(e);
                });
    }

    private static void incrementHeatmap() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String dateKey = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
        com.google.firebase.firestore.DocumentReference statsRef = FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .collection("stats")
                .document("heatmap");

        statsRef.set(new java.util.HashMap<String, Object>() {{
            put(dateKey, com.google.firebase.firestore.FieldValue.increment(1));
        }}, com.google.firebase.firestore.SetOptions.merge());
    }

    private static void updateGlobalStats(SessionMetrics metrics) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        long totalSessions = documentSnapshot.getLong("totalSessions") != null ? documentSnapshot.getLong("totalSessions") : 0;
                        float currentBest = documentSnapshot.getDouble("bestScore") != null ? documentSnapshot.getDouble("bestScore").floatValue() : 0f;
                        
                        java.util.Map<String, Object> updates = new java.util.HashMap<>();
                        updates.put("totalSessions", totalSessions + 1);
                        if (metrics.overallScore > currentBest) {
                            updates.put("bestScore", metrics.overallScore);
                        }
                        
                        // Track Tech vs Comm (Simplified for MVP radar)
                        // Tech: Scenario Complexity + AI Depth Rating
                        // Comm: Pace + Clarity
                        float techContribution = metrics.overallScore * 0.6f; // Weighted placeholder
                        float commContribution = (metrics.paceScore + metrics.clarityScore) / 2f;
                        
                        updates.put("totalTechScore", com.google.firebase.firestore.FieldValue.increment(techContribution));
                        updates.put("totalCommScore", com.google.firebase.firestore.FieldValue.increment(commContribution));

                        FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(user.getUid())
                                .update(updates);
                    }
                });
    }

    public static void getSessions(ListCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onFailure(new Exception("User not authenticated"));
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .collection(COLLECTION_SESSIONS)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<SessionMetrics> sessions = queryDocumentSnapshots.toObjects(SessionMetrics.class);
                    callback.onSuccess(sessions);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting sessions", e);
                    callback.onFailure(e);
                });
    }

    public static void getSession(String sessionId, SingleSessionCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onFailure(new Exception("User not authenticated"));
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .collection(COLLECTION_SESSIONS)
                .document(sessionId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    SessionMetrics session = documentSnapshot.toObject(SessionMetrics.class);
                    if (session != null) {
                        callback.onSuccess(session);
                    } else {
                        callback.onFailure(new Exception("Session not found"));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting session details", e);
                    callback.onFailure(e);
                });
    }

    public static void saveUserProfile(String name, String email, UserCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getUid() == null) return;

        java.util.Map<String, Object> profile = new java.util.HashMap<>();
        profile.put("name", name);
        profile.put("email", email);
        profile.put("memberSince", new java.util.Date()); // Normalized to match User.java

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .set(profile, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }
}
