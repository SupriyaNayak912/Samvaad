package com.example.samvaad;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModel;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.samvaad.databinding.FragmentScenariosBinding;
import com.example.samvaad.ui.base.BaseFragment;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ScenariosFragment extends BaseFragment<FragmentScenariosBinding> {

    // ── Stub ViewModel satisfying BaseFragment ───────────────────────
    public static class ScenariosViewModel extends ViewModel {}

    private ScenarioAdapter adapter;
    private List<Scenario> allScenarios = new ArrayList<>();
    private String activeCategory   = "All";
    private String activeDifficulty = "All";
    private String activeQuery      = "";

    @NonNull
    @Override
    protected FragmentScenariosBinding inflateBinding(@NonNull LayoutInflater inflater,
                                                      @Nullable ViewGroup container,
                                                      boolean attachToRoot) {
        return FragmentScenariosBinding.inflate(inflater, container, attachToRoot);
    }

    @Override
    protected void setupUI() {
        if (getActivity() instanceof MainActivity) {
            MainActivity main = (MainActivity) getActivity();
            String pendingCategory = main.getPendingVaultCategory();
            String pendingDifficulty = main.getPendingVaultDifficulty();

            if (pendingCategory != null) {
                this.activeCategory = pendingCategory;
                // Basic visual sync for category chips
                if (pendingCategory.equalsIgnoreCase("HR")) getBinding().chipGroupCategory.check(R.id.chip_hr);
                else if (pendingCategory.equalsIgnoreCase("Technical")) getBinding().chipGroupCategory.check(R.id.chip_technical);
                else if (pendingCategory.equalsIgnoreCase("Behavioral")) getBinding().chipGroupCategory.check(R.id.chip_behavioral);
            }

            if (pendingDifficulty != null) {
                this.activeDifficulty = pendingDifficulty;
            }
            
            main.clearPendingVaultConfig(); // Done consuming
        }

        setupRecyclerView();
        setupSearch();
        setupChips();
        setupFab();
        fetchScenarios();
    }

    // ── RecyclerView ─────────────────────────────────────────────────
    private void setupRecyclerView() {
        adapter = new ScenarioAdapter(new ArrayList<>(), this::navigateToSession, false);
        getBinding().rvScenarios.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().rvScenarios.setAdapter(adapter);
    }

    // ── Search bar ───────────────────────────────────────────────────
    private void setupSearch() {
        getBinding().etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                activeQuery = s.toString();
                applyFilters();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    // ── Category chips ───────────────────────────────────────────────
    private void setupChips() {
        getBinding().chipGroupCategory.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                activeCategory = "All";
            } else {
                int id = checkedIds.get(0);
                if      (id == R.id.chip_hr)         activeCategory = "HR";
                else if (id == R.id.chip_technical)  activeCategory = "Technical";
                else if (id == R.id.chip_behavioral) activeCategory = "Behavioral";
                else if (id == R.id.chip_custom)     activeCategory = "Custom";
                else                                 activeCategory = "All";
            }
            applyFilters();
        });
    }

    // ── FAB → BottomSheet ─────────────────────────────────────────────
    private void setupFab() {
        getBinding().fabCreateScenario.setOnClickListener(v -> {
            CreateScenarioBottomSheet sheet = new CreateScenarioBottomSheet();
            sheet.setOnScenarioCreatedListener(scenario -> {
                allScenarios.add(0, scenario);
                applyFilters();
                showError("Scenario \"" + scenario.getTitle() + "\" created!");
                // TODO: also persist to Firestore here
            });
            sheet.show(getParentFragmentManager(), CreateScenarioBottomSheet.class.getSimpleName());
        });
    }

    // ── Fetch from API ────────────────────────────────────────────────
    private void fetchScenarios() {
        getBinding().pbLoading.setVisibility(android.view.View.VISIBLE);

        ScenarioRepository.getStandardScenarios(new ScenarioRepository.ListCallback() {
            @Override
            public void onSuccess(List<Scenario> scenarios) {
                allScenarios.clear();
                allScenarios.addAll(scenarios);
                fetchCustomScenarios(); // Merge with user's own creations
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("ScenariosFragment", "Error fetching standard scenarios", e);
                // Fallback to minimal data if Firestore is unreachable
                loadPlaceholderData();
                fetchCustomScenarios();
            }
        });
    }

    private void fetchCustomScenarios() {
        ScenarioRepository.getCustomScenarios(new ScenarioRepository.ListCallback() {
            @Override
            public void onSuccess(List<Scenario> scenarios) {
                getBinding().pbLoading.setVisibility(android.view.View.GONE);
                // Add unique only
                for (Scenario s : scenarios) {
                    boolean exists = false;
                    for (Scenario existing : allScenarios) {
                        if (existing.getId() != null && existing.getId().equals(s.getId())) {
                            exists = true; break;
                        }
                    }
                    if (!exists) allScenarios.add(0, s);
                }
                applyFilters();
            }

            @Override
            public void onFailure(Exception e) {
                getBinding().pbLoading.setVisibility(android.view.View.GONE);
                applyFilters();
            }
        });
    }

    // ── Placeholder data (shown on network failure) ───────────────────
    private void loadPlaceholderData() {
        allScenarios.clear();
        
        Scenario hr1 = new Scenario("1", "Tell me about yourself", "HR", "Beginner", 5, 82f);
        hr1.setQuestions(java.util.Arrays.asList(
            "Tell me about yourself and your professional journey.",
            "Why do you want to work for our company?",
            "What is your greatest professional achievement?",
            "Where do you see yourself in five years?",
            "Do you have any questions for us?"
        ));
        allScenarios.add(hr1);

        Scenario tech1 = new Scenario("2", "Explain a recent project", "Technical", "Intermediate", 7, 74f);
        tech1.setQuestions(java.util.Arrays.asList(
            "Walk me through the most complex project you've worked on.",
            "What was your specific role in that project?",
            "How did you handle a major technical roadblock?",
            "Explain the architecture of your system.",
            "If you had to redo that project, what would you change?"
        ));
        allScenarios.add(tech1);

        Scenario beh1 = new Scenario("3", "Describe a conflict at work", "Behavioral", "Expert", 4, 88f);
        beh1.setQuestions(java.util.Arrays.asList(
            "Describe a time you had a conflict with a teammate. How did you resolve it?",
            "Tell me about a time you failed and what you learned.",
            "Describe a situation where you had to work under a tight deadline.",
            "Give me an example of how you handled a disagreement with your manager."
        ));
        allScenarios.add(beh1);

        Scenario hr2 = new Scenario("4", "Strengths & Weaknesses", "HR", "Beginner", 3, 79f);
        hr2.setQuestions(java.util.Arrays.asList(
            "What are your greatest professional strengths?",
            "What do you consider to be your biggest weakness?",
            "How do you handle pressure and stress?"
        ));
        allScenarios.add(hr2);

        Scenario hr3 = new Scenario("5", "Resume Walkthrough", "HR", "Intermediate", 6, 85f);
        hr3.setQuestions(java.util.Arrays.asList(
            "Walk me through your resume in detail.",
            "Why did you choose your field of study?",
            "What motivates you to perform at your best?",
            "Describe your ideal work environment.",
            "What are your salary expectations?"
        ));
        allScenarios.add(hr3);

        Scenario tech2 = new Scenario("6", "Testing & Quality", "Technical", "Expert", 8, 91f);
        tech2.setQuestions(java.util.Arrays.asList(
            "Describe your overall approach to software testing.",
            "What's the difference between unit testing and integration testing?",
            "How do you handle flaky tests in a CI/CD pipeline?",
            "Explain how you would test a high-traffic microservices architecture.",
            "What tools do you prefer for automated testing and why?"
        ));
        allScenarios.add(tech2);

        applyFilters();
    }

    // ── Combined search + category filter ─────────────────────────────
    private void applyFilters() {
        List<Scenario> filtered = allScenarios.stream()
                .filter(s -> activeCategory.equals("All") ||
                        (s.getCategory() != null &&
                         s.getCategory().equalsIgnoreCase(activeCategory)))
                .filter(s -> activeDifficulty.equals("All") ||
                        (s.getDifficulty() != null &&
                         s.getDifficulty().equalsIgnoreCase(activeDifficulty)))
                .filter(s -> activeQuery.isEmpty() ||
                        (s.getTitle() != null &&
                         s.getTitle().toLowerCase(Locale.getDefault())
                                 .contains(activeQuery.toLowerCase(Locale.getDefault()))))
                .collect(Collectors.toList());

        adapter.updateData(filtered);
    }

    // ── Navigate to Live Session with Scenario ────────────────────────
    private void navigateToSession(Scenario scenario) {
        Intent intent = new Intent(requireContext(), LiveSessionActivity.class);
        intent.putExtra("scenario", scenario);
        startActivity(intent);
    }
}