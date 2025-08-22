package com.mindfultalk.growthlab.repository;

import com.mindfultalk.growthlab.model.*;

import jakarta.transaction.Transactional;

import java.time.OffsetDateTime;
import java.util.*;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSessionMappingRepository extends JpaRepository<UserSessionMapping, String> {
	Optional<UserSessionMapping> findBySessionId(String sessionId);
	List<UserSessionMapping> findByUser_UserId(String userId);
	List<UserSessionMapping> findByUser_UserIdAndCohort_CohortIdAndSessionEndTimestampIsNull(
		    String userId, String cohortId);
//	Optional<UserSessionMapping> findByUser_UserIdAndCohort_CohortIdAndSessionEndTimestampIsNull(
//		    String userId, String cohortId);
	@Query("SELECT us FROM UserSessionMapping us WHERE us.user.userId = :userId AND us.cohort.cohortId = :cohortId AND us.sessionEndTimestamp IS NULL")
	Optional<UserSessionMapping> findActiveSession(@Param("userId") String userId, @Param("cohortId") String cohortId);

	@Query("SELECT u FROM User u JOIN u.userSessions us WHERE us.sessionStartTimestamp < :timestamp")
	List<User> findInactiveUsersSince(@Param("timestamp") OffsetDateTime timestamp);

    // ===== NEW METHODS FOR SINGLE DEVICE LOGIN =====
    
    /**
     * Find all active sessions for a user across all devices and cohorts
     */
    @Query("SELECT us FROM UserSessionMapping us WHERE us.user.userId = :userId AND us.sessionEndTimestamp IS NULL")
    List<UserSessionMapping> findAllActiveSessionsForUser(@Param("userId") String userId);
    
    /**
     * Find all active sessions for a user in a specific cohort
     */
    @Query("SELECT us FROM UserSessionMapping us WHERE us.user.userId = :userId AND us.cohort.cohortId = :cohortId AND us.sessionEndTimestamp IS NULL")
    List<UserSessionMapping> findAllActiveSessionsForUserInCohort(@Param("userId") String userId, @Param("cohortId") String cohortId);
    
    /**
     * Terminate all active sessions for a user except the current one
     */
    @Modifying
    @Transactional
    @Query("UPDATE UserSessionMapping us SET us.sessionEndTimestamp = :endTime WHERE us.user.userId = :userId AND us.sessionEndTimestamp IS NULL AND us.sessionId != :currentSessionId")
    int terminateAllOtherActiveSessions(@Param("userId") String userId, 
                                       @Param("currentSessionId") String currentSessionId, 
                                       @Param("endTime") OffsetDateTime endTime);
    
    /**
     * Terminate all active sessions for a user in a specific cohort except the current one
     */
    @Modifying
    @Transactional
    @Query("UPDATE UserSessionMapping us SET us.sessionEndTimestamp = :endTime WHERE us.user.userId = :userId AND us.cohort.cohortId = :cohortId AND us.sessionEndTimestamp IS NULL AND us.sessionId != :currentSessionId")
    int terminateAllOtherActiveSessionsInCohort(@Param("userId") String userId, 
                                               @Param("cohortId") String cohortId,
                                               @Param("currentSessionId") String currentSessionId, 
                                               @Param("endTime") OffsetDateTime endTime);
    
    /**
     * Count active sessions for a user
     */
    @Query("SELECT COUNT(us) FROM UserSessionMapping us WHERE us.user.userId = :userId AND us.sessionEndTimestamp IS NULL")
    long countActiveSessionsForUser(@Param("userId") String userId);
    
    @Query("SELECT usm FROM UserSessionMapping usm WHERE usm.sessionEndTimestamp IS NULL")
    List<UserSessionMapping> findBySessionEndTimestampIsNull();

}