package com.mindfultalk.growthlab.exception;

public class CohortNotFoundException extends RuntimeException {
    public CohortNotFoundException(String message) {
        super(message);
    }
}
