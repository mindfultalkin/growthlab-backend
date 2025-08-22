package com.mindfultalk.growthlab.service;

import com.mindfultalk.growthlab.dto.ProgramConceptsMappingResponseDTO;
import com.mindfultalk.growthlab.model.*;

import java.util.*;

import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public interface ProgramConceptsMappingService {
	Optional<ProgramConceptsMappingResponseDTO> getProgramConceptsMappingByUnitId(String userId, String unitId);
	
    List<ProgramConceptsMapping> getAllProgramConceptsMappings();
    
    Optional<ProgramConceptsMapping> getProgramConceptsMappingById(Long programConceptId);
    ProgramConceptsMapping createProgramConceptsMapping(ProgramConceptsMapping programConceptsMapping);
    
    ResponseEntity<Map<String, Object>> bulkUpload(MultipartFile file);
    
    ProgramConceptsMapping updateProgramConceptsMapping(Long programConceptId, ProgramConceptsMapping programConceptsMapping);
    void deleteProgramConceptsMapping(Long programConceptId);

	Map<Concept, List<Subconcept>> getConceptsAndSubconceptsByProgram(String programId);

	List<Concept> getAllConceptsInProgram(String programId);

	Map<String, Object> getConceptsAndUserProgress(String programId, String userId);
}