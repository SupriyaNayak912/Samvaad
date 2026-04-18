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
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("SamvaadPrefs", android.content.Context.MODE_PRIVATE);
        float score = prefs.getFloat("latest_global_score", 0f);

        if (score == 0f) {
            getBinding().tvStabilityScore.setText("--%");
            getBinding().tvInsightAdvice.setText("Complete a Live Session to generate your first professional assessment timeline!");
        } else {
            getBinding().tvStabilityScore.setText(String.format(java.util.Locale.getDefault(), "%.1f%%", score));
            String advice;
            if (score > 80) {
                advice = "You're demonstrating excellent presence. Try a 'Stress Test' scenario today.";
            } else {
                advice = "Your recent score indicates room for improvement. Let's focus on speech rate and eye contact.";
            }
            getBinding().tvInsightAdvice.setText(advice);
        }
    }

    // ── Recommended horizontal RecyclerView ─────────────────────────
    private void setupRecommendedScenarios() {
        // Placeholder list — replace with Firestore fetch
        List<Scenario> recommended = new ArrayList<>();
        recommended.add(new Scenario("1", "Tell me about yourself", "HR", "Beginner", 5, 82f));
        recommended.add(new Scenario("2", "Explain a recent project", "Technical", "Intermediate", 7, 74f));
        recommended.add(new Scenario("3", "Describe a conflict at work", "Behavioral", "Expert", 4, 88f));

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

    // ── Start session button ────────────────────────────────────────
    private void setupSessionButton() {
        getBinding().btnStartSession.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), LiveSessionActivity.class);
            // Optionally pass empty EXTRA_QUESTIONS to load defaults
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