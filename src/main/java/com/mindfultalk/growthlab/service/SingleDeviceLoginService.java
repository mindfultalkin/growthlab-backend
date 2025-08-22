package com.mindfultalk.growthlab.service;

import com.mindfultalk.growthlab.model.UserSessionMapping;
import com.mindfultalk.growthlab.repository.UserSessionMappingRepository;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class SingleDeviceLoginService {
    
    private static final Logger logger = LoggerFactory.getLogger(SingleDeviceLoginService.class);
    
    // Redis key prefixes for single device login management
    private static final String ACTIVE_SESSION_PREFIX = "activeSession:";
    private static final String USER_DEVICE_PREFIX = "userDevice:";
    private static final String DEVICE_FINGERPRINT_PREFIX = "deviceFingerprint:";
    private static final String USER_LOGIN_ATTEMPT_PREFIX = "loginAttempt:";
    
    @Autowired
    private UserSessionMappingRepository userSessionMappingRepository;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * Enable/Disable single device login feature
     * Set this to false if you want to allow multiple device logins
     */
    private static final boolean SINGLE_DEVICE_LOGIN_ENABLED = true;
    
    /**
     * Enable/Disable forced logout on new device login
     * Set this to false if you want to just prevent new logins instead of forcing logout
     */
    private static final boolean FORCE_LOGOUT_ON_NEW_DEVICE = true;
    
    /**
     * Check if single device login is enabled
     */
    public boolean isSingleDeviceLoginEnabled() {
        return SINGLE_DEVICE_LOGIN_ENABLED;
    }
    
    /**
     * Check if forced logout on new device is enabled
     */
    public boolean isForcedLogoutEnabled() {
        return FORCE_LOGOUT_ON_NEW_DEVICE;
    }
    
    /**
     * Handle single device login logic during user login
     * This method should be called after successful authentication but before creating the new session
     * 
     * @param userId - User ID attempting to login
     * @param cohortId - Cohort ID for the session (can be null for initial login)
     * @param deviceFingerprint - Device identifier (IP + User-Agent hash or similar)
     * @return SingleDeviceLoginResult containing information about terminated sessions
     */
    @Transactional
    public SingleDeviceLoginResult handleSingleDeviceLogin(String userId, String cohortId, String deviceFingerprint) {
        if (!SINGLE_DEVICE_LOGIN_ENABLED) {
            return SingleDeviceLoginResult.success("Multiple device login allowed", 0);
        }
        
        try {
            logger.info("Checking for existing active sessions - userId: {}, cohortId: {}, device: {}", userId, cohortId, deviceFingerprint);
            
            // Find all active sessions for the user
            List<UserSessionMapping> activeSessions = userSessionMappingRepository.findAllActiveSessionsForUser(userId);
            
            if (activeSessions.isEmpty()) {
                logger.debug("No existing active sessions found for user: {}", userId);
                return SingleDeviceLoginResult.success("No existing sessions to terminate", 0);
            }
            
            // Check if user is trying to login from the same device
            if (FORCE_LOGOUT_ON_NEW_DEVICE && isSameDevice(userId, deviceFingerprint)) {
                logger.info("Same device login detected for user: {}", userId);
                return SingleDeviceLoginResult.success("Same device login - no sessions terminated", 0);
            }
            
            // Log existing sessions for debugging
            logger.info("Found {} active session(s) for user: {}", activeSessions.size(), userId);
            for (UserSessionMapping session : activeSessions) {
                logger.debug("Existing session - ID: {}, Cohort: {}, Start: {}", 
                    session.getSessionId(), 
                    session.getCohort() != null ? session.getCohort().getCohortId() : "null", 
                    session.getSessionStartTimestamp());
            }
            
            // Terminate all existing active sessions
            OffsetDateTime terminationTime = OffsetDateTime.now(ZoneOffset.UTC);
            int terminatedCount = 0;
            
            for (UserSessionMapping session : activeSessions) {
                session.setSessionEndTimestamp(terminationTime);
                userSessionMappingRepository.save(session);
                
                // Clear session from cache
                clearSessionFromCache(session.getSessionId(), userId);
                terminatedCount++;
                
                logger.info("Terminated existing session - sessionId: {}, userId: {}, cohortId: {}", 
                    session.getSessionId(), userId, 
                    session.getCohort() != null ? session.getCohort().getCohortId() : "null");
            }
            
            // Clear user's device tracking from cache
            clearUserDeviceCache(userId);
            
            // Store new device fingerprint
            if (deviceFingerprint != null) {
                storeDeviceFingerprint(userId, deviceFingerprint);
            }
            
            String message = String.format("Terminated %d existing session(s) for single device login", terminatedCount);
            if (FORCE_LOGOUT_ON_NEW_DEVICE) {
                message += " - Previous device has been logged out";
            }
            
            return SingleDeviceLoginResult.success(message, terminatedCount);
            
        } catch (Exception e) {
            logger.error("Error handling single device login for userId: {}", userId, e);
            return SingleDeviceLoginResult.error("Failed to handle single device login: " + e.getMessage());
        }
    }
    
    /**
     * Overloaded method for backward compatibility
     */
    @Transactional
    public SingleDeviceLoginResult handleSingleDeviceLogin(String userId, String cohortId) {
        return handleSingleDeviceLogin(userId, cohortId, null);
    }
    
    /**
     * Check if the login attempt is from the same device
     */
    private boolean isSameDevice(String userId, String deviceFingerprint) {
        if (deviceFingerprint == null) {
            return false;
        }
        
        try {
            String deviceKey = DEVICE_FINGERPRINT_PREFIX + userId;
            String storedFingerprint = (String) redisTemplate.opsForValue().get(deviceKey);
            return deviceFingerprint.equals(storedFingerprint);
        } catch (Exception e) {
            logger.warn("Error checking device fingerprint: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Store device fingerprint for the user
     */
    private void storeDeviceFingerprint(String userId, String deviceFingerprint) {
        try {
            String deviceKey = DEVICE_FINGERPRINT_PREFIX + userId;
            redisTemplate.opsForValue().set(deviceKey, deviceFingerprint, 8, TimeUnit.HOURS);
            logger.debug("Stored device fingerprint for userId: {}", userId);
        } catch (Exception e) {
            logger.warn("Error storing device fingerprint: {}", e.getMessage());
        }
    }
    
    /**
     * Check if user can login from this device
     * This method can be used to prevent login instead of forcing logout
     */
    public DeviceLoginCheckResult checkDeviceLoginPermission(String userId, String deviceFingerprint) {
        if (!SINGLE_DEVICE_LOGIN_ENABLED) {
            return DeviceLoginCheckResult.allowed("Multiple device login enabled");
        }
        
        try {
            // Find all active sessions for the user
            List<UserSessionMapping> activeSessions = userSessionMappingRepository.findAllActiveSessionsForUser(userId);
            
            if (activeSessions.isEmpty()) {
                return DeviceLoginCheckResult.allowed("No active sessions found");
            }
            
            // Check if it's the same device
            if (isSameDevice(userId, deviceFingerprint)) {
                return DeviceLoginCheckResult.allowed("Same device login");
            }
            
            // If forced logout is disabled, prevent new login
            if (!FORCE_LOGOUT_ON_NEW_DEVICE) {
                return DeviceLoginCheckResult.denied(
                    "You are already logged in on another device. Please logout from that device first or contact support if you need assistance.",
                    "ALREADY_LOGGED_IN_ELSEWHERE"
                );
            }
            
            // If forced logout is enabled, allow but indicate sessions will be terminated
            return DeviceLoginCheckResult.allowedWithTermination(
                "Your previous session on another device will be terminated.",
                activeSessions.size()
            );
            
        } catch (Exception e) {
            logger.error("Error checking device login permission for userId: {}", userId, e);
            return DeviceLoginCheckResult.error("Unable to verify device login permission");
        }
    }
    
    /**
     * Register a new active session in cache
     */
    public void registerActiveSession(String userId, String sessionId, String cohortId) {
        registerActiveSession(userId, sessionId, cohortId, null);
    }
    
    /**
     * Register a new active session in cache with device fingerprint
     */
    public void registerActiveSession(String userId, String sessionId, String cohortId, String deviceFingerprint) {
        if (!SINGLE_DEVICE_LOGIN_ENABLED) {
            return;
        }
        
        try {
            String activeSessionKey = ACTIVE_SESSION_PREFIX + userId;
            String deviceKey = USER_DEVICE_PREFIX + userId;
            
            // Store active session info
            SessionInfo sessionInfo = new SessionInfo(sessionId, cohortId, OffsetDateTime.now(ZoneOffset.UTC));
            redisTemplate.opsForValue().set(activeSessionKey, sessionInfo, 8, TimeUnit.HOURS);
            
            // Store device info
            redisTemplate.opsForValue().set(deviceKey, sessionId, 8, TimeUnit.HOURS);
            
            // Store device fingerprint if provided
            if (deviceFingerprint != null) {
                storeDeviceFingerprint(userId, deviceFingerprint);
            }
            
            logger.debug("Registered active session in cache - userId: {}, sessionId: {}", userId, sessionId);
            
        } catch (Exception e) {
            logger.warn("Error registering active session in cache: {}", e.getMessage());
        }
    }
    
    /**
     * Validate session for single device login in filter
     */
    public SessionDeviceValidationResult validateSessionDevice(String userId, String sessionId, String currentDeviceFingerprint) {
        if (!SINGLE_DEVICE_LOGIN_ENABLED) {
            return SessionDeviceValidationResult.valid();
        }
        
        try {
            // Check if session is registered as active
            String activeSessionKey = ACTIVE_SESSION_PREFIX + userId;
            SessionInfo sessionInfo = (SessionInfo) redisTemplate.opsForValue().get(activeSessionKey);
            
            if (sessionInfo == null) {
                return SessionDeviceValidationResult.invalid("Session not found in active sessions", "SESSION_NOT_ACTIVE");
            }
            
            if (!sessionInfo.getSessionId().equals(sessionId)) {
                return SessionDeviceValidationResult.invalid("This session has been terminated because you logged in from another device", "SESSION_TERMINATED_NEW_LOGIN");
            }
            
            // Check device fingerprint if available
            if (currentDeviceFingerprint != null && !isSameDevice(userId, currentDeviceFingerprint)) {
                return SessionDeviceValidationResult.invalid("Session terminated - login detected from different device", "DEVICE_MISMATCH");
            }
            
            return SessionDeviceValidationResult.valid();
            
        } catch (Exception e) {
            logger.error("Error validating session device for userId: {}, sessionId: {}", userId, sessionId, e);
            return SessionDeviceValidationResult.invalid("Unable to validate session device", "VALIDATION_ERROR");
        }
    }
    
    /**
     * Create device fingerprint from request info
     */
    public String createDeviceFingerprint(String userAgent, String remoteAddr) {
        if (userAgent == null) userAgent = "unknown";
        if (remoteAddr == null) remoteAddr = "unknown";
        
        // Simple fingerprint - in production, you might want more sophisticated device identification
        return Integer.toString((userAgent + "|" + remoteAddr).hashCode());
    }
    
    /**
     * Create device fingerprint from detailed request info (overloaded method)
     */
    public String createDeviceFingerprint(String userAgent, String remoteAddr, String acceptLanguage, String acceptEncoding) {
        if (userAgent == null) userAgent = "unknown";
        if (remoteAddr == null) remoteAddr = "unknown";
        if (acceptLanguage == null) acceptLanguage = "unknown";
        if (acceptEncoding == null) acceptEncoding = "unknown";
        
        // More detailed fingerprint using multiple headers
        String fingerprint = userAgent + "|" + remoteAddr + "|" + acceptLanguage + "|" + acceptEncoding;
        return Integer.toString(fingerprint.hashCode());
    }
    
    /**
     * Clear session from cache
     */
    private void clearSessionFromCache(String sessionId, String userId) {
        try {
            Set<String> keysToDelete = Set.of(
                ACTIVE_SESSION_PREFIX + userId,
                USER_DEVICE_PREFIX + userId,
                "session:" + sessionId,
                "lastActivity:" + sessionId,
                "sessionValidation:" + sessionId
            );
            
            redisTemplate.delete(keysToDelete);
            logger.debug("Cleared session cache for sessionId: {}, userId: {}", sessionId, userId);
            
        } catch (Exception e) {
            logger.warn("Error clearing session cache: {}", e.getMessage());
        }
    }
    
    /**
     * Clear user device cache
     */
    private void clearUserDeviceCache(String userId) {
        try {
            Set<String> keysToDelete = Set.of(
                ACTIVE_SESSION_PREFIX + userId,
                USER_DEVICE_PREFIX + userId,
                DEVICE_FINGERPRINT_PREFIX + userId,
                "userData:" + userId,
                "userActive:" + userId
            );
            
            redisTemplate.delete(keysToDelete);
            logger.debug("Cleared user device cache for userId: {}", userId);
            
        } catch (Exception e) {
            logger.warn("Error clearing user device cache: {}", e.getMessage());
        }
    }
    
    /**
     * Get active session count for a user
     */
    public long getActiveSessionCount(String userId) {
        try {
            return userSessionMappingRepository.countActiveSessionsForUser(userId);
        } catch (Exception e) {
            logger.error("Error getting active session count for userId: {}", userId, e);
            return 0;
        }
    }
    
    /**
     * Validate if user can create a new session
     */
    public boolean canCreateNewSession(String userId) {
        if (!SINGLE_DEVICE_LOGIN_ENABLED) {
            return true;
        }
        
        try {
            long activeSessionCount = getActiveSessionCount(userId);
            logger.debug("Active session count for userId {}: {}", userId, activeSessionCount);
            return activeSessionCount == 0 || FORCE_LOGOUT_ON_NEW_DEVICE;
        } catch (Exception e) {
            logger.error("Error validating session creation for userId: {}", userId, e);
            return false;
        }
    }
    
    // Result classes
    public static class SingleDeviceLoginResult {
        private final boolean success;
        private final String message;
        private final int terminatedSessionCount;
        private final String errorMessage;
        
        private SingleDeviceLoginResult(boolean success, String message, int terminatedSessionCount, String errorMessage) {
            this.success = success;
            this.message = message;
            this.terminatedSessionCount = terminatedSessionCount;
            this.errorMessage = errorMessage;
        }
        
        public static SingleDeviceLoginResult success(String message, int terminatedCount) {
            return new SingleDeviceLoginResult(true, message, terminatedCount, null);
        }
        
        public static SingleDeviceLoginResult error(String errorMessage) {
            return new SingleDeviceLoginResult(false, null, 0, errorMessage);
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public int getTerminatedSessionCount() { return terminatedSessionCount; }
        public String getErrorMessage() { return errorMessage; }
    }
    
    public static class DeviceLoginCheckResult {
        private final boolean allowed;
        private final String message;
        private final String errorCode;
        private final boolean willTerminateSessions;
        private final int sessionsToTerminate;
        
        private DeviceLoginCheckResult(boolean allowed, String message, String errorCode, 
                                     boolean willTerminateSessions, int sessionsToTerminate) {
            this.allowed = allowed;
            this.message = message;
            this.errorCode = errorCode;
            this.willTerminateSessions = willTerminateSessions;
            this.sessionsToTerminate = sessionsToTerminate;
        }
        
        public static DeviceLoginCheckResult allowed(String message) {
            return new DeviceLoginCheckResult(true, message, null, false, 0);
        }
        
        public static DeviceLoginCheckResult allowedWithTermination(String message, int sessionsToTerminate) {
            return new DeviceLoginCheckResult(true, message, null, true, sessionsToTerminate);
        }
        
        public static DeviceLoginCheckResult denied(String message, String errorCode) {
            return new DeviceLoginCheckResult(false, message, errorCode, false, 0);
        }
        
        public static DeviceLoginCheckResult error(String message) {
            return new DeviceLoginCheckResult(false, message, "CHECK_ERROR", false, 0);
        }
        
        public boolean isAllowed() { return allowed; }
        public String getMessage() { return message; }
        public String getErrorCode() { return errorCode; }
        public boolean willTerminateSessions() { return willTerminateSessions; }
        public int getSessionsToTerminate() { return sessionsToTerminate; }
    }
    
    public static class SessionDeviceValidationResult {
        private final boolean valid;
        private final String errorMessage;
        private final String errorCode;
        
        private SessionDeviceValidationResult(boolean valid, String errorMessage, String errorCode) {
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.errorCode = errorCode;
        }
        
        public static SessionDeviceValidationResult valid() {
            return new SessionDeviceValidationResult(true, null, null);
        }
        
        public static SessionDeviceValidationResult invalid(String errorMessage, String errorCode) {
            return new SessionDeviceValidationResult(false, errorMessage, errorCode);
        }
        
        public boolean isValid() { return valid; }
        public String getErrorMessage() { return errorMessage; }
        public String getErrorCode() { return errorCode; }
    }
    
    public static class SessionInfo {
        private String sessionId;
        private String cohortId;
        private OffsetDateTime createdAt;
        
        public SessionInfo() {}
        
        public SessionInfo(String sessionId, String cohortId, OffsetDateTime createdAt) {
            this.sessionId = sessionId;
            this.cohortId = cohortId;
            this.createdAt = createdAt;
        }
        
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public String getCohortId() { return cohortId; }
        public void setCohortId(String cohortId) { this.cohortId = cohortId; }
        public OffsetDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    }
}