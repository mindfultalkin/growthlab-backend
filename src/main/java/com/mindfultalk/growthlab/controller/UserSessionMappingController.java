package com.mindfultalk.growthlab.controller;

import com.mindfultalk.growthlab.model.UserSessionMapping;
import com.mindfultalk.growthlab.service.UserSessionMappingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/user-session-mappings")
public class UserSessionMappingController {

    @Autowired
    private UserSessionMappingService userSessionMappingService;

    @GetMapping
    public List<UserSessionMapping> getAllUserSessionMappings() {
        return userSessionMappingService.getAllUserSessionMappings();
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<UserSessionMapping> getUserSessionMappingById(@PathVariable String sessionId) {
        Optional<UserSessionMapping> userSessionMapping = userSessionMappingService.getUserSessionMappingById(sessionId);
        return userSessionMapping.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<UserSessionMapping>> getUserSessionMappingsByUserId(@PathVariable String userId) {
        List<UserSessionMapping> mappings = userSessionMappingService.getUserSessionMappingsByUserId(userId);
        return ResponseEntity.ok(mappings);
    }
    
    @PostMapping
    public UserSessionMapping createUserSessionMapping(@RequestBody UserSessionMapping userSessionMapping) {
        return userSessionMappingService.createUserSessionMapping(userSessionMapping);
    }

    @PutMapping("/{sessionId}")
    public ResponseEntity<UserSessionMapping> updateUserSessionMapping(@PathVariable String sessionId, @RequestBody UserSessionMapping userSessionMapping) {
        return ResponseEntity.ok(userSessionMappingService.updateUserSessionMapping(sessionId, userSessionMapping));
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> deleteUserSessionMapping(@PathVariable String sessionId) {
        userSessionMappingService.deleteUserSessionMapping(sessionId);
        return ResponseEntity.noContent().build();
    }
}