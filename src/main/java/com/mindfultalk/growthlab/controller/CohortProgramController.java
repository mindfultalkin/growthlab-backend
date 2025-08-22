package com.mindfultalk.growthlab.controller;

import com.mindfultalk.growthlab.model.CohortProgram;
import com.mindfultalk.growthlab.service.CohortProgramService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/cohortprogram")
public class CohortProgramController {

    @Autowired
    private CohortProgramService cohortProgramService;

    @GetMapping
    public List<CohortProgram> getAllCohortPrograms() {
        return cohortProgramService.getAllCohortPrograms();
    }

    @GetMapping("/{cohortProgramId}")
    public ResponseEntity<CohortProgram> getCohortProgram(@PathVariable Long cohortProgramId) {
        Optional<CohortProgram> cohortProgram = cohortProgramService.getCohortProgram(cohortProgramId);
        return cohortProgram.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/create")
    public ResponseEntity<?> createCohortProgram(@RequestBody CohortProgram cohortProgram) {
        try {
            CohortProgram createdCohortProgram = cohortProgramService.createCohortProgram(cohortProgram);
            return ResponseEntity.ok(createdCohortProgram);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{cohortProgramId}")
    public ResponseEntity<Void> deleteCohortProgram(@PathVariable Long cohortProgramId) {
        cohortProgramService.deleteCohortProgram(cohortProgramId);
        return ResponseEntity.noContent().build();
    }
}