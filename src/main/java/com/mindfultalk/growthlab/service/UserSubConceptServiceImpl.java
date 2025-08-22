package com.mindfultalk.growthlab.service;

import com.mindfultalk.growthlab.model.*;
import com.mindfultalk.growthlab.repository.*;
import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;

@Service
public class UserSubConceptServiceImpl implements UserSubConceptService {

    @Autowired
    private UserSubConceptRepository userSubConceptRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProgramRepository programRepository;

    @Autowired
    private StageRepository stageRepository;

    @Autowired
    private UnitRepository unitRepository;

    @Autowired
    private SubconceptRepository subconceptRepository;

    @Autowired
    private CacheManagementService cacheManagementService;
    
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(UserSubConceptServiceImpl.class);

    @Override
    @Cacheable(value = "userSubConcepts", key = "#userId + ':' + #programId + ':' + #stageId + ':' + #unitId + ':' + #subconceptId", unless = "#result == null")
    public Optional<UserSubConcept> findByUser_UserIdAndProgram_ProgramIdAndStage_StageIdAndUnit_UnitIdAndSubconcept_SubconceptId(
            String userId, String programId, String stageId, String unitId, String subconceptId) {
        logger.info("Finding UserSubConcept for userId: {}, programId: {}, stageId: {}, unitId: {}, subconceptId: {}", 
                userId, programId, stageId, unitId, subconceptId);
        return userSubConceptRepository
                .findByUser_UserIdAndProgram_ProgramIdAndStage_StageIdAndUnit_UnitIdAndSubconcept_SubconceptId(
                        userId, programId, stageId, unitId, subconceptId);
    }

    @Override
    @Transactional
    public UserSubConcept createUserSubConcept(UserSubConcept userSubConcept) {
        UserSubConcept saved = userSubConceptRepository.save(userSubConcept);
        
        // Evict caches when completion status changes
        String userId = userSubConcept.getUser().getUserId();
        String programId = userSubConcept.getProgram().getProgramId();
        cacheManagementService.evictUserCompletionCaches(userId, programId);
        
        return saved;
    }

    @Override
    @Transactional
    public UserSubConcept updateUserSubConcept(UserSubConcept userSubConcept) {
        UserSubConcept updated = userSubConceptRepository.save(userSubConcept);
        
        // Evict caches when completion status changes
        String userId = userSubConcept.getUser().getUserId();
        String programId = userSubConcept.getProgram().getProgramId();
        cacheManagementService.evictUserCompletionCaches(userId, programId);
        
        return updated;
    }

    @Override
    @Cacheable(value = "userSubConceptsById", key = "#userSubconceptId")
    public UserSubConcept getUserSubConceptById(Long userSubconceptId) {
        logger.info("Finding UserSubConcept by ID: {}", userSubconceptId);
        return userSubConceptRepository.findById(userSubconceptId).orElse(null);
    }

    @Override
    @Cacheable(value = "allUserSubConcepts", key = "'all'")
    public List<UserSubConcept> getAllUserSubConcepts() {
        logger.info("Retrieving all UserSubConcepts");
        return userSubConceptRepository.findAll();
    }

    @Override
    @Cacheable(value = "userSubConceptsByUser", key = "#userId")
    public List<UserSubConcept> getAllUserSubConceptsByUserId(String userId) {
        logger.info("Retrieving all UserSubConcepts for userId: {}", userId);
        return userSubConceptRepository.findAllByUser_UserId(userId);
    }

    @Override
    @Caching(
        put = @CachePut(value = "userSubConceptsById", key = "#userSubconceptId"),
        evict = {
            @CacheEvict(value = "userSubConceptsByUser", key = "#userSubConcept.user.userId"),
            @CacheEvict(value = "userSubConceptsByProgram", allEntries = true),
            @CacheEvict(value = "userSubConceptsByUnit", allEntries = true),
            @CacheEvict(value = "allUserSubConcepts", key = "'all'"),
            @CacheEvict(value = "userSubConcepts", allEntries = true),
            @CacheEvict(value = "completedSubconcepts", allEntries = true),
            @CacheEvict(value = "stageCompletionDates", allEntries = true)
        }
    )
    public UserSubConcept updateUserSubConcept(Long userSubconceptId, UserSubConcept userSubConcept) {
        logger.info("Updating UserSubConcept with ID: {} using provided data", userSubconceptId);
        
        return userSubConceptRepository.findById(userSubconceptId).map(existingSubConcept -> {
            
            // Update fields
            existingSubConcept.setUser(userSubConcept.getUser());
            existingSubConcept.setProgram(userSubConcept.getProgram());
            existingSubConcept.setStage(userSubConcept.getStage());
            existingSubConcept.setUnit(userSubConcept.getUnit());
            existingSubConcept.setSubconcept(userSubConcept.getSubconcept());
            existingSubConcept.setCompletionStatus(userSubConcept.getCompletionStatus());
            
            // Only update UUID if provided and different
            if (userSubConcept.getUuid() != null) {
                existingSubConcept.setUuid(userSubConcept.getUuid());
            }
            
            UserSubConcept updatedConcept = userSubConceptRepository.save(existingSubConcept);
            logger.info("Successfully updated UserSubConcept with ID: {}", userSubconceptId);
            
            return updatedConcept;
        }).orElseThrow(() -> {
            logger.error("UserSubConcept not found with ID: {}", userSubconceptId);
            return new RuntimeException("UserSubConcept not found with ID: " + userSubconceptId);
        });
    }

    @Override
    @Caching(evict = {
        @CacheEvict(value = "userSubConceptsById", key = "#userSubconceptId"),
        @CacheEvict(value = "userSubConceptsByUser", allEntries = true),
        @CacheEvict(value = "userSubConceptsByProgram", allEntries = true),
        @CacheEvict(value = "userSubConceptsByUnit", allEntries = true),
        @CacheEvict(value = "allUserSubConcepts", key = "'all'"),
        @CacheEvict(value = "userSubConcepts", allEntries = true),
        @CacheEvict(value = "completedSubconcepts", allEntries = true),
        @CacheEvict(value = "stageCompletionDates", allEntries = true)
    })
    public void deleteUserSubConcept(Long userSubconceptId) {
        logger.info("Deleting UserSubConcept with ID: {}", userSubconceptId);
        
        if (!userSubConceptRepository.existsById(userSubconceptId)) {
            logger.error("UserSubConcept not found with ID: {}", userSubconceptId);
            throw new RuntimeException("UserSubConcept not found with ID: " + userSubconceptId);
        }
        
        userSubConceptRepository.deleteById(userSubconceptId);
        logger.info("Successfully deleted UserSubConcept with ID: {}", userSubconceptId);
    }
}
