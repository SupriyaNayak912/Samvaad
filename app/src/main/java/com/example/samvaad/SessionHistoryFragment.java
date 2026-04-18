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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_session_history, container, false);
        
        rvLogs = view.findViewById(R.id.rv_logs);
        rvLogs.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new SessionHistoryAdapter();
        rvLogs.setAdapter(adapter);

        fetchLogs();

        return view;
    }

    private void fetchLogs() {
        logsList.clear();
        logsList.addAll(MockDatabase.sessionHistory);
        adapter.notifyDataSetChanged();
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
            
            holder.tvTitle.setText(session.scenarioTitle != null ? session.scenarioTitle : "Custom Session");

            long duration = session.durationSeconds;
            long m = duration / 60;
            long s = duration % 60;
            holder.tvDuration.setText(String.format(Locale.getDefault(), "Duration: %02d:%02d", m, s));

            SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault());
            holder.tvTime.setText(sdf.format(new Date(session.timestamp)));

            holder.tvScore.setText(String.valueOf((int) session.overallScore));

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.putExtra("SESSION_ID", session.id); 
                intent.setAction("ACTION_SHOW_STATS");
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return logsList.size();
        }

        class LogViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvDuration, tvTime, tvScore;

            LogViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tv_log_title);
                tvDuration = itemView.findViewById(R.id.tv_log_duration);
                tvTime = itemView.findViewById(R.id.tv_log_time);
                tvScore = itemView.findViewById(R.id.tv_log_score);
            }
        }
    }
}