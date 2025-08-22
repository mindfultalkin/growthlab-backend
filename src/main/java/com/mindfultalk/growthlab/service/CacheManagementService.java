package com.mindfultalk.growthlab.service;

import java.util.*;

import org.slf4j.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.*;
import com.mindfultalk.growthlab.repository.*;
import com.mindfultalk.growthlab.model.*;


@Service
@Component
public class CacheManagementService {
    
    private static final Logger logger = LoggerFactory.getLogger(CacheManagementService.class);
    
    @Autowired
    private CacheManager cacheManager;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private ProgramConceptsMappingRepository programConceptsMappingRepository;
    
    @Autowired
    private StageRepository stageRepository;
    
    @Autowired
    private UnitRepository unitRepository;
    
    @Autowired
    private UserCohortMappingRepository userCohortMappingRepository;
    
    /**
     * Evict all user-related caches when subconcept completion changes
     */
    public void evictUserCompletionCaches(String userId, String programId) {
        try {
            logger.info("Evicting completion caches for userId: {}, programId: {}", userId, programId);
            
         // Get all units and stages for this program to evict specific caches
            List<String> unitIds = getUnitIdsForProgram(programId);
            List<String> stageIds = getStageIdsForProgram(programId);
            
            // Evict program-level cache
            evictFromCache("programStagesUnits", userId + "_" + programId);
            
            // Evict all unit-level caches for this user in this program
            for (String unitId : unitIds) {
                evictFromCache("programConceptsByUnit", userId + "_" + unitId);
            }
            
            // IMPORTANT: Evict all report-related caches
            evictReportCaches(userId, programId, stageIds, unitIds);
            
            // IMPORTANT: Evict user progress caches
            evictUserProgressCaches(userId, programId);
            
            // Also evict session filter caches if needed
            evictSessionFilterCaches(userId);
            
            logger.info("Successfully evicted all completion caches for userId: {}", userId);
            
        } catch (Exception e) {
            logger.error("Error evicting user completion caches for userId: {}, programId: {}", 
                        userId, programId, e);
        }
    }
    
    /**
     * Evict all report-related caches for a user
     */
    public void evictReportCaches(String userId, String programId, List<String> stageIds, List<String> unitIds) {
        try {
            logger.info("Evicting report caches for userId: {}, programId: {}", userId, programId);
            
            // Evict program report cache
            evictFromCache("programReports", userId + "_" + programId);
            logger.debug("Evicted programReports cache for key: {}", userId + "_" + programId);
            
            // Evict stage report caches
            for (String stageId : stageIds) {
                evictFromCache("stageReports", userId + "_" + stageId);
                logger.debug("Evicted stageReports cache for key: {}", userId + "_" + stageId);
            }
            
            // Evict unit report caches
            for (String unitId : unitIds) {
                evictFromCache("unitReports", userId + "_" + unitId);
                logger.debug("Evicted unitReports cache for key: {}", userId + "_" + unitId);
            }
            
            // Evict user attempts cache - get all subconcepts for this program
            List<String> subconceptIds = getSubconceptIdsForProgram(programId);
            for (String subconceptId : subconceptIds) {
                evictFromCache("userAttempts", userId + "_" + subconceptId);
                logger.debug("Evicted userAttempts cache for key: {}", userId + "_" + subconceptId);
            }
            
            logger.info("Successfully evicted all report caches for userId: {}, programId: {}", userId, programId);
            
        } catch (Exception e) {
            logger.error("Error evicting report caches for userId: {}, programId: {}", userId, programId, e);
        }
    }
    
    /**
     * Evict user progress caches for a user
     */
    public void evictUserProgressCaches(String userId, String programId) {
        try {
            logger.info("Evicting user progress caches for userId: {}, programId: {}", userId, programId);
            
            // Evict the specific user progress cache for this program
            evictFromCache("userProgress", programId + "_" + userId);
            logger.debug("Evicted userProgress cache for key: {}", programId + "_" + userId);
            
            // Evict cohort progress caches - need to find which cohorts this user belongs to
            evictCohortProgressCachesForUser(userId, programId);
            
            logger.info("Successfully evicted user progress caches for userId: {}, programId: {}", userId, programId);
            
        } catch (Exception e) {
            logger.error("Error evicting user progress caches for userId: {}, programId: {}", userId, programId, e);
        }
    }
    
    /**
     * Evict cohort progress caches that include this user
     */
    private void evictCohortProgressCachesForUser(String userId, String programId) {
        try {
            // Find the cohort this user belongs to for this program
            Optional<UserCohortMapping> userCohortMapping = 
                userCohortMappingRepository.findByUserUserIdAndProgramId(userId, programId);
            
            if (userCohortMapping.isPresent()) {
                String cohortId = userCohortMapping.get().getCohort().getCohortId();
                evictFromCache("cohortProgress", programId + "_" + cohortId);
                logger.debug("Evicted cohortProgress cache for key: {}", programId + "_" + cohortId);
            }
            
        } catch (Exception e) {
            logger.warn("Error evicting cohort progress caches for userId: {}, programId: {}", userId, programId, e);
        }
    }
    
    /**
     * Evict report caches for a specific unit completion
     */
    public void evictUnitReportCaches(String userId, String unitId, String stageId, String programId) {
        try {
            logger.info("Evicting unit-specific report caches for userId: {}, unitId: {}", userId, unitId);
            
            // Evict the specific unit report
            evictFromCache("unitReports", userId + "_" + unitId);
            
            // Evict the parent stage report (since unit completion affects stage stats)
            evictFromCache("stageReports", userId + "_" + stageId);
            
            // Evict the program report (since unit completion affects program stats)
            evictFromCache("programReports", userId + "_" + programId);
            
            // Evict subconcept-specific attempts cache for this unit
            List<String> subconceptIds = getSubconceptIdsForUnit(unitId);
            for (String subconceptId : subconceptIds) {
                evictFromCache("userAttempts", userId + "_" + subconceptId);
            }
            
            // IMPORTANT: Also evict user progress caches since unit completion affects progress
            evictUserProgressCaches(userId, programId);
            
            logger.info("Successfully evicted unit-specific report caches for userId: {}, unitId: {}", userId, unitId);
            
        } catch (Exception e) {
            logger.error("Error evicting unit report caches for userId: {}, unitId: {}", userId, unitId, e);
        }
    }
    
    /**
     * Evict caches when a user attempts a subconcept (called from UserAttempts service)
     */
    public void evictCachesOnUserAttempt(String userId, String subconceptId, String unitId, String stageId, String programId) {
        try {
            logger.info("Evicting caches on user attempt for userId: {}, subconceptId: {}, programId: {}", 
                       userId, subconceptId, programId);
            
            // Evict user attempts cache for this specific subconcept
            evictFromCache("userAttempts", userId + "_" + subconceptId);
            
            // Evict report caches that might be affected
            evictFromCache("unitReports", userId + "_" + unitId);
            evictFromCache("stageReports", userId + "_" + stageId);
            evictFromCache("programReports", userId + "_" + programId);
            
            // IMPORTANT: Evict user progress caches since attempts affect progress calculations
            evictUserProgressCaches(userId, programId);
            
            logger.info("Successfully evicted caches on user attempt for userId: {}, subconceptId: {}", 
                       userId, subconceptId);
            
        } catch (Exception e) {
            logger.error("Error evicting caches on user attempt for userId: {}, subconceptId: {}", 
                        userId, subconceptId, e);
        }
    }
    
    /**
     * Get all unit IDs for a given program
     */
    private List<String> getUnitIdsForProgram(String programId) {
        try {
            return programConceptsMappingRepository.findDistinctUnitIdsByProgramId(programId);
        } catch (Exception e) {
            logger.error("Error fetching unit IDs for programId: {}", programId, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Get all stage IDs for a given program
     */
    private List<String> getStageIdsForProgram(String programId) {
        try {
            return stageRepository.findStageIdsByProgramId(programId);
        } catch (Exception e) {
            logger.error("Error fetching stage IDs for programId: {}", programId, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Get all subconcept IDs for a given program
     */
    private List<String> getSubconceptIdsForProgram(String programId) {
        try {
            return programConceptsMappingRepository.findDistinctSubconceptIdsByProgramId(programId);
        } catch (Exception e) {
            logger.error("Error fetching subconcept IDs for programId: {}", programId, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Get all subconcept IDs for a given unit
     */
    private List<String> getSubconceptIdsForUnit(String unitId) {
        try {
            return programConceptsMappingRepository.findSubconceptIdsByUnitId(unitId);
        } catch (Exception e) {
            logger.error("Error fetching subconcept IDs for unitId: {}", unitId, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Evict specific cache entry
     */
    private void evictFromCache(String cacheName, String key) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.evict(key);
                logger.debug("Evicted cache entry: {} with key: {}", cacheName, key);
            }
        } catch (Exception e) {
            logger.warn("Error evicting cache entry: {} with key: {}", cacheName, key, e);
        }
    }
    
    /**
     * Evict session filter related caches
     */
    private void evictSessionFilterCaches(String userId) {
        try {
            // Pattern matching for session filter caches
            Set<String> keysToDelete = new HashSet<>();
            keysToDelete.add("userData:" + userId);
            keysToDelete.add("userActive:" + userId);
            
            // You might need to add more keys based on your SessionValidationFilter cache keys
            
            redisTemplate.delete(keysToDelete);
            logger.debug("Evicted session filter caches for userId: {}", userId);
        } catch (Exception e) {
            logger.warn("Error evicting session filter caches for userId: {}", userId, e);
        }
    }
    
    /**
     * Evict all caches for a user (use sparingly)
     */
    public void evictAllUserCaches(String userId) {
        try {
            logger.info("Evicting ALL caches for userId: {}", userId);
            
            // Get all cache names and evict user-related entries
            Collection<String> cacheNames = cacheManager.getCacheNames();
            
            for (String cacheName : cacheNames) {
                Cache cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    // This is Redis-specific - you might need to adapt based on your cache implementation
                    try {
                        // Pattern-based deletion for Redis - fix the type declaration
                        Set<String> keys = redisTemplate.keys(cacheName + "::" + userId + "*");
                        if (keys != null && !keys.isEmpty()) {
                            redisTemplate.delete(keys);
                            logger.debug("Evicted {} keys from cache: {}", keys.size(), cacheName);
                        }
                    } catch (Exception e) {
                        logger.debug("Could not pattern delete from cache: {} (this might be normal)", cacheName);
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("Error evicting all user caches for userId: {}", userId, e);
        }
    }
    
    /**
     * Manually clear user progress cache for specific user and program
     */
    public void clearUserProgressCache(String programId, String userId) {
        logger.info("Manually clearing user progress cache for programId: {} and userId: {}", programId, userId);
        evictFromCache("userProgress", programId + "_" + userId);
    }
    
    /**
     * Manually clear cohort progress cache for specific program and cohort
     */
    public void clearCohortProgressCache(String programId, String cohortId) {
        logger.info("Manually clearing cohort progress cache for programId: {} and cohortId: {}", programId, cohortId);
        evictFromCache("cohortProgress", programId + "_" + cohortId);
    }
}