package com.example.samvaad;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ScenarioAdapter extends RecyclerView.Adapter<ScenarioAdapter.ViewHolder> {

    public interface OnScenarioClickListener {
        void onScenarioClick(Scenario scenario);
    }

    private List<Scenario> scenarios;
    private final OnScenarioClickListener listener;
    private final boolean isHorizontal;

    public ScenarioAdapter(List<Scenario> scenarios, OnScenarioClickListener listener, boolean isHorizontal) {
        this.scenarios = scenarios;
        this.listener = listener;
        this.isHorizontal = isHorizontal;
    }

    public void updateData(List<Scenario> newList) {
        this.scenarios = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = isHorizontal
                ? R.layout.item_scenario_horizontal
                : R.layout.item_scenario;
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Scenario scenario = scenarios.get(position);
        Context ctx = holder.itemView.getContext();

        holder.tvTitle.setText(scenario.getTitle());

        // Difficulty badge
        String diff = scenario.getDifficulty() != null ? scenario.getDifficulty() : "Beginner";
        holder.tvDifficulty.setText(diff);
        applyDifficultyBadge(ctx, holder.tvDifficulty, diff);

        // Metadata (vertical card only)
        if (!isHorizontal && holder.tvMetadata != null) {
            int qCount = scenario.getQuestionCount();
            float score = scenario.getLastScore();
            String meta = qCount + " Questions";
            if (score > 0) meta += "  ·  Last Score: " + (int) score + "%";
            holder.tvMetadata.setText(meta);
        }

        // Category chip (horizontal card only)
        if (isHorizontal && holder.tvCategoryChip != null && scenario.getCategory() != null) {
            holder.tvCategoryChip.setText(scenario.getCategory());
        }

        holder.itemView.setOnClickListener(v -> listener.onScenarioClick(scenario));
    }

    private void applyDifficultyBadge(Context ctx, TextView badge, String difficulty) {
        switch (difficulty.toLowerCase()) {
            case "beginner":
            case "easy":
                badge.setBackground(ContextCompat.getDrawable(ctx, R.drawable.badge_beginner));
                badge.setTextColor(ContextCompat.getColor(ctx, R.color.teal_accent));
                break;
            case "intermediate":
            case "medium":
                badge.setBackground(ContextCompat.getDrawable(ctx, R.drawable.badge_intermediate));
                badge.setTextColor(ContextCompat.getColor(ctx, R.color.status_orange));
                break;
            case "expert":
            case "hard":
                badge.setBackground(ContextCompat.getDrawable(ctx, R.drawable.badge_expert));
                badge.setTextColor(ContextCompat.getColor(ctx, R.color.status_red));
                break;
            default:
                badge.setBackground(ContextCompat.getDrawable(ctx, R.drawable.badge_beginner));
                badge.setTextColor(ContextCompat.getColor(ctx, R.color.teal_accent));
        }
    }

    @Override
    public int getItemCount() { return scenarios != null ? scenarios.size() : 0; }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDifficulty, tvMetadata, tvCategoryChip;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle        = itemView.findViewById(R.id.tv_scenario_title);
            tvDifficulty   = itemView.findViewById(R.id.tv_difficulty);
            tvMetadata     = itemView.findViewById(R.id.tv_metadata);
            tvCategoryChip = itemView.findViewById(R.id.tv_category_chip);
        }
    }
}
