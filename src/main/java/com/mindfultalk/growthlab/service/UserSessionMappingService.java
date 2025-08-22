package com.mindfultalk.growthlab.service;

import com.mindfultalk.growthlab.model.UserSessionMapping;

import java.util.List;
import java.util.Optional;

public interface UserSessionMappingService {
	
    List<UserSessionMapping> getAllUserSessionMappings();
    Optional<UserSessionMapping> getUserSessionMappingById(String sessionId);
    List<UserSessionMapping> getUserSessionMappingsByUserId(String userId);
    
    Optional<UserSessionMapping> findBySessionId(String sessionId);
  //  List<UserSessionMapping> findActiveSessionByUserIdAndCohortId(String userId, String cohortId);
    List<UserSessionMapping> findActiveSessionsByUserIdAndCohortId(String userId, String cohortId);
    void invalidateSession(String sessionId);
    void invalidateAllActiveSessions(String userId, String cohortId);
    UserSessionMapping createUserSessionMapping(UserSessionMapping userSessionMapping);
    UserSessionMapping updateUserSessionMapping(String sessionId, UserSessionMapping userSessionMapping);
    void deleteUserSessionMapping(String sessionId);
    void invalidateAllUserSessions(String userId);
    List<UserSessionMapping> findActiveSessionsForCleanup();
}