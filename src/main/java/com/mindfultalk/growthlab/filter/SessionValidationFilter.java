package com.mindfultalk.growthlab.filter;

import java.io.IOException;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;

import org.slf4j.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.mindfultalk.growthlab.model.*;
import com.mindfultalk.growthlab.repository.*;
import com.mindfultalk.growthlab.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

@Component
public class SessionValidationFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(SessionValidationFilter.class);

    // Session timeout - 1 hour
    private static final int SESSION_TIMEOUT_MINUTES = 60;

    // Warning time - 5 minutes before timeout
    private static final int WARNING_TIME_MINUTES = 5;

    // Maximum session duration regardless of activity - 8 hours
    private static final int MAX_SESSION_DURATION_HOURS = 8;

    // Cache TTL settings (in minutes)
    private static final int SESSION_CACHE_TTL = 30; // Cache session validation for 30 minutes
    private static final int USER_CACHE_TTL = 60; // Cache user data for 1 hour
    private static final int COHORT_CACHE_TTL = 120; // Cache cohort data for 2 hours
    private static final int ACTIVITY_CACHE_TTL = 5; // Cache activity check for 5 minutes

    // Cache key prefixes
    private static final String SESSION_CACHE_PREFIX = "session:";
    private static final String LAST_ACTIVITY_PREFIX = "lastActivity:";
    private static final String USER_ACTIVE_PREFIX = "userActive:";
    private static final String COHORT_STATUS_PREFIX = "cohortStatus:";
    private static final String SESSION_VALIDATION_PREFIX = "sessionValidation:";
    private static final String USER_DATA_PREFIX = "userData:";
    private static final String COHORT_DATA_PREFIX = "cohortData:";
    private static final String USER_COHORT_MAPPING_PREFIX = "userCohortMapping:";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserCohortMappingRepository userCohortMappingRepository;

    @Autowired
    private CohortRepository cohortRepository;

    @Autowired
    private UserSessionMappingService userSessionMappingService;

    @Autowired
    private UserAttemptsRepository userAttemptsRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired(required = false)
    private SingleDeviceLoginService singleDeviceLoginService;

    // Protected endpoints that require session validation
    private static final List<String> PROTECTED_ENDPOINTS = Arrays.asList(
            "/api/v1/units/{userId}/program/{programId}",
            "/api/v1/programconceptsmappings/{userId}/unit/",
            "/api/v1/user-attempts/",
            "/api/v1/assignments/submit",
            "/api/v1/userSubConceptsCompletion/");

    // Public endpoints that don't require session validation
    private static final List<String> PUBLIC_ENDPOINTS = Arrays.asList(
            "/api/v1/users/signin",
            "/api/v1/users/select-cohort",
            "/api/v1/users/logout",
            "/api/v1/organizations/login",
            "/api/v1/users/create");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestURI = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();

        logger.debug("Processing request: {} {}", method, requestURI);

        // Skip validation for public endpoints and OPTIONS requests
        if (isPublicEndpoint(requestURI) || "OPTIONS".equals(method)) {
            chain.doFilter(request, response);
            return;
        }

        // Check if this is a protected endpoint
        if (isProtectedEndpoint(requestURI)) {
            SessionValidationResult validationResult = validateSessionWithCaching(httpRequest);

            if (!validationResult.isValid()) {
                logger.warn("Session validation failed for {}: {}", requestURI, validationResult.getErrorMessage());
                sendErrorResponse(httpResponse, validationResult);
                return;
            }

            // Add validated session info to request attributes for use in controllers
            httpRequest.setAttribute("validatedUserId", validationResult.getUserId());
            httpRequest.setAttribute("validatedCohortId", validationResult.getCohortId());
            httpRequest.setAttribute("validatedSessionId", validationResult.getSessionId());

            logger.debug("Session validation successful for user: {}", validationResult.getUserId());
        }

        chain.doFilter(request, response);
    }

    private boolean isPublicEndpoint(String requestURI) {
        return PUBLIC_ENDPOINTS.stream().anyMatch(requestURI::contains);
    }

    private boolean isProtectedEndpoint(String requestURI) {
        return PROTECTED_ENDPOINTS.stream().anyMatch(endpoint -> requestURI.contains(endpoint));
    }

    /**
     * Enhanced session validation with multi-layer caching
     */
    private SessionValidationResult validateSessionWithCaching(HttpServletRequest request) {
        // Get session data
        HttpSession httpSession = request.getSession(false);
        if (httpSession == null) {
            return SessionValidationResult.invalid("No active session. Please login again.", "INVALID_SESSION");
        }

        String userId = (String) httpSession.getAttribute("userId");
        String cohortId = (String) httpSession.getAttribute("cohortId");
        String sessionId = (String) httpSession.getAttribute("sessionId");

        if (userId == null || cohortId == null || sessionId == null) {
            return SessionValidationResult.invalid("Invalid session data. Please login again.", "INVALID_SESSION_DATA");
        }

        // NEW: Single Device Login Validation
        SessionValidationResult singleDeviceValidation = validateSingleDeviceLogin(request, userId, sessionId);
        if (!singleDeviceValidation.isValid()) {
            // Terminate session if single device validation fails
            invalidateSessionAndLogout(httpSession, sessionId, "SINGLE_DEVICE_VIOLATION");
            return singleDeviceValidation;
        }

        // Check cached session validation result first
        String sessionValidationKey = SESSION_VALIDATION_PREFIX + sessionId;
        try {
            SessionValidationResult cachedResult = (SessionValidationResult) redisTemplate.opsForValue()
                    .get(sessionValidationKey);
            if (cachedResult != null && cachedResult.isValid()) {
                logger.debug("Using cached session validation for sessionId: {}", sessionId);

                // Still need to check activity for timeout warnings
                EnhancedActivityCheckResult activityResult = checkEnhancedUserActivityWithCache(userId, sessionId);
                if (activityResult.getStatus() == EnhancedActivityStatus.WARNING) {
                    return SessionValidationResult.warning(userId, cohortId, sessionId,
                            "You will be logged out in " + activityResult.getMinutesRemaining()
                                    + " minutes due to inactivity.",
                            "INACTIVITY_WARNING");
                }

                return cachedResult;
            }
        } catch (Exception e) {
            logger.warn("Error retrieving cached session validation: {}", e.getMessage());
        }

        // Perform full validation if not cached
        SessionValidationResult validationResult = performFullSessionValidation(userId, cohortId, sessionId,
                httpSession);

        // Cache successful validation results
        if (validationResult.isValid() && !validationResult.isWarning()) {
            try {
                redisTemplate.opsForValue().set(sessionValidationKey, validationResult, SESSION_CACHE_TTL,
                        TimeUnit.MINUTES);
                logger.debug("Cached session validation result for sessionId: {}", sessionId);
            } catch (Exception e) {
                logger.warn("Error caching session validation result: {}", e.getMessage());
            }
        }

        return validationResult;
    }

    /**
     * NEW: Validate single device login during session validation
     */
    private SessionValidationResult validateSingleDeviceLogin(HttpServletRequest request, String userId,
            String sessionId) {
        if (singleDeviceLoginService == null || !singleDeviceLoginService.isSingleDeviceLoginEnabled()) {
            return SessionValidationResult.valid(userId, null, sessionId);
        }

        try {
            // Build device fingerprint from current request
            String userAgent = request.getHeader("User-Agent");
            String remoteAddr = getClientIpAddress(request);
            String acceptLanguage = request.getHeader("Accept-Language");
            String acceptEncoding = request.getHeader("Accept-Encoding");
            String currentDeviceFingerprint = singleDeviceLoginService.createDeviceFingerprint(
                    userAgent, remoteAddr, acceptLanguage, acceptEncoding);

            // 1. Check active session in Redis
            String activeSessionKey = "activeSession:" + userId;
            SingleDeviceLoginService.SessionInfo sessionInfo = (SingleDeviceLoginService.SessionInfo) redisTemplate
                    .opsForValue().get(activeSessionKey);

            if (sessionInfo == null || !sessionId.equals(sessionInfo.getSessionId())) {
                logger.warn("SessionValidationFilter: SessionId mismatch or not active for userId: {}", userId);
                return SessionValidationResult.invalid(
                        "Your session has been terminated because you logged in from another device.",
                        "SESSION_TERMINATED_NEW_LOGIN");
            }

            // 2. Check device fingerprint in Redis
            String deviceKey = "deviceFingerprint:" + userId;
            String storedFingerprint = (String) redisTemplate.opsForValue().get(deviceKey);
            if (storedFingerprint == null || !storedFingerprint.equals(currentDeviceFingerprint)) {
                logger.warn("SessionValidationFilter: Device fingerprint mismatch for userId: {}", userId);
                return SessionValidationResult.invalid(
                        "Session terminated - login detected from a different device.",
                        "DEVICE_MISMATCH");
            }

            return SessionValidationResult.valid(userId, null, sessionId);

        } catch (Exception e) {
            logger.error("Error validating single device login for userId: {}, sessionId: {}", userId, sessionId, e);
            return SessionValidationResult.invalid(
                    "Unable to validate device session. Please login again.",
                    "DEVICE_VALIDATION_ERROR");
        }
    }

    /**
     * NEW: Extract client IP address considering various proxy headers
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR",
                "HTTP_X_FORWARDED",
                "HTTP_X_CLUSTER_CLIENT_IP",
                "HTTP_CLIENT_IP",
                "HTTP_FORWARDED_FOR",
                "HTTP_FORWARDED",
                "HTTP_VIA",
                "REMOTE_ADDR"
        };

        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // Handle comma-separated IPs (take the first one)
                if (ip.contains(",")) {
                    ip = ip.split(",")[0];
                }
                return ip.trim();
            }
        }

        return request.getRemoteAddr();
    }

    /**
     * Perform full session validation with database checks
     */
    private SessionValidationResult performFullSessionValidation(String userId, String cohortId, String sessionId,
            HttpSession httpSession) {
        // Basic validations with caching
        SessionValidationResult basicValidation = performCachedBasicValidations(userId, cohortId, sessionId,
                httpSession);
        if (!basicValidation.isValid()) {
            return basicValidation;
        }

        // Enhanced activity-based timeout check with caching
        EnhancedActivityCheckResult activityResult = checkEnhancedUserActivityWithCache(userId, sessionId);

        switch (activityResult.getStatus()) {
            case ACTIVE:
                return SessionValidationResult.valid(userId, cohortId, sessionId);

            case WARNING:
                return SessionValidationResult.warning(userId, cohortId, sessionId,
                        "You will be logged out in " + activityResult.getMinutesRemaining()
                                + " minutes due to inactivity.",
                        "INACTIVITY_WARNING");

            case TIMEOUT:
                invalidateSessionAndLogout(httpSession, sessionId, "INACTIVITY_TIMEOUT");
                return SessionValidationResult.invalid(
                        "Session timed out due to inactivity. Please login again.",
                        "SESSION_TIMEOUT");

            case MAX_DURATION_EXCEEDED:
                invalidateSessionAndLogout(httpSession, sessionId, "MAX_DURATION_EXCEEDED");
                return SessionValidationResult.invalid(
                        "Session expired due to maximum duration limit. Please login again.",
                        "MAX_DURATION_TIMEOUT");

            default:
                return SessionValidationResult.invalid("Unable to verify session activity.", "ACTIVITY_CHECK_ERROR");
        }
    }

    /**
     * Basic validations with caching support
     */
    private SessionValidationResult performCachedBasicValidations(String userId, String cohortId, String sessionId,
            HttpSession httpSession) {
        // Validate user with caching
        User user = getCachedUser(userId);
        if (user == null) {
            return SessionValidationResult.invalid("User not found. Please login again.", "USER_NOT_FOUND");
        }

        if (!user.isActive()) {
            invalidateSessionAndLogout(httpSession, sessionId, "USER_DEACTIVATED");
            return SessionValidationResult.invalid(
                    "Your account has been deactivated. Please contact your administrator.", "USER_DEACTIVATED");
        }

        // Validate user-cohort mapping with caching
        UserCohortMapping cohortMapping = getCachedUserCohortMapping(userId, cohortId);
        if (cohortMapping == null || !cohortMapping.isActive()) {
            invalidateSessionAndLogout(httpSession, sessionId, "COHORT_ACCESS_DEACTIVATED");
            return SessionValidationResult.invalid(
                    "Your access to this program has been deactivated. Please contact your administrator.",
                    "COHORT_ACCESS_DEACTIVATED");
        }

        // Validate cohort with caching
        Cohort cohort = getCachedCohort(cohortId);
        if (cohort == null) {
            invalidateSessionAndLogout(httpSession, sessionId, "COHORT_NOT_FOUND");
            return SessionValidationResult.invalid("Program not found. Please login again.", "COHORT_NOT_FOUND");
        }

        if (isCohortEnded(cohort)) {
            invalidateSessionAndLogout(httpSession, sessionId, "COHORT_ENDED");
            return SessionValidationResult.invalid(
                    "This program has ended. Please select a different program or contact your administrator.",
                    "COHORT_ENDED");
        }

        // Validate session exists in database (this check should not be cached for
        // security)
        Optional<UserSessionMapping> sessionOpt = userSessionMappingService.getUserSessionMappingById(sessionId);
        if (sessionOpt.isEmpty()) {
            return SessionValidationResult.invalid("Invalid session. Please login again.", "SESSION_NOT_FOUND");
        }

        UserSessionMapping userSession = sessionOpt.get();
        if (userSession.getSessionEndTimestamp() != null) {
            return SessionValidationResult.invalid("Session has expired. Please login again.", "SESSION_EXPIRED");
        }

        return SessionValidationResult.valid(userId, cohortId, sessionId);
    }

    /**
     * Get user from cache or database
     */
    private User getCachedUser(String userId) {
        String userKey = USER_DATA_PREFIX + userId;
        try {
            User cachedUser = (User) redisTemplate.opsForValue().get(userKey);
            if (cachedUser != null) {
                logger.debug("Retrieved user from cache: {}", userId);
                return cachedUser;
            }
        } catch (Exception e) {
            logger.warn("Error retrieving cached user data: {}", e.getMessage());
        }

        // Fetch from database and cache
        Optional<User> userOpt = userRepository.findByUserId(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            try {
                redisTemplate.opsForValue().set(userKey, user, USER_CACHE_TTL, TimeUnit.MINUTES);
                logger.debug("Cached user data: {}", userId);
            } catch (Exception e) {
                logger.warn("Error caching user data: {}", e.getMessage());
            }
            return user;
        }

        return null;
    }

    /**
     * Get user-cohort mapping from cache or database
     */
    private UserCohortMapping getCachedUserCohortMapping(String userId, String cohortId) {
        String mappingKey = USER_COHORT_MAPPING_PREFIX + userId + ":" + cohortId;
        try {
            UserCohortMapping cachedMapping = (UserCohortMapping) redisTemplate.opsForValue().get(mappingKey);
            if (cachedMapping != null) {
                logger.debug("Retrieved user-cohort mapping from cache: {}:{}", userId, cohortId);
                return cachedMapping;
            }
        } catch (Exception e) {
            logger.warn("Error retrieving cached user-cohort mapping: {}", e.getMessage());
        }

        // Fetch from database and cache
        Optional<UserCohortMapping> mappingOpt = userCohortMappingRepository
                .findByUser_UserIdAndCohort_CohortId(userId, cohortId);

        if (mappingOpt.isPresent()) {
            UserCohortMapping mapping = mappingOpt.get();
            try {
                redisTemplate.opsForValue().set(mappingKey, mapping, USER_CACHE_TTL, TimeUnit.MINUTES);
                logger.debug("Cached user-cohort mapping: {}:{}", userId, cohortId);
            } catch (Exception e) {
                logger.warn("Error caching user-cohort mapping: {}", e.getMessage());
            }
            return mapping;
        }

        return null;
    }

    /**
     * Get cohort from cache or database
     */
    private Cohort getCachedCohort(String cohortId) {
        String cohortKey = COHORT_DATA_PREFIX + cohortId;
        try {
            Cohort cachedCohort = (Cohort) redisTemplate.opsForValue().get(cohortKey);
            if (cachedCohort != null) {
                logger.debug("Retrieved cohort from cache: {}", cohortId);
                return cachedCohort;
            }
        } catch (Exception e) {
            logger.warn("Error retrieving cached cohort data: {}", e.getMessage());
        }

        // Fetch from database and cache
        Optional<Cohort> cohortOpt = cohortRepository.findById(cohortId);
        if (cohortOpt.isPresent()) {
            Cohort cohort = cohortOpt.get();
            try {
                redisTemplate.opsForValue().set(cohortKey, cohort, COHORT_CACHE_TTL, TimeUnit.MINUTES);
                logger.debug("Cached cohort data: {}", cohortId);
            } catch (Exception e) {
                logger.warn("Error caching cohort data: {}", e.getMessage());
            }
            return cohort;
        }

        return null;
    }

    /**
     * Enhanced activity check with caching
     */
    private EnhancedActivityCheckResult checkEnhancedUserActivityWithCache(String userId, String sessionId) {
        String activityKey = LAST_ACTIVITY_PREFIX + sessionId;
        try {
            EnhancedActivityCheckResult cachedResult = (EnhancedActivityCheckResult) redisTemplate.opsForValue()
                    .get(activityKey);
            if (cachedResult != null) {
                logger.debug("Retrieved activity check from cache for sessionId: {}", sessionId);
                return cachedResult;
            }
        } catch (Exception e) {
            logger.warn("Error retrieving cached activity check: {}", e.getMessage());
        }

        // Perform actual activity check
        EnhancedActivityCheckResult result = performEnhancedActivityCheck(userId, sessionId);

        // Cache the result with shorter TTL for activity-sensitive data
        try {
            redisTemplate.opsForValue().set(activityKey, result, ACTIVITY_CACHE_TTL, TimeUnit.MINUTES);
            logger.debug("Cached activity check result for sessionId: {}", sessionId);
        } catch (Exception e) {
            logger.warn("Error caching activity check result: {}", e.getMessage());
        }

        return result;
    }

    /**
     * Perform the actual enhanced activity check
     */
    private EnhancedActivityCheckResult performEnhancedActivityCheck(String userId, String sessionId) {
        try {
            // Get session start time
            Optional<UserSessionMapping> sessionOpt = userSessionMappingService.getUserSessionMappingById(sessionId);
            if (sessionOpt.isEmpty()) {
                return EnhancedActivityCheckResult.timeout();
            }

            OffsetDateTime sessionStart = sessionOpt.get().getSessionStartTimestamp();
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

            // Check if session has exceeded maximum duration (8 hours)
            long hoursSinceStart = ChronoUnit.HOURS.between(sessionStart, now);
            if (hoursSinceStart >= MAX_SESSION_DURATION_HOURS) {
                logger.info("Session exceeded maximum duration - sessionId: {}, hours: {}", sessionId, hoursSinceStart);
                return EnhancedActivityCheckResult.maxDurationExceeded();
            }

            // Check if session has exceeded inactivity timeout (1 hour)
            long minutesSinceStart = ChronoUnit.MINUTES.between(sessionStart, now);
            if (minutesSinceStart > SESSION_TIMEOUT_MINUTES) {
                return EnhancedActivityCheckResult.timeout();
            }

            // Get latest user attempt within the session timeframe
            List<UserAttempts> userAttempts = userAttemptsRepository.findAttemptsByUserId(userId);

            OffsetDateTime lastActivityTime = sessionStart; // Default to session start

            // Find the most recent activity after session start
            for (UserAttempts attempt : userAttempts) {
                if (attempt.getUserAttemptEndTimestamp() != null &&
                        attempt.getUserAttemptEndTimestamp().isAfter(sessionStart) &&
                        attempt.getUserAttemptEndTimestamp().isAfter(lastActivityTime)) {
                    lastActivityTime = attempt.getUserAttemptEndTimestamp();
                }
            }

            // Calculate minutes since last activity
            long minutesSinceActivity = ChronoUnit.MINUTES.between(lastActivityTime, now);

            // Check timeout threshold (1 hour of inactivity)
            if (minutesSinceActivity >= SESSION_TIMEOUT_MINUTES) {
                logger.info("Session timed out due to inactivity - sessionId: {}, minutes since activity: {}",
                        sessionId, minutesSinceActivity);
                return EnhancedActivityCheckResult.timeout();
            }

            // Check warning threshold (55 minutes of inactivity - 5 minutes before timeout)
            long warningThreshold = SESSION_TIMEOUT_MINUTES - WARNING_TIME_MINUTES;
            if (minutesSinceActivity >= warningThreshold) {
                long minutesRemaining = SESSION_TIMEOUT_MINUTES - minutesSinceActivity;
                return EnhancedActivityCheckResult.warning(minutesRemaining);
            }

            // User is active
            return EnhancedActivityCheckResult.active();

        } catch (Exception e) {
            logger.error("Error checking enhanced user activity for userId: {}", userId, e);
            return EnhancedActivityCheckResult.timeout();
        }
    }

    /**
     * Update user activity in cache (call this from controllers on user actions)
     */
    public void updateUserActivity(String sessionId, String userId) {
        try {
            String activityKey = LAST_ACTIVITY_PREFIX + sessionId;
            // Remove cached activity result to force fresh check
            redisTemplate.delete(activityKey);

            // Also update a simple last-seen timestamp
            String lastSeenKey = USER_ACTIVE_PREFIX + userId;
            redisTemplate.opsForValue().set(lastSeenKey, OffsetDateTime.now(ZoneOffset.UTC),
                    SESSION_TIMEOUT_MINUTES, TimeUnit.MINUTES);

            logger.debug("Updated user activity for sessionId: {}, userId: {}", sessionId, userId);
        } catch (Exception e) {
            logger.warn("Error updating user activity in cache: {}", e.getMessage());
        }
    }

    /**
     * Invalidate user-related cache entries
     */
    public void invalidateUserCache(String userId, String cohortId) {
        try {
            Set<String> keysToDelete = new HashSet<>();
            keysToDelete.add(USER_DATA_PREFIX + userId);
            keysToDelete.add(USER_COHORT_MAPPING_PREFIX + userId + ":" + cohortId);
            keysToDelete.add(USER_ACTIVE_PREFIX + userId);

            redisTemplate.delete(keysToDelete);
            logger.debug("Invalidated cache for userId: {}, cohortId: {}", userId, cohortId);
        } catch (Exception e) {
            logger.warn("Error invalidating user cache: {}", e.getMessage());
        }
    }

    private boolean isCohortEnded(Cohort cohort) {
        if (cohort.getCohortEndDate() == null) {
            return false;
        }

        OffsetDateTime cohortEndDate = cohort.getCohortEndDate()
                .withHour(23).withMinute(59).withSecond(59);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        return now.isAfter(cohortEndDate);
    }

    private void invalidateSessionAndLogout(HttpSession httpSession, String sessionId, String reason) {
        try {
            // Update session end timestamp with reason
            Optional<UserSessionMapping> sessionOpt = userSessionMappingService.getUserSessionMappingById(sessionId);
            if (sessionOpt.isPresent()) {
                UserSessionMapping userSession = sessionOpt.get();
                userSession.setSessionEndTimestamp(OffsetDateTime.now(ZoneOffset.UTC));
                userSessionMappingService.updateUserSessionMapping(sessionId, userSession);

                // Invalidate session cache
                String sessionValidationKey = SESSION_VALIDATION_PREFIX + sessionId;
                redisTemplate.delete(sessionValidationKey);
            }

            // Invalidate HTTP session
            httpSession.invalidate();
            logger.info("Session invalidated - sessionId: {}, reason: {}", sessionId, reason);
        } catch (Exception e) {
            logger.error("Error invalidating session: {}", sessionId, e);
        }
    }

    private void sendErrorResponse(HttpServletResponse response, SessionValidationResult result)
            throws IOException {
        int statusCode = result.isWarning() ? HttpServletResponse.SC_OK : HttpServletResponse.SC_UNAUTHORIZED;

        response.setStatus(statusCode);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("message", result.getErrorMessage());
        errorResponse.put("errorCode", result.getErrorCode());
        errorResponse.put("isWarning", result.isWarning());

        if (!result.isWarning()) {
            errorResponse.put("requiresLogin", true);
        }

        errorResponse.put("timestamp", OffsetDateTime.now(ZoneOffset.UTC));

        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(jsonResponse);
    }

    // Enhanced Activity Check Result Class
    private static class EnhancedActivityCheckResult {
        private final EnhancedActivityStatus status;
        private final long minutesRemaining;

        private EnhancedActivityCheckResult(EnhancedActivityStatus status, long minutesRemaining) {
            this.status = status;
            this.minutesRemaining = minutesRemaining;
        }

        public static EnhancedActivityCheckResult active() {
            return new EnhancedActivityCheckResult(EnhancedActivityStatus.ACTIVE, 0);
        }

        public static EnhancedActivityCheckResult warning(long minutesRemaining) {
            return new EnhancedActivityCheckResult(EnhancedActivityStatus.WARNING, minutesRemaining);
        }

        public static EnhancedActivityCheckResult timeout() {
            return new EnhancedActivityCheckResult(EnhancedActivityStatus.TIMEOUT, 0);
        }

        public static EnhancedActivityCheckResult maxDurationExceeded() {
            return new EnhancedActivityCheckResult(EnhancedActivityStatus.MAX_DURATION_EXCEEDED, 0);
        }

        public EnhancedActivityStatus getStatus() {
            return status;
        }

        public long getMinutesRemaining() {
            return minutesRemaining;
        }
    }

    public enum EnhancedActivityStatus {
        ACTIVE, WARNING, TIMEOUT, MAX_DURATION_EXCEEDED
    }

    // Session Validation Result Class
    private static class SessionValidationResult {
        private final boolean valid;
        private final boolean warning;
        private final String errorMessage;
        private final String errorCode;
        private final String userId;
        private final String cohortId;
        private final String sessionId;

        private SessionValidationResult(boolean valid, boolean warning, String errorMessage, String errorCode,
                String userId, String cohortId, String sessionId) {
            this.valid = valid;
            this.warning = warning;
            this.errorMessage = errorMessage;
            this.errorCode = errorCode;
            this.userId = userId;
            this.cohortId = cohortId;
            this.sessionId = sessionId;
        }

        public static SessionValidationResult valid(String userId, String cohortId, String sessionId) {
            return new SessionValidationResult(true, false, null, null, userId, cohortId, sessionId);
        }

        public static SessionValidationResult warning(String userId, String cohortId, String sessionId,
                String warningMessage, String warningCode) {
            return new SessionValidationResult(true, true, warningMessage, warningCode, userId, cohortId, sessionId);
        }

        public static SessionValidationResult invalid(String errorMessage, String errorCode) {
            return new SessionValidationResult(false, false, errorMessage, errorCode, null, null, null);
        }

        // Getters
        public boolean isValid() {
            return valid;
        }

        public boolean isWarning() {
            return warning;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public String getUserId() {
            return userId;
        }

        public String getCohortId() {
            return cohortId;
        }

        public String getSessionId() {
            return sessionId;
        }
    }

    // 6. Enhanced SessionValidationFilter cache eviction method
    // Add this method to your SessionValidationFilter class:
    public void evictUserSessionCaches(String userId, String cohortId, String sessionId) {
        try {
            Set<String> keysToDelete = new HashSet<>();
            keysToDelete.add(USER_DATA_PREFIX + userId);
            keysToDelete.add(USER_COHORT_MAPPING_PREFIX + userId + ":" + cohortId);
            keysToDelete.add(USER_ACTIVE_PREFIX + userId);
            keysToDelete.add(LAST_ACTIVITY_PREFIX + sessionId);
            keysToDelete.add(SESSION_VALIDATION_PREFIX + sessionId);

            redisTemplate.delete(keysToDelete);
            logger.debug("Evicted session caches for userId: {}, cohortId: {}, sessionId: {}",
                    userId, cohortId, sessionId);
        } catch (Exception e) {
            logger.warn("Error evicting session caches: {}", e.getMessage());
        }
    }
}
