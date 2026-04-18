package com.example.samvaad;

import java.util.ArrayList;

/**
 * MockDatabase handles in-memory storage for session history during frontend-only demonstrations.
 * All data in this class is cleared when the app process is terminated.
 */
public class MockDatabase {
    public static ArrayList<SessionMetrics> sessionHistory = new ArrayList<>();

    /**
     * Finds a session by its unique ID within the mock history.
     * @param id The session ID to find.
     * @return The session metrics if found, or null otherwise.
     */
    public static SessionMetrics getSessionById(int id) {
        for (SessionMetrics session : sessionHistory) {
            if (session.id == id) {
                return session;
            }
        }
        return null;
    }
}
