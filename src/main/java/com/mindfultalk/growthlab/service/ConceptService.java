package com.mindfultalk.growthlab.service;

import com.mindfultalk.growthlab.model.*;
import com.mindfultalk.growthlab.dto.*;
import java.util.*;


import org.springframework.web.multipart.MultipartFile;

public interface ConceptService {
    List<Concept> getAllConcepts();
    Optional<Concept> getConceptById(String conceptId);
    Concept createConcept(Concept concept);
    Concept updateConcept(String conceptId, Concept concept);
    void deleteConcept(String conceptId);
    Map<String, Object> bulkUploadConcepts(MultipartFile file);
    // New methods for concept mapping
   // Map<ConceptDTO, List<SubconceptReportStageDTO >> groupSubconceptsByConcept(StageReportStageDTO stageReport);
    Map<ConceptDTO, List<SubconceptReportStageDTO>> groupSubconceptsByConcept(
            StageReportStageDTO stageReport,
            Map<String, ConceptDTO> subconceptConceptMap);
    List<ConceptSummaryDTO> generateConceptSummaries(Map<ConceptDTO, List<SubconceptReportDTO>> conceptMapping);
    //List<ConceptSummaryDTO> getConceptSummariesForStage(String userId, String stageId);
}