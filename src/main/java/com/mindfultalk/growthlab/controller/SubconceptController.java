package com.mindfultalk.growthlab.controller;


import com.mindfultalk.growthlab.dto.SubconceptResponseDTO;
import com.mindfultalk.growthlab.model.Subconcept;
import com.mindfultalk.growthlab.service.SubconceptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api/v1/subconcepts")
public class SubconceptController {

    @Autowired
    private SubconceptService subconceptService;

    @GetMapping("/all")
    public List<Subconcept> getAllSubconcept() {
        return subconceptService.getAllSubconcept();
    }
    @GetMapping
    public List<SubconceptResponseDTO> getAllSubconcepts() {
        return subconceptService.getAllSubconcepts();
    }

    @GetMapping("/{subconceptId}")
    public ResponseEntity<SubconceptResponseDTO> getSubconceptById(@PathVariable String subconceptId) {
        Optional<SubconceptResponseDTO> subconcept = subconceptService.getSubconceptById(subconceptId);
        return subconcept.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
    
    @PostMapping("/create")
    public Subconcept createSubconcept(@RequestBody Subconcept subconcept) {
        return subconceptService.createSubconcept(subconcept);
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadSubconcepts(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = subconceptService.uploadSubconceptsCSV(file);
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{subconceptId}")
    public ResponseEntity<Subconcept> updateSubconcept(@PathVariable String subconceptId, @RequestBody Subconcept subconcept) {
        return ResponseEntity.ok(subconceptService.updateSubconcept(subconceptId, subconcept));
    }
    
    @PutMapping("/bulk-update")
    public ResponseEntity<Map<String, Object>> updateSubconceptsBulk(@RequestParam("file") MultipartFile file) {
        try {
            // Validate file
            if (file.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "File is empty");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Check file type
            String contentType = file.getContentType();
            if (contentType == null || (!contentType.equals("text/csv") && !contentType.equals("application/vnd.ms-excel"))) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Invalid file type. Please upload a CSV file.");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            Map<String, Object> response = subconceptService.updateSubconceptsCSV(file);
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "An unexpected error occurred: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @DeleteMapping("/{subconceptId}")
    public ResponseEntity<Void> deleteSubconcept(@PathVariable String subconceptId) {
        subconceptService.deleteSubconcept(subconceptId);
        return ResponseEntity.noContent().build();
    }
}