package com.example.samvaad;

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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LogsFragment extends Fragment {

    private RecyclerView rvLogs;
    private FirebaseFirestore db;
    private LogAdapter adapter;
    private List<DocumentSnapshot> logsList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_logs, container, false);
        
        rvLogs = view.findViewById(R.id.rv_logs);
        rvLogs.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new LogAdapter();
        rvLogs.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        fetchLogs();

        return view;
    }

    private void fetchLogs() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        db.collection("session_logs")
                .whereEqualTo("uid", uid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        logsList.clear();
                        logsList.addAll(task.getResult().getDocuments());
                        adapter.notifyDataSetChanged();
                    } else {
                        Log.e("LogsFragment", "Error getting documents: ", task.getException());
                    }
                });
    }

    private class LogAdapter extends RecyclerView.Adapter<LogAdapter.LogViewHolder> {

        @NonNull
        @Override
        public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_log, parent, false);
            return new LogViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
            DocumentSnapshot doc = logsList.get(position);
            
            String title = doc.getString("scenario_title");
            holder.tvTitle.setText(title != null ? title : "Custom Session");

            Long durationObj = doc.getLong("duration_total_seconds");
            long duration = durationObj != null ? durationObj : 0;
            long m = duration / 60;
            long s = duration % 60;
            holder.tvDuration.setText(String.format(Locale.getDefault(), "Duration: %02d:%02d", m, s));

            Long timeObj = doc.getLong("timestamp");
            if (timeObj != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault());
                holder.tvTime.setText(sdf.format(new Date(timeObj)));
            }

            Double scoreObj = doc.getDouble("global_score");
            int score = scoreObj != null ? scoreObj.intValue() : 0;
            holder.tvScore.setText(String.valueOf(score));

            holder.itemView.setOnClickListener(v -> {
                SessionMetrics metrics = new SessionMetrics();
                Double wpm = doc.getDouble("avg_wpm");
                metrics.avgWpm = wpm != null ? wpm.floatValue() : 140f;
                
                Long chaos = doc.getLong("chaos_count");
                metrics.chaosDistractionCount = chaos != null ? chaos.intValue() : 0;
                
                Long fillers = doc.getLong("filler_words");
                metrics.fillerWordCount = fillers != null ? fillers.intValue() : 0;
                
                // Derive recovery Time based on score (mocking reverse)
                metrics.recoveryTimeMs = score > 80 ? 1000 : 4000;
                metrics.postureStability = score > 80 ? 0.95f : 0.7f;
                
                Intent intent = new Intent(requireContext(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.putExtra("SESSION_METRICS", metrics);
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