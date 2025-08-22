package com.mindfultalk.growthlab.controller;

import com.mindfultalk.growthlab.dto.*;
import com.mindfultalk.growthlab.model.*;
import com.mindfultalk.growthlab.service.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZonedDateTime;

@RestController
@RequestMapping("/api/v1/assignment-with-attempt")
public class AssignmentWithAttemptController {
    private static final Logger logger = LoggerFactory.getLogger(AssignmentWithAttemptController.class);
    
    @Autowired
    private UserAssignmentAttemptTransactionalService transactionCoordinator;

    /**
     * Combined endpoint for assignment submission with attempt creation.
     * This ensures that both operations succeed or fail together.
     */
    @PostMapping("/submit")
    public ResponseEntity<UserAssignmentMinimalDTO> submitAssignmentWithAttempt(
            @RequestParam("userId") String userId,
            @RequestParam("cohortId") String cohortId,
            @RequestParam("programId") String programId,
            @RequestParam("stageId") String stageId,
            @RequestParam("unitId") String unitId,
            @RequestParam("subconceptId") String subconceptId,
            @RequestParam("sessionId") String sessionId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("userAttemptStartTimestamp") String userAttemptStartTimestamp,
            @RequestParam("userAttemptEndTimestamp") String userAttemptEndTimestamp,
            @RequestParam("userAttemptScore") Integer userAttemptScore,
            @RequestParam("userAttemptFlag") Boolean userAttemptFlag) throws IOException {
        
        logger.info("Received assignment submission request with attempt for userId: {}", userId);
        
        try {
            // Convert the request parameters to UserAttemptRequestDTO
            UserAttemptRequestDTO attemptRequestDTO = new UserAttemptRequestDTO();
            attemptRequestDTO.setUserId(userId);
            attemptRequestDTO.setProgramId(programId);
            attemptRequestDTO.setStageId(stageId);
            attemptRequestDTO.setUnitId(unitId);
            attemptRequestDTO.setSubconceptId(subconceptId);
            attemptRequestDTO.setSessionId(sessionId);
            
            // Robust timestamp parsing
            attemptRequestDTO.setUserAttemptStartTimestamp(parseTimestamp(userAttemptStartTimestamp));
            attemptRequestDTO.setUserAttemptEndTimestamp(parseTimestamp(userAttemptEndTimestamp));
            
            attemptRequestDTO.setUserAttemptScore(userAttemptScore);
            attemptRequestDTO.setUserAttemptFlag(userAttemptFlag);
            
            UserAssignment result = transactionCoordinator.processAssignmentWithAttempt(
                userId, cohortId, programId, stageId, unitId, subconceptId, sessionId, file, attemptRequestDTO);
            
            UserAssignmentMinimalDTO dto = transactionCoordinator.toMinimalDTO(result);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            logger.error("Failed to process assignment with attempt: {}", e.getMessage(), e);
            throw e; // Let the global exception handler take care of it
        }
    }
    
    /**
     * Robust timestamp parsing method to handle multiple input formats
     * 
     * @param timestampStr Input timestamp string
     * @return Parsed LocalDateTime
     */
    private LocalDateTime parseTimestamp(String timestampStr) {
        try {
            // Try parsing as ZonedDateTime first (handles ISO 8601 formats with timezone)
            return ZonedDateTime.parse(timestampStr).toLocalDateTime();
        } catch (Exception e) {
            try {
                // Try parsing with custom formatter that allows multiple formats
                return LocalDateTime.parse(timestampStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (Exception ex) {
                // Fallback to more flexible parsing
                logger.warn("Fallback timestamp parsing for: {}", timestampStr);
                return LocalDateTime.parse(timestampStr.substring(0, 19), 
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
            }
        }
    }
}