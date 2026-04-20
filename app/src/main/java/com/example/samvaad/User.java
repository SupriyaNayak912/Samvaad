package com.example.samvaad;

public class User {
    private String name;
    private String email;
    @com.google.firebase.firestore.Exclude
    private java.util.Date memberSince;
    private int totalSessions;
    private int practiceStreak;
    private float bestScore;
    private String role;
    private float totalTechScore;
    private float totalCommScore;

    public User() {} // Required for Firestore

    public User(String name, String email, String role) {
        this.name = name;
        this.email = email;
        this.role = role;
        this.memberSince = new java.util.Date();
        this.totalSessions = 0;
        this.practiceStreak = 0;
        this.bestScore = 0f;
        this.totalTechScore = 0f;
        this.totalCommScore = 0f;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    @com.google.firebase.firestore.PropertyName("memberSince")
    public java.util.Date getMemberSince() { return memberSince; }

    @com.google.firebase.firestore.PropertyName("memberSince")
    public void setMemberSince(Object value) {
        if (value instanceof Long) {
            this.memberSince = new java.util.Date((Long) value);
        } else if (value instanceof com.google.firebase.Timestamp) {
            this.memberSince = ((com.google.firebase.Timestamp) value).toDate();
        } else if (value instanceof java.util.Date) {
            this.memberSince = (java.util.Date) value;
        }
    }
    public int getTotalSessions() { return totalSessions; }
    public void setTotalSessions(int totalSessions) { this.totalSessions = totalSessions; }
    public int getPracticeStreak() { return practiceStreak; }
    public void setPracticeStreak(int practiceStreak) { this.practiceStreak = practiceStreak; }
    public float getBestScore() { return bestScore; }
    public void setBestScore(float bestScore) { this.bestScore = bestScore; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public float getTotalTechScore() { return totalTechScore; }
    public void setTotalTechScore(float totalTechScore) { this.totalTechScore = totalTechScore; }
    public float getTotalCommScore() { return totalCommScore; }
    public void setTotalCommScore(float totalCommScore) { this.totalCommScore = totalCommScore; }
}