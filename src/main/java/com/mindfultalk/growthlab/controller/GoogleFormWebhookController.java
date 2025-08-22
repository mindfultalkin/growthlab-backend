package com.mindfultalk.growthlab.controller;

import com.mindfultalk.growthlab.dto.UserCohortMappingDTO;
import com.mindfultalk.growthlab.service.UserCohortMappingService;

import org.slf4j.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/webhooks")
public class GoogleFormWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(GoogleFormWebhookController.class);
    
    @Autowired
    private UserCohortMappingService userCohortMappingService;
    
    @PostMapping("/google-form")
    public ResponseEntity<?> handleGoogleFormSubmission(@RequestBody Map<String, Object> formData) {
        logger.info("Received webhook from Google Form: {}", formData);
        
        try {
            // Extract the required fields from the form submission
            String userId = extractField(formData, "userId");
            String cohortId = extractField(formData, "cohortId");
            Integer score = extractScore(formData);
            
            logger.info("Processing score update: userId={}, cohortId={}, score={}", userId, cohortId, score);
            
            // Update the leaderboard score
            UserCohortMappingDTO updatedMapping = userCohortMappingService.updateLeaderboardScore(userId, cohortId, score);
            
            // Return a success response
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Leaderboard score updated successfully");
            response.put("updatedMapping", updatedMapping);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error processing Google Form submission: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    /**
     * Extracts a field from the form data.
     * @param formData The form data map
     * @param fieldName The name of the field to extract
     * @return The value of the field
     * @throws IllegalArgumentException if the field is missing or empty
     */
    private String extractField(Map<String, Object> formData, String fieldName) {
        Object value = formData.get(fieldName);
        
        if (value == null) {
            throw new IllegalArgumentException("Required field '" + fieldName + "' is missing");
        }
        
        String strValue = value.toString().trim();
        
        if (strValue.isEmpty()) {
            throw new IllegalArgumentException("Required field '" + fieldName + "' cannot be empty");
        }
        
        return strValue;
    }
    
    /**
     * Extracts and parses the score from the form data.
     * @param formData The form data map
     * @return The parsed score as an Integer
     * @throws IllegalArgumentException if the score is missing, empty, or not a valid number
     */
    private Integer extractScore(Map<String, Object> formData) {
        Object scoreValue = formData.get("score");
        
        if (scoreValue == null) {
            throw new IllegalArgumentException("Required field 'score' is missing");
        }
        
        String scoreStr = scoreValue.toString().trim();
        
        if (scoreStr.isEmpty()) {
            throw new IllegalArgumentException("Score cannot be empty");
        }
        
        try {
            return Integer.parseInt(scoreStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Score must be a valid number");
        }
    }
}