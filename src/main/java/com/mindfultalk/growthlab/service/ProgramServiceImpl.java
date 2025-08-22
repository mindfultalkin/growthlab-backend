package com.mindfultalk.growthlab.service;

import com.mindfultalk.growthlab.dto.*;
import com.mindfultalk.growthlab.exception.ResourceNotFoundException;
import com.mindfultalk.growthlab.model.*;
import com.mindfultalk.growthlab.repository.*;
import org.slf4j.*;
import org.springframework.cache.annotation.*;
import com.opencsv.CSVReader;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProgramServiceImpl implements ProgramService {

    @Autowired
    private ProgramRepository programRepository;
    
    private static final Logger logger = LoggerFactory.getLogger(ProgramServiceImpl.class);

    @Override
    @Cacheable(value = "programs", key = "'all_programs'")
    public List<Program> getAllPrograms() {
        logger.info("Fetching all programs");
        long startTime = System.currentTimeMillis();
        
        try {
            List<Program> programs = programRepository.findAll();
            
            long endTime = System.currentTimeMillis();
            logger.info("Successfully fetched {} programs in {}ms", programs.size(), (endTime - startTime));
            
            return programs;
        } catch (Exception e) {
            logger.error("Error fetching all programs", e);
            throw e;
        }
    }

    @Override
    @Cacheable(value = "programs", key = "#programId")
    public Optional<Program> getProgramById(String programId) {
        logger.info("Fetching program by ID: {}", programId);
        long startTime = System.currentTimeMillis();
        
        try {
            Optional<Program> program = programRepository.findById(programId);
            
            long endTime = System.currentTimeMillis();
            if (program.isPresent()) {
                logger.info("Successfully found program: {} (ID: {}) in {}ms", 
                    program.get().getProgramName(), programId, (endTime - startTime));
            } else {
                logger.warn("Program not found with ID: {} in {}ms", programId, (endTime - startTime));
            }
            
            return program;
        } catch (Exception e) {
            logger.error("Error fetching program by ID: {}", programId, e);
            throw e;
        }
    }

    @Override
    @CacheEvict(value = "programs", allEntries = true)
    public Program createProgram(Program program) {
        logger.info("Creating new program: {} (ID: {})", program.getProgramName(), program.getProgramId());
        long startTime = System.currentTimeMillis();
        
        try {
            // Check if program already exists
            if (programRepository.existsById(program.getProgramId())) {
                throw new IllegalArgumentException("Program already exists with ID: " + program.getProgramId());
            }
            
         // Generate UUID if not provided
            if (program.getUuid() == null) {
                program.setUuid(UUID.randomUUID());
            }

            
            Program savedProgram = programRepository.save(program);
            
            long endTime = System.currentTimeMillis();
            logger.info("Successfully created program: {} (ID: {}) in {}ms", 
                savedProgram.getProgramName(), savedProgram.getProgramId(), (endTime - startTime));
            
            return savedProgram;
        } catch (Exception e) {
            logger.error("Error creating program: {} (ID: {})", program.getProgramName(), program.getProgramId(), e);
            throw e;
        }
    }
    
    @Override
    @Cacheable(value = "programs", key = "#programId")
    public Optional<Program> findByProgramId(String programId) {
        logger.info("Finding program by programId: {}", programId);
        long startTime = System.currentTimeMillis();
        
        try {
            Optional<Program> program = programRepository.findByProgramId(programId);
            
            long endTime = System.currentTimeMillis();
            if (program.isPresent()) {
                logger.info("Successfully found program: {} (ID: {}) in {}ms", 
                    program.get().getProgramName(), programId, (endTime - startTime));
            } else {
                logger.warn("Program not found with programId: {} in {}ms", programId, (endTime - startTime));
            }
            
            return program;
        } catch (Exception e) {
            logger.error("Error finding program by programId: {}", programId, e);
            throw e;
        }
    }
    
    @Override
    @CacheEvict(value = "programs", allEntries = true)
    public Map<String, Object> uploadProgramsCSV(MultipartFile file) {
        logger.info("Starting CSV upload for programs. File: {}, Size: {} bytes", 
            file.getOriginalFilename(), file.getSize());
        long startTime = System.currentTimeMillis();
        
        Map<String, Object> result = new HashMap<>();
        int createdCount = 0;
        int failedCount = 0;
        List<String> failedIds = new ArrayList<>();
        Set<String> csvProgramIds = new HashSet<>(); // To track duplicates in the CSV file

        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            List<String[]> records = reader.readAll();
            logger.info("Processing {} records from CSV file", records.size() - 1); // Exclude header

            for (int i = 1; i < records.size(); i++) { // Skip header row
                String[] record = records.get(i);
                
                if (record.length < 5) {
                    failedCount++;
                    failedIds.add("Row " + (i+1) + ": Insufficient columns in record");
                    continue;
                }
                
                String programId = record[0].trim(); // Assuming programId is the first column

                // Check for duplicate programId in the CSV file
                if (!csvProgramIds.add(programId)) {
                    failedCount++;
                    failedIds.add("Row " + (i+1) + ": Program ID '" + programId + "' is duplicated in the CSV.");
                    logger.warn("Duplicate programId found in CSV at row {}: {}", i+1, programId);
                    continue;
                }

                // Check if programId already exists in the database
                if (programRepository.existsById(programId)) {
                    failedCount++;
                    failedIds.add("Row " + (i+1) + ": Program ID '" + programId + "' already exists in the database.");
                    logger.warn("Program already exists in database at row {}: {}", i+1, programId);
                    continue;
                }

                try {
                    // Create and populate the Program entity
                    Program program = new Program();
                    program.setProgramId(programId);
                    program.setStages(Integer.parseInt(record[1].trim())); // Assuming stages is the second column
                    program.setUnitCount(Integer.parseInt(record[2].trim())); // Assuming unit count is the third column
                    program.setProgramDesc(record[3].trim()); // Assuming description is the fourth column
                    program.setProgramName(record[4].trim()); // Assuming name is the fifth column
                    program.setUuid(UUID.randomUUID()); // Generate UUID

                    // Save the valid program to the database
                    programRepository.save(program);
                    createdCount++;
                    logger.debug("Successfully created program from row {}: {} (ID: {})", i+1, program.getProgramName(), programId);
                } catch (NumberFormatException e) {
                    failedCount++;
                    failedIds.add("Row " + (i+1) + ": Invalid number format in record for program ID '" + programId + "'");
                    logger.warn("Number format error at row {} for programId {}: {}", i+1, programId, e.getMessage());
                } catch (Exception e) {
                    failedCount++;
                    failedIds.add("Row " + (i+1) + ": Error processing program ID '" + programId + "': " + e.getMessage());
                    logger.warn("Error processing program at row {} for programId {}: {}", i+1, programId, e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.error("Failed to process CSV file: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("Failed to process CSV file: " + e.getMessage());
        }

        result.put("createdCount", createdCount);
        result.put("failedCount", failedCount);
        result.put("failedIds", failedIds);
        
        long endTime = System.currentTimeMillis();
        logger.info("Completed CSV upload for programs in {}ms. Created: {}, Failed: {}, Total processed: {}", 
            (endTime - startTime), createdCount, failedCount, createdCount + failedCount);
        
        return result;
    }

    @Override
    @CachePut(value = "programs", key = "#programId")
    @CacheEvict(value = "programs", key = "'all_programs'")
    public Program updateProgram(String programId, Program updatedProgram) {
        logger.info("Updating program with ID: {}", programId);
        long startTime = System.currentTimeMillis();
        
        try {
            Program program = programRepository.findById(programId)
                .map(existingProgram -> {
                    logger.debug("Found existing program: {} (ID: {})", existingProgram.getProgramName(), programId);
                    
                    existingProgram.setProgramName(updatedProgram.getProgramName());
                    existingProgram.setProgramDesc(updatedProgram.getProgramDesc());
                    existingProgram.setStages(updatedProgram.getStages());
                    existingProgram.setUnitCount(updatedProgram.getUnitCount());
                    
                    return programRepository.save(existingProgram);
                })
                .orElseThrow(() -> new ResourceNotFoundException("Program not found with ID: " + programId));
            
            long endTime = System.currentTimeMillis();
            logger.info("Successfully updated program: {} (ID: {}) in {}ms", 
                program.getProgramName(), programId, (endTime - startTime));
            
            return program;
        } catch (Exception e) {
            logger.error("Error updating program with ID: {}", programId, e);
            throw e;
        }
    }

    @Override
    @CacheEvict(value = "programs", allEntries = true)
    public void deleteProgram(String programId) {
        logger.info("Deleting program with ID: {}", programId);
        long startTime = System.currentTimeMillis();
        
        try {
            // Check if program exists before deletion
            if (!programRepository.existsById(programId)) {
                throw new ResourceNotFoundException("Program not found with ID: " + programId);
            }
            
            programRepository.deleteById(programId);
            
            long endTime = System.currentTimeMillis();
            logger.info("Successfully deleted program with ID: {} in {}ms", programId, (endTime - startTime));
        } catch (Exception e) {
            logger.error("Error deleting program with ID: {}", programId, e);
            throw e;
        }
    }

    @Override
    @CacheEvict(value = "programs", allEntries = true)
    public void deletePrograms(List<String> programIds) {
        logger.info("Deleting {} programs with IDs: {}", programIds.size(), programIds);
        long startTime = System.currentTimeMillis();
        
        try {
            // Validate that all programs exist
            List<String> nonExistentIds = programIds.stream()
                .filter(id -> !programRepository.existsById(id))
                .collect(Collectors.toList());
                
            if (!nonExistentIds.isEmpty()) {
                logger.warn("Some programs not found for deletion: {}", nonExistentIds);
                throw new ResourceNotFoundException("Programs not found with IDs: " + nonExistentIds);
            }
            
            programRepository.deleteAllById(programIds);
            
            long endTime = System.currentTimeMillis();
            logger.info("Successfully deleted {} programs in {}ms", programIds.size(), (endTime - startTime));
        } catch (Exception e) {
            logger.error("Error deleting programs with IDs: {}", programIds, e);
            throw e;
        }
    }

    @Override
    @Cacheable(value = "programDTOs", key = "#program.programId")
    public ProgramDTO convertToDTO(Program program) {
        logger.debug("Converting program to DTO: {} (ID: {})", program.getProgramName(), program.getProgramId());
        
        try {
            ProgramDTO dto = new ProgramDTO();
            dto.setProgramId(program.getProgramId());
            dto.setProgramName(program.getProgramName());
            dto.setProgramDesc(program.getProgramDesc());
            dto.setStagesCount(program.getStages());
            dto.setUnitCount(program.getUnitCount());
            
            logger.debug("Successfully converted program to DTO: {}", program.getProgramId());
            return dto;
        } catch (Exception e) {
            logger.error("Error converting program to DTO: {} (ID: {})", program.getProgramName(), program.getProgramId(), e);
            throw e;
        }
    }

    // Additional cache management methods
    
    @CacheEvict(value = "programs", allEntries = true)
    public void clearAllProgramCache() {
        logger.info("Clearing all program caches");
    }
    
    @CacheEvict(value = "programs", key = "#programId")
    public void clearProgramCache(String programId) {
        logger.info("Clearing cache for program ID: {}", programId);
    }
    
    @CacheEvict(value = "programDTOs", allEntries = true)
    public void clearProgramDTOCache() {
        logger.info("Clearing all program DTO caches");
    }

    // Utility method to get program count (cached)
    @Cacheable(value = "programCount", key = "'total_count'")
    public long getProgramCount() {
        logger.info("Fetching total program count");
        long startTime = System.currentTimeMillis();
        
        try {
            long count = programRepository.count();
            
            long endTime = System.currentTimeMillis();
            logger.info("Successfully fetched program count: {} in {}ms", count, (endTime - startTime));
            
            return count;
        } catch (Exception e) {
            logger.error("Error fetching program count", e);
            throw e;
        }
    }
    
    // Method to check if program exists (cached)
    @Cacheable(value = "programExists", key = "#programId")
    public boolean programExists(String programId) {
        logger.debug("Checking if program exists: {}", programId);
        
        try {
            boolean exists = programRepository.existsById(programId);
            logger.debug("Program exists check for {}: {}", programId, exists);
            return exists;
        } catch (Exception e) {
            logger.error("Error checking if program exists: {}", programId, e);
            throw e;
        }
    }
}