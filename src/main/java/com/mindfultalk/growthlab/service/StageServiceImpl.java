package com.mindfultalk.growthlab.service;

import com.mindfultalk.growthlab.model.*;
import com.mindfultalk.growthlab.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.util.*;
import org.slf4j.*;
import org.springframework.cache.annotation.*;

@Service
public class StageServiceImpl implements StageService {

    @Autowired
    private StageRepository stageRepository;
    
    @Autowired
    private ProgramRepository programRepository;
    
    private static final Logger logger = LoggerFactory.getLogger(StageServiceImpl.class);

    @Override
    @Cacheable(value = "stages", key = "'all_stages'")
    public List<Stage> getAllStages() {
        logger.info("Fetching all stages from database");
        List<Stage> stages = stageRepository.findAll();
        logger.info("Retrieved {} stages", stages.size());
        return stages;
    }

    @Override
    @Cacheable(value = "stages", key = "#stageId")
    public Optional<Stage> getStageById(String stageId) {
        logger.info("Fetching stage by ID: {}", stageId);
        Optional<Stage> stage = stageRepository.findById(stageId);
        if (stage.isPresent()) {
            logger.info("Stage found with ID: {}", stageId);
        } else {
            logger.warn("Stage not found with ID: {}", stageId);
        }
        return stage;
    }
    
    @Override
    @Cacheable(value = "stages", key = "#stageId")
    public Optional<Stage> findByStageId(String stageId) {
        logger.info("Finding stage by stage ID: {}", stageId);
        Optional<Stage> stage = stageRepository.findByStageId(stageId);
        if (stage.isPresent()) {
            logger.info("Stage found with stage ID: {}", stageId);
        } else {
            logger.warn("Stage not found with stage ID: {}", stageId);
        }
        return stage;
    }
    
    @Override
    @CacheEvict(value = "stages", allEntries = true)
    public Stage createStage(Stage stage) {
        logger.info("Creating new stage with ID: {}", stage.getStageId());
        try {
            Stage savedStage = stageRepository.save(stage);
            logger.info("Successfully created stage with ID: {}", savedStage.getStageId());
            return savedStage;
        } catch (Exception e) {
            logger.error("Error creating stage with ID: {}", stage.getStageId(), e);
            throw new RuntimeException("Failed to create stage: " + e.getMessage());
        }
    }
    
    @Override
    @CacheEvict(value = "stages", allEntries = true)
    public Map<String, Object> uploadStagesCSV(MultipartFile file) {
        logger.info("Starting CSV upload process for file: {}", file.getOriginalFilename());
        
        Map<String, Object> result = new HashMap<>();
        List<String> errorMessages = new ArrayList<>();
        Set<String> csvStageIds = new HashSet<>(); // Track stage IDs within this CSV
        int successfulInserts = 0;
        int failedInserts = 0;
        int lineNumber = 0;

        if (file.isEmpty()) {
            logger.error("Uploaded file is empty");
            throw new RuntimeException("File is empty");
        }
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
        boolean isFirstLine = true;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                
                // Skip header row
                if (isFirstLine) {
                    logger.info("Skipping header row: {}", line);
                    isFirstLine = false;
                    continue;
                }

                try {
                    String[] data = parseCsvLine(line);
                    
                    // Validate CSV format
                    if (data.length < 4) {
                        String error = String.format("Line %d: Invalid CSV format - expected 4 columns, found %d", 
                                                    lineNumber, data.length);
                        logger.warn(error);
                        errorMessages.add(error);
                        failedInserts++;
                        continue;
                    }

                    String stageId = data[0].trim();
                    String stageName = data[1].trim();
                    String stageDesc = data[2].trim();
                    String programId = data[3].trim();

                    // Validate required fields
                    if (stageId.isEmpty() || stageName.isEmpty() || programId.isEmpty()) {
                        String error = String.format("Line %d: Required fields cannot be empty (stageId, stageName, programId)", lineNumber);
                        logger.warn(error);
                        errorMessages.add(error);
                        failedInserts++;
                        continue;
                    }

                    // Check for duplicates within the same CSV file
                    if (csvStageIds.contains(stageId)) {
                        String error = String.format("Line %d: Duplicate Stage ID '%s' found in CSV", lineNumber, stageId);
                        logger.warn(error);
                        errorMessages.add(error);
                        failedInserts++;
                        continue;
                    }

                    // Check if stageId already exists in the database
                    if (stageRepository.existsById(stageId)) {
                        String error = String.format("Line %d: Stage ID '%s' already exists in database", lineNumber, stageId);
                        logger.warn(error);
                        errorMessages.add(error);
                        failedInserts++;
                        continue;
                    }

                    // Validate program exists
                    Optional<Program> program = programRepository.findById(programId);
                    if (program.isEmpty()) {
                        String error = String.format("Line %d: Program ID '%s' not found for Stage ID '%s'", 
                                                    lineNumber, programId, stageId);
                        logger.warn(error);
                        errorMessages.add(error);
                        failedInserts++;
                        continue;
                    }
                    
                 // All validations passed - create the stage
                    Stage newStage = new Stage(stageId, stageName, stageDesc, program.get(), UUID.randomUUID());
                    stageRepository.save(newStage);
                    csvStageIds.add(stageId);
                    successfulInserts++;
                    
                    logger.debug("Successfully processed line {}: Stage ID '{}'", lineNumber, stageId);

                } catch (Exception e) {
                    String error = String.format("Line %d: Error processing record - %s", lineNumber, e.getMessage());
                    logger.error(error, e);
                    errorMessages.add(error);
                    failedInserts++;
                }
            }

            result.put("successfulInserts", successfulInserts);
            result.put("failedInserts", failedInserts);
            result.put("totalLines", lineNumber - 1); // Exclude header
            result.put("errors", errorMessages);
            
            logger.info("CSV upload completed. Success: {}, Failed: {}, Total processed: {}", 
                       successfulInserts, failedInserts, lineNumber - 1);

        } catch (IOException e) {
            logger.error("IO error while processing CSV file: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("Error reading CSV file: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error while processing CSV: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("Error while processing CSV: " + e.getMessage());
        }

        return result;
    }

    /**
     * Parse CSV line handling commas within quoted fields
     */
    private String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder currentField = new StringBuilder();
        
        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(currentField.toString());
                currentField = new StringBuilder();
            } else {
                currentField.append(c);
            }
        }
        result.add(currentField.toString());
        
        return result.toArray(new String[0]);
    }

    @Override
    @CachePut(value = "stages", key = "#stageId")
    @CacheEvict(value = "stages", key = "'all_stages'")
    public Stage updateStage(String stageId, Stage stage) {
        logger.info("Updating stage with ID: {}", stageId);
        
        return stageRepository.findById(stageId).map(existingStage -> {
            logger.debug("Found existing stage, updating fields");
            existingStage.setStageName(stage.getStageName());
            existingStage.setStageDesc(stage.getStageDesc());
            existingStage.setProgram(stage.getProgram());
            existingStage.setUuid(stage.getUuid());
            
            Stage updatedStage = stageRepository.save(existingStage);
            logger.info("Successfully updated stage with ID: {}", stageId);
            return updatedStage;
            
        }).orElseThrow(() -> {
            logger.error("Stage not found for update with ID: {}", stageId);
            return new RuntimeException("Stage not found with ID: " + stageId);
        });
    }

    @Override
    @CacheEvict(value = "stages", allEntries = true)
    public void deleteStage(String stageId) {
        logger.info("Deleting stage with ID: {}", stageId);
        
        if (!stageRepository.existsById(stageId)) {
            logger.error("Attempted to delete non-existent stage with ID: {}", stageId);
            throw new RuntimeException("Stage not found with ID: " + stageId);
        }
        
        try {
            stageRepository.deleteById(stageId);
            logger.info("Successfully deleted stage with ID: {}", stageId);
        } catch (Exception e) {
            logger.error("Error deleting stage with ID: {}", stageId, e);
            throw new RuntimeException("Failed to delete stage: " + e.getMessage());
        }
    }
}