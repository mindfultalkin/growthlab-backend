package com.mindfultalk.growthlab.service;

import com.mindfultalk.growthlab.model.*;
import com.mindfultalk.growthlab.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;
import org.slf4j.*;

@Service
public class AssignmentNotificationService {

    @Autowired
    private UserAssignmentRepository userAssignmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;
    
    private static final Logger logger = LoggerFactory.getLogger(AssignmentNotificationService.class);
    private static final String PLATFORM_URL = "https://flowofenglish.thechippersage.com";
    private static final String PLATFORM_URL_ADMIN = "https://flowofenglish.thechippersage.com/admin";
    
    @Scheduled(cron = "0 0 21 * * ?", zone = "Asia/Kolkata") // Every day at 9:00 PM IST
    public void notifyMentorsAndAdmins() {
        LocalDate today = LocalDate.now();
        OffsetDateTime startOfDay = today.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime endOfDay = today.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        // Fetch all assignments submitted today
        List<UserAssignment> assignments = userAssignmentRepository.findBySubmittedDateBetween(startOfDay, endOfDay);

        // Group assignments by cohort
        Map<Cohort, List<UserAssignment>> assignmentsByCohort = new HashMap<>();
        for (UserAssignment assignment : assignments) {
            Cohort cohort = assignment.getCohort();
            assignmentsByCohort.computeIfAbsent(cohort, k -> new ArrayList<>()).add(assignment);
        }
     // Track notified admins per organization
        Map<Organization, List<User>> orgAdminsMap = new HashMap<>();

        // Notify mentors and admins for each cohort
        for (Map.Entry<Cohort, List<UserAssignment>> entry : assignmentsByCohort.entrySet()) {
            Cohort cohort = entry.getKey();
            List<UserAssignment> cohortAssignments = entry.getValue();
            Organization org = cohort.getOrganization();
            String orgAdminEmail = org.getOrganizationAdminEmail();

            // Fetch mentors for the cohort
            List<User> mentors = userRepository.findAllByUserTypeAndCohort("mentor", cohort);
            if (mentors.isEmpty()) {
                logger.warn("No mentors found for cohort: {}", cohort.getCohortId());
                continue;
            }
         // Find all admins of the organization
            List<User> orgAdmins = userRepository.findByOrganizationAndUserType(org, "admin");
            orgAdminsMap.putIfAbsent(org, orgAdmins);

         // Send emails to mentors
            for (User mentor : mentors) {
                sendMentorEmail(mentor, cohort, cohortAssignments);
            }

            // Send email to organization admin if exists
            if (orgAdminEmail != null && !orgAdminEmail.isEmpty()) {
                sendOrgAdminEmail(org, cohort, cohortAssignments);
            }

            // Send emails to all organization admins
            for (User admin : orgAdmins) {
                sendAdminEmail(admin, cohort, cohortAssignments);
            }
        }
    }
    private void sendMentorEmail(User mentor, Cohort cohort, List<UserAssignment> assignments) {
        String subject = "New Assignment Submissions Ready for Review - " + cohort.getCohortName();
        
        StringBuilder body = new StringBuilder();
        body.append("Dear ").append(mentor.getUserName()).append(",\n\n");
        body.append("Exciting news! ").append(assignments.size()).append(" new assignment");
        body.append(assignments.size() > 1 ? "s have" : " has").append(" been submitted today for your cohort: ");
        body.append(cohort.getCohortName()).append(".\n\n");
        
        body.append("Ready to hear your learners' progress? Here's how to review their work:\n\n");
        body.append("1. Log in to your Flow of English dashboard at: ").append(PLATFORM_URL).append("\n");
        body.append("2. Navigate to the 'View Assignments' section\n");
        body.append("3. Select the assignments from today's submissions\n");
        body.append("4. Use the 'Take Tour' button if you need guidance on the correction process\n\n");
        
        body.append("After reviewing, simply update the Score and Remarks columns. Your learners will automatically ");
        body.append("receive an email with their scores and your valuable feedback.\n\n");
        
        body.append("Today's submissions are from:\n");
        for (UserAssignment assignment : assignments) {
            body.append("- ").append(assignment.getUser().getUserName()).append(" (Assignment ID: ").append(assignment.getUuid()).append(")\n");
        }
        
        body.append("\nYour timely feedback makes a huge difference in your learners' motivation and progress!\n\n");
        body.append("Thank you for your dedication,\n");
        body.append("Team Chippersage");
        
        emailService.sendEmail(mentor.getUserEmail(), subject, body.toString());
    }
    
    private void sendAdminEmail(User admin, Cohort cohort, List<UserAssignment> assignments) {
        String subject = "Daily Assignment Submission Report - " + cohort.getCohortName();
        
        StringBuilder body = new StringBuilder();
        body.append("Dear ").append(admin.getUserName()).append(",\n\n");
        body.append("Here is your daily update on assignment submissions for cohort: ").append(cohort.getCohortName()).append(".\n\n");
        body.append("Total submissions today: ").append(assignments.size()).append("\n\n");
        
        body.append("To view detailed reports and monitor progress:\n");
        body.append("1. Log in to the admin dashboard at: ").append(PLATFORM_URL).append("\n");
        body.append("2. Navigate to 'View Assignments' section\n");
        body.append("3. Filter by cohort: ").append(cohort.getCohortName()).append("\n\n");
        
        body.append("Learners who submitted assignments today:\n");
        for (UserAssignment assignment : assignments) {
            body.append("- ").append(assignment.getUser().getUserName()).append(" (Assignment ID: ").append(assignment.getUuid()).append(")\n");
        }
        
        body.append("\nBest regards,\n");
        body.append("Team Chippersage");
        
        emailService.sendEmail(admin.getUserEmail(), subject, body.toString());
    }
    
    private void sendOrgAdminEmail(Organization org, Cohort cohort, List<UserAssignment> assignments) {
        String subject = "Organization Update: New Assignment Submissions - " + cohort.getCohortName();
        
        StringBuilder body = new StringBuilder();
        body.append("Dear ").append(org.getOrganizationAdminName()).append(",\n\n");
        body.append("We're pleased to inform you that ").append(assignments.size()).append(" new assignment");
        body.append(assignments.size() > 1 ? "s have" : " has").append(" been submitted today for cohort: ");
        body.append(cohort.getCohortName()).append(".\n\n");
        
        body.append("To review progress and analytics:\n");
        body.append("1. Access your organization dashboard at: ").append(PLATFORM_URL_ADMIN).append("\n");
        body.append("2. Go to the 'View Assignments' section\n");
        body.append("3. Use the filtering options to view specific cohorts or date ranges\n\n");
        
        body.append("The platform provides comprehensive insights into learner engagement and performance across all your cohorts.\n\n");
        
        body.append("Thank you for your continued partnership,\n");
        body.append("Team Chippersage");
        
        emailService.sendEmail(org.getOrganizationAdminEmail(), subject, body.toString());
    }
}