package com.example.samvaad;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;

public interface ApiService {
    // This is where your REST APIs are defined (Experiment 6)
    @GET("scenarios") // The endpoint URL
    Call<List<Scenario>> getScenarios();
    
    // Example for fetching a single random question
    @GET("random-question")
    Call<Scenario> getRandomQuestion();
}