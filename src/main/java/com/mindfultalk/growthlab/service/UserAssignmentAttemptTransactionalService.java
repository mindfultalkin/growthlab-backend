package com.mindfultalk.growthlab.service;

import com.mindfultalk.growthlab.model.*;
import com.mindfultalk.growthlab.dto.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.ZoneOffset;
import java.util.*;

@Service
public class UserAssignmentAttemptTransactionalService {
    private static final Logger logger = LoggerFactory.getLogger(UserAssignmentAttemptTransactionalService.class);
    @Autowired
    private UserAssignmentService userAssignmentService;

    @Autowired
    private UserAttemptsService userAttemptsService;

    @Autowired
    private UserService userService;

    @Autowired
    private UnitService unitService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private StageService stageService;

    @Autowired
    private UserSessionMappingService userSessionMappingService;

    @Autowired
    private SubconceptService subconceptService;

    @Autowired
    private UserSubConceptService userSubConceptService;

    @Autowired
    private UserCohortMappingService userCohortMappingService;

    /**
     * Transactional method to handle both assignment submission and user attempt creation.
     * Either both operations succeed or both fail.
     *
     * @param userId User ID
     * @param cohortId Cohort ID
     * @param programId Program ID
     * @param stageId Stage ID
     * @param unitId Unit ID
     * @param subconceptId Subconcept ID
     * @param sessionId Session ID
     * @param file Assignment file
     * @param attemptRequestDTO User attempt request data
     * @return The created user assignment
     * @throws IOException If file handling fails
     */
    @Transactional
    public UserAssignment processAssignmentWithAttempt(
            String userId,
            String cohortId,
            String programId, 
            String stageId,
            String unitId,
            String subconceptId,
            String sessionId,
            MultipartFile file,
            UserAttemptRequestDTO attemptRequestDTO) throws IOException {
        
        logger.info("Starting transaction to process assignment with attempt for userId: {}, cohortId: {}", userId, cohortId);
        
        try {
            // First, create the assignment submission
            UserAssignment assignment = userAssignmentService.submitNewAssignment(
                userId, cohortId, programId, stageId, unitId, subconceptId, file);
            logger.info("Assignment submission successful for userId: {}, assignmentId: {}", 
                userId, assignment.getUuid());
            
            // After successful assignment creation, create user attempt
            UserAttempts userAttempt = prepareUserAttempt(attemptRequestDTO);
            UserAttempts savedAttempt = createUserAttemptWithUpdates(userAttempt, cohortId);
            logger.info("User attempt created successfully for userId: {}, attemptId: {}", 
                userId, savedAttempt.getUserAttemptId());
            
            return assignment;
        } catch (Exception e) {
            logger.error("Transaction failed for assignment and attempt processing: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process assignment submission with attempt: " + e.getMessage());
        }
    }

    /**
     * Prepares the UserAttempts entity from the request DTO
     */
    private UserAttempts prepareUserAttempt(UserAttemptRequestDTO requestDTO) {
        // Fetch related entities based on IDs
        User user = userService.findByUserId(requestDTO.getUserId())
            .orElseThrow(() -> new RuntimeException("User not found"));
        Unit unit = unitService.findByUnitId(requestDTO.getUnitId())
            .orElseThrow(() -> new RuntimeException("Unit not found"));
        Program program = programService.findByProgramId(requestDTO.getProgramId())
            .orElseThrow(() -> new RuntimeException("Program not found"));
        Stage stage = stageService.findByStageId(requestDTO.getStageId())
            .orElseThrow(() -> new RuntimeException("Stage not found"));
        UserSessionMapping session = userSessionMappingService.findBySessionId(requestDTO.getSessionId())
            .orElseThrow(() -> new RuntimeException("Session not found"));
        Subconcept subconcept = subconceptService.findBySubconceptId(requestDTO.getSubconceptId())
            .orElseThrow(() -> new RuntimeException("Subconcept not found"));

        // Create UserAttempts entity
        UserAttempts userAttempt = new UserAttempts();
        userAttempt.setUserAttemptEndTimestamp(requestDTO.getUserAttemptEndTimestamp().atOffset(ZoneOffset.UTC));
        userAttempt.setUserAttemptFlag(requestDTO.isUserAttemptFlag());
        userAttempt.setUserAttemptScore(requestDTO.getUserAttemptScore());
        userAttempt.setUserAttemptStartTimestamp(requestDTO.getUserAttemptStartTimestamp().atOffset(ZoneOffset.UTC));
        userAttempt.setUser(user);
        userAttempt.setUnit(unit);
        userAttempt.setProgram(program);
        userAttempt.setStage(stage);
        userAttempt.setSession(session);
        userAttempt.setSubconcept(subconcept);
        
        return userAttempt;
    }

    /**
     * Creates a user attempt and performs all related updates in a single transaction
     */
    @Transactional
    private UserAttempts createUserAttemptWithUpdates(UserAttempts userAttempt, String cohortId) {
        try {
            // Save the user attempt first
            UserAttempts savedAttempt = userAttemptsService.saveUserAttempt(userAttempt);
            logger.info("User attempt saved successfully for userId: {}, attemptId: {}",
                    userAttempt.getUser().getUserId(), savedAttempt.getUserAttemptId());
            
            // Update leaderboard after saving attempt
            updateLeaderboard(savedAttempt, cohortId);
            
            // Update or create entry in UserSubConcept table
            updateUserSubConceptCompletionStatus(savedAttempt);
         
            return savedAttempt;
        } catch (Exception e) {
            logger.error("Error while creating user attempt for userId: {}, cohortId: {}, Error: {}",
                    userAttempt.getUser().getUserId(), cohortId, e.getMessage(), e);
            throw new RuntimeException("Failed to create user attempt: " + e.getMessage());
        }
    }
    
    /**
     * Updates the user's subconcept completion status
     */
    private void updateUserSubConceptCompletionStatus(UserAttempts userAttempt) { 
        try {
            // Retrieve the details from the user attempt 
            String userId = userAttempt.getUser().getUserId(); 
            String programId = userAttempt.getProgram().getProgramId();
            String stageId = userAttempt.getStage().getStageId();
            String unitId = userAttempt.getUnit().getUnitId();
            String subconceptId = userAttempt.getSubconcept().getSubconceptId();
            
            // Check if a UserSubConcept entry already exists for the unique constraint fields
            Optional<UserSubConcept> existingEntry = userSubConceptService
                    .findByUser_UserIdAndProgram_ProgramIdAndStage_StageIdAndUnit_UnitIdAndSubconcept_SubconceptId(
                        userId, programId, stageId, unitId, subconceptId);

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
                userSubConcept.setCompletionStatus(true); 
                userSubConceptService.updateUserSubConcept(userSubConcept);
                logger.info("Updated completion status for UserSubConcept, userId: {}, subconceptId: {}", userId, subconceptId);
            }
        } catch (Exception e) {
            logger.error("Error updating UserSubConcept for userId: {}, Error: {}", 
                userAttempt.getUser().getUserId(), e.getMessage(), e);
            throw new RuntimeException("Failed to update UserSubConcept: " + e.getMessage());
        }
    }

    /**
     * Updates the leaderboard for the user's cohort
     */
    private void updateLeaderboard(UserAttempts userAttempt, String cohortId) {
        try {
            User user = userAttempt.getUser();
            int score = userAttempt.getUserAttemptScore();

            // Retrieve the user's specific cohort mapping for the cohort tied to this attempt
            Optional<UserCohortMapping> userCohortMappingOpt = 
                userCohortMappingService.findByUser_UserIdAndCohort_CohortId(user.getUserId(), cohortId);

            if (userCohortMappingOpt.isPresent()) {
                // Update existing leaderboard score for the specified cohort
                UserCohortMapping userCohortMapping = userCohortMappingOpt.get();
                int updatedScore = userCohortMapping.getLeaderboardScore() + score;
                userCohortMapping.setLeaderboardScore(updatedScore);
                
                // Save the updated UserCohortMapping
                userCohortMappingService.updateUserCohortMapping(userCohortMapping.getUserCohortId(), userCohortMapping);
                logger.info("Updated leaderboard for userId: {}, cohortId: {}, newScore: {}", 
                    user.getUserId(), cohortId, updatedScore);
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
                logger.info("New leaderboard entry created for userId: {}, cohortId: {}, score: {}", 
                    user.getUserId(), cohortId, score);
            }
        } catch (Exception e) {
            logger.error("Error updating leaderboard for userId: {}, cohortId: {}, Error: {}", 
                userAttempt.getUser().getUserId(), cohortId, e.getMessage(), e);
            throw new RuntimeException("Failed to update leaderboard: " + e.getMessage());
        }
    }
    
    public UserAssignmentMinimalDTO toMinimalDTO(UserAssignment assignment) {
        UserAssignmentMinimalDTO dto = new UserAssignmentMinimalDTO();

        dto.setAssignmentId(assignment.getAssignmentId());

        // User
        dto.setUserId(assignment.getUser().getUserId());
        dto.setUserName(assignment.getUser().getUserName());

        // Cohort
        dto.setCohortId(assignment.getCohort().getCohortId());
        dto.setCohortName(assignment.getCohort().getCohortName());

        // Program
        dto.setProgramId(assignment.getProgram().getProgramId());
        dto.setProgramName(assignment.getProgram().getProgramName());

        // Stage
        dto.setStageId(assignment.getStage().getStageId());
        dto.setStageName(assignment.getStage().getStageName());

        // Unit
        dto.setUnitId(assignment.getUnit().getUnitId());
        dto.setUnitName(assignment.getUnit().getUnitName());

        // Subconcept
        dto.setSubconceptId(assignment.getSubconcept().getSubconceptId());
        dto.setSubconceptDesc(assignment.getSubconcept().getSubconceptDesc());
        dto.setSubconceptLink(assignment.getSubconcept().getSubconceptLink());

        // File (submitted)
        if (assignment.getSubmittedFile() != null) {
            dto.setFileName(assignment.getSubmittedFile().getFileName());
            dto.setFilePath(assignment.getSubmittedFile().getFilePath());
            dto.setFileSize(assignment.getSubmittedFile().getFileSize());
        }

        // Submission Info
        dto.setSubmittedDate(assignment.getSubmittedDate());
        dto.setScore(assignment.getScore());
        dto.setRemarks(assignment.getRemarks());

        return dto;
    }

}