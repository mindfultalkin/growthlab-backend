//package com.mindfultalk.growthlab.service;

//import java.time.OffsetDateTime;
//import java.time.ZoneOffset;
//import java.time.temporal.ChronoUnit;
//import java.util.*;
//
//import org.slf4j.*;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.*;
//
//import com.FlowofEnglish.repository.*;
//import com.FlowofEnglish.model.*;
//
//
//@Service
//@Component
//public class SessionCleanupService {
//
//  private static final Logger logger = LoggerFactory.getLogger(SessionCleanupService.class);
//
//  @Autowired
//  private UserSessionMappingService userSessionMappingService;
//
//  @Autowired
//  private UserAttemptsRepository userAttemptsRepository;
//
//  @Autowired
//  private RedisTemplate<String, Object> redisTemplate;
//
//  // Run every 5 minutes to check for expired sessions
//  @Scheduled(fixedRate = 5 * 60 * 1000) // 5 minutes
//  public void cleanupExpiredSessions() {
//      logger.debug("Starting scheduled session cleanup");
//      
//      try {
//          // Get all active sessions (sessions without end timestamp)
//          List<UserSessionMapping> activeSessions = userSessionMappingService.findActiveSessionsForCleanup();
//          
//          for (UserSessionMapping session : activeSessions) {
//              if (isSessionExpired(session)) {
//                  expireSession(session);
//              }
//          }
//          
//          logger.debug("Completed scheduled session cleanup. Processed {} sessions", activeSessions.size());
//      } catch (Exception e) {
//          logger.error("Error during scheduled session cleanup", e);
//      }
//  }
//
//  private boolean isSessionExpired(UserSessionMapping session) {
//      try {
//          OffsetDateTime sessionStart = session.getSessionStartTimestamp();
//          OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
//          
//          // Check maximum session duration (5 minutes for testing)
//          long minutesSinceStart = ChronoUnit.MINUTES.between(sessionStart, now);
//          if (minutesSinceStart >= 5) { // MAX_SESSION_DURATION_MINUTES
//              logger.debug("Session {} exceeded max duration: {} minutes", 
//                         session.getSessionId(), minutesSinceStart);
//              return true;
//          }
//          
//          // Check inactivity timeout
//          OffsetDateTime lastActivity = getLastActivityTime(session);
//          long minutesSinceActivity = ChronoUnit.MINUTES.between(lastActivity, now);
//          
//          if (minutesSinceActivity >= 3) { // SESSION_TIMEOUT_MINUTES
//              logger.debug("Session {} timed out due to inactivity: {} minutes since last activity", 
//                         session.getSessionId(), minutesSinceActivity);
//              return true;
//          }
//          
//          return false;
//      } catch (Exception e) {
//          logger.error("Error checking if session is expired: {}", session.getSessionId(), e);
//          return true; // Err on the side of caution
//      }
//  }
//
//  private OffsetDateTime getLastActivityTime(UserSessionMapping session) {
//      try {
//          // Get user attempts after session start
//          List<UserAttempts> attempts = userAttemptsRepository.findAttemptsByUserId(session.getUser().getUserId());
//          
//          OffsetDateTime lastActivity = session.getSessionStartTimestamp();
//          
//          for (UserAttempts attempt : attempts) {
//              if (attempt.getUserAttemptEndTimestamp() != null &&
//                  attempt.getUserAttemptEndTimestamp().isAfter(session.getSessionStartTimestamp()) &&
//                  attempt.getUserAttemptEndTimestamp().isAfter(lastActivity)) {
//                  lastActivity = attempt.getUserAttemptEndTimestamp();
//              }
//          }
//          
//          return lastActivity;
//      } catch (Exception e) {
//          logger.error("Error getting last activity time for session: {}", session.getSessionId(), e);
//          return session.getSessionStartTimestamp();
//      }
//  }
//
//  private void expireSession(UserSessionMapping session) {
//      try {
//          // Update session end timestamp
//          session.setSessionEndTimestamp(OffsetDateTime.now(ZoneOffset.UTC));
//          userSessionMappingService.updateUserSessionMapping(session.getSessionId(), session);
//          
//          // Clear related caches
//          clearSessionCaches(session);
//          
//          logger.info("Expired session due to cleanup: sessionId={}, userId={}", 
//                     session.getSessionId(), session.getUser().getUserId());
//      } catch (Exception e) {
//          logger.error("Error expiring session: {}", session.getSessionId(), e);
//      }
//  }
//
//  private void clearSessionCaches(UserSessionMapping session) {
//      try {
//          Set<String> keysToDelete = new HashSet<>();
//          String sessionId = session.getSessionId();
//          String userId = session.getUser().getUserId();
//          
//          keysToDelete.add("session:" + sessionId);
//          keysToDelete.add("lastActivity:" + sessionId);
//          keysToDelete.add("userActive:" + userId);
//          keysToDelete.add("sessionValidation:" + sessionId);
//          keysToDelete.add("userData:" + userId);
//          
//          redisTemplate.delete(keysToDelete);
//      } catch (Exception e) {
//          logger.error("Error clearing session caches", e);
//      }
//  }
//}