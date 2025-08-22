package com.mindfultalk.growthlab.service;

import com.mindfultalk.growthlab.model.*;
import com.mindfultalk.growthlab.repository.*;
import com.mindfultalk.growthlab.dto.*;

import org.slf4j.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

@Service
public class ConceptServiceImpl implements ConceptService {

    private static final Logger logger = LoggerFactory.getLogger(ConceptServiceImpl.class);

    @Autowired
    private ConceptRepository conceptRepository;

    @Autowired
    private ContentMasterRepository contentRepository;

    @Override
    @Cacheable(value = "concepts", key = "'all_concepts'")
    public List<Concept> getAllConcepts() {
        logger.info("Fetching all concepts from database - cache miss");
        return conceptRepository.findAll();
    }

    @Override
    @Cacheable(value = "concept", key = "#conceptId")
    public Optional<Concept> getConceptById(String conceptId) {
        logger.info("Fetching concept by ID from database - cache miss: {}", conceptId);
        return conceptRepository.findById(conceptId);
    }

    @Override
    @CacheEvict(value = {"concepts", "conceptSummaries", "conceptMappings"}, allEntries = true)
    @CachePut(value = "concept", key = "#result.conceptId", condition = "#result != null")
    public Concept createConcept(Concept concept) {
        logger.info("Creating new concept: {}", concept.getConceptId());
        Concept savedConcept = conceptRepository.save(concept);
        logger.info("Successfully created concept: {}", savedConcept.getConceptId());
        return savedConcept;
    }

    @Override
    @CacheEvict(value = {"concepts", "concept", "conceptSummaries", "conceptMappings"}, allEntries = true)
    public Map<String, Object> bulkUploadConcepts(MultipartFile file) {
        logger.info("Starting bulk upload of concepts from file: {}", file.getOriginalFilename());
        
        Map<String, Object> response = new HashMap<>();
        List<String> successIds = new ArrayList<>();
        List<String> failedIds = new ArrayList<>();
        List<Concept> conceptsToSave = new ArrayList<>();
        int totalInserted = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            Set<String> conceptIdsSeenInFile = new HashSet<>();
            
            // Skip the header row
            reader.readLine(); 
            
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(",");
                
                // Validate minimum required fields
                if (fields.length < 6) {
                    failedIds.add("Row with insufficient columns: " + line);
                    continue;
                }
                
                // Assuming CSV columns are: conceptId, conceptName, conceptDesc, conceptSkill1, conceptSkill2, contentId
                String conceptId = fields[0].trim();
                String conceptName = fields[1].trim();
                String conceptDesc = fields[2].trim();
                String conceptSkill1 = fields[3].trim();
                String conceptSkill2 = fields[4].trim();
                String contentId = fields[5].trim();

                // Skip duplicate conceptIds within the same file
                if (conceptIdsSeenInFile.contains(conceptId)) {
                    failedIds.add(conceptId + " (duplicate in file)");
                    continue;
                }

                conceptIdsSeenInFile.add(conceptId);

                // Check if the conceptId already exists in the database
                if (conceptRepository.existsById(conceptId)) {
                    failedIds.add(conceptId + " (already exists)");
                    continue;
                }

                // Fetch the associated ContentMaster by contentId
                try {
                    Integer contentIdInteger = Integer.parseInt(contentId);
                    Optional<ContentMaster> contentOptional = contentRepository.findById(contentIdInteger);
                    if (!contentOptional.isPresent()) {
                        failedIds.add(conceptId + " (invalid contentId: " + contentId + ")");
                        continue;
                    }

                    // Create a new Concept entity
                    ContentMaster content = contentOptional.get();
                    Concept concept = new Concept(conceptId, conceptName, conceptDesc, conceptSkill1, conceptSkill2, UUID.randomUUID(), content);

                    // Add to batch for bulk save
                    conceptsToSave.add(concept);
                    successIds.add(conceptId);

                } catch (NumberFormatException e) {
                    failedIds.add(conceptId + " (contentId not a valid number: " + contentId + ")");
                    continue;
                } catch (Exception e) {
                    failedIds.add(conceptId + " (error: " + e.getMessage() + ")");
                    continue;
                }
            }

            // Bulk save all valid concepts
            if (!conceptsToSave.isEmpty()) {
                List<Concept> savedConcepts = conceptRepository.saveAll(conceptsToSave);
                totalInserted = savedConcepts.size();
                logger.info("Successfully bulk inserted {} concepts", totalInserted);
            }

            // Build the response
            response.put("successfulIds", successIds);
            response.put("failedIds", failedIds);
            response.put("totalInserted", totalInserted);
            response.put("totalFailed", failedIds.size());
            response.put("totalProcessed", successIds.size() + failedIds.size());

        } catch (Exception e) {
            logger.error("Error during bulk upload of concepts", e);
            throw new RuntimeException("Error reading CSV file: " + e.getMessage(), e);
        }

        logger.info("Bulk upload completed. Inserted: {}, Failed: {}", totalInserted, failedIds.size());
        return response;
    }
    
    @Override
    @Caching(
        put = @CachePut(value = "concept", key = "#conceptId"),
        evict = @CacheEvict(value = {"concepts", "conceptSummaries", "conceptMappings"}, allEntries = true)    )
    public Concept updateConcept(String conceptId, Concept concept) {
        logger.info("Updating concept: {}", conceptId);
        return conceptRepository.findById(conceptId).map(existingConcept -> {
            existingConcept.setConceptDesc(concept.getConceptDesc());
            existingConcept.setConceptSkill1(concept.getConceptSkill1());
            existingConcept.setConceptSkill2(concept.getConceptSkill2());
            existingConcept.setContent(concept.getContent());
            Concept updatedConcept = conceptRepository.save(existingConcept);
            logger.info("Successfully updated concept: {}", conceptId);
            return updatedConcept;
        }).orElseThrow(() -> {
            logger.error("Concept not found for update: {}", conceptId);
            return new RuntimeException("Concept not found with ID: " + conceptId);
        });
    }

    @Override
    @CacheEvict(value = {"concepts", "concept", "conceptSummaries", "conceptMappings"}, allEntries = true)
    public void deleteConcept(String conceptId) {
        logger.info("Deleting concept: {}", conceptId);
        if (!conceptRepository.existsById(conceptId)) {
            logger.error("Concept not found for deletion: {}", conceptId);
            throw new RuntimeException("Concept not found with ID: " + conceptId);
        }
        conceptRepository.deleteById(conceptId);
        logger.info("Successfully deleted concept: {}", conceptId);
    }
    
    /**
     * Groups subconcepts by their parent concepts for a given stage
     * @param stageReport The stage report containing units and their subconcepts
     * @return Map of concepts to their associated subconcepts
     */
    @Override
    @Cacheable(value = "conceptMappings", 
               key = "#stageReport.stageId + '_' + #stageReport.hashCode()", 
               condition = "#stageReport != null and #stageReport.stageId != null")
    public Map<ConceptDTO, List<SubconceptReportStageDTO>> groupSubconceptsByConcept(
            StageReportStageDTO stageReport,
            Map<String, ConceptDTO> subconceptConceptMap) {

        logger.info("Grouping subconcepts by concept for stage: {}", 
                    stageReport != null ? stageReport.getStageId() : "null");

        if (stageReport == null || stageReport.getUnits() == null) {
            logger.warn("Stage report or units is null, returning empty map");
            return new HashMap<>();
        }

        Map<ConceptDTO, List<SubconceptReportStageDTO>> conceptMapping = new HashMap<>();

        // Process each unit in the stage
        for (UnitReportStageDTO unit : stageReport.getUnits()) {
            if (unit == null || unit.getSubconcepts() == null) {
                continue;
            }
            
            // Process each subconcept in the unit
            for (SubconceptReportStageDTO subconcept : unit.getSubconcepts()) {
                if (subconcept == null || subconcept.getSubconceptId() == null) {
                    continue;
                }
                
                // Retrieve the associated concept from the lookup using the subconcept ID.
                ConceptDTO concept = subconceptConceptMap.get(subconcept.getSubconceptId());
                if (concept == null) {
                    // Skip if no concept is associated with this subconcept.
                    logger.debug("No concept found for subconcept: {}", subconcept.getSubconceptId());
                    continue;
                }
                // Group subconcepts under their concept.
                conceptMapping.computeIfAbsent(concept, k -> new ArrayList<>()).add(subconcept);
            }
        }

        logger.info("Grouped {} concepts with their subconcepts", conceptMapping.size());
        return conceptMapping;
    }

    /**
     * Creates a summary of concepts and their subconcepts with progress metrics
     * @param conceptMapping Map of concepts to their subconcepts
     * @return List of concept summaries with progress information
     */
    @Override
    @Cacheable(value = "conceptSummaries", 
               key = "#conceptMapping.hashCode()", 
               condition = "#conceptMapping != null and !#conceptMapping.isEmpty()")
    public List<ConceptSummaryDTO> generateConceptSummaries(Map<ConceptDTO, List<SubconceptReportDTO>> conceptMapping) {
        logger.info("Generating concept summaries for {} concepts", 
                   conceptMapping != null ? conceptMapping.size() : 0);

        if (conceptMapping == null || conceptMapping.isEmpty()) {
            logger.warn("Concept mapping is null or empty, returning empty list");
            return new ArrayList<>();
        }

        List<ConceptSummaryDTO> summaries = new ArrayList<>();
        
        for (Map.Entry<ConceptDTO, List<SubconceptReportDTO>> entry : conceptMapping.entrySet()) {
            ConceptSummaryDTO summary = new ConceptSummaryDTO();
            ConceptDTO concept = entry.getKey();
            List<SubconceptReportDTO> subconcepts = entry.getValue();
            
            if (concept == null || subconcepts == null) {
                continue;
            }
            
            summary.setConceptId(concept.getConceptId());
            summary.setConceptName(concept.getConceptName());
            summary.setConceptDesc(concept.getConceptDesc());
            summary.setConceptSkills(Arrays.asList(concept.getConceptSkill1(), concept.getConceptSkill2()));
            summary.setContent(concept.getContent());
            summary.setTotalSubconcepts(subconcepts.size());
            summary.setCompletedSubconcepts((int) subconcepts.stream()
                .filter(SubconceptReportDTO::isCompleted)
                .count());
            summary.setAverageScore(calculateAverageScore(subconcepts));
            summary.setSubconcepts(subconcepts);
            
            summaries.add(summary);
        }
        
        logger.info("Generated {} concept summaries", summaries.size());
        return summaries;
    }
    
    private double calculateAverageScore(List<SubconceptReportDTO> subconcepts) {
        if (subconcepts == null || subconcepts.isEmpty()) {
            return 0.0;
        }
        
        return subconcepts.stream()
            .filter(Objects::nonNull)
            .mapToDouble(SubconceptReportDTO::getHighestScore)
            .filter(score -> score > 0)
            .average()
            .orElse(0.0);
    }

    // Additional utility methods for cache management
    
    /**
     * Manually evict all concept-related caches
     */
    @CacheEvict(value = {"concepts", "concept", "conceptSummaries", "conceptMappings"}, allEntries = true)
    public void evictAllConceptCaches() {
        logger.info("Manually evicting all concept-related caches");
    }

    /**
     * Manually evict specific concept from cache
     */
    @CacheEvict(value = "concept", key = "#conceptId")
    public void evictConceptCache(String conceptId) {
        logger.info("Manually evicting concept cache for: {}", conceptId);
    }
}