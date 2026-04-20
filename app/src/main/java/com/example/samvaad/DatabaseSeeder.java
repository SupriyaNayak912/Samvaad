package com.example.samvaad;

import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * DatabaseSeeder — Utility to populate Firestore with standard scenarios.
 * Decouples content from the app codebase for professional data management.
 */
public class DatabaseSeeder {
    private static final String TAG = "DatabaseSeeder";

    public interface SeedCallback {
        void onComplete();
        void onError(Exception e);
    }

    public static void checkAndSeed(SeedCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("scenarios").limit(1).get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (queryDocumentSnapshots.isEmpty()) {
                    Log.d(TAG, "Scenarios collection is empty. Initializing seed data...");
                    seedScenarios(callback);
                } else {
                    Log.d(TAG, "Scenarios already present. Skipping seed.");
                    if (callback != null) callback.onComplete();
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error checking scenarios", e);
                if (callback != null) callback.onError(e);
            });
    }

    private static void seedScenarios(SeedCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        WriteBatch batch = db.batch();

        List<Scenario> seedList = new ArrayList<>();

        // 1. HR Essentials
        Scenario hr1 = new Scenario("std_hr_1", "General HR Screening", "HR", "Beginner", 3, 0f);
        hr1.setQuestions(Arrays.asList(
            "Walk me through your professional journey and key milestones.",
            "What motivates you to work in this industry, and why our company?",
            "Describe your ideal work environment and management style."
        ));
        seedList.add(hr1);

        // 2. Software Engineering
        Scenario tech1 = new Scenario("std_tech_1", "Full-Stack System Design", "Technical", "Expert", 4, 0f);
        tech1.setQuestions(Arrays.asList(
            "How would you design a scalable notification system for 10M users?",
            "Tell me about a time you had to optimize a slow database query.",
            "Explain the trade-offs between REST and GraphQL in a microservices architecture.",
            "How do you ensure security in your API development process?"
        ));
        seedList.add(tech1);

        // 3. Behavioral / Leadership
        Scenario beh1 = new Scenario("std_beh_1", "Conflict & Resolution", "Behavioral", "Intermediate", 3, 0f);
        beh1.setQuestions(Arrays.asList(
            "Tell me about a time you had a significant disagreement with a teammate.",
            "How do you handle delivering bad news to a stakeholder?",
            "Describe a situation where you had to lead a project with limited resources."
        ));
        seedList.add(beh1);

        // 4. Data Science
        Scenario ds1 = new Scenario("std_ds_1", "ML Lifecycle & Metrics", "Technical", "Intermediate", 3, 0f);
        ds1.setQuestions(Arrays.asList(
            "How do you handle imbalanced datasets in a classification problem?",
            "Explain the difference between L1 and L2 regularization.",
            "Tell me about a time you successfully deployed a model into production."
        ));
        seedList.add(ds1);

        for (Scenario s : seedList) {
            batch.set(db.collection("scenarios").document(s.getId()), s);
        }

        batch.commit()
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Database seeded successfully.");
                if (callback != null) callback.onComplete();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Database seed failed", e);
                if (callback != null) callback.onError(e);
            });
    }
}
