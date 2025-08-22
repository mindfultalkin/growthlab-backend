package com.mindfultalk.growthlab.util;

public class DurationParser {

    public static int parseToSeconds(String input) {
        if (input == null || input.isBlank()) return 0;

        input = input.trim().toLowerCase();

        try {
            // Case: "19sec" or "19s"
            if (input.endsWith("sec") || input.endsWith("s")) {
                return Integer.parseInt(input.replaceAll("[^0-9.]", ""));
            }

            // Case: "2min", "2mins"
            if (input.endsWith("min") || input.endsWith("mins")) {
                String val = input.replaceAll("[^0-9.]", "");
                double minutes = Double.parseDouble(val);
                return (int) Math.round(minutes * 60);
            }

            // Case: "1.10" → 1 min 10 sec
            if (input.contains(".")) {
                String[] parts = input.split("\\.");
                if (parts.length == 2 && isNumeric(parts[0]) && isNumeric(parts[1])) {
                    int minutes = Integer.parseInt(parts[0]);
                    int seconds = Integer.parseInt(parts[1]);
                    return minutes * 60 + seconds;
                }
            }

            // Plain number → seconds
            if (isNumeric(input)) {
                return Integer.parseInt(input);
            }

        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid duration format: " + input);
        }

        throw new IllegalArgumentException("Unsupported duration format: " + input);
    }

    private static boolean isNumeric(String str) {
        return str != null && str.matches("\\d+");
    }
}
