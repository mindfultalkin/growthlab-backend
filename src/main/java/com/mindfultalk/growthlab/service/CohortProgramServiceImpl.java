package com.mindfultalk.growthlab.service;

import com.mindfultalk.growthlab.model.*;
import com.mindfultalk.growthlab.repository.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.*;
import java.util.*;

@Service
public class CohortProgramServiceImpl implements CohortProgramService {

    @Autowired
    private CohortProgramRepository cohortProgramRepository;

    private static final Logger logger = LoggerFactory.getLogger(CohortProgramServiceImpl.class);

    @Override
    @Cacheable(value = "cohortPrograms", key = "'all'")
    public List<CohortProgram> getAllCohortPrograms() {
        try {
            logger.info("Retrieving all cohort programs from database");
            List<CohortProgram> cohortPrograms = cohortProgramRepository.findAll();
            logger.info("Successfully retrieved {} cohort programs", cohortPrograms.size());
            return cohortPrograms;
        } catch (Exception e) {
            logger.error("Error occurred while retrieving all cohort programs: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve cohort programs", e);
        }
    }

    @Override
    @Cacheable(value = "cohortPrograms", key = "#cohortProgramId")
    public Optional<CohortProgram> getCohortProgram(Long cohortProgramId) {
        try {
            logger.info("Retrieving cohort program with ID: {}", cohortProgramId);
            
            if (cohortProgramId == null) {
                logger.warn("Cohort program ID is null");
                throw new IllegalArgumentException("Cohort program ID cannot be null");
            }
            
            Optional<CohortProgram> cohortProgram = cohortProgramRepository.findById(cohortProgramId);
            
            if (cohortProgram.isPresent()) {
                logger.info("Successfully found cohort program with ID: {}", cohortProgramId);
            } else {
                logger.warn("No cohort program found with ID: {}", cohortProgramId);
            }
            
            return cohortProgram;
        } catch (IllegalArgumentException e) {
            logger.error("Invalid argument for getCohortProgram: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error occurred while retrieving cohort program with ID {}: {}", cohortProgramId, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve cohort program", e);
        }
    }

    @Override
    @CacheEvict(value = "cohortPrograms", allEntries = true)
    public CohortProgram createCohortProgram(CohortProgram cohortProgram) {
        try {
            logger.info("Creating new cohort program");
            
            if (cohortProgram == null) {
                logger.error("Cohort program object is null");
                throw new IllegalArgumentException("Cohort program cannot be null");
            }
            
            if (cohortProgram.getCohort() == null || cohortProgram.getCohort().getCohortId() == null) {
                logger.error("Cohort or cohort ID is null in the provided cohort program");
                throw new IllegalArgumentException("Cohort and cohort ID must be provided");
            }
            
            String cohortId = cohortProgram.getCohort().getCohortId();
            logger.debug("Checking if cohort with ID {} already has a program assigned", cohortId);
            
            // Check if cohort already has a program assigned
            Optional<CohortProgram> existingCohortProgram = cohortProgramRepository.findByCohortCohortId(cohortId);
            
            if (existingCohortProgram.isPresent()) {
                logger.warn("Cohort with ID {} is already assigned to a program with ID {}", 
                           cohortId, existingCohortProgram.get().getCohortProgramId());
                throw new IllegalArgumentException("The cohort is already assigned to a program.");
            }
            
            CohortProgram savedCohortProgram = cohortProgramRepository.save(cohortProgram);
            logger.info("Successfully created cohort program with ID: {} for cohort ID: {}", 
                       savedCohortProgram.getCohortProgramId(), cohortId);
            
            return savedCohortProgram;
        } catch (IllegalArgumentException e) {
            logger.error("Invalid argument for createCohortProgram: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error occurred while creating cohort program: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create cohort program", e);
        }
    }

    @Override
    @CacheEvict(value = "cohortPrograms", allEntries = true)
    public void deleteCohortProgram(Long cohortProgramId) {
        try {
            logger.info("Deleting cohort program with ID: {}", cohortProgramId);
            
            if (cohortProgramId == null) {
                logger.error("Cohort program ID is null for deletion");
                throw new IllegalArgumentException("Cohort program ID cannot be null");
            }
            
            // Check if the cohort program exists before deletion
            Optional<CohortProgram> existingCohortProgram = cohortProgramRepository.findById(cohortProgramId);
            
            if (!existingCohortProgram.isPresent()) {
                logger.warn("Cohort program with ID {} not found for deletion", cohortProgramId);
                throw new IllegalArgumentException("Cohort program not found with ID: " + cohortProgramId);
            }
            
            cohortProgramRepository.deleteById(cohortProgramId);
            logger.info("Successfully deleted cohort program with ID: {}", cohortProgramId);
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid argument for deleteCohortProgram: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error occurred while deleting cohort program with ID {}: {}", cohortProgramId, e.getMessage(), e);
            throw new RuntimeException("Failed to delete cohort program", e);
        }
    }
}