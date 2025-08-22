package com.mindfultalk.growthlab.repository;

import com.mindfultalk.growthlab.model.*;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CohortProgramRepository extends JpaRepository<CohortProgram, Long> {
    
	Optional<CohortProgram> findByCohortCohortId(String cohortId);
	List<CohortProgram> findAllByCohort_CohortId(String cohortId);
	List<CohortProgram> findByProgramProgramId(String programId);
	
	 @Query("SELECT cp.program FROM CohortProgram cp " +
	           "JOIN cp.cohort c " +
	           "WHERE c.organization.organizationId = :organizationId")
	    List<Program> findProgramsByOrganizationId(@Param("organizationId") String organizationId);
	 
	 @Query("SELECT cp FROM CohortProgram cp " +
		       "JOIN FETCH cp.program p " +
		       "JOIN FETCH cp.cohort c " +
		       "WHERE c.organization.organizationId = :organizationId")
		List<CohortProgram> findCohortsByOrganizationId(@Param("organizationId") String organizationId);
	 List<CohortProgram> findByProgramProgramIdAndCohort_Organization_OrganizationId(String programId, String organizationId);
}