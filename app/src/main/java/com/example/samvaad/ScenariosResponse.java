package com.example.samvaad;

import java.util.List;

public class ScenariosResponse {
    private List<Scenario> scenarios;
    private int page;
    private int limit;

    public List<Scenario> getScenarios() {
        return scenarios;
    }
}
