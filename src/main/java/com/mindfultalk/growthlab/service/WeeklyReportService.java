package com.mindfultalk.growthlab.service;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import com.mindfultalk.growthlab.model.*;
import com.mindfultalk.growthlab.repository.*;
import org.slf4j.*;
import jakarta.transaction.Transactional;

@Service
public class WeeklyReportService {
    
    private static final Logger logger = LoggerFactory.getLogger(WeeklyReportService.class);
    private static final int DEFAULT_INACTIVITY_DAYS = 5;
    private static final String SUPPORT_EMAIL = "support@thechippersage.com";
    private static final String PLATFORM_URL = "https://flowofenglish.thechippersage.com";
    
    @Autowired
    private UserAttemptsRepository userAttemptsRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JavaMailSender mailSender;
    
    public int weeklyReportServiceTestSingleUser(List<User> users) {
        return sendUserNotifications(users);
    }

    /**
     * Main method - now uses optimized database filtering
     * No need for manual filtering since the database query already applies all filters
     */
    @Transactional
    public void sendWeeklyReports() {
        logger.info("Starting weekly email report process...");
        int successCount = 0;
        
        try {
            // This now only returns users that are:
            // 1. ACTIVE status
            // 2. In cohorts that haven't ended
            // 3. Inactive for the specified days
            List<User> inactiveUsers = getInactiveUsers(DEFAULT_INACTIVITY_DAYS);
            logger.info("Found {} inactive users (filtered for active users in active cohorts)", inactiveUsers.size());

            // All users returned already have valid conditions, so we just need email validation
            List<User> usersWithValidEmails = filterUsersWithValidEmails(inactiveUsers);
            logger.info("Found {} users with valid emails", usersWithValidEmails.size());

            sendAdminReports(inactiveUsers);
            
         // Track success count when sending user notifications
            successCount = sendUserNotifications(usersWithValidEmails);

            logger.info("Completed weekly email report process. Successfully sent {} emails out of {}.", 
                        successCount, usersWithValidEmails.size());
            
        } catch (Exception e) {
            logger.error("Error in weekly report process: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Updated method using the optimized repository query
     * Now filters at database level instead of in-memory
     */
    @Transactional
    public List<User> getInactiveUsers(int inactivityDays) {
        OffsetDateTime cutoffTime = OffsetDateTime.now().minusDays(inactivityDays);

        // - ACTIVE users only
        // - Users in active cohorts only (cohortEndDate > now or null)
        List<Object[]> latestAttempts = userAttemptsRepository.findLatestAttemptTimestampsForActiveUsersInActiveCohorts();

        return latestAttempts.stream()
            .filter(attempt -> {
                OffsetDateTime lastAttempt = (OffsetDateTime) attempt[1];
                return lastAttempt.isBefore(cutoffTime);
            })
            .map(attempt -> userRepository.findById((String) attempt[0]).orElse(null))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
    
    
    private List<User> filterUsersWithValidEmails(List<User> users) {
        return users.stream()
            .filter(user -> isValidEmail(user.getUserEmail()))
            .collect(Collectors.toList());
    }
    
    private boolean isValidEmail(String email) {
        return email != null && !email.trim().isEmpty();
    }
    
    private void sendAdminReports(List<User> inactiveUsers) {
        Map<String, Map<String, List<User>>> orgCohortMap = groupUsersByOrgAndCohort(inactiveUsers);
        
        orgCohortMap.forEach((orgId, cohortUsers) -> {
            cohortUsers.forEach((cohortId, users) -> {
                try {
                    UserCohortMapping topper = findTopScorer(users, cohortId);
                    sendAdminReport(orgId, cohortId, users, topper);
                    logger.info("Sent admin report for organization: {}, cohort: {}", orgId, cohortId);
                } catch (Exception e) {
                    logger.error("Failed to send admin report for org: {}, cohort: {}. Error: {}", 
                               orgId, cohortId, e.getMessage(), e);
                }
            });
        });
    }
    
    private Map<String, Map<String, List<User>>> groupUsersByOrgAndCohort(List<User> users) {
        return users.stream()
            .collect(Collectors.groupingBy(
                user -> String.valueOf(user.getOrganization().getOrganizationId()),
                Collectors.groupingBy(this::getCohortId)
            ));
    }
    
    private String getCohortId(User user) {
        List<UserCohortMapping> mappings = user.getUserCohortMappings();
        return mappings != null && !mappings.isEmpty() 
               ? String.valueOf(mappings.get(0).getCohort().getCohortId())
               : "Unknown";
    }
    
    private UserCohortMapping findTopScorer(List<User> users, String cohortId) {
        return users.stream()
            .flatMap(user -> user.getUserCohortMappings().stream())
            .filter(mapping -> cohortId.equals(String.valueOf(mapping.getCohort().getCohortId())))
            .max(Comparator.comparingInt(UserCohortMapping::getLeaderboardScore))
            .orElse(null);
    }
    
    private int sendUserNotifications(List<User> users) {
        Set<String> processedKeys = new HashSet<>();

        int successCount = 0;
        
        for (User user : users) {
            String email = user.getUserEmail();
            String name = user.getUserName();

            if (!isValidEmail(email)) {
                logger.warn("Skipping user {} due to invalid email", user.getUserId());
                continue;
            }

            String uniqueKey = email.trim().toLowerCase() + "::" + name.trim().toLowerCase();

            if (processedKeys.contains(uniqueKey)) {
                logger.debug("Duplicate found: Skipping email to {} ({})", name, email);
                continue;
            }

            try {
                sendInactiveUserNotification(user);
                processedKeys.add(uniqueKey);
                successCount++;
                logger.info("Notification sent to user: {} ({})", name, email);
            } catch (Exception e) {
                logger.error("Failed to send notification to user: {} ({}) - Error: {}", name, email, e.getMessage(), e);
            }
        }
        
        return successCount;
    }

    private void sendAdminReport(String orgId, String cohortId, List<User> inactiveUsers, UserCohortMapping topper) {
        if (inactiveUsers.isEmpty()) {
            return;
        }

        String adminEmail = inactiveUsers.get(0).getOrganization().getOrganizationAdminEmail();
        if (!isValidEmail(adminEmail)) {
            logger.warn("Invalid admin email for organization: {}", orgId);
            return;
        }

        String subject = String.format("Weekly Report - Cohort %s (Organization %s)", cohortId, orgId);
        String emailBody = buildAdminReportBody(cohortId, inactiveUsers, topper);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(adminEmail);
        message.setSubject(subject);
        message.setText(emailBody);
        
        mailSender.send(message);
    }
    
    private String buildAdminReportBody(String cohortId, List<User> inactiveUsers, UserCohortMapping topper) {
        StringBuilder body = new StringBuilder();
        body.append("Weekly Inactive Users Report\n");
        body.append("=".repeat(30)).append("\n\n");
        body.append("Cohort: ").append(cohortId).append("\n");
        body.append("Report Date: ").append(OffsetDateTime.now().toLocalDate()).append("\n");
        body.append("Inactive Period: More than ").append(DEFAULT_INACTIVITY_DAYS).append(" days\n\n");
        
        body.append("Inactive Users (").append(inactiveUsers.size()).append("):\n");
        body.append("-".repeat(20)).append("\n");
        
        inactiveUsers.forEach(user -> {
            body.append("• ").append(user.getUserName());
            if (isValidEmail(user.getUserEmail())) {
                body.append(" (").append(user.getUserEmail()).append(")");
            } else {
                body.append(" (No email provided)");
            }
            body.append("\n");
        });

        if (topper != null) {
            body.append("\nCohort Leader:\n");
            body.append("-".repeat(15)).append("\n");
            body.append("• ").append(topper.getUser().getUserName());
            body.append(" - Score: ").append(topper.getLeaderboardScore()).append("\n");
        }
        
        body.append("\n---\n");
        body.append("ChipperSage Team\n");
        body.append("Support: ").append(SUPPORT_EMAIL);
        
        return body.toString();
    }

    private void sendInactiveUserNotification(User user) {
        if (!isValidEmail(user.getUserEmail())) {
            logger.warn("Invalid email for user: {}", user.getUserId());
            return;
        }
        
        String subject = "Your Flow of English is Waiting for You! 📚";
        String emailBody = buildUserNotificationBody(user);
        
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getUserEmail());
        message.setSubject(subject);
        message.setText(emailBody);
        
        mailSender.send(message);
    }

    private String buildUserNotificationBody(User user) {
        String firstName = user.getUserName() != null ? user.getUserName() : "[First Name]";
        StringBuilder body = new StringBuilder();

        body.append("Hi ").append(firstName).append(",\n\n");

        body.append("It’s been a little quiet without you in the Flow of English app. Even the words are asking, \"Where is ").append(firstName).append("?\" 😄\n\n");

        body.append("Were you able to open the app this week? If something stopped you — like internet issues, Application issues, school work, or even your pet sitting on your keyboard — just reply and tell us. We are happy to help.\n\n");

        body.append("Your lessons are still waiting. Even 5–10 minutes today can help you keep learning and make your English better.\n\n");

        body.append("👉 Start learning again: ").append(PLATFORM_URL).append("\n\n");

        body.append("If you need help or want to share how your week went, reply to this email or write to us at ").append(SUPPORT_EMAIL).append(". We always like to hear from you.\n\n");

        body.append("With warm wishes,\n");
        body.append("The ChipperSage Team\n\n");
        body.append("P.S. A small step today is better than no step at all. Your future self will be proud. 💪");

        return body.toString();
    }

}