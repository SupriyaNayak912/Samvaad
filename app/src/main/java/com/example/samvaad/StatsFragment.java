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
    private TextView tvAnalysisSummary;
    private TextView tvScorePace, tvScoreResilience, tvScorePresence;
    private LineChart chartVolume;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stats, container, false);

        progressGlobalScore = view.findViewById(R.id.progress_global_score);
        tvGlobalScore = view.findViewById(R.id.tv_global_score);
        tvInsightTip = view.findViewById(R.id.tv_insight_tip);
        tvAnalysisSummary = view.findViewById(R.id.tv_analysis_summary);
        tvScorePace = view.findViewById(R.id.tv_score_pace);
        tvScoreResilience = view.findViewById(R.id.tv_score_resilience);
        tvScorePresence = view.findViewById(R.id.tv_score_presence);
        chartVolume = view.findViewById(R.id.chart_volume);

        view.findViewById(R.id.btn_share_stats).setOnClickListener(v -> sharePerformanceSnapshot());

        view.findViewById(R.id.btn_done_stats).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToHome();
            }
        });

        Bundle args = getArguments();
        if (args != null && args.containsKey("EXTRA_SESSION_ID")) {
            String sid = args.getString("EXTRA_SESSION_ID");
            loadSessionFromDatabase(sid);
        } else if (args != null && args.containsKey("SESSION_METRICS")) {
            SessionMetrics metrics = args.getParcelable("SESSION_METRICS");
            displayMetrics(metrics);
        }

        return view;
    }

    private void loadSessionFromDatabase(String id) {
        View progress = getView() != null ? getView().findViewById(R.id.progress_load_stats) : null;
        View content = getView() != null ? getView().findViewById(R.id.scroll_stats) : null;
        
        if (progress != null) progress.setVisibility(View.VISIBLE);
        if (content != null) content.setVisibility(View.GONE);

        SessionRepository.getSession(id, new SessionRepository.SingleSessionCallback() {
            @Override
            public void onSuccess(SessionMetrics session) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    if (progress != null) progress.setVisibility(View.GONE);
                    if (content != null) content.setVisibility(View.VISIBLE);
                    displayMetrics(session);
                });
            }

            @Override
            public void onFailure(Exception e) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    if (progress != null) progress.setVisibility(View.GONE);
                    if (content != null) content.setVisibility(View.VISIBLE);
                    android.widget.Toast.makeText(getContext(), "Failed to load report", android.widget.Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void displayMetrics(SessionMetrics metrics) {
        calculateGlobalScore(metrics);
    }

    private SessionMetrics createMockMetrics() {
        SessionMetrics mock = new SessionMetrics();
        mock.avgWpm = 145f;
        mock.chaosDistractionCount = 1;
        mock.recoveryTimeMs = 1200;
        mock.postureStability = 0.85f;
        mock.fillerWordCount = 2;
        for(int i=0; i<30; i++) mock.amplitudeTimeline.add((float)(-40 + Math.random()*20));

        // Mock LLM Feedback for UI testing
        LlmFeedback feedback = new LlmFeedback();
        feedback.setSummary("Great pacing, but work on filler words.");
        feedback.setCoachingTip("Practice holding silence instead of saying 'um'.");
        feedback.setOverallScore(82);
        feedback.setPaceScore(85f);
        feedback.setClarityScore(70f);
        feedback.setResilienceScore(88f);
        feedback.setPresenceScore(90f);
        
        List<QuestionFeedback> qaList = new ArrayList<>();
        qaList.add(new QuestionFeedback(
            "Tell me about a challenge you faced.",
            "You gave a high-level overview but missed specific details.",
            "Use the STAR method and explicitly state the result and metrics."
        ));
        qaList.add(new QuestionFeedback(
            "Why do you want this role?",
            "You focused too much on what the company can do for you.",
            "Align your answer with the company's current goals and how you add value."
        ));
        feedback.setQuestionAnalysis(qaList);
        mock.llmFeedback = feedback;

        return mock;
    }

    private void calculateGlobalScore(SessionMetrics metrics) {
        // AI-First Logic: Only use AI generated scores for the performance overview
        LlmFeedback ai = metrics.llmFeedback;
        
        float globalScore = (ai != null) ? ai.getOverallScore() : 0f;
        float paceScore = (ai != null) ? ai.getPaceScore() : 0f;
        float resilienceScore = (ai != null) ? ai.getResilienceScore() : 0f;
        float presenceScore = (ai != null) ? ai.getPresenceScore() : 0f;

        // Save score for HomeFragment / UI
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("SamvaadPrefs", android.content.Context.MODE_PRIVATE);
        prefs.edit().putFloat("latest_global_score", globalScore).apply();

        // Update UI components
        int scoreInt = (int) globalScore;
        tvGlobalScore.setText(scoreInt + "%");
        progressGlobalScore.setProgressWithAnimation((float) scoreInt, 1500L);

        tvScorePace.setText((int) paceScore + "%");
        tvScoreResilience.setText((int) resilienceScore + "%");
        tvScorePresence.setText((int) presenceScore + "%");

        String tierText = globalScore >= 85 ? "👑 Legend Tier" : (globalScore >= 70 ? "💎 Pro Tier" : "🌟 Apprentice Tier");

        // Insight Logic
        if (metrics.llmFeedback != null) {
            android.util.Log.d("SamvaadStats", "LLM Feedback detected. Summary: " + metrics.llmFeedback.summary);
            // Priority to real AI feedback if it exists
            if (metrics.llmFeedback.coachingTip != null && !metrics.llmFeedback.coachingTip.isEmpty()) {
                tvInsightTip.setText(tierText + " • " + metrics.llmFeedback.coachingTip);
            }
            if (metrics.llmFeedback.summary != null && !metrics.llmFeedback.summary.isEmpty()) {
                tvAnalysisSummary.setVisibility(View.VISIBLE);
                tvAnalysisSummary.setText(metrics.llmFeedback.summary);
            } else {
                tvAnalysisSummary.setVisibility(View.GONE);
            }
        } else {
            android.util.Log.w("SamvaadStats", "No LLM Feedback found in session metrics.");
            // Fallback to telemetry-based tips
            if (paceScore < 70) {
                tvInsightTip.setText("You are speaking outside the ideal range. Practice rhythmic breathing.");
            } else if (resilienceScore < 70) {
                tvInsightTip.setText("Distractions threw you off. Try practicing with 'Chaos Mode' more often.");
            } else if (presenceScore < 70) {
                tvInsightTip.setText("You moved out of frame frequently. Maintain eye contact with the camera.");
            } else {
                tvInsightTip.setText("Excellent performance! Your pacing, stability, and focus are very professional.");
            }
            tvAnalysisSummary.setVisibility(View.GONE);
        }

        setupChart(metrics.amplitudeTimeline);
        setupDetailedAnalysis(metrics.llmFeedback);
    }
    
    private void setupDetailedAnalysis(LlmFeedback feedback) {
        if (getView() == null || feedback == null) {
            android.util.Log.w("SamvaadStats", "Detailed analysis skipped: feedback is null");
            return;
        }

        android.util.Log.d("SamvaadStats", "Setting up detailed analysis. Strengths count: " 
                + (feedback.strengths != null ? feedback.strengths.size() : "null"));

        // 1. Populate Strengths
        androidx.recyclerview.widget.RecyclerView rvStrengths = getView().findViewById(R.id.rv_strengths);
        View tvStrengthsLabel = getView().findViewById(R.id.tv_strengths_label);
        if (rvStrengths != null && feedback.strengths != null && !feedback.strengths.isEmpty()) {
            tvStrengthsLabel.setVisibility(View.VISIBLE);
            rvStrengths.setVisibility(View.VISIBLE);
            FeedbackCarouselAdapter adapter = new FeedbackCarouselAdapter(feedback.strengths, "🌟");
            rvStrengths.setAdapter(adapter);
        } else if (tvStrengthsLabel != null) {
            tvStrengthsLabel.setVisibility(View.GONE);
        }

        // 2. Populate Focus Areas
        androidx.recyclerview.widget.RecyclerView rvFocus = getView().findViewById(R.id.rv_focus_areas);
        View tvFocusLabel = getView().findViewById(R.id.tv_focus_label);
        java.util.List<String> focusList = new java.util.ArrayList<>();
        if (feedback.areasToImprove != null) focusList.addAll(feedback.areasToImprove);
        if (feedback.coachingTip != null && !feedback.coachingTip.isEmpty()) {
            focusList.add("COACH'S TIP: " + feedback.coachingTip);
        }

        if (rvFocus != null && !focusList.isEmpty()) {
            tvFocusLabel.setVisibility(View.VISIBLE);
            rvFocus.setVisibility(View.VISIBLE);
            FeedbackCarouselAdapter adapter = new FeedbackCarouselAdapter(focusList, "🎯");
            rvFocus.setAdapter(adapter);
        } else if (tvFocusLabel != null) {
            tvFocusLabel.setVisibility(View.GONE);
        }

        // 3. Populate Question Breakdown
        androidx.recyclerview.widget.RecyclerView rvQA = getView().findViewById(R.id.rv_question_analysis);
        View tvBreakdownLabel = getView().findViewById(R.id.tv_breakdown_label);
        if (rvQA != null && tvBreakdownLabel != null) {
            if (feedback.questionAnalysis == null || feedback.questionAnalysis.isEmpty()) {
                rvQA.setVisibility(View.GONE);
                tvBreakdownLabel.setVisibility(View.GONE);
            } else {
                rvQA.setVisibility(View.VISIBLE);
                tvBreakdownLabel.setVisibility(View.VISIBLE);
                QuestionAnalysisAdapter adapter = new QuestionAnalysisAdapter(feedback.questionAnalysis);
                rvQA.setAdapter(adapter);
            }
        }
    }
    
    private class QuestionAnalysisAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<QuestionAnalysisAdapter.QAViewHolder> {
        private java.util.List<QuestionFeedback> qaList;
        public QuestionAnalysisAdapter(java.util.List<QuestionFeedback> qaList) { this.qaList = qaList; }
        
        @NonNull @Override
        public QAViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            View view = android.view.LayoutInflater.from(parent.getContext()).inflate(R.layout.item_question_analysis, parent, false);
            return new QAViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull QAViewHolder holder, int position) {
            QuestionFeedback qf = qaList.get(position);
            holder.tvQuestion.setText("Q: " + qf.getQuestion());
            holder.tvWhatYouSaid.setText("You Said: " + qf.getWhatYouSaidSummary());
            holder.tvBetterApproach.setText("Expert Tip: " + qf.getBetterApproach());
        }
        
        @Override public int getItemCount() { return qaList.size(); }
        
        class QAViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            android.widget.TextView tvQuestion, tvWhatYouSaid, tvBetterApproach;
            QAViewHolder(@NonNull View itemView) {
                super(itemView);
                tvQuestion = itemView.findViewById(R.id.tv_qa_question);
                tvWhatYouSaid = itemView.findViewById(R.id.tv_qa_user_summary);
                tvBetterApproach = itemView.findViewById(R.id.tv_qa_better_approach);
            }
        }
    }

    private void setupChart(ArrayList<Float> timeline) {
        chartVolume.getDescription().setEnabled(false);
        chartVolume.getLegend().setEnabled(false);
        chartVolume.getAxisRight().setEnabled(false);
        chartVolume.setDrawGridBackground(false);
        chartVolume.setTouchEnabled(false);

        XAxis xAxis = chartVolume.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(false);
        xAxis.setTextColor(Color.parseColor("#8E8E93"));

        chartVolume.getAxisLeft().setDrawGridLines(false);
        chartVolume.getAxisLeft().setDrawAxisLine(false);
        chartVolume.getAxisLeft().setTextColor(Color.parseColor("#8E8E93"));

        List<Entry> entries = new ArrayList<>();
        if (timeline == null || timeline.isEmpty()) {
            // Placeholder smooth wave
            for (int i = 0; i < 20; i++) {
                float val = (float) (Math.sin(i * 0.5) * 10 + 20);
                entries.add(new Entry(i, val));
            }
        } else {
            for (int i = 0; i < timeline.size(); i++) {
                // Map DB values to positive range for visualization
                float val = Math.max(0, timeline.get(i) + 60); 
                entries.add(new Entry(i, val));
            }
        }

        LineDataSet dataSet = new LineDataSet(entries, "Energy");
        dataSet.setColor(Color.parseColor("#4DEEEA"));
        dataSet.setDrawCircles(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setLineWidth(3f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#4DEEEA"));
        dataSet.setFillAlpha(40);

        LineData lineData = new LineData(dataSet);
        lineData.setDrawValues(false);
        chartVolume.setData(lineData);
        chartVolume.animateX(1000);
        chartVolume.invalidate();
    }

    private void sharePerformanceSnapshot() {
        if (getView() == null) return;
        
        // Directly capture the isolated summary container, preventing layout glitches
        View shareContainer = getView().findViewById(R.id.view_share_snapshot);
        if (shareContainer == null) return; // Fallback

        android.graphics.Bitmap bitmap = captureView(shareContainer);
        
        if (bitmap != null) {
            saveAndShareBitmap(bitmap);
        }
    }

    private android.graphics.Bitmap captureView(View view) {
        try {
            int height = view.getHeight();
            if (view instanceof android.widget.ScrollView) {
                height = ((android.widget.ScrollView) view).getChildAt(0).getHeight();
            }
            
            android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(view.getWidth(), height, android.graphics.Bitmap.Config.ARGB_8888);
            android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
            
            // Use Dark Theme background for share instead of white
            canvas.drawColor(Color.parseColor("#0F1021")); // Matches bg_dark
            
            view.draw(canvas);
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void saveAndShareBitmap(android.graphics.Bitmap bitmap) {
        try {
            java.io.File cachePath = new java.io.File(requireContext().getCacheDir(), "images");
            cachePath.mkdirs();
            java.io.File file = new java.io.File(cachePath, "performance_snapshot.png");
            java.io.FileOutputStream stream = new java.io.FileOutputStream(file);
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();

            android.net.Uri contentUri = androidx.core.content.FileProvider.getUriForFile(requireContext(), 
                    requireContext().getPackageName() + ".fileprovider", file);

            if (contentUri != null) {
                android.content.Intent shareIntent = new android.content.Intent();
                shareIntent.setAction(android.content.Intent.ACTION_SEND);
                shareIntent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                shareIntent.setDataAndType(contentUri, requireContext().getContentResolver().getType(contentUri));
                shareIntent.putExtra(android.content.Intent.EXTRA_STREAM, contentUri);
                shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "My Samvaad Interview Performance");
                shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, "Check out my AI Interview analytics from Samvaad! 🚀");
                startActivity(android.content.Intent.createChooser(shareIntent, "Share Performance Snapshot"));
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
}