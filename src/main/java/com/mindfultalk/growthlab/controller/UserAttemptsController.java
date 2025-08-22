package com.mindfultalk.growthlab.controller;

import com.mindfultalk.growthlab.dto.*;
import com.mindfultalk.growthlab.model.*;
import com.mindfultalk.growthlab.service.*;
import java.time.ZoneOffset;

import org.slf4j.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/user-attempts")
public class UserAttemptsController {
	
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
    
    private static final Logger logger = LoggerFactory.getLogger(UserAttemptsController.class);
    
    @PostMapping
    public ResponseEntity<?> createUserAttempt(@RequestBody UserAttemptRequestDTO requestDTO) {
    	try {
        // Fetch related entities based on IDs
        Optional<User> userOpt = userService.findByUserId(requestDTO.getUserId());
        Optional<Unit> unitOpt = unitService.findByUnitId(requestDTO.getUnitId());
        Optional<Program> programOpt = programService.findByProgramId(requestDTO.getProgramId());
        Optional<Stage> stageOpt = stageService.findByStageId(requestDTO.getStageId());
        Optional<UserSessionMapping> sessionOpt = userSessionMappingService.findBySessionId(requestDTO.getSessionId());
        Optional<Subconcept> subconceptOpt = subconceptService.findBySubconceptId(requestDTO.getSubconceptId());

        // Validate all entities are present
        if (!userOpt.isPresent() || !unitOpt.isPresent() || !programOpt.isPresent() ||
                !stageOpt.isPresent() || !sessionOpt.isPresent() || !subconceptOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid data provided. Please check all IDs.");
        }

        // Create UserAttempts entity
        UserAttempts userAttempt = new UserAttempts();
        userAttempt.setUserAttemptEndTimestamp(requestDTO.getUserAttemptEndTimestamp().atOffset(ZoneOffset.UTC));
        userAttempt.setUserAttemptFlag(requestDTO.isUserAttemptFlag());
        userAttempt.setUserAttemptScore(requestDTO.getUserAttemptScore());
        userAttempt.setUserAttemptStartTimestamp(requestDTO.getUserAttemptStartTimestamp().atOffset(ZoneOffset.UTC));
        userAttempt.setUser(userOpt.get());
        userAttempt.setUnit(unitOpt.get());
        userAttempt.setProgram(programOpt.get());
        userAttempt.setStage(stageOpt.get());
        userAttempt.setSession(sessionOpt.get());
        userAttempt.setSubconcept(subconceptOpt.get());
        // UUID will be generated automatically via @PrePersist

     // Create the user attempt
        UserAttempts createdAttempt = userAttemptsService.createUserAttempt(userAttempt, requestDTO.getCohortId());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdAttempt);
        
    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    } catch (Exception e) {
        logger.error("Error creating user attempt", e);
        return ResponseEntity.internalServerError()
                .body("Failed to create attempt: " + e.getMessage());
    }
}
    
    
    @GetMapping
    public List<UserAttempts> getAllUserAttempts() {
        return userAttemptsService.getAllUserAttempts();
    }

    
    @GetMapping("/{userAttemptId}")
    public ResponseEntity<UserAttempts> getUserAttemptById(@PathVariable Long userAttemptId) {
        Optional<UserAttempts> userAttempt = userAttemptsService.getUserAttemptById(userAttemptId);
        return userAttempt.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
    

    @PutMapping("/{userAttemptId}")
    public ResponseEntity<UserAttempts> updateUserAttempt(@PathVariable Long userAttemptId, @RequestBody UserAttempts userAttempt) {
        return ResponseEntity.ok(userAttemptsService.updateUserAttempt(userAttemptId, userAttempt));
    }

    @DeleteMapping("/{userAttemptId}")
    public ResponseEntity<Void> deleteUserAttempt(@PathVariable Long userAttemptId) {
        userAttemptsService.deleteUserAttempt(userAttemptId);
        return ResponseEntity.noContent().build();
    }
}