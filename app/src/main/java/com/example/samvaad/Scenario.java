package com.example.samvaad;

import java.io.Serializable;
import java.util.List;

public class Scenario implements Serializable {
    private String id;
    private String title;
    private String category;
    private String difficulty;
    private int questionCount;
    private float lastScore;
    private List<String> questions;

    // Required no-arg constructor for Firestore
    public Scenario() {}

    public Scenario(String id, String title, String category, String difficulty, int questionCount, float lastScore) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.difficulty = difficulty;
        this.questionCount = questionCount;
        this.lastScore = lastScore;
    }

    public String getId()           { return id; }
    public String getTitle()        { return title; }
    public String getCategory()     { return category; }
    public String getDifficulty()   { return difficulty; }
    public int getQuestionCount()   { return questionCount; }
    public float getLastScore()     { return lastScore; }
    public List<String> getQuestions() { return questions; }

    public void setId(String id)                       { this.id = id; }
    public void setTitle(String title)                 { this.title = title; }
    public void setCategory(String category)           { this.category = category; }
    public void setDifficulty(String difficulty)       { this.difficulty = difficulty; }
    public void setQuestionCount(int questionCount)    { this.questionCount = questionCount; }
    public void setLastScore(float lastScore)          { this.lastScore = lastScore; }
    public void setQuestions(List<String> questions)   { this.questions = questions; }
}