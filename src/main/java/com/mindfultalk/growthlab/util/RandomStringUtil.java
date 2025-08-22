package com.mindfultalk.growthlab.util;

import java.security.SecureRandom;

public class RandomStringUtil {
    private static final String ALPHANUMERIC_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final String ALPHABETIC_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final String NUMERIC_CHARACTERS = "0123456789";
    private static final SecureRandom RANDOM = new SecureRandom(); // Use SecureRandom for better randomness

    // Method to generate random alphanumeric string
    public static String generateRandomAlphanumeric(int length) {
        return generateRandomString(length, ALPHANUMERIC_CHARACTERS);
    }

    // Method to generate random alphabetic string
    public static String generateRandomAlphabetic(int length) {
        return generateRandomString(length, ALPHABETIC_CHARACTERS);
    }

    // Method to generate random numeric string
    public static String generateRandomNumeric(int length) {
        return generateRandomString(length, NUMERIC_CHARACTERS);
    }

    // Generic method to generate random string from a given set of characters
    private static String generateRandomString(int length, String characters) {
        StringBuilder result = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = RANDOM.nextInt(characters.length());
            result.append(characters.charAt(index));
        }
        return result.toString();
    }
}