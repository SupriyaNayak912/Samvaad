package com.example.samvaad;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModel;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.samvaad.databinding.FragmentHomeBinding;
import com.example.samvaad.ui.base.BaseFragment;
import com.example.samvaad.GrowthAnalyst;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends BaseFragment<FragmentHomeBinding> {

    public static class HomeViewModel extends ViewModel {}

    @NonNull
    @Override
    protected FragmentHomeBinding inflateBinding(@NonNull LayoutInflater inflater,
                                                 @Nullable ViewGroup container, boolean attachToRoot) {
        return FragmentHomeBinding.inflate(inflater, container, attachToRoot);
    }

    @Override
    protected void setupUI() {
        updateGreetingText();
        updateInsightCard();
        setupRecommendedScenarios();
        setupSessionButton();
    }

    private void updateGreetingText() {
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        String greeting;
        if (hour < 12)      greeting = "Good morning,";
        else if (hour < 17) greeting = "Good afternoon,";
        else                greeting = "Good evening,";

        getBinding().tvWelcomeBack.setText(greeting);

        String[] morningTaglines = {
            "Your dream job is one session away ☀️",
            "Champions practice before the sun rises 🌅",
            "First one in, last one standing. Let's go! 💪"
        };
        String[] afternoonTaglines = {
            "Momentum is everything. Keep pushing 🚀",
            "Mid-day grind = interview ready 🎯",
            "Pressure makes diamonds. Simulate yours 💎"
        };
        String[] eveningTaglines = {
            "The best interviews happen after 10,000 reps 🌙",
            "One more session before bed? Your future self says yes ⭐",
            "They're still at the office. You're getting sharper 🔥"
        };

        String[] taglines = hour < 12 ? morningTaglines : (hour < 17 ? afternoonTaglines : eveningTaglines);
        String tagline = taglines[new java.util.Random().nextInt(taglines.length)];
        getBinding().tvTagline.setText(tagline);

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            FirebaseFirestore.getInstance()
                .collection("users").document(firebaseUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> runWithBinding(binding -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            String fName = user.getName() != null ? user.getName() : "there";
                            if (fName.contains(" ")) fName = fName.substring(0, fName.indexOf(" "));
                            binding.tvUserName.setText(fName + " 👋");
                        }
                    } else {
                        binding.tvUserName.setText("there 👋");
                    }
                }));
        }
    }

    private void updateInsightCard() {
        SessionRepository.getSessions(new SessionRepository.ListCallback() {
            @Override
            public void onSuccess(List<SessionMetrics> sessions) {
                runWithBinding(binding -> {
                    if (sessions.isEmpty()) {
                        binding.tvStabilityScore.setText("--%");
                        binding.tvInsightAdvice.setText("Complete a Live Session to generate your first professional assessment timeline!");
                    } else {
                        float totalScore = 0f;
                        long totalSeconds = 0;
                        for (SessionMetrics session : sessions) {
                            totalScore += session.overallScore;
                            totalSeconds += session.durationSeconds;
                        }
                        float avgOverall = totalScore / sessions.size();
                        
                        binding.tvStabilityScore.setText(String.format(java.util.Locale.getDefault(), "%.1f%%", avgOverall));
                        
                        long hours = totalSeconds / 3600;
                        long minutes = (totalSeconds % 3600) / 60;
                        String timeStr = (hours > 0 ? hours + "h " : "") + minutes + "m";
                        binding.tvMetricsValue.setText(timeStr);

                        setupPerformanceRadar(sessions.get(0));

                        float growthValue = 0f;
                        if (sessions.size() >= 2) {
                            int maxPrevIndices = Math.min(3, sessions.size() - 1);
                            float sumPrev = 0;
                            for (int i = 1; i <= maxPrevIndices; i++) {
                                sumPrev += sessions.get(i).overallScore;
                            }
                            float avgPrev = sumPrev / maxPrevIndices;
                            float current = sessions.get(0).overallScore;
                            growthValue = current - avgPrev;
                            
                            String trend = (growthValue >= 0 ? "+" : "") + String.format("%.0f", growthValue) + "% Growth";
                            binding.tvGrowthTrend.setText(trend);
                            binding.tvGrowthBasis.setText("(vs last 3 sessions)");
                            binding.tvGrowthTrend.setTextColor(growthValue >= 0 ? android.graphics.Color.parseColor("#4ADE80") : android.graphics.Color.parseColor("#F87171"));
                        } else {
                            binding.tvGrowthTrend.setText("Baseline Set");
                            binding.tvGrowthBasis.setText("First session");
                        }

                        binding.tvMetricsLabel.setText("Total Practice Time · Performance Stability");
                        binding.tvIriLabel.setOnClickListener(v -> {
                            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                                .setTitle("Interview Readiness Index (IRI)")
                                .setMessage("The IRI is a weighted average of performance across all sessions. Scores above 80% indicate interview readiness.")
                                .setPositiveButton("Got it", null)
                                .show();
                        });

                        setupBadges(sessions, growthValue);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                runWithBinding(binding -> {
                    binding.tvStabilityScore.setText("--%");
                    binding.tvInsightAdvice.setText("Failed to load insights. Check your connection.");
                });
            }
        });
    }

    private void setupPerformanceRadar(SessionMetrics latest) {
        // Safe access via findViewById to avoid potential ViewBinding sync lags
        com.github.mikephil.charting.charts.RadarChart radar = getBinding().getRoot().findViewById(R.id.radar_chart);
        if (radar == null) return;

        List<com.github.mikephil.charting.data.RadarEntry> entries = new ArrayList<>();
        float tech = latest.overallScore * 0.9f; 
        float comm = (latest.paceScore + latest.clarityScore) / 2f;
        
        float pres = 80f;
        float resi = 100f;
        
        if (latest.telemetry != null) {
            pres = latest.telemetry.postureStability * 100f;
            resi = latest.telemetry.chaosDistractionCount > 0 ? 85f : 100f;
        }
        float cons = 80f;

        entries.add(new com.github.mikephil.charting.data.RadarEntry(tech));
        entries.add(new com.github.mikephil.charting.data.RadarEntry(comm));
        entries.add(new com.github.mikephil.charting.data.RadarEntry(pres));
        entries.add(new com.github.mikephil.charting.data.RadarEntry(resi));
        entries.add(new com.github.mikephil.charting.data.RadarEntry(cons));

        com.github.mikephil.charting.data.RadarDataSet dataSet = new com.github.mikephil.charting.data.RadarDataSet(entries, "Performance Data");
        dataSet.setColor(android.graphics.Color.parseColor("#8B5CF6"));
        dataSet.setFillColor(android.graphics.Color.parseColor("#8B5CF6"));
        dataSet.setDrawFilled(true);
        dataSet.setFillAlpha(120);
        dataSet.setLineWidth(2f);
        dataSet.setDrawValues(false);

        com.github.mikephil.charting.data.RadarData data = new com.github.mikephil.charting.data.RadarData(dataSet);
        radar.setData(data);
        
        // Setup Axis Labels
        radar.getXAxis().setValueFormatter(new com.github.mikephil.charting.formatter.IndexAxisValueFormatter(
            new String[]{"Technical", "Comm", "Presence", "Resilience", "Consistency"}
        ));
        radar.getXAxis().setTextColor(android.graphics.Color.WHITE);
        radar.getXAxis().setTextSize(8f);
        radar.getYAxis().setEnabled(false);
        radar.getYAxis().setAxisMinimum(0f);
        radar.getYAxis().setAxisMaximum(100f);
        radar.getLegend().setEnabled(false);
        radar.getDescription().setEnabled(false);
        radar.setRotationEnabled(false);
        radar.setExtraOffsets(24f, 24f, 24f, 24f); // Extra padding for labels
        radar.invalidate();
    }

    private void setupBadges(List<SessionMetrics> sessions, float growthPercent) {
        GrowthAnalyst.generateInsight(sessions, growthPercent, new GrowthAnalyst.InsightCallback() {
            @Override
            public void onSuccess(String insight) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        getBinding().tvInsightAdvice.setText(insight);
                    });
                }
            }
            @Override
            public void onFailure(Exception e) {
                getBinding().tvInsightAdvice.setText("Continue practicing to unlock personalized growth insights.");
            }
        });
    }

    private void setupRecommendedScenarios() {
        List<Scenario> recommended = new ArrayList<>();
        Scenario s1 = new Scenario("1", "Tell me about yourself", "HR", "Beginner", 5, 82f);
        s1.setQuestions(java.util.Arrays.asList(
            "Tell me about yourself and your career so far.",
            "What makes you a good fit for this role?",
            "What is your greatest professional accomplishment?"
        ));
        recommended.add(s1);

        Scenario s2 = new Scenario("2", "Explain a recent project", "Technical", "Intermediate", 7, 74f);
        s2.setQuestions(java.util.Arrays.asList(
            "Walk me through a project you're particularly proud of.",
            "What technical challenges did you face and how did you resolve them?",
            "How did you ensure the quality of your code in that project?"
        ));
        recommended.add(s2);

        ScenarioAdapter adapter = new ScenarioAdapter(recommended, scenario -> navigateToSession(scenario), true);
        getBinding().rvRecommended.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        getBinding().rvRecommended.setAdapter(adapter);

        getBinding().tvSeeAll.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).switchToScenariosTab();
            }
        });
    }

    private void setupSessionButton() {
        getBinding().btnStartSession.setOnClickListener(v -> {
            SessionChoiceBottomSheet bottomSheet = new SessionChoiceBottomSheet(() -> {
                VaultCategoryBottomSheet catSheet = new VaultCategoryBottomSheet((category, difficulty) -> {
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).switchToVaultTab(category, difficulty);
                    }
                });
                catSheet.show(getParentFragmentManager(), "VaultCategory");
            });
            bottomSheet.show(getParentFragmentManager(), "SessionChoice");
        });
        
        getBinding().btnDailyDrill.setOnClickListener(v -> {
            Scenario drill = new Scenario("drill_" + System.currentTimeMillis(), "1-Min Elevator Pitch Drill", "General", "Mixed", 1, 0f);
            drill.setQuestions(java.util.Arrays.asList(
                "You have 60 seconds. Deliver your best elevator pitch."
            ));
            Intent intent = new Intent(requireContext(), LiveSessionActivity.class);
            intent.putExtra("scenario", drill);
            intent.putExtra("isDrillMode", true);
            startActivity(intent);
        });
    }

    private void navigateToSession(Scenario scenario) {
        Intent intent = new Intent(requireContext(), LiveSessionActivity.class);
        intent.putExtra("scenario", scenario);
        startActivity(intent);
    }
}