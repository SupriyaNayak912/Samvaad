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

public class LlmQuestionGenerator {

    public interface QuestionCallback {
        void onSuccess(List<String> questions);

        void onFailure(Exception e);
    }

    public static void generateQuestions(String role, String companyType, String roundType,
            String experience, QuestionCallback callback) {

        // Inject a random nonce so the LLM cannot repeat cached responses
        String nonce = Long.toHexString(new java.util.Random().nextLong()).toUpperCase();

        String prompt = "You are a brutally honest, world-class interviewer at a top-tier " + companyType + " company.\n" +
                "You are interviewing a " + experience + "-level candidate for a " + role + " role.\n" +
                "This is a " + roundType + " round.\n\n" +
                "SESSION NONCE: " + nonce + " — use this to ensure completely fresh questions.\n\n" +
                "STRICT RULES — NEVER BREAK THESE:\n" +
                "1. FORBIDDEN questions: 'Tell me about yourself', 'greatest strength/weakness', 'where do you see yourself in 5 years', 'why do you want to work here'. These are too predictable.\n" +
                "2. Each question must be UNIQUE and SURPRISING — the candidate must not have seen it before.\n" +
                "3. Mix question types: situational ('You have 2 hours before a critical demo and the build breaks...'), technical ('Walk me through how you'd debug a production memory leak...'), behavioral ('Tell me about the last time you had to push back on your manager...'), and curveball ('If you had to replace yourself with a junior hire in 3 months, how would you prepare them?').\n" +
                "4. At least 2 questions must create a sense of healthy pressure or real urgency.\n" +
                "5. Questions should feel like they came from 6 DIFFERENT interviewers, not one person.\n\n" +
                "Return ONLY a JSON object with key 'questions' containing an array of exactly 6 interview question strings. No markdown, no extra text.";

        String apiKey = BuildConfig.GROQ_API_KEY;
        if (apiKey == null || apiKey.isEmpty()) {
            callback.onFailure(new IllegalStateException("Groq API Key missing"));
            return;
        }

        GroqApiClient.ChatCompletionRequest request = new GroqApiClient.ChatCompletionRequest(
                "llama-3.1-8b-instant", 0.7);
        request.maxTokens = 1024;
        request.addMessage("system",
                "You are a helpful assistant that outputs only JSON. You must return a JSON object with the format: {\"questions\": [\"q1\", \"q2\", ...]}");
        request.addMessage("user", prompt);
        request.setJsonResponse();

        GroqApiClient.getApiService().createChatCompletion(request)
                .enqueue(new Callback<GroqApiClient.ChatCompletionResponse>() {
                    @Override
                    public void onResponse(Call<GroqApiClient.ChatCompletionResponse> call,
                            Response<GroqApiClient.ChatCompletionResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            String text = response.body().getFirstChoiceContent();
                            if (text == null || text.isEmpty()) {
                                generateFallback(callback);
                                return;
                            }

                            try {
                                text = text.trim();
                                // Handle potential markdown backticks that some models still include
                                if (text.startsWith("```json"))
                                    text = text.substring(7);
                                else if (text.startsWith("```"))
                                    text = text.substring(3);
                                if (text.endsWith("```"))
                                    text = text.substring(0, text.length() - 3);
                                text = text.trim();

                                JSONObject json = new JSONObject(text);
                                JSONArray jsonArray = json.getJSONArray("questions");
                                List<String> questions = new ArrayList<>();
                                for (int i = 0; i < jsonArray.length(); i++) {
                                    questions.add(jsonArray.getString(i));
                                }

                                if (questions.isEmpty()) {
                                    generateFallback(callback);
                                    return;
                                }

                                new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(questions));

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

    private static void generateFallback(QuestionCallback callback) {
        List<String> questions = new ArrayList<>();
        questions.add("Tell me about yourself and your background.");
        questions.add("What are your greatest strengths?");
        questions.add("Describe a challenge you faced and how you overcame it.");
        questions.add("Where do you see yourself in 5 years?");
        questions.add("Why do you want to work here?");
        questions.add("Do you have any questions for me?");
        new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(questions));
    }
}
