package com.example.samvaad;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;


public class GroqApiClient {

    private static final String BASE_URL = "https://api.groq.com/openai/v1/";
    private static GroqApiService apiService;

    public static GroqApiService getApiService() {
        if (apiService == null) {
            String groqApiKey = BuildConfig.GROQ_API_KEY;

            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(80, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .addInterceptor(logging)
                    .addInterceptor(chain -> {
                        Request original = chain.request();
                        Request.Builder requestBuilder = original.newBuilder()
                                .header("Authorization", "Bearer " + groqApiKey)
                                .header("Content-Type", "application/json");
                        Request request = requestBuilder.build();
                        return chain.proceed(request);
                    })
                    .build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            apiService = retrofit.create(GroqApiService.class);
        }
        return apiService;
    }

    public interface GroqApiService {
        @POST("chat/completions")
        Call<ChatCompletionResponse> createChatCompletion(@Body ChatCompletionRequest request);

        @Multipart
        @POST("audio/transcriptions")
        Call<WhisperResponse> transcribeAudio(
                @Part MultipartBody.Part file,
                @Part("model") RequestBody model,
                @Part("response_format") RequestBody responseFormat
        );
    }


    // --- Request Models ---
    public static class ChatCompletionRequest {
        @SerializedName("model")
        public String model;
        
        @SerializedName("messages")
        public List<Message> messages;
        
        @SerializedName("temperature")
        public double temperature;

        @SerializedName("response_format")
        public ResponseFormat responseFormat;

        @SerializedName("max_tokens")
        public Integer maxTokens;

        public ChatCompletionRequest(String model, double temperature) {
            this.model = model;
            this.temperature = temperature;
            this.messages = new ArrayList<>();
        }

        public void addMessage(String role, String content) {
            this.messages.add(new Message(role, content));
        }

        public void setJsonResponse() {
            this.responseFormat = new ResponseFormat("json_object");
        }
    }

    public static class Message {
        @SerializedName("role")
        public String role;
        
        @SerializedName("content")
        public String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    public static class ResponseFormat {
        @SerializedName("type")
        public String type;

        public ResponseFormat(String type) {
            this.type = type;
        }
    }

    // --- Response Models ---
    public static class ChatCompletionResponse {
        @SerializedName("choices")
        public List<Choice> choices;

        public String getFirstChoiceContent() {
            if (choices != null && !choices.isEmpty()) {
                Message message = choices.get(0).message;
                if (message != null && message.content != null) {
                    return message.content;
                }
            }
            return "";
        }
    }

    public static class Choice {
        @SerializedName("message")
        public Message message;
    }

    public static class WhisperResponse {
        @SerializedName("text")
        public String text;
    }
}

