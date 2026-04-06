package com.example.samvaad;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.mikhaellopez.circularprogressbar.CircularProgressBar;

import java.util.ArrayList;
import java.util.List;

public class StatsFragment extends Fragment {

    private CircularProgressBar progressGlobalScore;
    private TextView tvGlobalScore;
    private TextView tvInsightTip;
    private TextView tvScorePace, tvScoreResilience, tvScorePresence;
    private LineChart chartVolume;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stats, container, false);

        progressGlobalScore = view.findViewById(R.id.progress_global_score);
        tvGlobalScore = view.findViewById(R.id.tv_global_score);
        tvInsightTip = view.findViewById(R.id.tv_insight_tip);
        tvScorePace = view.findViewById(R.id.tv_score_pace);
        tvScoreResilience = view.findViewById(R.id.tv_score_resilience);
        tvScorePresence = view.findViewById(R.id.tv_score_presence);
        chartVolume = view.findViewById(R.id.chart_volume);

        view.findViewById(R.id.btn_done_stats).setOnClickListener(v -> {
            // Re-route to home via MainActivity
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).onLoginSuccess(); // hacky way to revert to home tab
            }
        });

        Bundle args = getArguments();
        if (args != null && args.containsKey("SESSION_METRICS")) {
            SessionMetrics metrics = args.getParcelable("SESSION_METRICS");
            calculateGlobalScore(metrics);
        } else {
            // Mock Data for direct navigation testing
            SessionMetrics mock = new SessionMetrics();
            mock.avgWpm = 145f;
            mock.chaosDistractionCount = 1;
            mock.recoveryTimeMs = 1200;
            mock.postureStability = 0.85f;
            mock.fillerWordCount = 2;
            for(int i=0; i<30; i++) mock.amplitudeTimeline.add((float)(-40 + Math.random()*20));
            calculateGlobalScore(mock);
        }

        return view;
    }

    private void calculateGlobalScore(SessionMetrics metrics) {
        // 1. Pace Score (30%)
        // 100 if 130-150. Deduct 2 for every 5 wpm outside.
        float paceScore = 100f;
        if (metrics.avgWpm < 130) {
            paceScore -= ((130 - metrics.avgWpm) / 5f) * 2f;
        } else if (metrics.avgWpm > 150) {
            paceScore -= ((metrics.avgWpm - 150) / 5f) * 2f;
        }
        paceScore = Math.max(0, Math.min(100, paceScore));

        // 2. Resilience Score (40%)
        // Start 100. Deduct 10 for every chaos event > 3000ms recovery.
        // If recoveryTimeMs is the average, we can simulate by checking if average > 3000ms.
        float resilienceScore = 100f;
        if (metrics.recoveryTimeMs > 3000) {
            resilienceScore -= (metrics.chaosDistractionCount * 10);
        }
        resilienceScore = Math.max(0, Math.min(100, resilienceScore));

        // 3. Presence Score (30%)
        float presenceScore = metrics.postureStability * 100f;

        // Global Score
        float globalScore = (paceScore * 0.3f) + (resilienceScore * 0.4f) + (presenceScore * 0.3f);

        // Save score for HomeFragment
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("SamvaadPrefs", android.content.Context.MODE_PRIVATE);
        prefs.edit().putFloat("latest_global_score", globalScore).apply();

        // Update UI
        int scoreInt = (int) globalScore;
        tvGlobalScore.setText(scoreInt + "%");
        progressGlobalScore.setProgressWithAnimation((float) scoreInt, 1500L);

        tvScorePace.setText((int) paceScore + "%");
        tvScoreResilience.setText((int) resilienceScore + "%");
        tvScorePresence.setText((int) presenceScore + "%");

        // Insight Logic
        if (paceScore < 70) {
            tvInsightTip.setText("You are speaking outside the ideal range. Practice rhythmic breathing.");
        } else if (resilienceScore < 70) {
            tvInsightTip.setText("Distractions threw you off. Try practicing with 'Chaos Mode' more often.");
        } else if (presenceScore < 70) {
            tvInsightTip.setText("You moved out of frame frequently. Maintain eye contact with the camera.");
        } else {
            tvInsightTip.setText("Excellent performance! Your pacing, stability, and focus are very professional.");
        }

        setupChart(metrics.amplitudeTimeline);
    }

    private void setupChart(ArrayList<Float> timeline) {
        chartVolume.getDescription().setEnabled(false);
        chartVolume.getLegend().setEnabled(false);
        chartVolume.getAxisRight().setEnabled(false);

        XAxis xAxis = chartVolume.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(Color.LTGRAY);

        chartVolume.getAxisLeft().setDrawGridLines(true);
        chartVolume.getAxisLeft().setTextColor(Color.LTGRAY);

        List<Entry> entries = new ArrayList<>();
        if (timeline == null || timeline.isEmpty()) {
            for (int i = 0; i < 20; i++) entries.add(new Entry(i, 0f));
        } else {
            for (int i = 0; i < timeline.size(); i++) {
                entries.add(new Entry(i, Math.max(0, timeline.get(i) + 50))); // Offset DB for visualization
            }
        }

        LineDataSet dataSet = new LineDataSet(entries, "Volume");
        dataSet.setColor(Color.parseColor("#4DEEEA"));
        dataSet.setDrawCircles(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setLineWidth(2f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#4DEEEA"));
        dataSet.setFillAlpha(50);

        LineData lineData = new LineData(dataSet);
        lineData.setDrawValues(false);
        chartVolume.setData(lineData);
        chartVolume.invalidate();
    }
}