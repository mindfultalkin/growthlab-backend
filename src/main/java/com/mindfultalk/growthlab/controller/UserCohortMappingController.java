package com.mindfultalk.growthlab.controller;

import com.mindfultalk.growthlab.dto.UserCohortMappingDTO;
import com.mindfultalk.growthlab.model.UserCohortMapping;
import com.mindfultalk.growthlab.service.UserCohortMappingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;


@RestController
@RequestMapping("/api/v1/user-cohort-mappings")
public class UserCohortMappingController {

    @Autowired
    private UserCohortMappingService userCohortMappingService;

    // GET all mappings
    @GetMapping
    public List<UserCohortMappingDTO> getAllUserCohortMappings() {
        return userCohortMappingService.getAllUserCohortMappings();
    }
    
 // GET user cohort mappings by cohortId
    @GetMapping("/cohort/{cohortId}")
    public ResponseEntity<Map<String, Object>> getUserCohortMappingsByCohortId(@PathVariable String cohortId) {
        try {
            Map<String, Object> response = userCohortMappingService.getUserCohortMappingsByCohortId(cohortId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body(Map.of("error", "An unexpected error occurred."));
        }
    }

    @GetMapping("/cohort/{cohortId}/learner")
    public List<UserCohortMappingDTO> getUserCohortMappingsCohortId(@PathVariable String cohortId) {
        return userCohortMappingService.getUserCohortMappingsCohortId(cohortId);
    }
    
    @GetMapping("/cohort/{cohortId}/leaderboard")
    public ResponseEntity<Map<String, Object>> getCohortLeaderboard(@PathVariable String cohortId) {
        try {
            Map<String, Object> leaderboardData = userCohortMappingService.getUserCohortMappingsWithLeaderboard(cohortId);
            return ResponseEntity.ok(leaderboardData);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    // GET user cohort mappings by userId
    @GetMapping("/user/{userId}")
    public List<UserCohortMappingDTO> getUserCohortMappingsByUserId(@PathVariable String userId) {
        return userCohortMappingService.getUserCohortMappingsByUserId(userId);
    }

    // POST (create) a new user-cohort mapping
    @PostMapping("/create")
    public ResponseEntity<UserCohortMapping> createUserCohortMapping(@RequestBody Map<String, String> request) {
        String userId = request.get("userId");
        String cohortId = request.get("cohortId");
        try {
            UserCohortMapping createdMapping = userCohortMappingService.createUserCohortMapping(userId, cohortId);
            return ResponseEntity.ok(createdMapping);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(null);
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body(null);
        }
    }    
    
    @PostMapping("/bulkcreate")
    public ResponseEntity<Map<String, List<String>>> importUserCohortMappings(@RequestParam("file") MultipartFile file) {
        Map<String, List<String>> response = userCohortMappingService.importUserCohortMappingsWithResponse(file);
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/cohort/{cohortId}")
    public ResponseEntity<String> updateUserCohortMappingByCohortId(
            @PathVariable String cohortId, 
            @RequestBody UserCohortMapping userCohortMapping) {
        try {
            userCohortMappingService.updateUserCohortMappingByCohortId(cohortId, userCohortMapping);
            return ResponseEntity.ok("User-Cohort mapping updated successfully.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Unexpected error: " + e.getMessage());
        }
    }

 // PUT (update) an existing mapping by userId
    @PutMapping("/user/{userId}/cohort/{cohortId}")
    public ResponseEntity<UserCohortMapping> updateUserCohortMapping(
            @PathVariable String userId,
            @PathVariable String cohortId,
            @RequestBody UserCohortMapping userCohortMapping) {
        try {
            UserCohortMapping updatedMapping = userCohortMappingService.updateUserCohortMapping(userId, cohortId, userCohortMapping);
            return ResponseEntity.ok(updatedMapping);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    
    // DELETE a specific mapping by userId
    @DeleteMapping("/user/{userId}")
    public ResponseEntity<Void> deleteUserCohortMappingByUserId(@PathVariable String userId) {
        userCohortMappingService.deleteUserCohortMappingByUserId(userId);
        return ResponseEntity.noContent().build();
    }
    // New endpoint to update leaderboard score from Google Forms
    @PutMapping("/update-score")
    public ResponseEntity<?> updateLeaderboardScore(@RequestBody Map<String, Object> request) {
        try {
            String userId = (String) request.get("userId");
            String cohortId = (String) request.get("cohortId");
            Integer scoreToAdd = Integer.valueOf(request.get("score").toString());
            
            UserCohortMappingDTO updatedMapping = userCohortMappingService.updateLeaderboardScore(userId, cohortId, scoreToAdd);
            return ResponseEntity.ok(updatedMapping);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body(Map.of("error", "An unexpected error occurred: " + ex.getMessage()));
        }
    }
}