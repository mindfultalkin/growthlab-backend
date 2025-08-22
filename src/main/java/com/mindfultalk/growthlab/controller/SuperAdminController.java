package com.mindfultalk.growthlab.controller;

import com.mindfultalk.growthlab.model.SuperAdmin;
import com.mindfultalk.growthlab.service.SuperAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/superadmin")
public class SuperAdminController {

    @Autowired
    private SuperAdminService superAdminService;

    @PostMapping("/create")
    public String createSuperAdmin(@RequestBody SuperAdmin superAdmin) {
        SuperAdmin createdSuperAdmin = superAdminService.createSuperAdmin(superAdmin);
        return "SuperAdmin created with UUID: " + createdSuperAdmin.getUuid();
    }
    

      
// superadmin login
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> payload) {
        String userId = payload.get("userId");
        String password = payload.get("password");
        
        // Debug logs
        System.out.println("Received userId: " + userId);
        System.out.println("Received password: " + password);
        
        SuperAdmin superAdmin = superAdminService.findByUserId(userId);
        Map<String, Object> response = new HashMap<>();

        // Log fetched SuperAdmin data
        if (superAdmin != null) {
            System.out.println("SuperAdmin found: " + superAdmin.getUserId());
            System.out.println("SuperAdmin stored password: " + superAdmin.getPassword());
        }

        if (superAdmin != null && superAdminService.verifyPassword(password, superAdmin.getPassword())) {
            response.put("token", "dummyToken"); // Replace with actual token generation if applicable
            response.put("userType", "superAdmin");
            return ResponseEntity.ok(response);
        } else {
            System.out.println("Invalid credentials");
            response.put("error", "Invalid userId or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }
    @PostMapping("/resetpassword")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        String currentPassword = payload.get("currentPassword");
        String newPassword = payload.get("newPassword");

        Map<String, String> response = new HashMap<>();

        // Find the user by username
        SuperAdmin superAdmin = superAdminService.findByUserId(username);

        if (superAdmin == null) {
            response.put("error", "User not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        // Verify current password
        if (!superAdminService.verifyPassword(currentPassword, superAdmin.getPassword())) {
            response.put("error", "Current password is incorrect");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        // Update password
        superAdmin.setPassword(superAdminService.getPasswordEncoder().encode(newPassword));
        superAdminService.updateSuperAdmin(superAdmin);

        response.put("message", "Password reset successfully");
        return ResponseEntity.ok(response);
    }
}