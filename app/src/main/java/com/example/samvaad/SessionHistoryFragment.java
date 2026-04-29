package com.example.samvaad;
import java.util.List;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.text.SimpleDateFormat;

import java.util.concurrent.Executors;

public class SessionHistoryFragment extends Fragment {

    private RecyclerView rvLogs;
    private SessionHistoryAdapter adapter;
    private List<SessionMetrics> logsList = new ArrayList<>();

    @Override
    public void onResume() {
        super.onResume();
        fetchLogs();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_session_history, container, false);
        
        rvLogs = view.findViewById(R.id.rv_logs);
        rvLogs.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new SessionHistoryAdapter();
        rvLogs.setAdapter(adapter);

        return view;
    }

    private void fetchLogs() {
        SessionRepository.getSessions(new SessionRepository.ListCallback() {
            @Override
            public void onSuccess(java.util.List<SessionMetrics> sessions) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    logsList.clear();
                    logsList.addAll(sessions);
                    adapter.notifyDataSetChanged();
                    
                    // Populate graph using history (reverse order to show chronological left-to-right)
                    View cardGraph = getView() != null ? getView().findViewById(R.id.card_graph) : null;
                    if (cardGraph != null) {
                        if (sessions.isEmpty()) {
                            cardGraph.setVisibility(View.GONE);
                        } else {
                            cardGraph.setVisibility(View.VISIBLE);
                            java.util.ArrayList<Float> points = new java.util.ArrayList<>();
                            for (int i = sessions.size() - 1; i >= 0; i--) {
                                points.add((float) sessions.get(i).overallScore);
                            }
                            com.example.samvaad.ui.components.PremiumLineChartView chart = getView().findViewById(R.id.graph_sessions);
                            if (chart != null) chart.setData(points);
                        }
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    android.widget.Toast.makeText(getContext(), "Failed to load history", android.widget.Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private class SessionHistoryAdapter extends RecyclerView.Adapter<SessionHistoryAdapter.LogViewHolder> {

        @NonNull
        @Override
        public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_log, parent, false);
            return new LogViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
            SessionMetrics session = logsList.get(position);
            
            // Domain & Scenario
            String title = session.scenarioTitle != null ? session.scenarioTitle : "Role-Based Interview";
            holder.tvTitle.setText(title);
            
            // Explicitly expose Role, Type, and Mode as requested
            String modeStr = session.sessionMode != null ? session.sessionMode.toUpperCase() : "GENERAL";
            String roleStr = (session.targetRole != null && !session.targetRole.isEmpty()) ? session.targetRole : "General Practice";
            
            // Metadata Pills
            holder.tvRolePill.setText(roleStr);
            holder.tvModePill.setText(modeStr);
            
            String focusStr = (session.sessionGoal != null && !session.sessionGoal.isEmpty()) ? session.sessionGoal.toUpperCase() : "GENERAL";
            holder.tvFocusPill.setText(focusStr);

            long duration = session.durationSeconds;
            long m = duration / 60;
            long s = duration % 60;
            holder.tvDuration.setText(String.format(Locale.getDefault(), "Duration: %02d:%02d", m, s));

            SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault());
            if (session.timestamp != null) {
                holder.tvTime.setText(sdf.format(session.timestamp));
            } else {
                holder.tvTime.setText("--");
            }

            // Tier/Badge Logic
            int score = (int) session.overallScore;
            holder.tvScore.setText(String.valueOf(score));
            
            if (score >= 85) {
                holder.vBadgeGlow.setBackgroundResource(R.drawable.ring_teal); // Imagine an even goldier one
                holder.tvTierIcon.setText("👑 Legend");
                holder.tvScore.setTextColor(holder.itemView.getContext().getColor(R.color.teal_accent));
            } else if (score >= 70) {
                holder.vBadgeGlow.setBackgroundResource(R.drawable.ring_teal); 
                holder.tvTierIcon.setText("💎 Professional");
                holder.tvScore.setTextColor(holder.itemView.getContext().getColor(R.color.white));
            } else {
                holder.vBadgeGlow.setBackgroundResource(R.drawable.ring_teal); // Bronze fallback
                holder.tvTierIcon.setText("🌟 Apprentice");
                holder.tvScore.setTextColor(holder.itemView.getContext().getColor(R.color.text_secondary));
            }

            // Add Explanation Tooltip on Badge Click
            holder.tvTierIcon.setOnClickListener(v -> {
                new androidx.appcompat.app.AlertDialog.Builder(v.getContext(), R.style.CustomAlertDialog)
                    .setTitle("SRI Tiers Explained")
                    .setMessage("👑 Legend (85-100): Top 1% ready. Highly composed.\n\n💎 Professional (70-84): Solid, employable skills.\n\n🌟 Apprentice (<70): Keep practicing to build confidence.")
                    .setPositiveButton("Got it", null)
                    .show();
            });


            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.putExtra("EXTRA_SESSION_ID", session.firestoreId); 
                intent.setAction("ACTION_SHOW_STATS");
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return logsList.size();
        }

        class LogViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvRolePill, tvModePill, tvFocusPill, tvDuration, tvTime, tvScore, tvTierIcon;
            View vBadgeGlow;

            LogViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tv_log_title);
                tvRolePill = itemView.findViewById(R.id.tv_log_role_pill);
                tvModePill = itemView.findViewById(R.id.tv_log_mode_pill);
                tvFocusPill = itemView.findViewById(R.id.tv_log_focus_pill);
                tvDuration = itemView.findViewById(R.id.tv_log_duration);
                tvTime = itemView.findViewById(R.id.tv_log_time);
                tvScore = itemView.findViewById(R.id.tv_log_score);
                tvTierIcon = itemView.findViewById(R.id.tv_log_tier_icon);
                vBadgeGlow = itemView.findViewById(R.id.v_badge_glow);
            }
        }
    }
}