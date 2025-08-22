package com.mindfultalk.growthlab.repository;

import com.mindfultalk.growthlab.model.*;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.*;

@Repository
public interface UserSubConceptRepository extends JpaRepository<UserSubConcept, Long> {
 	List<UserSubConcept> findByUser_UserIdAndUnit_UnitId(String userId, String unitId);
	List<UserSubConcept> findAllByUser_UserId(String userId);  
	List<UserSubConcept> findByUser_UserIdAndProgram_ProgramId(String userId, String programId);
	
	Optional<UserSubConcept> findByUser_UserIdAndProgram_ProgramIdAndStage_StageIdAndUnit_UnitIdAndSubconcept_SubconceptId(
		    String userId, String programId, String stageId, String unitId, String subconceptId);
	
	@Query("SELECT MIN(usc.completionDate) " +
		       "FROM UserSubConcept usc " +
		       "WHERE usc.user.userId = :userId AND usc.stage.stageId = :stageId")
		Optional<OffsetDateTime> findEarliestCompletionDateByUserIdAndStageId(@Param("userId") String userId,
		                                                                     @Param("stageId") String stageId);

	@Query("SELECT usc.completionDate FROM UserSubConcept usc " +
	           "WHERE usc.user.userId = :userId AND usc.stage.stageId = :stageId " +
	           "ORDER BY usc.completionDate DESC")
	    List<OffsetDateTime> findCompletionDatesByUserIdAndStageIdOrderByDateDesc(
	        @Param("userId") String userId,
	        @Param("stageId") String stageId
	    );

	    @Query("SELECT MIN(usc.completionDate) FROM UserSubConcept usc " +
	           "WHERE usc.user.userId = :userId AND usc.stage.stageId = :stageId " +
	           "AND usc.completionDate IS NOT NULL")
	    Optional<OffsetDateTime> findFirstCompletionDateByUserIdAndStageId(
	        @Param("userId") String userId,
	        @Param("stageId") String stageId
	    );

	    @Query("SELECT MAX(usc.completionDate) FROM UserSubConcept usc " +
	           "WHERE usc.user.userId = :userId AND usc.stage.stageId = :stageId " +
	           "AND usc.completionDate IS NOT NULL")
	    Optional<OffsetDateTime> findLatestCompletionDateByUserIdAndStageId(
	        @Param("userId") String userId,
	        @Param("stageId") String stageId
	    );
	    @Query("SELECT usc.subconcept.subconceptId FROM UserSubConcept usc WHERE usc.user.userId = :userId AND usc.completionDate IS NOT NULL")
	    Set<String> findCompletedSubconceptIdsByUser_UserId(@Param("userId") String userId);

}
