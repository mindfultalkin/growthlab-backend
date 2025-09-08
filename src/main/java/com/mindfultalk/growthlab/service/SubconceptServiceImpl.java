package com.mindfultalk.growthlab.service;

import com.mindfultalk.growthlab.dto.*;
import com.mindfultalk.growthlab.model.*;
import com.mindfultalk.growthlab.repository.*;
import com.mindfultalk.growthlab.util.DurationParser;
import com.opencsv.CSVReader;
import org.springframework.cache.annotation.*;
import org.slf4j.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SubconceptServiceImpl implements SubconceptService {

    @Autowired
    private SubconceptRepository subconceptRepository;
    
    @Autowired
    private ConceptRepository conceptRepository; 
    
    @Autowired
    private ContentMasterRepository contentRepository; 

    private static final Logger logger = LoggerFactory.getLogger(SubconceptServiceImpl.class);
   
    @Override
    @Cacheable(value = "subconcepts", key = "'all_subconcepts'")
    public List<Subconcept> getAllSubconcept() {
        logger.info("Fetching all subconcepts from database");
        try {
            List<Subconcept> subconcepts = subconceptRepository.findAll();
            logger.info("Retrieved {} subconcepts", subconcepts.size());
            return subconcepts;
        } catch (Exception e) {
            logger.error("Error fetching all subconcepts", e);
            throw new RuntimeException("Failed to fetch subconcepts: " + e.getMessage());
        }
    }
    
    @Override
    @Cacheable(value = "subconceptDTOs", key = "'all_subconcept_dtos'")
    public List<SubconceptResponseDTO> getAllSubconcepts() {
        logger.info("Fetching all subconcepts as DTOs");
        try {
            List<SubconceptResponseDTO> dtos = subconceptRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
            logger.info("Retrieved {} subconcept DTOs", dtos.size());
            return dtos;
        } catch (Exception e) {
            logger.error("Error fetching all subconcept DTOs", e);
            throw new RuntimeException("Failed to fetch subconcept DTOs: " + e.getMessage());
        }
    }

    
    @Override
    @Cacheable(value = "subconcepts", key = "#subconceptId")
    public Optional<Subconcept> findBySubconceptId(String subconceptId) {
        logger.info("Finding subconcept by ID: {}", subconceptId);
        
        if (subconceptId == null || subconceptId.trim().isEmpty()) {
            logger.error("Subconcept ID is null or empty");
            throw new IllegalArgumentException("Subconcept ID cannot be null or empty");
        }
        
        try {
            Optional<Subconcept> subconcept = subconceptRepository.findBySubconceptId(subconceptId);
            if (subconcept.isPresent()) {
                logger.info("Subconcept found with ID: {}", subconceptId);
            } else {
                logger.warn("Subconcept not found with ID: {}", subconceptId);
            }
            return subconcept;
        } catch (Exception e) {
            logger.error("Error finding subconcept by ID: {}", subconceptId, e);
            throw new RuntimeException("Failed to find subconcept: " + e.getMessage());
        }
    }
    
    @Override
    @Cacheable(value = "subconceptDTOs", key = "#subconceptId")
    public Optional<SubconceptResponseDTO> getSubconceptById(String subconceptId) {
        logger.info("Getting subconcept DTO by ID: {}", subconceptId);
        
        if (subconceptId == null || subconceptId.trim().isEmpty()) {
            logger.error("Subconcept ID is null or empty");
            throw new IllegalArgumentException("Subconcept ID cannot be null or empty");
        }
        
        try {
            Optional<SubconceptResponseDTO> dto = subconceptRepository.findById(subconceptId)
                .map(this::convertToDTO);
            if (dto.isPresent()) {
                logger.info("Subconcept DTO found with ID: {}", subconceptId);
            } else {
                logger.warn("Subconcept DTO not found with ID: {}", subconceptId);
            }
            return dto;
        } catch (Exception e) {
            logger.error("Error getting subconcept DTO by ID: {}", subconceptId, e);
            throw new RuntimeException("Failed to get subconcept DTO: " + e.getMessage());
        }
    }

    @Override
    @CacheEvict(value = {"subconcepts", "subconceptDTOs"}, allEntries = true)
    public Subconcept createSubconcept(Subconcept subconcept) {
        logger.info("Creating new subconcept with ID: {}", subconcept.getSubconceptId());
        
        try {
            Subconcept savedSubconcept = subconceptRepository.save(subconcept);
            logger.info("Successfully created subconcept with ID: {}", savedSubconcept.getSubconceptId());
            return savedSubconcept;
        } catch (Exception e) {
            logger.error("Error creating subconcept with ID: {}", subconcept.getSubconceptId(), e);
            throw new RuntimeException("Failed to create subconcept: " + e.getMessage());
        }
    }
    
    
    @Override 
    @CacheEvict(value = {"subconcepts", "subconceptDTOs"}, allEntries = true)
    public Map<String, Object> uploadSubconceptsCSV(MultipartFile file) {
        logger.info("Starting CSV upload process for file: {}", file.getOriginalFilename());
        
        Map<String, Object> result = new HashMap<>();
        int createdCount = 0;
        int failedCount = 0;
        List<String> failedIds = new ArrayList<>();
        int lineNumber = 0;
        
        if (file.isEmpty()) {
            logger.error("Uploaded file is empty");
            throw new RuntimeException("File is empty");
        }
        
        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            List<String[]> records = reader.readAll();
            if (records.isEmpty()) {
                throw new RuntimeException("CSV file is empty");
            }

            // --- Step 1: Build header map (handle exact column names from CSV) ---
            String[] headers = records.get(0);
            Map<String, Integer> headerMap = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                String cleanHeader = headers[i].trim().toLowerCase();
                headerMap.put(cleanHeader, i);
                
                // Map variations to standard names for easier lookup
                switch (cleanHeader) {
                    case "subconceptid":
                        headerMap.put("subconcept_id", i);
                        break;
                    case "showto":
                        headerMap.put("show_to", i);
                        break;
                    case "subconceptdesc":
                        headerMap.put("subconcept_desc", i);
                        break;
                    case "subconceptdesc2":
                        headerMap.put("subconcept_desc_2", i);
                        break;
                    case "subconceptgroup":
                        headerMap.put("subconcept_group", i);
                        break;
                    case "subconceptlink":
                        headerMap.put("subconcept_link", i);
                        break;
                    case "subconcepttype":
                        headerMap.put("subconcept_type", i);
                        break;
                    case "numquestions":
                        headerMap.put("num_questions", i);
                        break;
                    case "subconceptmaxscore":
                        headerMap.put("subconcept_maxscore", i);
                        break;
                    case "conceptid":
                        headerMap.put("concept_id", i);
                        break;
                    case "contentid":
                        headerMap.put("content_id", i);
                        break;
                    case "subconceptduration":
                        headerMap.put("duration", i);
                        break;
                }
            }

            logger.info("CSV Header mapping: {}", headerMap);

            // --- Step 2: Process rows ---
            for (int i = 1; i < records.size(); i++) {
                lineNumber = i + 1;
                String[] record = records.get(i);

                try {
                    // --- Required fields ---
                    String subconceptId = getValue(record, headerMap, "subconcept_id");
                    if (subconceptId == null || subconceptId.isEmpty()) {
                        fail(lineNumber, "Subconcept ID cannot be empty", failedIds);
                        failedCount++;
                        continue;
                    }

                    if (subconceptRepository.existsById(subconceptId)) {
                        fail(lineNumber, "SubconceptId " + subconceptId + " already exists", failedIds);
                        failedCount++;
                        continue;
                    }

                    Subconcept subconcept = new Subconcept();
                    subconcept.setSubconceptId(subconceptId);
                    subconcept.setDependency(getValue(record, headerMap, "dependency"));
                    subconcept.setShowTo(getValue(record, headerMap, "show_to"));
                    subconcept.setSubconceptDesc(getValue(record, headerMap, "subconcept_desc"));
                    subconcept.setSubconceptDesc2(getValue(record, headerMap, "subconcept_desc_2"));
                    subconcept.setSubconceptGroup(getValue(record, headerMap, "subconcept_group"));
                    subconcept.setSubconceptLink(getValue(record, headerMap, "subconcept_link"));
                    subconcept.setSubconceptType(getValue(record, headerMap, "subconcept_type"));

                    // --- Numeric fields ---
                    String numQuestionsStr = getValue(record, headerMap, "num_questions");
                    String maxScoreStr = getValue(record, headerMap, "subconcept_maxscore");

                    if (numQuestionsStr != null && !numQuestionsStr.isBlank()) {
                        int numQuestions = Integer.parseInt(numQuestionsStr);
                        if (numQuestions < 0) throw new IllegalArgumentException("NumQuestions must be >= 0");
                        subconcept.setNumQuestions(numQuestions);
                    }

                    if (maxScoreStr != null && !maxScoreStr.isBlank()) {
                        int maxScore = Integer.parseInt(maxScoreStr);
                        if (maxScore < 0) throw new IllegalArgumentException("MaxScore must be >= 0");
                        subconcept.setSubconceptMaxscore(maxScore);
                    }

                    // --- Duration (Fixed to handle decimal minutes properly) ---
                    String durationStr = getValue(record, headerMap, "duration");
                    int duration = parseSubconceptDuration(durationStr);
                    subconcept.setSubconceptDuration(duration);

                    // --- Concept and Content ---
                    String conceptId = getValue(record, headerMap, "concept_id");
                    String contentId = getValue(record, headerMap, "content_id");

                    // Validate concept exists
                    if (conceptId == null || conceptId.isEmpty()) {
                        fail(lineNumber, "Concept ID cannot be empty", failedIds);
                        failedCount++;
                        continue;
                    }

                    Optional<Concept> concept = conceptRepository.findById(conceptId);
                    if (concept.isEmpty()) {
                        fail(lineNumber, "ConceptId " + conceptId + " not found", failedIds);
                        failedCount++;
                        continue;
                    }

                    // Validate content exists (if provided)
                    Optional<ContentMaster> content = Optional.empty();
                    if (contentId != null && !contentId.isEmpty()) {
                        try {
                            int contentInt = Integer.parseInt(contentId);
                            content = contentRepository.findById(contentInt);
                            if (content.isEmpty()) {
                                fail(lineNumber, "ContentId " + contentId + " not found", failedIds);
                                failedCount++;
                                continue;
                            }
                        } catch (NumberFormatException ex) {
                            fail(lineNumber, "Invalid ContentId format: " + contentId, failedIds);
                            failedCount++;
                            continue;
                        }
                    }

                    subconcept.setConcept(concept.get());
                    if (content.isPresent()) {
                        subconcept.setContent(content.get());
                    }
                    subconcept.setUuid(UUID.randomUUID());

                    subconceptRepository.save(subconcept);
                    createdCount++;
                    logger.debug("Created subconcept: {}", subconceptId);

                } catch (Exception e) {
                    fail(lineNumber, "Unexpected error - " + e.getMessage(), failedIds);
                    failedCount++;
                    logger.error("Error processing line {}: {}", lineNumber, e.getMessage(), e);
                }
            }

        } catch (Exception e) {
            logger.error("Failed to process CSV file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process CSV file: " + e.getMessage(), e);
        }

        result.put("createdCount", createdCount);
        result.put("failedCount", failedCount);
        result.put("totalProcessed", createdCount + failedCount);
        result.put("failedIds", failedIds);

        logger.info("CSV upload completed. Created: {}, Failed: {}", createdCount, failedCount);
        return result;
    }

    // --- Utility: Get value safely ---
    private String getValue(String[] record, Map<String, Integer> headerMap, String column) {
        Integer idx = headerMap.get(column.toLowerCase());
        if (idx != null && idx < record.length) {
            String value = record[idx].trim();
            return value.isEmpty() ? null : value;
        }
        return null;
    }

    // --- Utility: Parse duration to seconds ---
    private int parseSubconceptDuration(String durationStr) {
        if (durationStr == null || durationStr.trim().isEmpty()) {
            return 0; // Default to 0 seconds if no duration provided
        }
        
        try {
            double minutes = Double.parseDouble(durationStr.trim());
            // Convert minutes to seconds and round to nearest integer
            return (int) Math.round(minutes * 60);
        } catch (NumberFormatException e) {
            logger.warn("Invalid duration format: '{}', defaulting to 0 seconds", durationStr);
            return 0;
        }
    }

    // --- Utility: Fail logging ---
    private void fail(int lineNumber, String msg, List<String> failedIds) {
        String error = "Line " + lineNumber + ": " + msg;
        logger.warn(error);
        failedIds.add(error);
    }
    
    @Override
    @CachePut(value = "subconcepts", key = "#subconceptId")
    @CacheEvict(value = "subconceptDTOs", allEntries = true)
    public Subconcept updateSubconcept(String subconceptId, Subconcept subconcept) {
    logger.info("Updating subconcept with ID: {}", subconceptId);
        
        if (subconceptId == null || subconceptId.trim().isEmpty()) {
            logger.error("Subconcept ID is null or empty");
            throw new IllegalArgumentException("Subconcept ID cannot be null or empty");
        }
        
        if (subconcept == null) {
            logger.error("Subconcept object is null");
            throw new IllegalArgumentException("Subconcept cannot be null");
        }
        
        return subconceptRepository.findById(subconceptId).map(existingSubconcept -> {
            logger.debug("Found existing subconcept, updating fields");
            existingSubconcept.setDependency(subconcept.getDependency());
            existingSubconcept.setShowTo(subconcept.getShowTo());
            existingSubconcept.setSubconceptDesc(subconcept.getSubconceptDesc());
            existingSubconcept.setSubconceptGroup(subconcept.getSubconceptGroup());
            existingSubconcept.setSubconceptLink(subconcept.getSubconceptLink());
            existingSubconcept.setSubconceptDesc2(subconcept.getSubconceptDesc2());
            existingSubconcept.setSubconceptType(subconcept.getSubconceptType());
            existingSubconcept.setNumQuestions(subconcept.getNumQuestions());
            existingSubconcept.setSubconceptMaxscore(subconcept.getSubconceptMaxscore());
            existingSubconcept.setConcept(subconcept.getConcept());
            existingSubconcept.setContent(subconcept.getContent());
            try {
                Subconcept updatedSubconcept = subconceptRepository.save(existingSubconcept);
                logger.info("Successfully updated subconcept with ID: {}", subconceptId);
                return updatedSubconcept;
            } catch (Exception e) {
                logger.error("Error saving updated subconcept with ID: {}", subconceptId, e);
                throw new RuntimeException("Failed to save updated subconcept: " + e.getMessage());
            }
        }).orElseThrow(() -> {
            logger.error("Subconcept not found for update with ID: {}", subconceptId);
            return new RuntimeException("Subconcept not found with ID: " + subconceptId);
        });
    }

 

    @Override
    @CacheEvict(value = {"subconcepts", "subconceptDTOs"}, allEntries = true)
    public Map<String, Object> updateSubconceptsCSV(MultipartFile file) {
        logger.info("Starting CSV update process for file: {}", file.getOriginalFilename());
        
        Map<String, Object> result = new HashMap<>();
        int updatedCount = 0;
        int failedCount = 0;
        int notFoundCount = 0;
        List<String> failedIds = new ArrayList<>();
        List<String> notFoundIds = new ArrayList<>();
        List<String> updateLogs = new ArrayList<>();

        if (file.isEmpty()) {
            logger.error("Uploaded file is empty");
            throw new RuntimeException("File is empty");
        }

        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            List<String[]> records = reader.readAll();
            
            // Validate header row exists
            if (records.isEmpty()) {
                logger.error("CSV file is empty");
                throw new RuntimeException("CSV file is empty");
            }
            
            logger.info("Processing {} records from CSV file", records.size() - 1);
            
            // Read and validate header row
            String[] headers = records.get(0);
            Map<String, Integer> headerMap = new HashMap<>();
            
            // Create header mapping
            for (int i = 0; i < headers.length; i++) {
                headerMap.put(headers[i].trim().toLowerCase(), i);
            }
            
            // Validate required subconceptId column exists
            if (!headerMap.containsKey("subconceptid")) {
                logger.error("CSV must contain 'subconceptId' column");
                throw new RuntimeException("CSV must contain 'subconceptId' column");
            }
            
            int subconceptIdIndex = headerMap.get("subconceptid");
            logger.debug("Found subconceptId column at index: {}", subconceptIdIndex);
            
            // Process data rows
            for (int i = 1; i < records.size(); i++) {
                String[] record = records.get(i);
                int lineNumber = i + 1;
                
                try {
                    // Skip rows with insufficient data
                    if (record.length <= subconceptIdIndex) {
                        String error = "Line " + lineNumber + ": Missing subconceptId column";
                        logger.warn(error);
                        failedCount++;
                        failedIds.add(error);
                        continue;
                    }
                    
                    String subconceptId = record[subconceptIdIndex].trim();
                    
                    // Skip empty subconcept IDs
                    if (subconceptId.isEmpty()) {
                        String error = "Line " + lineNumber + ": Empty SubconceptId";
                        logger.warn(error);
                        failedCount++;
                        failedIds.add(error);
                        continue;
                    }

                    // Check if subconcept exists
                    Optional<Subconcept> existingSubconceptOpt = subconceptRepository.findById(subconceptId);
                    if (existingSubconceptOpt.isEmpty()) {
                        String error = "SubconceptId: " + subconceptId + " not found";
                        logger.warn(error);
                        notFoundCount++;
                        notFoundIds.add(error);
                        continue;
                    }

                    Subconcept existingSubconcept = existingSubconceptOpt.get();
                    List<String> updatedFields = new ArrayList<>();
                    boolean hasUpdates = false;
                    
                    // Update fields dynamically based on header mapping
                    hasUpdates |= updateField(headerMap, record, "dependency", 
                        existingSubconcept::setDependency, updatedFields);
                    hasUpdates |= updateField(headerMap, record, "showto", 
                        existingSubconcept::setShowTo, updatedFields);
                    hasUpdates |= updateField(headerMap, record, "subconceptdesc", 
                        existingSubconcept::setSubconceptDesc, updatedFields);
                    hasUpdates |= updateField(headerMap, record, "subconceptdesc2", 
                        existingSubconcept::setSubconceptDesc2, updatedFields);
                    hasUpdates |= updateField(headerMap, record, "subconceptgroup", 
                        existingSubconcept::setSubconceptGroup, updatedFields);
                    hasUpdates |= updateField(headerMap, record, "subconceptlink", 
                        existingSubconcept::setSubconceptLink, updatedFields);
                    hasUpdates |= updateField(headerMap, record, "subconcepttype", 
                        existingSubconcept::setSubconceptType, updatedFields);
                    
                    // Update numeric fields with validation
                    if (updateNumericField(headerMap, record, "numquestions", 
                        existingSubconcept::setNumQuestions, updatedFields, subconceptId, failedIds)) {
                        hasUpdates = true;
                    }
                    
                    if (updateNumericField(headerMap, record, "subconceptmaxscore", 
                        existingSubconcept::setSubconceptMaxscore, updatedFields, subconceptId, failedIds)) {
                        hasUpdates = true;
                    }
                    
                    // Save only if there are updates
                    if (hasUpdates) {
                        subconceptRepository.save(existingSubconcept);
                        updatedCount++;
                        String logMessage = "SubconceptId: " + subconceptId + " updated fields: " + String.join(", ", updatedFields);
                        updateLogs.add(logMessage);
                        logger.debug(logMessage);
                    } else {
                        String logMessage = "SubconceptId: " + subconceptId + " - No fields to update (all provided fields were empty)";
                        updateLogs.add(logMessage);
                        logger.debug(logMessage);
                    }
                    
                } catch (Exception e) {
                    String error = "Line " + lineNumber + ": Unexpected error - " + e.getMessage();
                    logger.error(error, e);
                    failedCount++;
                    failedIds.add(error);
                }
            }
            
        } catch (Exception e) {
            logger.error("Failed to process CSV file: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("Failed to process CSV file: " + e.getMessage());
        }

        // Prepare result summary
        result.put("updatedCount", updatedCount);
        result.put("failedCount", failedCount);
        result.put("notFoundCount", notFoundCount);
        result.put("totalProcessed", updatedCount + failedCount + notFoundCount);
        result.put("failedIds", failedIds);
        result.put("notFoundIds", notFoundIds);
        result.put("updateLogs", updateLogs);
        result.put("message", "Update completed. Only non-empty fields were updated, existing data preserved for empty fields.");
        
        logger.info("CSV update completed. Updated: {}, Failed: {}, Not Found: {}, Total processed: {}", 
                   updatedCount, failedCount, notFoundCount, updatedCount + failedCount + notFoundCount);
        return result;
    }
    
    // Helper method to update string fields
    private boolean updateField(Map<String, Integer> headerMap, String[] record, String fieldName, 
                               java.util.function.Consumer<String> setter, List<String> updatedFields) {
        if (headerMap.containsKey(fieldName) && 
            record.length > headerMap.get(fieldName) && 
            !record[headerMap.get(fieldName)].trim().isEmpty()) {
            setter.accept(record[headerMap.get(fieldName)].trim());
            updatedFields.add(fieldName);
            return true;
        }
        return false;
    }
    
    // Helper method to update numeric fields
    private boolean updateNumericField(Map<String, Integer> headerMap, String[] record, String fieldName,
                                     java.util.function.Consumer<Integer> setter, List<String> updatedFields,
                                     String subconceptId, List<String> failedIds) {
        if (headerMap.containsKey(fieldName) && 
            record.length > headerMap.get(fieldName) && 
            !record[headerMap.get(fieldName)].trim().isEmpty()) {
            try {
                int value = Integer.parseInt(record[headerMap.get(fieldName)].trim());
                if (value < 0) {
                    failedIds.add("SubconceptId: " + subconceptId + " failed - " + fieldName + " cannot be negative");
                    return false;
                }
                setter.accept(value);
                updatedFields.add(fieldName);
                return true;
            } catch (NumberFormatException e) {
                failedIds.add("SubconceptId: " + subconceptId + " failed - Invalid number format for " + fieldName);
                return false;
            }
        }
        return false;
    }
    
    @Override
    @CacheEvict(value = {"subconcepts", "subconceptDTOs"}, allEntries = true)
    public void deleteSubconcept(String subconceptId) {
        logger.info("Deleting subconcept with ID: {}", subconceptId);
        
        if (subconceptId == null || subconceptId.trim().isEmpty()) {
            logger.error("Subconcept ID is null or empty");
            throw new IllegalArgumentException("Subconcept ID cannot be null or empty");
        }
        
        if (!subconceptRepository.existsById(subconceptId)) {
            logger.error("Attempted to delete non-existent subconcept with ID: {}", subconceptId);
            throw new RuntimeException("Subconcept not found with ID: " + subconceptId);
        }
        
        try {
            subconceptRepository.deleteById(subconceptId);
            logger.info("Successfully deleted subconcept with ID: {}", subconceptId);
        } catch (Exception e) {
            logger.error("Error deleting subconcept with ID: {}", subconceptId, e);
            throw new RuntimeException("Failed to delete subconcept: " + e.getMessage());
        }
    }
    
    // Convert Sub concept entity to SubconceptResponseDTO
    private SubconceptResponseDTO convertToDTO(Subconcept subconcept) {
        try {
            SubconceptResponseDTO dto = new SubconceptResponseDTO();
            dto.setSubconceptId(subconcept.getSubconceptId());
            dto.setDependency(subconcept.getDependency());
            dto.setSubconceptMaxscore(subconcept.getSubconceptMaxscore());
            dto.setSubconceptDesc(subconcept.getSubconceptDesc());
            dto.setSubconceptGroup(subconcept.getSubconceptGroup());
            dto.setSubconceptType(subconcept.getSubconceptType());
            dto.setSubconceptLink(subconcept.getSubconceptLink());
            return dto;
        } catch (Exception e) {
            logger.error("Error converting subconcept to DTO: {}", subconcept.getSubconceptId(), e);
            throw new RuntimeException("Failed to convert subconcept to DTO: " + e.getMessage());
        }
    }
}