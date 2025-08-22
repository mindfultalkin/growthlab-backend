package com.mindfultalk.growthlab.service;

import com.mindfultalk.growthlab.dto.*;
import com.mindfultalk.growthlab.exception.CohortValidationException;
import com.mindfultalk.growthlab.model.*;
import com.mindfultalk.growthlab.repository.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CohortServiceImpl implements CohortService {

    @Autowired
    private CohortRepository cohortRepository;

    private static final Logger logger = LoggerFactory.getLogger(CohortServiceImpl.class);

    @Override
    @Cacheable(value = "cohorts", key = "'all'")
    public List<Cohort> getAllCohorts() {
        try {
            logger.info("Retrieving all cohorts from database");
            List<Cohort> cohorts = cohortRepository.findAll();
            logger.info("Successfully retrieved {} cohorts", cohorts.size());
            return cohorts;
        } catch (Exception e) {
            logger.error("Error occurred while retrieving all cohorts: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve cohorts", e);
        }
    }

    @Override
    @Cacheable(value = "cohorts", key = "#cohortId")
    public Optional<Cohort> getCohortById(String cohortId) {
        try {
            logger.info("Retrieving cohort with ID: {}", cohortId);
            
            if (cohortId == null || cohortId.trim().isEmpty()) {
                logger.warn("Cohort ID is null or empty");
                throw new IllegalArgumentException("Cohort ID cannot be null or empty");
            }
            
            Optional<Cohort> cohort = cohortRepository.findById(cohortId);
            
            if (cohort.isPresent()) {
                logger.info("Successfully found cohort with ID: {}", cohortId);
            } else {
                logger.warn("No cohort found with ID: {}", cohortId);
            }
            
            return cohort;
        } catch (IllegalArgumentException e) {
            logger.error("Invalid argument for getCohortById: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error occurred while retrieving cohort with ID {}: {}", cohortId, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve cohort", e);
        }
    }

    @Override
    @Cacheable(value = "cohortsByOrg", key = "#organizationId")
    public List<Cohort> getCohortsByOrganizationId(String organizationId) {
        try {
            logger.info("Retrieving cohorts for organization ID: {}", organizationId);
            
            if (organizationId == null || organizationId.trim().isEmpty()) {
                logger.warn("Organization ID is null or empty");
                throw new IllegalArgumentException("Organization ID cannot be null or empty");
            }
            
            List<Cohort> cohorts = cohortRepository.findByOrganizationOrganizationId(organizationId);
            logger.info("Successfully retrieved {} cohorts for organization ID: {}", cohorts.size(), organizationId);
            return cohorts;
        } catch (IllegalArgumentException e) {
            logger.error("Invalid argument for getCohortsByOrganizationId: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error occurred while retrieving cohorts for organization ID {}: {}", organizationId, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve cohorts for organization", e);
        }
    }

    @Override
    @CacheEvict(value = {"cohorts", "cohortsByOrg"}, allEntries = true)
    public Cohort createCohort(Cohort cohort) {
        try {
            logger.info("Creating new cohort");
            
            if (cohort == null) {
                logger.error("Cohort object is null");
                throw new IllegalArgumentException("Cohort cannot be null");
            }
            
            // Validate cohort end date
            if (cohort.getCohortEndDate() != null 
                    && cohort.getCohortEndDate().isBefore(cohort.getCohortStartDate())) {
                logger.warn("Invalid cohort dates - end date {} is before start date {}", 
                           cohort.getCohortEndDate(), cohort.getCohortStartDate());
                throw new CohortValidationException("Cohort end date must be greater than the cohort start date.");
            }
            
            // Ensure organization and cohort name are not null
            if (cohort.getCohortName() == null || cohort.getOrganization() == null) {
                logger.error("Missing required fields - cohort name: {}, organization: {}", 
                           cohort.getCohortName(), cohort.getOrganization());
                throw new CohortValidationException("Cohort name and organization are required.");
            }
            
            // Prepare cohort name and organization details
            String orgId = cohort.getOrganization().getOrganizationId();
            String originalCohortName = cohort.getCohortName();
            String sanitizedCohortName = originalCohortName.replaceAll("\\s+", "");
            
            logger.debug("Processing cohort creation for organization ID: {} with name: {}", orgId, originalCohortName);
            
            // Generate name prefix
            String namePrefix = sanitizedCohortName.length() >= 4
                    ? sanitizedCohortName.substring(0, 4).toUpperCase()
                    : sanitizedCohortName.toUpperCase();
            
            logger.debug("Generated name prefix: {}", namePrefix);
            
            // Find existing cohorts with the same name in the organization
            long cohortCount = cohortRepository.countByCohortNameAndOrganization(originalCohortName, orgId);
            logger.debug("Found {} existing cohorts with same name in organization", cohortCount);
            
            // Generate unique cohort ID
            String newCohortId = namePrefix + "-" + orgId + "-" + (cohortCount + 1);
            cohort.setCohortId(newCohortId);
            logger.debug("Generated cohort ID: {}", newCohortId);
            
            // Generate UUID if not present
            if (cohort.getUuid() == null) {
                UUID uuid = UUID.randomUUID();
                cohort.setUuid(uuid);
                logger.debug("Generated UUID: {}", uuid);
            }
            
            // Save cohort
            Cohort savedCohort = cohortRepository.save(cohort);
            logger.info("Successfully created cohort with ID: {} for organization: {}", 
                       savedCohort.getCohortId(), orgId);
            
            return savedCohort;
        } catch (CohortValidationException e) {
            logger.error("Validation error during cohort creation: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error occurred while creating cohort: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create cohort", e);
        }
    }

    @Override
    @CachePut(value = "cohorts", key = "#cohortId")
    @CacheEvict(value = {"cohorts", "cohortsByOrg"}, key = "'all'")
    public Cohort updateCohort(String cohortId, Cohort updatedCohort) {
        try {
            logger.info("Updating cohort with ID: {}", cohortId);
            
            if (cohortId == null || cohortId.trim().isEmpty()) {
                logger.error("Cohort ID is null or empty for update");
                throw new IllegalArgumentException("Cohort ID cannot be null or empty");
            }
            
            if (updatedCohort == null) {
                logger.error("Updated cohort object is null");
                throw new IllegalArgumentException("Updated cohort cannot be null");
            }
            
            return cohortRepository.findById(cohortId)
                    .map(cohort -> {
                        logger.debug("Found existing cohort with ID: {}, updating fields", cohortId);
                        
                        if (updatedCohort.getCohortEndDate() != null 
                                && updatedCohort.getCohortEndDate().isBefore(cohort.getCohortStartDate())) {
                            logger.warn("Invalid update - end date {} is before start date {}", 
                                       updatedCohort.getCohortEndDate(), cohort.getCohortStartDate());
                            throw new CohortValidationException("Cohort end date must be greater than the cohort start date.");
                        }
                        
                        cohort.setCohortName(updatedCohort.getCohortName());
                        cohort.setCohortEndDate(updatedCohort.getCohortEndDate());
                        cohort.setShowLeaderboard(updatedCohort.isShowLeaderboard());
                        cohort.setDelayedStageUnlock(updatedCohort.isDelayedStageUnlock());
                        cohort.setDelayInDays(updatedCohort.getDelayInDays());
                        cohort.setOrganization(updatedCohort.getOrganization());
                        
                        Cohort savedCohort = cohortRepository.save(cohort);
                        logger.info("Successfully updated cohort with ID: {}", cohortId);
                        return savedCohort;
                    })
                    .orElseThrow(() -> {
                        logger.error("Cohort not found with ID: {} for update", cohortId);
                        return new IllegalArgumentException("Cohort not found with ID: " + cohortId);
                    });
        } catch (CohortValidationException | IllegalArgumentException e) {
            logger.error("Validation error during cohort update: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error occurred while updating cohort with ID {}: {}", cohortId, e.getMessage(), e);
            throw new RuntimeException("Failed to update cohort", e);
        }
    }

    @Override
    @CacheEvict(value = {"cohorts", "cohortsByOrg"}, allEntries = true)
    public void deleteCohort(String cohortId) {
        try {
            logger.info("Deleting cohort with ID: {}", cohortId);
            
            if (cohortId == null || cohortId.trim().isEmpty()) {
                logger.error("Cohort ID is null or empty for deletion");
                throw new IllegalArgumentException("Cohort ID cannot be null or empty");
            }
            
            if (!cohortRepository.existsById(cohortId)) {
                logger.warn("Cohort not found with ID: {} for deletion", cohortId);
                throw new IllegalArgumentException("Cohort not found with id: " + cohortId);
            }
            
            cohortRepository.deleteById(cohortId);
            logger.info("Successfully deleted cohort with ID: {}", cohortId);
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid argument for deleteCohort: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error occurred while deleting cohort with ID {}: {}", cohortId, e.getMessage(), e);
            throw new RuntimeException("Failed to delete cohort", e);
        }
    }
    
    // Implementation of convertToDTO method
    @Override
    public CohortDTO convertToDTO(Cohort cohort) {
        try {
            logger.debug("Converting cohort to DTO: {}", cohort != null ? cohort.getCohortId() : "null");
            
            if (cohort == null) {
                logger.warn("Cohort is null, cannot convert to DTO");
                throw new IllegalArgumentException("Cohort cannot be null");
            }
            
            CohortDTO dto = new CohortDTO();
            dto.setCohortId(cohort.getCohortId());
            dto.setCohortName(cohort.getCohortName());
            
            logger.debug("Successfully converted cohort {} to DTO", cohort.getCohortId());
            return dto;
        } catch (IllegalArgumentException e) {
            logger.error("Invalid argument for convertToDTO: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error occurred while converting cohort to DTO: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to convert cohort to DTO", e);
        }
    }
}