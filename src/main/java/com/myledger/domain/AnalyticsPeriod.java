package com.myledger.domain;

public enum AnalyticsPeriod {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    YEARLY("Yearly");

    private final String label;

    AnalyticsPeriod(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
