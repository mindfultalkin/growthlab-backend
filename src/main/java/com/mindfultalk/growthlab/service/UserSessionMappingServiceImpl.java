package com.mindfultalk.growthlab.service;

import com.mindfultalk.growthlab.model.UserSessionMapping;
import com.mindfultalk.growthlab.repository.UserSessionMappingRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Service
public class UserSessionMappingServiceImpl implements UserSessionMappingService {

    @Autowired
    private UserSessionMappingRepository userSessionMappingRepository;
    
    private static final Logger logger = LoggerFactory.getLogger(UserSessionMappingServiceImpl.class);

    @Override
    public List<UserSessionMapping> getAllUserSessionMappings() {
        return userSessionMappingRepository.findAll();
    }

    @Override
    public Optional<UserSessionMapping> getUserSessionMappingById(String sessionId) {
        return userSessionMappingRepository.findById(sessionId);
    }
    
    @Override
    public List<UserSessionMapping> getUserSessionMappingsByUserId(String userId) {
        return userSessionMappingRepository.findByUser_UserId(userId);
    }

    @Override
    public List<UserSessionMapping> findActiveSessionsByUserIdAndCohortId(String userId, String cohortId) {
        // This should return all active sessions for the given userId and cohortId
        return userSessionMappingRepository.findByUser_UserIdAndCohort_CohortIdAndSessionEndTimestampIsNull(userId, cohortId);
    }
    
    @Override
    public List<UserSessionMapping> findActiveSessionsForCleanup() {
        // You'll need to add this method to your repository
        return userSessionMappingRepository.findBySessionEndTimestampIsNull();
    }
    
    @Override
    public void invalidateSession(String sessionId) {
        Optional<UserSessionMapping> sessionOpt = userSessionMappingRepository.findById(sessionId);
        if (sessionOpt.isPresent()) {
            UserSessionMapping session = sessionOpt.get();
            session.setSessionEndTimestamp(OffsetDateTime.now(ZoneOffset.UTC));
            userSessionMappingRepository.save(session);
        }
    }
    @Override
    public void invalidateAllActiveSessions(String userId, String cohortId) {
        List<UserSessionMapping> activeSessions = findActiveSessionsByUserIdAndCohortId(userId, cohortId);
        for (UserSessionMapping session : activeSessions) {
            session.setSessionEndTimestamp(OffsetDateTime.now(ZoneOffset.UTC));
            userSessionMappingRepository.save(session);
        }
    }
    @Override
    public UserSessionMapping createUserSessionMapping(UserSessionMapping userSessionMapping) {
        return userSessionMappingRepository.save(userSessionMapping);
    }

    @Override
    public Optional<UserSessionMapping> findBySessionId(String sessionId) {
        return userSessionMappingRepository.findBySessionId(sessionId);
    }
    
    @Override
    public UserSessionMapping updateUserSessionMapping(String sessionId, UserSessionMapping userSessionMapping) {
        return userSessionMappingRepository.findById(sessionId).map(existingMapping -> {
            existingMapping.setSessionEndTimestamp(userSessionMapping.getSessionEndTimestamp());
            existingMapping.setSessionStartTimestamp(userSessionMapping.getSessionStartTimestamp());
            existingMapping.setUuid(userSessionMapping.getUuid());
            existingMapping.setSessionId(userSessionMapping.getSessionId());
            existingMapping.setCohort(userSessionMapping.getCohort());
            existingMapping.setUser(userSessionMapping.getUser());
            return userSessionMappingRepository.save(existingMapping);
        }).orElseThrow(() -> new RuntimeException("UserSessionMapping not found"));
    }

    @Override
    public void deleteUserSessionMapping(String sessionId) {
        userSessionMappingRepository.deleteById(sessionId);
    }
    
 // Add this method to your UserSessionMappingService implementation class

    @Override
    public void invalidateAllUserSessions(String userId) {
        try {
            logger.info("Invalidating all sessions for user: {}", userId);
            
            // Get all active sessions for the user
            List<UserSessionMapping> userSessions = getUserSessionMappingsByUserId(userId);
            
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            
            for (UserSessionMapping session : userSessions) {
                // Only invalidate sessions that are still active (no end timestamp)
                if (session.getSessionEndTimestamp() == null) {
                    session.setSessionEndTimestamp(now);
                    userSessionMappingRepository.save(session);
                    logger.debug("Invalidated session: {} for user: {}", session.getSessionId(), userId);
                }
            }
            
            logger.info("Successfully invalidated all active sessions for user: {}", userId);
            
        } catch (Exception e) {
            logger.error("Error invalidating all sessions for user: {}", userId, e);
            throw new RuntimeException("Failed to invalidate user sessions", e);
        }
    }
}