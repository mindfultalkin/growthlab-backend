package com.mindfultalk.growthlab.service;

import com.mindfultalk.growthlab.dto.*;
import com.mindfultalk.growthlab.model.*;

import java.time.OffsetDateTime;
import java.util.*;

public interface OrganizationService {
    
    // Basic CRUD operations
    List<Organization> getAllOrganizations();
    Optional<Organization> getOrganizationById(String organizationId);
    Organization getOrganizationByEmail(String email);
    Organization createOrganization(Organization organization);
    Organization updateOrganization(String organizationId, Organization updatedOrganization);
    void deleteOrganization(String organizationId);
    
    // Bulk operations
    List<Organization> createOrganizations(List<Organization> organizations);
    
    // Password related operations
    void sendForgotPasswordOTP(String email);
    String verifyOTPAndGenerateNewPassword(String email, String otp);
    void resetPasswordWithOldPassword(String email, String oldPassword, String newPassword);
    boolean verifyPassword(String plainPassword, String encodedPassword);
    
    // Organization-specific business operations
    List<Program> getProgramsByOrganizationId(String organizationId);
    List<Cohort> getCohortsByOrganizationId(String organizationId);
    List<ProgramResponseDTO> getProgramsWithCohorts(String organizationId);
    
    // Cohort date calculations
    long calculateDaysToEnd(OffsetDateTime cohortEndDate);
    Map<String, Object> getCohortDetailsWithDaysToEnd(String cohortId);
    
    // Notification methods
    void notifyCohortEndDates();
}