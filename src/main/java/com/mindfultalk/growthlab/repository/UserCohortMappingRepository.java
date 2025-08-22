package com.mindfultalk.growthlab.repository;

import com.mindfultalk.growthlab.model.*;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public interface UserCohortMappingRepository extends JpaRepository<UserCohortMapping, Integer> {
	
	List<UserCohortMapping> findAllByCohortCohortId(String cohortId);
	List<UserCohortMapping> findByCohortCohortId(String cohortId);
	List<UserCohortMapping> findByCohort(Cohort cohort);
	Optional<UserCohortMapping> findByUser_UserIdAndCohort_CohortId(String userId, String cohortId);
	boolean existsByUser_UserIdAndCohort_CohortId(String userId, String cohortId);

    Optional<UserCohortMapping> findByUserUserId(String userId);

    List<UserCohortMapping> findAllByUserUserId(String userId);
    void deleteByUser_UserIdAndCohort_CohortId(String userId, String cohortId);
    void deleteByUserUserId(String userId);
    // Count the number of users in a specific cohort
    int countByCohortCohortId(String cohortId);
 // Add this method to your repository
    @Query("SELECT u FROM UserCohortMapping u WHERE u.user.userId = :userId AND u.cohort.cohortId IN " +
           "(SELECT cp.cohort.cohortId FROM CohortProgram cp WHERE cp.program.programId = :programId)")
    Optional<UserCohortMapping> findByUserUserIdAndProgramId(@Param("userId") String userId, @Param("programId") String programId);
 // Updated method to check if user is active in any cohort using status field instead of isActive property
    boolean existsByUserUserIdAndStatusEquals(String userId, String status);
    
    // Find all active users in a cohort
    List<UserCohortMapping> findByCohortCohortIdAndStatusEquals(String cohortId, String status);
    
    // Count Active users in a cohort
    @Query("SELECT COUNT(u) FROM UserCohortMapping u WHERE u.cohort.cohortId = :cohortId AND u.status = 'ACTIVE'")
    int countActiveByCohortCohortId(@Param("cohortId") String cohortId);
    
    // Find all active mappings for a user
    List<UserCohortMapping> findByUserUserIdAndStatusEquals(String userId, String status);
}
