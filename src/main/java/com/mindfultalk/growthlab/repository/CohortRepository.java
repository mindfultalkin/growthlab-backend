package com.mindfultalk.growthlab.repository;

import com.mindfultalk.growthlab.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface CohortRepository extends JpaRepository<Cohort, String> {

    List<Cohort> findByOrganizationOrganizationId(String organizationId);
    
 // Find cohorts by exact name and organization ID
    List<Cohort> findByCohortNameAndOrganizationOrganizationId(String cohortName, String orgId);
    
    // Custom method to count existing cohorts
    @Query("SELECT COUNT(c) FROM Cohort c WHERE c.cohortName = :cohortName AND c.organization.organizationId = :orgId")
    long countByCohortNameAndOrganization(@Param("cohortName") String cohortName, @Param("orgId") String orgId);
    
    @Query("SELECT c FROM Cohort c WHERE c.cohortEndDate BETWEEN :startDate AND :endDate")
    List<Cohort> findCohortsEndingSoon(@Param("startDate") OffsetDateTime startDate, @Param("endDate") OffsetDateTime endDate);

    @Query("SELECT c, DATE_PART('day', c.cohortEndDate - CURRENT_DATE) as daysToEnd " +
    	       "FROM Cohort c WHERE c.cohortEndDate > CURRENT_DATE")
    	List<Object[]> findCohortsWithDaysToEnd();

}