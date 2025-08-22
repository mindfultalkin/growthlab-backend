package com.mindfultalk.growthlab.model;

public enum UserType {
    LEARNER("learner"),
    MENTOR("mentor");

    private final String value;

    UserType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static UserType fromString(String text) {
        if (text != null) {
            String lowercaseText = text.toLowerCase();
            for (UserType type : UserType.values()) {
                if (type.value.equals(lowercaseText)) {
                    return type;
                }
            }
        }
        throw new IllegalArgumentException("Invalid usertype. Only 'learner' and 'mentor' are allowed.");
    }
}