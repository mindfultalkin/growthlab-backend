package com.mindfultalk.growthlab.service;

import com.mindfultalk.growthlab.model.*;
import com.mindfultalk.growthlab.repository.*;

import org.springframework.transaction.annotation.Transactional;
import org.slf4j.*;
import org.springframework.cache.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class UserAttemptsServiceImpl implements UserAttemptsService {

    @Autowired
    private UserAttemptsRepository userAttemptsRepository;

    @Autowired
    private UserCohortMappingService userCohortMappingService; 
    
    @Autowired
    private UserSubConceptService userSubConceptService;
    
    @Autowired
    private CacheManagementService cacheManagementService;
 
    private static final Logger logger = LoggerFactory.getLogger(UserAttemptsServiceImpl.class);


    @Override
    @Cacheable(value = "userAttempts", key = "'all'")
    public List<UserAttempts> getAllUserAttempts() {
        try {
            logger.info("Retrieving all user attempts from database");
            List<UserAttempts> userAttempts = userAttemptsRepository.findAll();
            logger.info("Successfully retrieved {} user attempts", userAttempts.size());
            return userAttempts;
        } catch (Exception e) {
            logger.error("Error occurred while retrieving all user attempts: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve user attempts", e);
        }
    }

    @Override
    @Cacheable(value = "userAttempts", key = "#userAttemptId")
    public Optional<UserAttempts> getUserAttemptById(Long userAttemptId) {
        try {
            logger.info("Retrieving user attempt with ID: {}", userAttemptId);
            
            if (userAttemptId == null) {
                logger.warn("User attempt ID is null");
                throw new IllegalArgumentException("User attempt ID cannot be null");
            }
            
            Optional<UserAttempts> userAttempt = userAttemptsRepository.findById(userAttemptId);
            
            if (userAttempt.isPresent()) {
                logger.info("Successfully found user attempt with ID: {}", userAttemptId);
            } else {
                logger.warn("No user attempt found with ID: {}", userAttemptId);
            }
            
            return userAttempt;
        } catch (IllegalArgumentException e) {
            logger.error("Invalid argument for getUserAttemptById: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error occurred while retrieving user attempt with ID {}: {}", userAttemptId, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve user attempt", e);
        }
    }
    
    @Override
    @Transactional
    @CacheEvict(value = "userAttempts", allEntries = true)
    public UserAttempts saveUserAttempt(UserAttempts userAttempt) {
        try {
            logger.info("Saving user attempt for user ID: {}", 
                       userAttempt != null && userAttempt.getUser() != null ? userAttempt.getUser().getUserId() : "null");
            
            if (userAttempt == null) {
                logger.error("User attempt object is null");
                throw new IllegalArgumentException("User attempt cannot be null");
            }
            
            if (userAttempt.getUser() == null) {
                logger.error("User is null in user attempt");
                throw new IllegalArgumentException("User cannot be null in user attempt");
            }
            
            UserAttempts savedAttempt = userAttemptsRepository.save(userAttempt);
            logger.info("Successfully saved user attempt with ID: {} for user: {}", 
                       savedAttempt.getUserAttemptId(), userAttempt.getUser().getUserId());
            
            return savedAttempt;
        } catch (IllegalArgumentException e) {
            logger.error("Invalid argument for saveUserAttempt: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error saving user attempt for user: {}: {}", 
                        userAttempt != null && userAttempt.getUser() != null ? userAttempt.getUser().getUserId() : "null", 
                        e.getMessage(), e);
            throw new RuntimeException("Failed to save user attempt", e);
        }
    }
    
    @Override
    @Transactional(timeout = 30) // 30 seconds timeout
    public UserAttempts createUserAttempt(UserAttempts userAttempt, String cohortId) {
        try {
            logger.info("Creating user attempt for user ID: {} in cohort: {}", 
                       userAttempt != null && userAttempt.getUser() != null ? userAttempt.getUser().getUserId() : "null", 
                       cohortId);
            
            if (userAttempt == null) {
                logger.error("User attempt object is null");
                throw new IllegalArgumentException("User attempt cannot be null");
            }
            
            if (cohortId == null || cohortId.trim().isEmpty()) {
                logger.error("Cohort ID is null or empty");
                throw new IllegalArgumentException("Cohort ID cannot be null or empty");
            }
            
            if (userAttempt.getUser() == null) {
                logger.error("User is null in user attempt");
                throw new IllegalArgumentException("User cannot be null in user attempt");
            }
            
    	// Save the user attempt first
        UserAttempts savedAttempt = userAttemptsRepository.save(userAttempt);
        logger.info("User attempt saved successfully for userId: {}, attemptId: {}",
                userAttempt.getUser().getUserId(), savedAttempt.getUserAttemptId());
        
        // Update leaderboard after saving attempt
        updateLeaderboard(savedAttempt, cohortId);
        
        // Update or create entry in UserSubConcept table
        updateUserSubConceptCompletionStatus(savedAttempt);
        
     // IMPORTANT: Enhanced cache eviction after completion status changes
        String userId = savedAttempt.getUser().getUserId();
        String programId = savedAttempt.getProgram().getProgramId();
        String stageId = savedAttempt.getStage().getStageId();
        String unitId = savedAttempt.getUnit().getUnitId();
        String subconceptId = savedAttempt.getSubconcept().getSubconceptId();
        
        // Evict all user completion caches (existing functionality)
        cacheManagementService.evictUserCompletionCaches(userId, programId);
        
        // ADDITIONAL: Evict specific unit-level report caches for more targeted cache management
        cacheManagementService.evictUnitReportCaches(userId, unitId, stageId, programId);
        
       // OPTIONAL: If you have the ProgramReportService available here, you can also call:
        // programReportService.evictUserReportCaches(userId, programId, stageId, unitId, subconceptId);
        
     
        logger.info("Successfully created user attempt with ID: {} for user: {} in cohort: {} and evicted related caches", 
                savedAttempt.getUserAttemptId(), userAttempt.getUser().getUserId(), cohortId);
     
     return savedAttempt;
 } catch (IllegalArgumentException e) {
     logger.error("Invalid argument for createUserAttempt: {}", e.getMessage());
     throw e;
 } catch (Exception e) {
     logger.error("Error while creating user attempt for userId: {}, cohortId: {}, Error: {}",
             userAttempt != null && userAttempt.getUser() != null ? userAttempt.getUser().getUserId() : "null", 
             cohortId, e.getMessage(), e);
     throw new RuntimeException("Failed to create user attempt. Please try again later.", e);
 }
}
    
    private void updateUserSubConceptCompletionStatus(UserAttempts userAttempt) { 
    	try {
    		logger.debug("Updating UserSubConcept completion status for user: {}", 
                    userAttempt.getUser().getUserId());
    		
    	// Retrieve the details from the user attempt 
    	String userId = userAttempt.getUser().getUserId(); 
        String programId = userAttempt.getProgram().getProgramId();
        String stageId = userAttempt.getStage().getStageId();
        String unitId = userAttempt.getUnit().getUnitId();
        String subconceptId = userAttempt.getSubconcept().getSubconceptId();
        
        logger.debug("Processing UserSubConcept for userId: {}, programId: {}, stageId: {}, unitId: {}, subconceptId: {}", 
                userId, programId, stageId, unitId, subconceptId);
        
     // Check if a UserSubConcept entry already exists for the unique constraint fields
        Optional<UserSubConcept> existingEntry = userSubConceptService
                .findByUser_UserIdAndProgram_ProgramIdAndStage_StageIdAndUnit_UnitIdAndSubconcept_SubconceptId(userId, programId, stageId, unitId, subconceptId);

        if (existingEntry.isEmpty()) {
            // No entry exists, create a new one
    	UserSubConcept userSubConcept = new UserSubConcept(); 
    	userSubConcept.setUser(userAttempt.getUser()); 
    	userSubConcept.setProgram(userAttempt.getProgram()); 
    	userSubConcept.setStage(userAttempt.getStage()); 
    	userSubConcept.setUnit(userAttempt.getUnit()); 
    	userSubConcept.setSubconcept(userAttempt.getSubconcept()); 
    	userSubConcept.setCompletionStatus(true);
    	userSubConcept.setUuid(UUID.randomUUID());
    	
    	userSubConceptService.createUserSubConcept(userSubConcept);
    	logger.info("New UserSubConcept entry created for userId: {}, subconceptId: {}", userId, subconceptId);
        } else {
            // Entry already exists, update completion status if needed
            UserSubConcept userSubConcept = existingEntry.get();
            if (!userSubConcept.isCompletionStatus()) {
                userSubConcept.setCompletionStatus(true); 
                userSubConceptService.updateUserSubConcept(userSubConcept);
                logger.info("Updated completion status for UserSubConcept, userId: {}, subconceptId: {}", userId, subconceptId);
            } else {
                logger.debug("UserSubConcept already marked as complete for userId: {}, subconceptId: {}", userId, subconceptId);
            }
        }
    } catch (Exception e) {
        logger.error("Error updating UserSubConcept for userId: {}, Error: {}", 
                    userAttempt.getUser().getUserId(), e.getMessage(), e);
        throw new RuntimeException("Failed to update UserSubConcept. Please contact support.", e);
    }
}

   // Revised updateLeaderboard method to handle multiple cohorts
    private void updateLeaderboard(UserAttempts userAttempt, String cohortId) {
    	try {
    		logger.debug("Updating leaderboard for user: {} in cohort: {}", 
                    userAttempt.getUser().getUserId(), cohortId);
    		
        User user = userAttempt.getUser();
        int score = userAttempt.getUserAttemptScore();

        logger.debug("User attempt score: {} for user: {}", score, user.getUserId());
        
        // Retrieve the user's specific cohort mapping for the cohort tied to this attempt
        Optional<UserCohortMapping> userCohortMappingOpt = 
            userCohortMappingService.findByUser_UserIdAndCohort_CohortId(user.getUserId(), cohortId);

        if (userCohortMappingOpt.isPresent()) {
            // Update existing leaderboard score for the specified cohort
            UserCohortMapping userCohortMapping = userCohortMappingOpt.get();
            int previousScore = userCohortMapping.getLeaderboardScore();
            int updatedScore = previousScore + score;
            userCohortMapping.setLeaderboardScore(updatedScore);
            
            
         // Save the updated UserCohortMapping
            userCohortMappingService.updateUserCohortMapping(userCohortMapping.getUserCohortId(), userCohortMapping);
            logger.info("Updated leaderboard for userId: {}, cohortId: {}, previousScore: {}, newScore: {}", 
            		user.getUserId(), cohortId, previousScore, updatedScore);
        } else {
            // If no mapping found, create a new leaderboard entry
            UserCohortMapping newEntry = new UserCohortMapping();
            Cohort cohort = new Cohort();
            cohort.setCohortId(cohortId); 
            newEntry.setCohort(cohort); 
            newEntry.setUser(user);
            newEntry.setLeaderboardScore(score);
            newEntry.setUuid(UUID.randomUUID());
            
            
            // Save the new UserCohortMapping entry
            userCohortMappingService.createUserCohortMapping(newEntry);
            logger.info("New leaderboard entry created for userId: {}, cohortId: {}, score: {}", user.getUserId(), cohortId, score);
        }
    } catch (Exception e) {
        logger.error("Error updating leaderboard for userId: {}, cohortId: {}, Error: {}", userAttempt.getUser().getUserId(), cohortId, e.getMessage(), e);
        throw new RuntimeException("Failed to update leaderboard. Please try again later.");
    }
}

    @Override
    @CachePut(value = "userAttempts", key = "#userAttemptId")
    @CacheEvict(value = "userAttempts", key = "'all'")
    public UserAttempts updateUserAttempt(Long userAttemptId, UserAttempts userAttempt) {
        try {
            logger.info("Updating user attempt with ID: {}", userAttemptId);
            
            if (userAttemptId == null) {
                logger.error("User attempt ID is null for update");
                throw new IllegalArgumentException("User attempt ID cannot be null");
            }
            
            if (userAttempt == null) {
                logger.error("Updated user attempt object is null");
                throw new IllegalArgumentException("Updated user attempt cannot be null");
            }
            
            return userAttemptsRepository.findById(userAttemptId)
                    .map(existingAttempt -> {
                        logger.debug("Found existing user attempt with ID: {}, updating fields", userAttemptId);
                        
                        existingAttempt.setUserAttemptEndTimestamp(userAttempt.getUserAttemptEndTimestamp());
                        existingAttempt.setUserAttemptFlag(userAttempt.isUserAttemptFlag());
                        existingAttempt.setUserAttemptScore(userAttempt.getUserAttemptScore());
                        existingAttempt.setUserAttemptStartTimestamp(userAttempt.getUserAttemptStartTimestamp());
                        existingAttempt.setUser(userAttempt.getUser());
                        existingAttempt.setStage(userAttempt.getStage());
                        existingAttempt.setUnit(userAttempt.getUnit());
                        existingAttempt.setProgram(userAttempt.getProgram());
                        existingAttempt.setSession(userAttempt.getSession());
                        existingAttempt.setSubconcept(userAttempt.getSubconcept());
                        existingAttempt.setUuid(userAttempt.getUuid());
                        
                        UserAttempts savedAttempt = userAttemptsRepository.save(existingAttempt);
                        logger.info("Successfully updated user attempt with ID: {}", userAttemptId);
                        return savedAttempt;
                    })
                    .orElseThrow(() -> {
                        logger.error("User attempt not found with ID: {} for update", userAttemptId);
                        return new IllegalArgumentException("UserAttempt not found with ID: " + userAttemptId);
                    });
        } catch (IllegalArgumentException e) {
            logger.error("Invalid argument for updateUserAttempt: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error occurred while updating user attempt with ID {}: {}", userAttemptId, e.getMessage(), e);
            throw new RuntimeException("Failed to update user attempt", e);
        }
    }
    
    @Override
    @CacheEvict(value = "userAttempts", allEntries = true)
    public void deleteUserAttempt(Long userAttemptId) {
        try {
            logger.info("Deleting user attempt with ID: {}", userAttemptId);
            
            if (userAttemptId == null) {
                logger.error("User attempt ID is null for deletion");
                throw new IllegalArgumentException("User attempt ID cannot be null");
            }
            
            if (!userAttemptsRepository.existsById(userAttemptId)) {
                logger.warn("User attempt not found with ID: {} for deletion", userAttemptId);
                throw new IllegalArgumentException("User attempt not found with ID: " + userAttemptId);
            }
            
            userAttemptsRepository.deleteById(userAttemptId);
            logger.info("Successfully deleted user attempt with ID: {}", userAttemptId);
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid argument for deleteUserAttempt: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error occurred while deleting user attempt with ID {}: {}", userAttemptId, e.getMessage(), e);
            throw new RuntimeException("Failed to delete user attempt", e);
        }
    }
}