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
    private android.widget.VideoView videoPlayback;
    private android.widget.ProgressBar pbVideoLoading;

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
        videoPlayback = view.findViewById(R.id.video_playback);
        pbVideoLoading = view.findViewById(R.id.pb_video_loading);

        view.findViewById(R.id.btn_done_stats).setOnClickListener(v -> {
            // Re-route to home via MainActivity
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).onLoginSuccess(); // hacky way to revert to home tab
            }
        });

        Bundle args = getArguments();
        if (args != null && args.containsKey("SESSION_ID")) {
            int sessionId = args.getInt("SESSION_ID");
            loadSessionFromDatabase(sessionId);
        } else if (args != null && args.containsKey("SESSION_METRICS")) {
            SessionMetrics metrics = args.getParcelable("SESSION_METRICS");
            displayMetrics(metrics);
        } else {
            // Mock Data
            displayMetrics(createMockMetrics());
        }

        return view;
    }

    private void loadSessionFromDatabase(int id) {
        SessionMetrics session = MockDatabase.getSessionById(id);
        if (session != null) {
            displayMetrics(session);
        }
    }

    private void displayMetrics(SessionMetrics metrics) {
        calculateGlobalScore(metrics);
        setupVideoPlayback(metrics.videoFilePath);
    }

    private SessionMetrics createMockMetrics() {
        SessionMetrics mock = new SessionMetrics();
        mock.avgWpm = 145f;
        mock.chaosDistractionCount = 1;
        mock.recoveryTimeMs = 1200;
        mock.postureStability = 0.85f;
        mock.fillerWordCount = 2;
        for(int i=0; i<30; i++) mock.amplitudeTimeline.add((float)(-40 + Math.random()*20));
        return mock;
    }

    private void calculateGlobalScore(SessionMetrics metrics) {
        // Reuse existing logic but ensure it uses clarityScore/paceScore if they exist
        float paceScore = (metrics.paceScore > 0) ? metrics.paceScore : 100f;
        if (metrics.paceScore <= 0) {
            if (metrics.avgWpm < 130) paceScore -= ((130 - metrics.avgWpm) / 5f) * 2f;
            else if (metrics.avgWpm > 150) paceScore -= ((metrics.avgWpm - 150) / 5f) * 2f;
        }
        paceScore = Math.max(0, Math.min(100, paceScore));

        float resilienceScore = 100f;
        if (metrics.recoveryTimeMs > 3000) resilienceScore -= (metrics.chaosDistractionCount * 10);
        resilienceScore = Math.max(0, Math.min(100, resilienceScore));

        float presenceScore = (metrics.clarityScore > 0) ? metrics.clarityScore : (metrics.postureStability * 100f);

        float globalScore = (metrics.overallScore > 0) ? metrics.overallScore : (paceScore * 0.3f) + (resilienceScore * 0.4f) + (presenceScore * 0.3f);

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

    private void setupVideoPlayback(String path) {
        if (path == null || path.isEmpty()) {
            if (getView() != null) {
                View card = getView().findViewById(R.id.card_video_playback);
                if (card != null) card.setVisibility(View.GONE);
            }
            return;
        }

        java.io.File file = new java.io.File(path);
        if (!file.exists()) {
            if (getView() != null) {
                View card = getView().findViewById(R.id.card_video_playback);
                if (card != null) card.setVisibility(View.GONE);
            }
            return;
        }

        pbVideoLoading.setVisibility(View.VISIBLE);
        videoPlayback.setVideoPath(path);
        
        android.widget.MediaController mediaController = new android.widget.MediaController(requireContext());
        mediaController.setAnchorView(videoPlayback);
        videoPlayback.setMediaController(mediaController);

        videoPlayback.setOnPreparedListener(mp -> {
            pbVideoLoading.setVisibility(View.GONE);
            mp.setLooping(true);
            videoPlayback.start();
        });

        videoPlayback.setOnErrorListener((mp, what, extra) -> {
            pbVideoLoading.setVisibility(View.GONE);
            if (getView() != null) {
                View card = getView().findViewById(R.id.card_video_playback);
                if (card != null) card.setVisibility(View.GONE);
            }
            return true;
        });
    }
}