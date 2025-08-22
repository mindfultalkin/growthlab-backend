package com.mindfultalk.growthlab.controller;

import com.mindfultalk.growthlab.model.ProgramSubscription;
import com.mindfultalk.growthlab.service.ProgramSubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/subscriptions")
public class ProgramSubscriptionController {

    @Autowired
    private ProgramSubscriptionService programSubscriptionService;

    // Create a new Program Subscription
    @PostMapping
    public ResponseEntity<ProgramSubscription> createProgramSubscription(@RequestBody ProgramSubscription subscription) {
        ProgramSubscription createdSubscription = programSubscriptionService.createProgramSubscription(subscription);
        return new ResponseEntity<>(createdSubscription, HttpStatus.CREATED);
    }

    // Update an existing Program Subscription
    @PutMapping("/{subscriptionId}")
    public ResponseEntity<ProgramSubscription> updateProgramSubscription(@PathVariable Long subscriptionId,
                                                                          @RequestBody ProgramSubscription subscriptionDetails) {
        ProgramSubscription updatedSubscription = programSubscriptionService.updateProgramSubscription(subscriptionId, subscriptionDetails);
        return updatedSubscription != null ?
                new ResponseEntity<>(updatedSubscription, HttpStatus.OK) :
                new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    // Get a Program Subscription by ID
    @GetMapping("/{subscriptionId}")
    public ResponseEntity<ProgramSubscription> getProgramSubscription(@PathVariable Long subscriptionId) {
        Optional<ProgramSubscription> subscription = programSubscriptionService.getProgramSubscription(subscriptionId);
        return subscription.isPresent() ?
                new ResponseEntity<>(subscription.get(), HttpStatus.OK) :
                new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    // Get all Program Subscriptions
    @GetMapping
    public ResponseEntity<List<ProgramSubscription>> getAllProgramSubscriptions() {
        List<ProgramSubscription> subscriptions = programSubscriptionService.getAllProgramSubscriptions();
        return new ResponseEntity<>(subscriptions, HttpStatus.OK);
    }

    // Get Program Subscriptions by Organization ID
    @GetMapping("/organization/{organizationId}")
    public ResponseEntity<List<ProgramSubscription>> getProgramSubscriptionsByOrganization(@PathVariable String organizationId) {
        List<ProgramSubscription> subscriptions = programSubscriptionService.getProgramSubscriptionsByOrganization(organizationId);
        return new ResponseEntity<>(subscriptions, HttpStatus.OK);
    }

    // Delete a Program Subscription by ID
    @DeleteMapping("/{subscriptionId}")
    public ResponseEntity<Void> deleteProgramSubscription(@PathVariable Long subscriptionId) {
        programSubscriptionService.deleteProgramSubscription(subscriptionId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}