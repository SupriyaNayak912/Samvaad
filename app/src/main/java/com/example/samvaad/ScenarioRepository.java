package com.example.samvaad;

import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.List;

public class ScenarioRepository {

    private static final String COLLECTION_CUSTOM_SCENARIOS = "custom_scenarios";
    private static final String TAG = "ScenarioRepository";

    public interface ScenarioCallback {
        void onSuccess(String id);
        void onFailure(Exception e);
    }

    public interface ListCallback {
        void onSuccess(List<Scenario> scenarios);
        void onFailure(Exception e);
    }

    public static void saveCustomScenario(Scenario scenario, ScenarioCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onFailure(new Exception("User not authenticated"));
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .collection(COLLECTION_CUSTOM_SCENARIOS)
                .add(scenario)
                .addOnSuccessListener(documentReference -> {
                    String id = documentReference.getId();
                    scenario.setId(id);
                    // Update document with ID for consistency
                    documentReference.update("id", id);
                    callback.onSuccess(id);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving scenario", e);
                    callback.onFailure(e);
                });
    }

    public static void getCustomScenarios(ListCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onFailure(new Exception("User not authenticated"));
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .collection(COLLECTION_CUSTOM_SCENARIOS)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Scenario> scenarios = queryDocumentSnapshots.toObjects(Scenario.class);
                    callback.onSuccess(scenarios);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting custom scenarios", e);
                    callback.onFailure(e);
                });
    }

    public static void getStandardScenarios(ListCallback callback) {
        FirebaseFirestore.getInstance()
                .collection("scenarios")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Scenario> scenarios = queryDocumentSnapshots.toObjects(Scenario.class);
                    callback.onSuccess(scenarios);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting standard scenarios", e);
                    callback.onFailure(e);
                });
    }
}
