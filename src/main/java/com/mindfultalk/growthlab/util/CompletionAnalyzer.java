package com.mindfultalk.growthlab.util;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import com.mindfultalk.growthlab.model.UserSubConcept;

public class CompletionAnalyzer {
    
    public void analyzeCompletions(String userId, String programId, List<UserSubConcept> completions, 
                                 boolean delayedStageUnlock, int delayInDays) {
        
        // Group completions by stage
        Map<String, List<UserSubConcept>> completionsByStage = completions.stream()
            .collect(Collectors.groupingBy(u -> u.getStage().getStageId()));
            
        // For each stage, analyze completions
        completionsByStage.forEach((stageId, stageCompletions) -> {
            System.out.println("\n=== Analysis for Stage " + stageId + " ===");
            
            // Print all completions chronologically
            System.out.println("\nAll subconcept completions:");
            stageCompletions.stream()
                .sorted(Comparator.comparing(UserSubConcept::getCompletionDate))
                .forEach(completion -> {
                    System.out.printf("Subconcept: %s, Completed at: %s%n",
                        completion.getSubconcept().getSubconceptId(),
                        completion.getCompletionDate().format(
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                });
                
            // Find last completion in stage
            Optional<UserSubConcept> lastCompletion = stageCompletions.stream()
                .max(Comparator.comparing(UserSubConcept::getCompletionDate));
                
            lastCompletion.ifPresent(completion -> {
                System.out.println("\nLast completed subconcept:");
                System.out.printf("Subconcept ID: %s%n", 
                    completion.getSubconcept().getSubconceptId());
                System.out.printf("Completion Time: %s%n",
                    completion.getCompletionDate().format(
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                
                // Calculate next stage availability if delayed unlock is enabled
                if (delayedStageUnlock) {
                    OffsetDateTime completionDate = completion.getCompletionDate();
                    OffsetDateTime currentDate = OffsetDateTime.now();
                    OffsetDateTime unlockDate = completionDate.plusDays(delayInDays);
                    
                    System.out.println("\nNext stage unlock analysis:");
                    System.out.printf("Stage completion: %s%n", 
                        completionDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    System.out.printf("Configured delay: %d days%n", delayInDays);
                    System.out.printf("Unlock date: %s%n", 
                        unlockDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    
                    if (currentDate.isBefore(unlockDate)) {
                        long daysRemaining = ChronoUnit.DAYS.between(currentDate, unlockDate);
                        long hoursRemaining = ChronoUnit.HOURS.between(currentDate, unlockDate) % 24;
                        System.out.printf("Time until unlock: %d days and %d hours%n", 
                            daysRemaining, hoursRemaining);
                    } else {
                        System.out.println("Stage is already unlocked!");
                    }
                }
            });
        });
    }
}