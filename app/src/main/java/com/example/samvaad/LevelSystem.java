package com.example.samvaad;

import java.util.ArrayList;
import java.util.List;

public class LevelSystem {

    public static class Level {
        public int id;
        public String title;
        public String goal;
        public float minScore;

        public Level(int id, String title, String goal, float minScore) {
            this.id = id;
            this.title = title;
            this.goal = goal;
            this.minScore = minScore;
        }
    }

    public static List<Level> getLevels() {
        List<Level> levels = new ArrayList<>();
        levels.add(new Level(1, "Chaos Initiate", "Achieve 30% SRI Score", 30f));
        levels.add(new Level(2, "Steady Speaker", "Achieve 50% SRI Score", 50f));
        levels.add(new Level(3, "The Eloquent One", "Achieve 70% SRI Score", 70f));
        levels.add(new Level(4, "Resilient Mind", "Achieve 85% SRI Score", 85f));
        levels.add(new Level(5, "Focus Master", "Achieve 90% SRI Score", 90f));
        levels.add(new Level(6, "The Storyteller", "Complete 5 STAR sessions", 92f));
        levels.add(new Level(7, "Technical Titan", "Score 95% in Tech rounds", 95f));
        levels.add(new Level(8, "Pressure Expert", "Survive 5 Chaos sessions", 97f));
        levels.add(new Level(9, "Job Ready", "Top 5% of all candidates", 98f));
        levels.add(new Level(10, "Samvaad Legend", "Perfect 100% Score", 100f));
        return levels;
    }

    public static Level getLevelForScore(float score) {
        List<Level> levels = getLevels();
        Level current = levels.get(0);
        for (Level l : levels) {
            if (score >= l.minScore) {
                current = l;
            } else {
                break;
            }
        }
        return current;
    }
}
