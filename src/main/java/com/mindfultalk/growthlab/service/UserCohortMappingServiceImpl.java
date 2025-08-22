package com.mindfultalk.growthlab.service;

import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.mindfultalk.growthlab.dto.*;
import com.mindfultalk.growthlab.model.*;
import com.mindfultalk.growthlab.repository.*;
import com.opencsv.CSVReader;

@Service
public class UserCohortMappingServiceImpl implements UserCohortMappingService {

    @Autowired
    private UserCohortMappingRepository userCohortMappingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CohortRepository cohortRepository;
    
    @Autowired
    private CohortProgramRepository cohortProgramRepository;

    @Autowired
    private EmailService emailService;
    
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(UserCohortMappingServiceImpl.class);

    @Override
    @CachePut(value = "userCohortMappings", key = "#userId + ':' + #cohortId")
    @CacheEvict(value = {"cohortLeaderboards", "userMappings"}, key = "#cohortId")
    public UserCohortMappingDTO updateLeaderboardScore(String userId, String cohortId, Integer scoreToAdd) {
        logger.info("Updating leaderboard score for userId: {}, cohortId: {}, scoreToAdd: {}", userId, cohortId,
                scoreToAdd);

        if (userId == null || userId.isEmpty()) {
            logger.error("User ID is empty");
            throw new IllegalArgumentException("User ID cannot be empty");
        }

        if (cohortId == null || cohortId.isEmpty()) {
            logger.error("Cohort ID is empty");
            throw new IllegalArgumentException("Cohort ID cannot be empty");
        }

        if (scoreToAdd == null) {
            logger.error("Score to add is null");
            throw new IllegalArgumentException("Score cannot be null");
        }

        // Find the user-cohort mapping (this will be cached)
        Optional<UserCohortMapping> mappingOpt = findByUser_UserIdAndCohort_CohortId(userId, cohortId);

        if (!mappingOpt.isPresent()) {
            logger.error("No mapping found for userId: {} and cohortId: {}", userId, cohortId);
            throw new IllegalArgumentException("No mapping found for the specified user and cohort");
        }

        UserCohortMapping mapping = mappingOpt.get();

        // Check if cohort has leaderboard enabled (cached cohort lookup)
        Cohort cohort = getCachedCohort(cohortId);
        if (!cohort.isShowLeaderboard()) {
            logger.warn("Leaderboard is disabled for cohort: {}", cohortId);
            throw new IllegalArgumentException("Leaderboard is disabled for this cohort");
        }

        // Get current score and add the new score
        int currentScore = mapping.getLeaderboardScore();
        int newScore = currentScore + scoreToAdd;

        logger.info("Updating score from {} to {} for user: {}", currentScore, newScore, userId);

        // Update the score
        mapping.setLeaderboardScore(newScore);
        UserCohortMapping updatedMapping = userCohortMappingRepository.save(mapping);

        logger.info("Successfully updated leaderboard score for userId: {}", userId);

        // Convert to DTO and return
        return convertToDTO(updatedMapping);
    }

    @Override
    @CachePut(value = "userCohortMappings", key = "#userId + ':' + #cohortId")
    @CacheEvict(value = {"cohortMappings", "userMappings"}, allEntries = true)
    public UserCohortMapping createUserCohortMapping(String userId, String cohortId) {
        logger.info("Starting createUserCohortMapping for userId: {}, cohortId: {}", userId, cohortId);

        // Fetch user and cohort details (these will be cached)
        User user = getCachedUser(userId);
        if (user == null) {
            logger.error("User not found with ID: {}", userId);
            throw new IllegalArgumentException("User not found. Please check the user ID and try again.");
        }

        Cohort cohort = getCachedCohort(cohortId);
        if (cohort == null) {
            logger.error("Cohort not found with ID: {}", cohortId);
            throw new IllegalArgumentException("Cohort not found. Please check the cohort ID and try again.");
        }

        logger.info("Found user: {}, email: {}", user.getUserName(), user.getUserEmail());

        // Organization validation
        if (!user.getOrganization().getOrganizationId().equals(cohort.getOrganization().getOrganizationId())) {
            logger.error("User and Cohort belong to different organizations. UserOrg: {}, CohortOrg: {}",
                    user.getOrganization().getOrganizationId(), cohort.getOrganization().getOrganizationId());
            throw new IllegalArgumentException("User and Cohort must belong to the same organization.");
        }

        // Check for existing mapping
        if (userCohortMappingRepository.existsByUser_UserIdAndCohort_CohortId(userId, cohortId)) {
            logger.warn("User with ID {} is already mapped to Cohort with ID {}", userId, cohortId);
            throw new IllegalArgumentException("This user is already mapped to the selected cohort.");
        }

        // Send email notification if email exists
        if (user.getUserEmail() != null && !user.getUserEmail().isEmpty()) {
            logger.info("User has a valid email: {}", user.getUserEmail());

            try {
                Optional<CohortProgram> cohortProgramOpt = getCachedCohortProgram(cohortId);

                if (cohortProgramOpt.isPresent()) {
                    CohortProgram cohortProgram = cohortProgramOpt.get();
                    logger.info("Found cohort program mapping. Program name: {}",
                            cohortProgram.getProgram().getProgramName());

                    // Send email
                    try {
                        emailService.sendCohortAssignmentEmail(
                                user.getUserEmail(),
                                user.getUserName(),
                                cohort.getCohortName(),
                                cohortProgram.getProgram().getProgramName(),
                                user.getOrganization().getOrganizationName());
                        logger.info("Successfully sent cohort assignment email to {}", user.getUserEmail());
                    } catch (Exception e) {
                        logger.error("Failed to send email to {}. Error: {}", user.getUserEmail(), e.getMessage(), e);
                    }
                } else {
                    logger.warn("No cohort program mapping found for cohortId: {}", cohortId);
                }
            } catch (Exception e) {
                logger.error("Error while processing cohort program mapping for cohortId: {}. Error: {}", cohortId,
                        e.getMessage(), e);
            }
        } else {
            logger.warn("User with ID {} has no email address. Skipping email notification.", userId);
        }

        // Create user-cohort mapping
        UserCohortMapping mapping = new UserCohortMapping();
        mapping.setUser(user);
        mapping.setCohort(cohort);
        mapping.setUuid(UUID.randomUUID());
        mapping.setLeaderboardScore(0);

        UserCohortMapping savedMapping = userCohortMappingRepository.save(mapping);
        logger.info("Successfully created user-cohort mapping for userId: {}", userId);

        return savedMapping;
    }

    @Override
    @CachePut(value = "cohortMappings", key = "#cohortId")
    public UserCohortMapping updateUserCohortMappingByCohortId(String cohortId, UserCohortMapping userCohortMapping) {
        List<UserCohortMapping> existingMappings = userCohortMappingRepository.findAllByCohortCohortId(cohortId);
        if (existingMappings.isEmpty()) {
            throw new IllegalArgumentException("UserCohortMapping not found with ID: " + cohortId);
        }
        UserCohortMapping existingMapping = existingMappings.get(0);

        // Find the new cohort by its ID (cached)
        Cohort newCohort = getCachedCohort(cohortId);
        if (newCohort == null) {
            throw new IllegalArgumentException("Cohort with ID " + cohortId + " not found.");
        }

        // Check if the user and the new cohort belong to the same organization
        if (!existingMapping.getUser().getOrganization().getOrganizationId()
                .equals(newCohort.getOrganization().getOrganizationId())) {
            throw new IllegalArgumentException("User and new Cohort must belong to the same organization.");
        }

        // Update the cohort and save the mapping
        existingMapping.setCohort(newCohort);
        userCohortMappingRepository.save(existingMapping);

        System.out.println("User-Cohort mapping successfully updated for Cohort ID: " + cohortId);

        return existingMapping;
    }

    // Cache validation methods for CSV import
    @Cacheable(value = "validationCache", key = "'user:' + #userId")
    private boolean isUserExists(String userId) {
        return userRepository.existsById(userId);
    }

    @Cacheable(value = "validationCache", key = "'cohort:' + #cohortId")
    private boolean isCohortExists(String cohortId) {
        return cohortRepository.existsById(cohortId);
    }

    private String validateCsvData(String userId, String cohortId) {
        if (userId == null || userId.isEmpty()) {
            return "User ID is empty.";
        }
        if (cohortId == null || cohortId.isEmpty()) {
            return "Cohort ID is empty.";
        }
        if (!isUserExists(userId)) {
            return "User with ID " + userId + " not found.";
        }
        if (!isCohortExists(cohortId)) {
            return "Cohort with ID " + cohortId + " not found.";
        }
        if (userCohortMappingRepository.existsByUser_UserIdAndCohort_CohortId(userId, cohortId)) {
            return "User " + userId + " is already mapped to Cohort " + cohortId + ".";
        }
        return null; // No errors
    }

    @Override
    @CacheEvict(value = {"userCohortMappings", "cohortMappings", "userMappings"}, allEntries = true)
    public void updateUserCohortMapping(int userCohortId, UserCohortMapping userCohortMapping) {
        // Assuming userCohortId is the primary key, simply save the updated entity
        userCohortMappingRepository.save(userCohortMapping);
    }

    @CacheEvict(value = {"userCohortMappings", "cohortMappings", "userMappings"}, allEntries = true)
    public Map<String, List<String>> importUserCohortMappingsWithResponse(MultipartFile file) {
        List<String> successMessages = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            String[] line;
            reader.readNext(); // Skip header row

            while ((line = reader.readNext()) != null) {
                String userId = line[0].trim();
                String cohortId = line[1].trim();

                String validationError = validateCsvData(userId, cohortId);
                if (validationError != null) {
                    errorMessages.add("Row [" + userId + ", " + cohortId + "]: " + validationError);
                    continue;
                }

                try {
                    createUserCohortMapping(userId, cohortId);
                    successMessages.add("Successfully mapped User " + userId + " to Cohort " + cohortId + ".");
                } catch (Exception e) {
                    errorMessages.add("Row [" + userId + ", " + cohortId + "]: Unexpected error - " + e.getMessage());
                }
            }
        } catch (Exception e) {
            errorMessages.add("Critical Error: " + e.getMessage());
        }

        Map<String, List<String>> response = new HashMap<>();
        response.put("success", successMessages);
        response.put("errors", errorMessages);
        return response;
    }

    @Override
    @Caching(
        put = @CachePut(value = "userCohortMappings", key = "#userId + ':' + #cohortId"),
        evict = {
            @CacheEvict(value = "cohortMappings", key = "#cohortId"),
            @CacheEvict(value = "userMappings", key = "#userId")
        }
    )
    public UserCohortMapping updateUserCohortMapping(String userId, String cohortId, UserCohortMapping userCohortMapping) {
        logger.info("Updating user-cohort mapping for userId: {}, cohortId: {}", userId, cohortId);
        
        // Find existing mapping
        Optional<UserCohortMapping> existingMappingOpt = 
                findByUser_UserIdAndCohort_CohortId(userId, cohortId);
        
        if (existingMappingOpt.isEmpty()) {
            logger.error("User-cohort mapping not found for userId: {}", userId);
            throw new RuntimeException("User-cohort mapping not found for userId: " + userId);
        }
        
        UserCohortMapping existingMapping = existingMappingOpt.get();
        
        // Preserve immutable fields
        if (userCohortMapping.getCreatedAt() != null && 
                !userCohortMapping.getCreatedAt().equals(existingMapping.getCreatedAt())) {
            logger.warn("Attempt to modify createdAt timestamp was ignored for userId: {}", userId);
            // Ignore the attempt to modify createdAt
        }
        
        // Validate organization consistency
        if (userCohortMapping.getCohort() != null && 
                !userCohortMapping.getUser().getOrganization().getOrganizationId()
                .equals(userCohortMapping.getCohort().getOrganization().getOrganizationId())) {
            logger.error("User and Cohort must belong to the same organization. User Org: {}, Cohort Org: {}", 
                    userCohortMapping.getUser().getOrganization().getOrganizationId(),
                    userCohortMapping.getCohort().getOrganization().getOrganizationId());
            throw new IllegalArgumentException("User and Cohort must belong to the same organization.");
        }
        
        // Handle status changes
        if (userCohortMapping.getStatus() != null) {
            String newStatus = userCohortMapping.getStatus().toUpperCase();
            
            // Validate status is one of the allowed values
            if (!newStatus.equals("ACTIVE") && !newStatus.equals("DISABLED")) {
                logger.error("Invalid status value: {}. Only ACTIVE or DISABLED are allowed.", newStatus);
                throw new IllegalArgumentException("Invalid status value. Only ACTIVE or DISABLED are allowed.");
            }
            
            // Handle status transition to DISABLED
            if (newStatus.equals("DISABLED") && existingMapping.isActive()) {
                logger.info("Deactivating user: {} in cohort: {}", 
                        existingMapping.getUser().getUserName(),
                        existingMapping.getCohort().getCohortName());
                
                if (userCohortMapping.getDeactivatedReason() == null || userCohortMapping.getDeactivatedReason().trim().isEmpty()) {
                    logger.error("Deactivation reason is required when disabling a user");
                    throw new IllegalArgumentException("Deactivation reason is required when disabling a user.");
                }
                
                existingMapping.disable(userCohortMapping.getDeactivatedReason());
            }
            
            // Handle status transition to ACTIVE
            else if (newStatus.equals("ACTIVE") && !existingMapping.isActive()) {
                logger.info("Reactivating user: {} in cohort: {}", 
                        existingMapping.getUser().getUserName(),
                        existingMapping.getCohort().getCohortName());
                
                existingMapping.setStatus("ACTIVE");
                existingMapping.setDeactivatedAt(null);
                existingMapping.setDeactivatedReason(null);
            }
        }
        
        // Update mutable fields
        if (userCohortMapping.getCohort() != null) {
            existingMapping.setCohort(userCohortMapping.getCohort());
        }
        
        if (userCohortMapping.getLeaderboardScore() > 0) {
            existingMapping.setLeaderboardScore(userCohortMapping.getLeaderboardScore());
        }
        
        // Save and return updated mapping
        UserCohortMapping updatedMapping = userCohortMappingRepository.save(existingMapping);
        logger.info("Successfully updated user-cohort mapping for userId: {}, status: {}", 
                userId, updatedMapping.getStatus());
        
        return updatedMapping;
    }

    @Override
    @Cacheable(value = "userCohortMappings", key = "#userId + ':' + #cohortId")
    public Optional<UserCohortMapping> findByUser_UserIdAndCohort_CohortId(String userId, String cohortId) {
        return userCohortMappingRepository.findByUser_UserIdAndCohort_CohortId(userId, cohortId);
    }

    @Override
    @Cacheable(value = "allUserCohortMappings", key = "'all'")
    public List<UserCohortMappingDTO> getAllUserCohortMappings() {
        List<UserCohortMapping> mappings = userCohortMappingRepository.findAll();
        return mappings.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "cohortMappings", key = "#cohortId")
    public List<UserCohortMappingDTO> getUserCohortMappingsCohortId(String cohortId) {
        List<UserCohortMapping> mappings = userCohortMappingRepository.findAllByCohortCohortId(cohortId);
        return mappings.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "cohortLeaderboards", key = "#cohortId")
    public Map<String, Object> getUserCohortMappingsByCohortId(String cohortId) {
        Cohort cohort = getCachedCohort(cohortId);
        if (cohort == null) {
            throw new IllegalArgumentException("Cohort not found with ID: " + cohortId);
        }

        // Check the Show_leaderboard flag
        if (!cohort.isShowLeaderboard()) {
            // If the leaderboard is disabled, return the information with a "not available"
            // status
            return Map.of("leaderboardStatus", "not available", "message", "Leaderboard is disabled for this cohort.");
        }

        // If the leaderboard is enabled, fetch and return the data
        List<UserCohortMappingDTO> mappingDTOs = getUserCohortMappingsCohortId(cohortId);

        return Map.of("leaderboardStatus", "available", "leaderboardData", mappingDTOs);
    }

    @Override
    @Cacheable(value = "cohortLeaderboards", key = "#cohortId + ':withLeaderboard'")
    public Map<String, Object> getUserCohortMappingsWithLeaderboard(String cohortId) {
        Cohort cohort = getCachedCohort(cohortId);
        if (cohort == null) {
            throw new IllegalArgumentException("Cohort not found with ID: " + cohortId);
        }

        // Check the Show_leaderboard flag
        if (!cohort.isShowLeaderboard()) {
            // If the leaderboard is disabled, return the information with a "not available"
            // flag
            return Map.of("leaderboardStatus", "not available");
        }

        // Otherwise, return the leaderboard data
        List<UserCohortMappingDTO> mappingDTOs = getUserCohortMappingsCohortId(cohortId);

        return Map.of("leaderboardStatus", "available", "leaderboardData", mappingDTOs);
    }

    @Override
    @Cacheable(value = "userMappings", key = "#userId + ':single'")
    public UserCohortMapping findByUserUserId(String userId) {
        return userCohortMappingRepository.findByUserUserId(userId)
                .orElseThrow(() -> new RuntimeException("UserCohortMapping not found for userId: " + userId));
    }

    @Override
    @Cacheable(value = "userMappings", key = "#userId + ':optional'")
    public Optional<UserCohortMapping> getUserCohortMappingByUserId(String userId) {
        return userCohortMappingRepository.findByUserUserId(userId);
    }

    @Override
    @Cacheable(value = "userMappings", key = "#userId + ':' + #programId")
    public Optional<UserCohortMapping> findByUserUserIdAndProgramId(String userId, String programId) {
        return userCohortMappingRepository.findByUserUserIdAndProgramId(userId, programId);
    }

    @Override
    @Cacheable(value = "userMappings", key = "#userId + ':all'")
    public List<UserCohortMappingDTO> getUserCohortMappingsByUserId(String userId) {
        List<UserCohortMapping> mappings = userCohortMappingRepository.findAllByUserUserId(userId);
        return mappings.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    @CacheEvict(value = {"userCohortMappings", "cohortMappings", "userMappings"}, allEntries = true)
    public UserCohortMapping createUserCohortMapping(UserCohortMapping userCohortMapping) {
        return userCohortMappingRepository.save(userCohortMapping);
    }

    @Override
    @CacheEvict(value = {"userMappings", "userCohortMappings", "cohortMappings"}, key = "#userId")
    public void deleteUserCohortMappingByUserId(String userId) {
        userCohortMappingRepository.deleteByUserUserId(userId);
    }

    // Helper methods for cached entity lookups
    @Cacheable(value = "users", key = "#userId")
    private User getCachedUser(String userId) {
        return userRepository.findById(userId).orElse(null);
    }

    @Cacheable(value = "cohorts", key = "#cohortId")
    private Cohort getCachedCohort(String cohortId) {
        return cohortRepository.findById(cohortId).orElse(null);
    }

    @Cacheable(value = "cohortPrograms", key = "#cohortId")
    private Optional<CohortProgram> getCachedCohortProgram(String cohortId) {
        return cohortProgramRepository.findByCohortCohortId(cohortId);
    }

    private UserCohortMappingDTO convertToDTO(UserCohortMapping userCohortMapping) {
        UserCohortMappingDTO dto = new UserCohortMappingDTO();
        // dto.setOrganizationName(userCohortMapping.getCohort().getOrganization().getOrganizationName());
        dto.setCohortId(userCohortMapping.getCohort().getCohortId());
        dto.setUserId(userCohortMapping.getUser().getUserId());
        dto.setUserName(userCohortMapping.getUser().getUserName());
        dto.setUserType(userCohortMapping.getUser().getUserType());
        dto.setUserEmail(userCohortMapping.getUser().getUserEmail());
        dto.setCohortName(userCohortMapping.getCohort().getCohortName());
        dto.setLeaderboardScore(userCohortMapping.getLeaderboardScore());
        dto.setStatus(userCohortMapping.getStatus());

        return dto;
    }
}