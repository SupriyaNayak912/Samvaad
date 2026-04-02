package com.example.samvaad;

import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class FirestoreHelper {

    private static final String TAG = "FirestoreHelper";
    private static FirebaseFirestore db = FirebaseFirestore.getInstance();

    public static void logActivity(String screenName, String eventType, int duration) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        Map<String, Object> log = new HashMap<>();
        log.put("uid", uid);
        log.put("timestamp", System.currentTimeMillis());
        log.put("screen_name", screenName);
        log.put("event_type", eventType);
        log.put("duration_ms", duration);

        db.collection("activity_logs")
                .add(log)
                .addOnSuccessListener(documentReference -> Log.d(TAG, "Log added to Cloud"))
                .addOnFailureListener(e -> Log.e(TAG, "Error adding log", e));
    }
}