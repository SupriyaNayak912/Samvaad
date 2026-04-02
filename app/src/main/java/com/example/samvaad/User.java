package com.example.samvaad;

public class User {
    private String name;
    private String email;
    private long memberSince;
    private int totalSessions;
    private int practiceStreak;
    private float bestScore;
    private String role;

    public User() {} // Required for Firestore

    public User(String name, String email, String role) {
        this.name = name;
        this.email = email;
        this.role = role;
        this.memberSince = System.currentTimeMillis();
        this.totalSessions = 0;
        this.practiceStreak = 0;
        this.bestScore = 0f;
    }

    public String getName() { return name; }
    public String getEmail() { return email; }
    public long getMemberSince() { return memberSince; }
    public int getTotalSessions() { return totalSessions; }
    public int getPracticeStreak() { return practiceStreak; }
    public float getBestScore() { return bestScore; }
    public String getRole() { return role; }
}