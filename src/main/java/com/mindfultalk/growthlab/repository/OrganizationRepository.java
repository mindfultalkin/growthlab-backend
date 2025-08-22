package com.mindfultalk.growthlab.repository;

import com.mindfultalk.growthlab.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, String> {
    
    // Custom query to find Organization by admin email
    Organization findByOrganizationAdminEmail(String email);
    Organization findByOrganizationNameAndOrganizationAdminEmail(String organizationName, String email);
    boolean existsByOrganizationAdminEmail(String email);
    
}