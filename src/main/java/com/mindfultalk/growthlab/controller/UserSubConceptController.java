package com.mindfultalk.growthlab.controller;

import com.mindfultalk.growthlab.model.UserSubConcept;
import com.mindfultalk.growthlab.service.UserSubConceptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/userSubConceptsCompletion")
public class UserSubConceptController {

    @Autowired
    private UserSubConceptService userSubConceptService;

    @PostMapping
    public ResponseEntity<UserSubConcept> createUserSubConcept(@RequestBody UserSubConcept userSubConcept) {
        UserSubConcept createdUserSubConcept = userSubConceptService.createUserSubConcept(userSubConcept);
        return ResponseEntity.ok(createdUserSubConcept);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserSubConcept> getUserSubConceptById(@PathVariable Long id) {
        UserSubConcept userSubConcept = userSubConceptService.getUserSubConceptById(id);
        return ResponseEntity.ok(userSubConcept);
    }
    

    @GetMapping
    public ResponseEntity<List<UserSubConcept>> getAllUserSubConcepts() {
        List<UserSubConcept> userSubConcepts = userSubConceptService.getAllUserSubConcepts();
        return ResponseEntity.ok(userSubConcepts);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserSubConcept> updateUserSubConcept(@PathVariable Long id, @RequestBody UserSubConcept userSubConcept) {
        UserSubConcept updatedUserSubConcept = userSubConceptService.updateUserSubConcept(id, userSubConcept);
        return ResponseEntity.ok(updatedUserSubConcept);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUserSubConcept(@PathVariable Long id) {
        userSubConceptService.deleteUserSubConcept(id);
        return ResponseEntity.noContent().build();
    }
    

}