package com.myledger.domain;

public enum CompanionType {
    NONE("N/A"),
    ALONE("Alone"),
    WITH_OTHERS("With Others");

    private final String label;

    CompanionType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
