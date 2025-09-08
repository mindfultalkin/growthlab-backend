package com.mindfultalk.growthlab.controller;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import com.mindfultalk.growthlab.dto.*;
import com.mindfultalk.growthlab.model.*;
import com.mindfultalk.growthlab.service.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

@RestController
@RequestMapping("/api/v1/organizations")
public class OrganizationController {

    @Autowired
    private OrganizationService organizationService;

    // Get all organizations
    @GetMapping
    public ResponseEntity<List<Organization>> getAllOrganizations() {
        List<Organization> organizations = organizationService.getAllOrganizations();
        return ResponseEntity.ok(organizations);
    }
    
    @GetMapping("/{organizationId}")
    public ResponseEntity<Organization> getOrganizationById(@PathVariable String organizationId) {
    	Optional<Organization> organizationOpt = organizationService.getOrganizationById(organizationId);
        
        if (organizationOpt.isPresent()) {
            return ResponseEntity.ok(organizationOpt.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/{organizationId}/programs")
    public ResponseEntity<List<Program>> getOrganizationPrograms(@PathVariable String organizationId) {
        List<Program> programs = organizationService.getProgramsByOrganizationId(organizationId);
        return ResponseEntity.ok(programs);
    }
    
    
    @GetMapping("/{organizationId}/programs-with-cohorts")
    public ResponseEntity<Map<String, Object>> getProgramsWithCohortsDTO(@PathVariable String organizationId) {
        try {
            List<ProgramResponseDTO> programCohortsDTOList = organizationService.getProgramsWithCohorts(organizationId);

            // Wrap the response in a "programs" key
            Map<String, Object> response = new HashMap<>();
            response.put("programs", programCohortsDTOList);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Log the exception
            System.err.println("Error fetching programs with cohorts DTO: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    
    // Create a new organization
    @PostMapping("/create")
    public ResponseEntity<?> createOrganization(@RequestBody Organization organization) {
        try {
            Organization createdOrganization = organizationService.createOrganization(organization);
            return new ResponseEntity<>(createdOrganization, HttpStatus.CREATED);
        } catch (DataIntegrityViolationException e) {
            return new ResponseEntity<>(new ErrorResponse("Email already exists", e.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>(new ErrorResponse("Failed to create organization", e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    // Update an existing organization
    @PutMapping("/{organizationId}")
    public ResponseEntity<?> updateOrganization(@PathVariable String organizationId, @RequestBody Organization organization) {
        try {
            Organization updatedOrganization = organizationService.updateOrganization(organizationId, organization);
            return new ResponseEntity<>(updatedOrganization, HttpStatus.OK);
        } catch (EntityNotFoundException e) {
            return new ResponseEntity<>(new ErrorResponse("Organization not found", e.getMessage()), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(new ErrorResponse("Failed to update organization", e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Delete an organization by ID
    @DeleteMapping("/{organizationId}")
    public ResponseEntity<?> deleteOrganization(@PathVariable String organizationId) {
        try {
            organizationService.deleteOrganization(organizationId);
            return ResponseEntity.noContent().build();  // Successful deletion, no content to return
        } catch (EntityNotFoundException e) {
            return new ResponseEntity<>(new ErrorResponse("Organization not found", e.getMessage()), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(new ErrorResponse("Failed to delete organization", e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    // Delete hard organization by Id 
    @DeleteMapping("/{organizationId}/hard")
    public ResponseEntity<?> hardDeleteOrganization(@PathVariable String organizationId) {
        try {
            organizationService.hardDeleteOrganization(organizationId);
            return ResponseEntity.noContent().build();  // 204 No Content
        } catch (EntityNotFoundException e) {
            return new ResponseEntity<>(new ErrorResponse("Organization not found", e.getMessage()), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(new ErrorResponse("Failed to delete organization", e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    // Delete multiple organizations
    @DeleteMapping
    public ResponseEntity<Void> deleteOrganizations(@RequestBody List<String> organizationIds) {
        for (String id : organizationIds) {
            organizationService.deleteOrganization(id);
        }
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> loginData) {
        String adminEmail = loginData.get("organizationAdminEmail");
        String password = loginData.get("orgPassword");

        Organization organization;
        try {
            organization = organizationService.getOrganizationByEmail(adminEmail);
        } catch (IllegalStateException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Server error: multiple organizations found with the same email");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        //if (organization != null && organization.getOrgpassword().equals(password)) {
        	if (organization != null && organizationService.verifyPassword(password, organization.getOrgPassword())) {
            // Login successful
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Login successful");
            response.put("organizationId", organization.getOrganizationId());
            response.put("organizationName", organization.getOrganizationName());
            response.put("userType", "orgAdmin");
            
//            // Generate a secure JWT token
//            String token = jwtTokenUtil.generateToken(organization);
//            response.put("token", token);
            
         // Fetch cohorts and check for reminders
            List<Cohort> cohorts = organizationService.getCohortsByOrganizationId(organization.getOrganizationId());
            OffsetDateTime today = OffsetDateTime.now(ZoneOffset.UTC);
            List<String> cohortReminders = new ArrayList<>();

            for (Cohort cohort : cohorts) {
                if (cohort.getCohortEndDate() != null) {
                    long daysUntilEnd = today.until(cohort.getCohortEndDate(), java.time.temporal.ChronoUnit.DAYS);
                    if (daysUntilEnd > 0 && daysUntilEnd <= 15) {
                        cohortReminders.add("Cohort '" + cohort.getCohortName() + "' ends in " + daysUntilEnd + " day(s).");
                    } else if (daysUntilEnd == 0) {
                        cohortReminders.add("Cohort '" + cohort.getCohortName() + "' ends today.");
                    }
                }
            }

            if (!cohortReminders.isEmpty()) {
                response.put("cohortReminders", cohortReminders);
            }

            
            return ResponseEntity.ok(response);
        } else {
            // Invalid credentials
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Invalid email or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }
    
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String otp = request.get("otp");

        try {
            // First request - send OTP
            if (otp == null) {
                organizationService.sendForgotPasswordOTP(email);
                Map<String, String> response = new HashMap<>();
                response.put("message", "An OTP has been sent to your email address.");
                return ResponseEntity.ok(response);
            } 
            // Second request in same API - verify OTP and send new password
            else {
                String newGeneratedPassword = organizationService.verifyOTPAndGenerateNewPassword(email, otp);
                Map<String, String> response = new HashMap<>();
                response.put("message", "Your password has been reset successfully. A new password has been sent to your email.");
                return ResponseEntity.ok(response);
            }
        } catch (RuntimeException e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String oldPassword = request.get("oldPassword");
        String newPassword = request.get("newPassword");

        try {
            organizationService.resetPasswordWithOldPassword(email, oldPassword, newPassword);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Your password has been changed successfully.");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}