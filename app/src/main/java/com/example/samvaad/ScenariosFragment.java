package com.example.samvaad;

import android.content.Intent;
import android.os.Bundle;
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
    private String activeCategory = "All";
    private String activeQuery    = "";

    @NonNull
    @Override
    protected FragmentScenariosBinding inflateBinding(@NonNull LayoutInflater inflater,
                                                      @Nullable ViewGroup container,
                                                      boolean attachToRoot) {
        return FragmentScenariosBinding.inflate(inflater, container, attachToRoot);
    }

    @Override
    protected void setupUI() {
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

        RetrofitClient.getApiService().getScenarios().enqueue(new Callback<List<Scenario>>() {
            @Override
            public void onResponse(@NonNull Call<List<Scenario>> call,
                                   @NonNull Response<List<Scenario>> response) {
                getBinding().pbLoading.setVisibility(android.view.View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    allScenarios.clear();
                    allScenarios.addAll(response.body());
                    applyFilters();
                } else {
                    loadPlaceholderData();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Scenario>> call, @NonNull Throwable t) {
                getBinding().pbLoading.setVisibility(android.view.View.GONE);
                showError("Network error — showing cached scenarios");
                loadPlaceholderData();
            }
        });
    }

    // ── Placeholder data (shown on network failure) ───────────────────
    private void loadPlaceholderData() {
        allScenarios.clear();
        allScenarios.add(new Scenario("1",  "Tell me about yourself",         "HR",         "Beginner",    5, 82f));
        allScenarios.add(new Scenario("2",  "Explain a recent project",        "Technical",  "Intermediate",7, 74f));
        allScenarios.add(new Scenario("3",  "Describe a conflict at work",     "Behavioral", "Expert",      4, 88f));
        allScenarios.add(new Scenario("4",  "What are your greatest strengths?","HR",         "Beginner",    3, 79f));
        allScenarios.add(new Scenario("5",  "Walk me through your resume",     "HR",         "Intermediate",6, 85f));
        allScenarios.add(new Scenario("6",  "Describe your testing approach",  "Technical",  "Expert",      8, 91f));
        applyFilters();
    }

    // ── Combined search + category filter ─────────────────────────────
    private void applyFilters() {
        List<Scenario> filtered = allScenarios.stream()
                .filter(s -> activeCategory.equals("All") ||
                        (s.getCategory() != null &&
                         s.getCategory().equalsIgnoreCase(activeCategory)))
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