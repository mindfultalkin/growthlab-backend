package com.mindfultalk.growthlab.controller;

import com.mindfultalk.growthlab.model.*;
import com.mindfultalk.growthlab.service.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;

@RestController
@RequestMapping("/api/v1/programs")
public class ProgramController {

    @Autowired
    private ProgramService programService;
    
    @Autowired
    private ProgramConceptsMappingService programConceptsMappingService;

    @GetMapping
    public List<Program> getAllPrograms() {
        return programService.getAllPrograms();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Program> getProgramById(@PathVariable String id) {
        return programService.getProgramById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/create")
    public Program createProgram(@RequestBody Program program) {
        return programService.createProgram(program);
    }
    
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadProgramsCSV(@RequestParam("file") MultipartFile file) {
        try {
            Map<String, Object> result = programService.uploadProgramsCSV(file);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> errorResponse = Map.of("error", "Failed to upload programs: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }


    @PutMapping("/{id}")
    public ResponseEntity<Program> updateProgram(@PathVariable String id, @RequestBody Program program) {
        try {
            return ResponseEntity.ok(programService.updateProgram(id, program));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProgram(@PathVariable String id) {
        programService.deleteProgram(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/delete")
    public ResponseEntity<Void> deletePrograms(@RequestBody List<String> ids) {
        programService.deletePrograms(ids);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/{programId}/concepts")
    public ResponseEntity<Map<Concept, List<Subconcept>>> getConceptsAndSubconcepts(@PathVariable String programId) {
        Map<Concept, List<Subconcept>> conceptSubconceptMap = programConceptsMappingService.getConceptsAndSubconceptsByProgram(programId);
        return ResponseEntity.ok(conceptSubconceptMap);
    }

    @GetMapping("/{programId}/concepts/list")
    public ResponseEntity<Map<String, Object>> getAllConceptsInProgram(@PathVariable String programId) {
        List<Concept> concepts = programConceptsMappingService.getAllConceptsInProgram(programId);
        Map<String, Object> response = new HashMap<>();
        response.put("count", concepts.size());
        response.put("concepts", concepts);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{programId}/concepts/progress/{userId}")
    public ResponseEntity<Map<String, Object>> getConceptsAndProgress(
            @PathVariable String programId, 
            @PathVariable String userId) {
        Map<String, Object> response = programConceptsMappingService.getConceptsAndUserProgress(programId, userId);
        return ResponseEntity.ok(response);
    }

}