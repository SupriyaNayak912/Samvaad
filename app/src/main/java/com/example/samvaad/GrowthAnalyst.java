package com.example.samvaad;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GrowthAnalyst {

    public interface InsightCallback {
        void onSuccess(String insight);
        void onFailure(Exception e);
    }

    public static void generateInsight(List<SessionMetrics> history, float growthPercent, InsightCallback callback) {
        if (history == null || history.isEmpty()) {
            callback.onFailure(new Exception("No history available"));
            return;
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a world-class career coach. Analyze the user's interview session history and provide ONE short, data-driven sentence explaining their performance trend.\n\n");
        
        prompt.append("Historical Data (Last ").append(Math.min(5, history.size())).append(" sessions):\n");
        for (int i = 0; i < Math.min(5, history.size()); i++) {
            SessionMetrics s = history.get(i);
            prompt.append("- Session ").append(i + 1).append(": Score=").append((int)s.overallScore)
                  .append(", Pace=").append((int)s.paceScore).append(", Clarity=").append((int)s.clarityScore)
                  .append(", Distractions=").append(s.telemetry.chaosDistractionCount).append("\n");
        }

        prompt.append("\nUser's Recent Growth Trend: ").append(String.format("%.1f", growthPercent)).append("%\n");
        prompt.append("\nTask: Provide ONE sentence of data-driven insight explicitly stating WHY their score changed or stayed flat (e.g., 'Your 5% growth is driven by fewer filler words' or 'Your score dropped due to erratic pacing'). Be concise (max 20 words). Return raw text, no JSON, no quotes.");

        String apiKey = BuildConfig.GROQ_API_KEY;
        if (apiKey == null || apiKey.isEmpty()) {
            callback.onFailure(new IllegalStateException("Groq API Key missing"));
            return;
        }

        GroqApiClient.ChatCompletionRequest request = new GroqApiClient.ChatCompletionRequest(
                "llama-3.1-8b-instant", 0.6);
        request.maxTokens = 100;
        request.addMessage("system", "You are a professional career coach. You output single sentences of insight.");
        request.addMessage("user", prompt.toString());

        GroqApiClient.getApiService().createChatCompletion(request)
                .enqueue(new Callback<GroqApiClient.ChatCompletionResponse>() {
                    @Override
                    public void onResponse(Call<GroqApiClient.ChatCompletionResponse> call,
                                         Response<GroqApiClient.ChatCompletionResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            String insight = response.body().getFirstChoiceContent();
                            if (insight != null) insight = insight.trim();
                            
                            final String result = (insight != null && !insight.isEmpty()) ? insight : 
                                "Your performance is stabilizing nicely. Keep pushing for that Top 1% rank!";
                            
                            new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(result));
                        } else {
                            new Handler(Looper.getMainLooper()).post(() -> callback.onFailure(new Exception("API Error")));
                        }
                    }

                    @Override
                    public void onFailure(Call<GroqApiClient.ChatCompletionResponse> call, Throwable t) {
                        new Handler(Looper.getMainLooper()).post(() -> callback.onFailure(new Exception(t)));
                    }
                });
    }
}
