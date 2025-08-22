package com.mindfultalk.growthlab.controller;

import com.mindfultalk.growthlab.exception.*;
import com.mindfultalk.growthlab.model.Cohort;
import com.mindfultalk.growthlab.service.CohortService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cohorts")
public class CohortController {

    @Autowired
    private CohortService cohortService;

    @GetMapping
    public List<Cohort> getAllCohorts() {
        return cohortService.getAllCohorts();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Cohort> getCohortById(@PathVariable String id) {
        return cohortService.getCohortById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/organization/{organizationId}")
    public List<Cohort> getCohortsByOrganizationId(@PathVariable String organizationId) {
        return cohortService.getCohortsByOrganizationId(organizationId);
    }

    @PostMapping("/create")
    public ResponseEntity<?> createCohort(@RequestBody Cohort cohort) {
        try {
            Cohort createdCohort = cohortService.createCohort(cohort);
            return ResponseEntity.ok("Cohort created successfully with ID: " + createdCohort.getCohortId());
        } catch (CohortValidationException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + e.getMessage());
        }
    }


    @PutMapping("/{id}")
    public ResponseEntity<?> updateCohort(@PathVariable String id, @RequestBody Cohort cohort) {
        try {
            Cohort updatedCohort = cohortService.updateCohort(id, cohort);
            return ResponseEntity.ok().body("Cohort updated successfully: " + updatedCohort);
        } catch (CohortValidationException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCohort(@PathVariable String id) {
        try {
            cohortService.deleteCohort(id);
            return ResponseEntity.noContent().build();
        } catch (CohortNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
