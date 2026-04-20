package com.example.samvaad;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LlmFeedbackEngine {

    public interface FeedbackCallback {
        void onSuccess(LlmFeedback feedback);

        void onFailure(Exception e);
    }

    public static void generateFeedback(SessionSummary summary, FeedbackCallback callback) {

        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an expert interview coach analyzing a candidate's session.\n\n");
        prompt.append("Context:\n");
        prompt.append("- Target Role: ").append(summary.targetRole).append("\n");
        prompt.append("- Scenario: ").append(summary.scenarioTitle).append("\n");
        prompt.append("- Objective: ").append(summary.sessionGoal).append("\n\n");

        prompt.append("Raw Telemetry Data (Evaluate and Score this):\n");
        prompt.append("- Average Pace: ").append(summary.avgWpm).append(" WPM\n");
        prompt.append("- Filler Words Count: ").append(summary.fillerWordCount).append("\n");
        prompt.append("- Chaos/Distraction Hits: ").append(summary.chaosDistractionCount).append("\n");
        prompt.append("- Silence/Pauses Count: ").append(summary.silenceCount).append("\n");
        prompt.append("- Session Duration: ").append(summary.durationSeconds).append(" seconds\n\n");

        prompt.append("Interview Transcript (Transcribed via Whisper):\n");
        if (summary.masterTranscript != null && !summary.masterTranscript.isEmpty()) {
            prompt.append("Full Audio Transcript: \"").append(summary.masterTranscript).append("\"\n\n");
            prompt.append("Questions Asked by Interviewer (Match the candidate's answers to these):\n");
            if (summary.transcript != null) {
                for (int i = 0; i < summary.transcript.size(); i++) {
                    QnAPair qna = summary.transcript.get(i);
                    prompt.append("Q").append(i + 1).append(": ").append(qna.getQuestion()).append("\n");
                }
            }
        } else {
            prompt.append("(No speech recorded)\n\n");
        }

        prompt.append("Task:\n");
        prompt.append("You are a strictly professional and BRUTALLY CRITICAL interview analyst. ");
        prompt.append("Your goal is to provide a 'reality check' for the candidate. Do NOT give participation points. ");
        prompt.append("Evaluate the candidate's answers based on logical consistency, depth of technical or situational examples (STAR method), and professional polish. ");
        prompt.append("CRITICAL: If the 'Full Audio Transcript' is empty, contains only noise (e.g., 'Booo', 'Mumbles'), or is unrelated to the questions, you MUST assign a score of 0 for Pace, Clarity, and Resilience. ");
        prompt.append("In the summary, state clearly: 'No professional participation detected. Performance categorized as non-responsive.' ");
        prompt.append("Do NOT provide generic encouragement. If an answer is weak, short, or lacks substance, be blunt and highlight EXACTLY why. ");
        prompt.append("Analyze the provided metrics (WPM, Fillers, Composure) and penalize the score heavily if fillers are high or pacing is erratic. ");
        prompt.append("Return EXACTLY a JSON object with this exact structure:\n");
        prompt.append("{\n");
        prompt.append("  \"summary\": \"Direct, critical assessment of overall performance.\",\n");
        prompt.append("  \"strengths\": [\"Strength 1 (Be specific)\", \"Strength 2\"],\n");
        prompt.append("  \"areasToImprove\": [\"Critical Weakness 1\", \"Critical Weakness 2\"],\n");
        prompt.append("  \"coachingTip\": \"A specific, hard-hitting drill for next time.\",\n");
        prompt.append("  \"overallScore\": 85,\n");
        prompt.append("  \"paceScore\": 90,\n");
        prompt.append("  \"clarityScore\": 75,\n");
        prompt.append("  \"resilienceScore\": 80,\n");
        prompt.append("  \"presenceScore\": 85,\n");
        prompt.append("  \"questionAnalysis\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"question\": \"The question asked\",\n");
        prompt.append("      \"whatYouSaidSummary\": \"Extremely brief, objective summary\",\n");
        prompt.append("      \"betterApproach\": \"A superior way to have handled this specific question\"\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n");
        prompt.append("Focus on fact-based analysis. Do not include markdown or other text. Just raw JSON.");

        String apiKey = BuildConfig.GROQ_API_KEY;
        if (apiKey == null || apiKey.isEmpty()) {
            callback.onFailure(new IllegalStateException("Groq API Key missing"));
            return;
        }

        GroqApiClient.ChatCompletionRequest request = new GroqApiClient.ChatCompletionRequest(
                "llama-3.1-8b-instant", 0.5);
        request.maxTokens = 2048;
        request.addMessage("system", "You output only pure JSON objects.");
        request.addMessage("user", prompt.toString());
        request.setJsonResponse();

        GroqApiClient.getApiService().createChatCompletion(request)
                .enqueue(new Callback<GroqApiClient.ChatCompletionResponse>() {
                    @Override
                    public void onResponse(Call<GroqApiClient.ChatCompletionResponse> call,
                            Response<GroqApiClient.ChatCompletionResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            String text = response.body().getFirstChoiceContent();
                            if (text == null)
                                text = "{}";

                            try {
                                text = text.trim();
                                if (text.startsWith("```json"))
                                    text = text.substring(7);
                                else if (text.startsWith("```"))
                                    text = text.substring(3);
                                if (text.endsWith("```"))
                                    text = text.substring(0, text.length() - 3);
                                text = text.trim();

                                JSONObject json = new JSONObject(text);
                                LlmFeedback feedback = new LlmFeedback();
                                feedback.setSummary(json.optString("summary", "Analysis completed."));
                                feedback.setBehavioralProfile(summary.behavioralProfile);
                                feedback.setCoachingTip(json.optString("coachingTip", "Keep practicing."));
                                feedback.setOverallScore(json.optInt("overallScore", 75));
                                feedback.setPaceScore((float) json.optDouble("paceScore", 70.0));
                                feedback.setClarityScore((float) json.optDouble("clarityScore", 70.0));
                                feedback.setResilienceScore((float) json.optDouble("resilienceScore", 70.0));
                                feedback.setPresenceScore((float) json.optDouble("presenceScore", 70.0));


                                List<String> strengths = new ArrayList<>();
                                JSONArray strArr = json.optJSONArray("strengths");
                                if (strArr != null) {
                                    for (int i = 0; i < strArr.length(); i++)
                                        strengths.add(strArr.getString(i));
                                }
                                feedback.setStrengths(strengths);

                                List<String> areas = new ArrayList<>();
                                JSONArray areaArr = json.optJSONArray("areasToImprove");
                                if (areaArr != null) {
                                    for (int i = 0; i < areaArr.length(); i++)
                                        areas.add(areaArr.getString(i));
                                }
                                feedback.setAreasToImprove(areas);
                                
                                List<QuestionFeedback> qaList = new ArrayList<>();
                                JSONArray qaArr = json.optJSONArray("questionAnalysis");
                                if (qaArr != null) {
                                    for (int i = 0; i < qaArr.length(); i++) {
                                        JSONObject qObj = qaArr.getJSONObject(i);
                                        qaList.add(new QuestionFeedback(
                                            qObj.optString("question", ""),
                                            qObj.optString("whatYouSaidSummary", ""),
                                            qObj.optString("betterApproach", "")
                                        ));
                                    }
                                }
                                feedback.setQuestionAnalysis(qaList);

                                new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(feedback));

                            } catch (Exception e) {
                                new Handler(Looper.getMainLooper()).post(() -> callback.onFailure(e));
                            }
                        } else {
                            String errorMsg = "Status: " + response.code();
                            try {
                                if (response.errorBody() != null) {
                                    String body = response.errorBody().string();
                                    JSONObject errJson = new JSONObject(body);
                                    if (errJson.has("error")) {
                                        errorMsg = errJson.getJSONObject("error").optString("message", body);
                                    }
                                }
                            } catch (Exception ignored) {
                            }

                            final String finalMsg = errorMsg;
                            new Handler(Looper.getMainLooper()).post(() -> callback.onFailure(new Exception(finalMsg)));
                        }
                    }

                    @Override
                    public void onFailure(Call<GroqApiClient.ChatCompletionResponse> call, Throwable t) {
                        new Handler(Looper.getMainLooper()).post(() -> callback.onFailure(new Exception(t)));
                    }
                });
    }
}
