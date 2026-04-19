package com.example.samvaad;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.samvaad.databinding.FragmentHomeBinding;
import com.example.samvaad.ui.base.BaseFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class HomeFragment extends BaseFragment<FragmentHomeBinding> {

    // ── Stub ViewModel satisfying BaseFragment without forcing MVVM ──
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

    // ── Dynamic time-based greeting ──────────────────────────────────
    private void updateGreetingText() {
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        String greeting;
        if (hour < 12)      greeting = "Good morning,";
        else if (hour < 17) greeting = "Good afternoon,";
        else                greeting = "Good evening,";

        getBinding().tvWelcomeBack.setText(greeting);

        // Creative rotating taglines
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

        com.google.firebase.auth.FirebaseUser firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users").document(firebaseUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> runWithBinding(binding -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            String fName = user.getName() != null ? user.getName() : "there";
                            // Use short name or first word 
                            if (fName.contains(" ")) fName = fName.substring(0, fName.indexOf(" "));
                            binding.tvUserName.setText(fName + " 👋");
                            binding.tvStreak.setText(user.getPracticeStreak() + " Days");
                        }
                    } else {
                        binding.tvUserName.setText("there 👋");
                    }
                }));
        }
    }

    // ── Insight card with dynamic advice ────────────────────────────
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
                        for (SessionMetrics session : sessions) {
                            totalScore += session.overallScore;
                        }
                        float score = totalScore / sessions.size();
                        
                        binding.tvStabilityScore.setText(String.format(java.util.Locale.getDefault(), "%.1f%%", score));
                        String advice;
                        if (score > 80) {
                            advice = "You're demonstrating excellent presence. Try a 'Stress Test' scenario today.";
                        } else {
                            advice = "Your recent score indicates room for improvement. Let's focus on speech rate and eye contact.";
                        }
                        binding.tvInsightAdvice.setText(advice);

                        // Dynamic Metrics Update (Phase 7)
                        long totalSeconds = 0;
                        float sumScore = 0;
                        for (SessionMetrics s : sessions) {
                            totalSeconds += s.durationSeconds;
                            sumScore += s.overallScore;
                        }
                        
                        long hours = totalSeconds / 3600;
                        long minutes = (totalSeconds % 3600) / 60;
                        String timeStr = (hours > 0 ? hours + "h " : "") + minutes + "m";
                        binding.tvMetricsValue.setText(timeStr);

                        float growth = 0f;
                        if (sessions.size() >= 2) {
                            // Calculate average of up to 3 previous sessions
                            int maxPrevIndices = Math.min(3, sessions.size() - 1);
                            float sumPrev = 0;
                            for (int i = 1; i <= maxPrevIndices; i++) {
                                sumPrev += sessions.get(i).overallScore;
                            }
                            float avgPrev = sumPrev / maxPrevIndices;
                            float current = sessions.get(0).overallScore;
                            
                            growth = current - avgPrev;
                            String trend = (growth >= 0 ? "+" : "") + String.format("%.0f", growth) + "% Growth";
                            binding.tvGrowthTrend.setText(trend);
                            binding.tvGrowthBasis.setText("(vs last 3 sessions)");
                            
                            // Visual feedback
                            binding.tvGrowthTrend.setTextColor(growth >= 0 ? android.graphics.Color.parseColor("#4ADE80") : android.graphics.Color.parseColor("#F87171"));
                        } else {
                            binding.tvGrowthTrend.setText("Baseline Set");
                            binding.tvGrowthBasis.setText("First session");
                        }

                        // Add basis explanation label
                        binding.tvMetricsLabel.setText("Total Practice Time · Volatility Index");

                        // Add info dialog for IRI
                        binding.tvIriLabel.setOnClickListener(v -> {
                            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                                .setTitle("Interview Readiness Index (IRI)")
                                .setMessage("The IRI is a weighted average of your performance across all sessions, including Pace, Clarity, Presence, and Resilience. A score above 80% indicates you're likely ready for a real interview!")
                                .setPositiveButton("Got it", null)
                                .show();
                        });

                        // Gamification Update & Growth Insight
                        setupBadges(sessions, growth);
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

    // ── Gamification ───────────────────────────────────────────────
    private void setupBadges(List<SessionMetrics> sessions, float growthPercent) {
        androidx.recyclerview.widget.RecyclerView rvRoadmap = getBinding().getRoot().findViewById(R.id.rv_roadmap);
        if (rvRoadmap == null) return;

        class BadgeDef {
            String name; boolean unlocked; int icon;
            BadgeDef(String n, boolean u, int i) { name = n; unlocked = u; icon = i; }
        }
        
        List<BadgeDef> badges = new ArrayList<>();
        
        badges.add(new BadgeDef("Ice Breaker", sessions.size() >= 1, android.R.drawable.ic_menu_myplaces));
        badges.add(new BadgeDef("Consistency", sessions.size() >= 3, android.R.drawable.ic_menu_day));
        
        boolean chaosMaster = false;
        boolean eloquent = false;
        boolean persistent = sessions.size() >= 5;
        
        for(SessionMetrics s : sessions) {
            if (s.chaosDistractionCount > 0 && (s.overallScore > 80)) {
                chaosMaster = true;
            }
            if (s.durationSeconds > 60 && s.fillerWordCount == 0) {
                eloquent = true;
            }
        }

        // ─── AI GROWTH INSIGHT ──────────────────────────────────────
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

    // ── Recommended horizontal RecyclerView ─────────────────────────
    private void setupRecommendedScenarios() {
        // Placeholder list — replace with Firestore fetch
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

        Scenario s3 = new Scenario("3", "Describe a conflict at work", "Behavioral", "Expert", 4, 88f);
        s3.setQuestions(java.util.Arrays.asList(
            "Describe a situation where you had a disagreement with a colleague.",
            "Tell me about a time you had to lead a team through a difficult period.",
            "How do you handle feedback that you don't agree with?"
        ));
        recommended.add(s3);

        ScenarioAdapter adapter = new ScenarioAdapter(
                recommended,
                scenario -> navigateToSession(scenario),
                true  // horizontal
        );

        getBinding().rvRecommended.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        getBinding().rvRecommended.setAdapter(adapter);

        getBinding().tvSeeAll.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new ScenariosFragment())
                        .commit();
            }
        });
    }

    // ── Start session buttons ───────────────────────────────────────
    private void setupSessionButton() {
        getBinding().btnStartSession.setOnClickListener(v -> {
            SessionChoiceBottomSheet bottomSheet = new SessionChoiceBottomSheet(() -> {
                // If Vault selected from HomeFragment, ask what type
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
            // Rapid retention loop: Single prompt, 60 seconds
            Scenario drill = new Scenario("drill_" + System.currentTimeMillis(), "1-Min Elevator Pitch Drill", "General", "Mixed", 1, 0f);
            drill.setQuestions(java.util.Arrays.asList(
                "You have 60 seconds. Deliver your best elevator pitch explaining who you are and what value you bring."
            ));
            
            Intent intent = new Intent(requireContext(), LiveSessionActivity.class);
            intent.putExtra("scenario", drill);
            intent.putExtra("isDrillMode", true); // Pass flag if LiveSessionActivity wants to handle it specially
            startActivity(intent);
        });
    }

    // ── Navigate to Live Session with Scenario payload ──────────────
    private void navigateToSession(Scenario scenario) {
        Intent intent = new Intent(requireContext(), LiveSessionActivity.class);
        intent.putExtra("scenario", scenario);
        startActivity(intent);
    }
}