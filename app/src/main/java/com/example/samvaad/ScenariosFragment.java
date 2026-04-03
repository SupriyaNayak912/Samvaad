package com.example.samvaad;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ScenariosFragment extends Fragment {

    private LinearLayout scenariosContainer;
    private ProgressBar pbLoading;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_scenarios, container, false);

        scenariosContainer = view.findViewById(R.id.scenarios_container);
        pbLoading = view.findViewById(R.id.pb_loading);

        fetchScenarios();

        return view;
    }

    private void fetchScenarios() {
        pbLoading.setVisibility(View.VISIBLE);
        
        RetrofitClient.getApiService().getScenarios().enqueue(new Callback<ScenariosResponse>() {
            @Override
            public void onResponse(Call<ScenariosResponse> call, Response<ScenariosResponse> response) {
                pbLoading.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().getScenarios() != null) {
                    displayScenarios(response.body().getScenarios());
                } else {
                    Toast.makeText(getContext(), "Failed to fetch data", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ScenariosResponse> call, Throwable t) {
                pbLoading.setVisibility(View.GONE);
                Log.e("ScenariosFragment", "Error: " + t.getMessage());
                Toast.makeText(getContext(), "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayScenarios(List<Scenario> scenarios) {
        scenariosContainer.removeAllViews();
        for (Scenario scenario : scenarios) {
            View itemView = getLayoutInflater().inflate(R.layout.item_scenario, scenariosContainer, false);
            
            TextView tvTitle = itemView.findViewById(R.id.tv_scenario_title);
            TextView tvDiff = itemView.findViewById(R.id.tv_difficulty);
            
            tvTitle.setText(scenario.getTitle());
            tvDiff.setText(scenario.getDifficulty());
            
            // Set color based on difficulty
            if (scenario.getDifficulty().equalsIgnoreCase("Easy")) {
                tvDiff.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_green));
                tvDiff.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.bg_start));
            } else if (scenario.getDifficulty().equalsIgnoreCase("Hard")) {
                tvDiff.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_red));
            }

            itemView.setOnClickListener(v -> {
                // Navigate to session with this question
                SessionFragment sessionFragment = new SessionFragment();
                Bundle args = new Bundle();
                args.putString("scenario_id", scenario.getId());
                args.putString("scenario_title", scenario.getTitle());
                args.putString("scenario_category", scenario.getCategory());
                args.putString("scenario_difficulty", scenario.getDifficulty());
                sessionFragment.setArguments(args);

                if (getFragmentManager() != null) {
                    getFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, sessionFragment)
                            .addToBackStack(null)
                            .commit();
                }
            });

            scenariosContainer.addView(itemView);
        }
    }
}