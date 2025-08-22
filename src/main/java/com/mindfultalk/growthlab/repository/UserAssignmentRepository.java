package com.mindfultalk.growthlab.repository;

import com.mindfultalk.growthlab.model.*;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.*;

@Repository
public interface UserAssignmentRepository extends JpaRepository<UserAssignment, String> {

    List<UserAssignment> findByUserUserId(String userId);

    List<UserAssignment> findByCohortCohortId(String cohortId);
    
    Optional<UserAssignment> findByUserUserIdAndSubconceptSubconceptId(String userId, String subconceptId);
    
    List<UserAssignment> findByCohortCohortIdAndUserUserId(String cohortId, String userId);
    
    Optional<UserAssignment> findByUserUserIdAndProgramProgramIdAndStageStageIdAndUnitUnitIdAndSubconceptSubconceptId(
    	    String userId, String programId, String stageId, String unitId, String subconceptId
    	);
    
    List<UserAssignment> findBySubmittedDateBetween(OffsetDateTime startDate, OffsetDateTime endDate);

}