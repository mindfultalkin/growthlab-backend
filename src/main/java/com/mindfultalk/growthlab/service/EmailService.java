package com.mindfultalk.growthlab.service;

import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import com.mindfultalk.growthlab.model.*;
import com.mindfultalk.growthlab.repository.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;
    
//    @Autowired
//    private TemplateEngine templateEngine;

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserAssignmentRepository userAssignmentRepository;
    
    @Autowired
    private S3StorageService s3StorageService;
    
 //   private static final String LOGO_IMAGE = "images/ChipperSageLogo.png";
    
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(EmailService.class);


    public void sendEmailWithAttachment(String to, String mentorName, String cohortName, Path filePath) {
        try {
            logger.info("Sending email with assignment attachment to {}", to);

            String subject = "Assignments for Cohort " + cohortName + " – Please Review and Provide Feedback";

            String body = "<html><body style='font-family: Arial, sans-serif; line-height: 1.6;'>" +
                    "<p>Dear " + mentorName + ",</p>" +
                    "<p>I hope this email finds you well.</p>" +
                    "<p>Please find attached a ZIP file containing the assignments submitted by learners from the cohort <b>" + cohortName + "</b>. " +
                    "The ZIP file includes individual assignment files as well as a CSV file (assignments-details.csv) with relevant details.</p>" +
                    "<p><b>Instructions for Review:</b></p>" +
                    "<ol>" +
                    "<li>Extract the ZIP File: Use any standard extraction tool to open the ZIP file.</li>" +
                    "<li>Review Assignments: Evaluate each assignment based on the given criteria.</li>" +
                    "<li>Update the CSV File:" +
                    "<ul>" +
                    "<li>Enter the score for each learner in the 'Score' column.</li>" +
                    "<li>Provide any necessary feedback in the 'Remarks' column.</li>" +
                    "</ul></li>" +
                    "<li>Send Back the Reviewed Assignments: Reply to this email with the updated CSV file and any additional comments.</li>" +
                    "</ol>" +
                    "<p><b>Submission Deadline:</b></p>" +
                    "<p>Kindly complete the review and return the corrected assignments within 5 days from the date of this email.</p>" +
                    "<p>If you have any questions or require assistance, feel free to reach out.</p>" +
                    "<p>Thank you for your time and support in guiding the learners.</p>" +
                    "<p>Best regards,<br>" +
                    "The Mindfultalk Team<br>" +
                    "support@mindfultalk.in</p>" +
                    "</body></html>";

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true); // Set true to enable HTML formatting

            // Attach the file
            FileSystemResource file = new FileSystemResource(filePath.toFile());
            helper.addAttachment(file.getFilename(), file);

            mailSender.send(message);

            logger.info("Email with attachment sent successfully to {}", to);
        } catch (MessagingException e) {
            logger.error("Failed to send email with attachment to {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send email with attachment", e);
        } finally {
            try {
                Files.deleteIfExists(filePath);
                logger.info("Temporary zip file deleted: {}", filePath);
            } catch (IOException e) {
                logger.error("Failed to delete temporary zip file: " + filePath, e);
            }
        }
    }
    
    public void sendEmail(String to, String subject, String body) {
        logger.info("Attempting to send email to: {}", to);
        System.out.println("Attempting to send email to: " + to);
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            
            logger.info("Email content prepared. Subject: {}", subject);
            System.out.println("Email content prepared. Subject: " + subject);
            
            mailSender.send(message);
            
            logger.info("Email successfully sent to: {}", to);
            System.out.println("Email successfully sent to: " + to);
        } catch (Exception e) {
            logger.error("Failed to send email to: {}. Error: {}", to, e.getMessage());
            System.err.println("Failed to send email to: " + to + ". Error: " + e.getMessage());
            e.printStackTrace(); // Print full stack trace to console
            throw e; // Re-throw to be handled by caller
        }
    }

    
    public void sendEmailWithCSVAttachment(String recipientEmail, String recipientName, String cohortName, Path csvFilePath) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
            helper.setTo(recipientEmail);
            helper.setSubject("Assignment Review Instructions for Cohort: " + cohortName);
            
            // Get organization admin email from the mentor's organization
            String orgAdminEmail = getUserOrganizationAdminEmail(recipientEmail);
            
            String emailBody = 
                "Dear " + recipientName + ",\n\n" +
                "Attached is the assignments CSV file for cohort: " + cohortName + ". Please follow these instructions for reviewing:\n\n" +
                "1. Open the attached CSV file to view all learner assignments\n" +
                "2. Click on the FileDownloadLink for each submission to access the learner's work\n" +
                "3. Review each assignment carefully based on our assessment rubric\n" +
                "4. When scoring, please note the MaxScore column and ensure scores do not exceed this value\n" +
                "5. For each assignment, provide detailed feedback in the Remarks column addressing:\n" +
                "   - Strengths of the submission\n" +
                "   - Areas needing improvement\n" +
                "   - Specific suggestions for growth\n\n" +
                "6. If you need to provide corrected files or additional resources, please:\n" +
                "   - Save the file with naming format: [UserID]_[AssignmentId]_corrected\n" +
                "   - Update the Score and Remarks columns in the CSV\n" +
                "   - Send the completed CSV file and any corrected files to: " + orgAdminEmail + "\n\n" +
                "7. **Important:** In the **'CorrectedFileAttached'** column, mark **'YES'** if a corrected file is attached, otherwise mark **'NO'**.\n\n" +
                "Please complete your reviews within 3 business days. Your thoughtful feedback is essential to our learners' growth and success.\n\n" +
                "Thank you for your dedication to our learners' development.\n\n" +
                "Best regards,\n" +
                "The Mindfultalk Team";
            
            helper.setText(emailBody);
            
            // Attach the CSV file
            FileSystemResource file = new FileSystemResource(csvFilePath.toFile());
            helper.addAttachment("assignments-details.csv", file);
            
            mailSender.send(message);
            logger.info("Email with CSV attachment and instructions sent successfully to {}", recipientEmail);
        } catch (Exception e) {
            logger.error("Failed to send email with CSV attachment: {}", e.getMessage(), e);
            throw new RuntimeException("Error sending email", e);
        }
    }
    
    // New helper method to get organization admin email for a user
    private String getUserOrganizationAdminEmail(String userEmail) {
        return userRepository.findByUserEmail(userEmail)
                .map(User::getOrganization)
                .map(Organization::getOrganizationAdminEmail)
                .orElse("support@mindfultalk.in");
    }

    
    public void sendUserCreationEmail(String userEmail, String userName, String userId, String plainPassword, 
    		List<String> programNames, List<String> cohortNames, String orgAdminEmail, String orgName, String userType) { 
    	
        String subject = "Welcome to Your Learning Journey!";
        
     // Building the list of programs and cohorts dynamically
        StringBuilder programCohortDetails = new StringBuilder();
        for (int i = 0; i < programNames.size(); i++) {
            programCohortDetails.append("Program Name: ").append(programNames.get(i)).append("\n");
            if (i < cohortNames.size()) {
                programCohortDetails.append("Cohort Name: ").append(cohortNames.get(i)).append("\n\n");
            }
        }
        
        String body =
        	    "Dear " + userName + ",\n\n" +
        	    "🌱 Welcome to your transformative learning journey with **Mindfultalk**!\n\n" +
        	    "We’re excited to confirm your enrollment in the following program(s):\n\n" +
        	    programCohortDetails + "\n\n" +

        	    "🔑 **Your Login Credentials**\n" +
        	    "• User ID: " + userId + "\n" +
        	    "• Password: " + plainPassword + "\n" +
        	    "• User Type: " + userType + "\n\n" +
        	    "👉 Access your personal learning portal here: https://courses.mindfultalk.in\n" +
        	    "💡 Pro Tip: Bookmark this link for quick access anytime.\n\n" +

        	    "🤝 **Need Support?**\n" +
        	    "Our team is here whenever you need us:\n" +
        	    "• Administrator: " + orgAdminEmail + "\n" +
        	    "• Organization: " + orgName + "\n\n" +
        	    "Got questions or technical issues? Just reach out—we’ve got you covered.\n\n" +

        	    "🚀 **Next Steps**\n" +
        	    "1. Log in to your portal using the credentials above\n" +
        	    "2. Explore your enrolled programs and course materials\n" +
        	    "3. Connect with fellow learners in your cohort\n" +
        	    "4. Begin your first module and kick-start your journey\n\n" +

        	    "This is more than just a course—it’s your opportunity to unlock potential, build valuable skills, and achieve your goals. Remember, we’re here to support you every step of the way.\n\n" +
        	    "We can’t wait to celebrate your progress and growth! 🎉\n\n" +

        	    "Warm regards,\n\n" +
        	    "The Mindfultalk Team\n" +
        	    "Empowering Growth, One Mind at a Time\n\n" +
        	    "P.S. Keep an eye on your inbox for course updates, learning tips, and inspiring success stories from your fellow learners.";

        sendEmail(userEmail, subject, body);
    }
    
    public void sendCohortAssignmentEmail(String userEmail, String userName, String cohortName, 
            String programName, String orgName) {
    	logger.info("Preparing cohort assignment email for user: {}, cohort: {}, program: {}", 
                userName, cohortName, programName);
            System.out.println("Preparing cohort assignment email for user: " + userName + 
                ", cohort: " + cohortName + ", program: " + programName);
            String subject = "Your Learning Adventure Just Got Even Better! 🌟";

            String body = "Hi " + userName + ",\n\n"
                        + "We’re thrilled to welcome you to the next step in your learning journey at " + orgName + "! 💡\n\n"
                        + "Here’s what’s new for you:\n"
                        + "👉 **Program Name**: " + programName + "\n"
                        + "👉 **Cohort Name**: " + cohortName + "\n\n"
                        + "This program is designed to help you grow, connect, and achieve your goals. We're confident that you'll find it both enriching and inspiring. 🎯\n\n"
                        + "Ready to get started? Simply log in to your account here:\n"
                        + "[Access Your Program](https://courses.mindfultalk.in)\n\n"
                        + "Take this opportunity to:\n"
                        + "✅ Dive into new program content\n"
                        + "✅ Collaborate with your cohort members\n"
                        + "✅ Continue building your skills and knowledge\n\n"
                        + "Your growth matters to us, and we’re here to support you every step of the way. If you have any questions, feel free to reach out—we’ve got your back! 💪\n\n"
                        + "Let’s make this an amazing chapter in your learning journey.\n\n"
                        + "Warm regards,\n"
                        + "The Mindfultalk Team\n\n"
                        + "P.S. Remember, every step you take is one closer to achieving your goals. Let’s do this together! 🚀";

        try {
            sendEmail(userEmail, subject, body);
        } catch (Exception e) {
            logger.error("Failed to send cohort assignment email. User: {}, Error: {}", userName, e.getMessage());
            System.err.println("Failed to send cohort assignment email. User: " + userName + ", Error: " + e.getMessage());
            throw e;
        }
    }
    
    public void sendAssignmentCorrectionEmail(String assignmentId) {
        try {
            // Fetch the assignment
            UserAssignment assignment = userAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found with ID: " + assignmentId));
            
            // Check if user email is available
            User user = assignment.getUser();
            if (user == null || user.getUserEmail() == null || user.getUserEmail().trim().isEmpty()) {
                logger.info("Skipping correction email for assignment {} - user email not available", assignmentId);
                return;
            }
            
            // Get all relevant details
            String userName = user.getUserName();
            String userEmail = user.getUserEmail();
            String programName = assignment.getProgram().getProgramName();
            String stageName = assignment.getStage().getStageName();
            String unitName = assignment.getUnit().getUnitName();
            String subconceptDesc = assignment.getSubconcept().getSubconceptDesc();
            Integer maxScore = assignment.getSubconcept().getSubconceptMaxscore();
            Integer actualScore = assignment.getScore();
            String remarks = assignment.getRemarks();
            
            // Generate download link if corrected file exists
            String correctedFileLink = "";
            boolean hasCorrectedFile = assignment.getCorrectedFile() != null;
            if (hasCorrectedFile) {
                MediaFile file = assignment.getCorrectedFile();
                // Ensure the file is publicly accessible
                s3StorageService.makeFilePublic(file.getFilePath());
                // Get public URL without credentials
                correctedFileLink = s3StorageService.generatePublicUrl(file.getFilePath());
            }
            
            // Create email subject
            String subject = "Your Assignment for " + programName + " Has Been Evaluated";
            
            // Build email body
            StringBuilder bodyBuilder = new StringBuilder();
            bodyBuilder.append("Dear ").append(userName).append(",\n\n");
            bodyBuilder.append("Great news! Your assignment has been reviewed by your mentor. Here are the details:\n\n");
            bodyBuilder.append("Assignment ID: ").append(assignmentId).append("\n");
            bodyBuilder.append("Program: ").append(programName).append("\n");
            bodyBuilder.append("Stage: ").append(stageName).append("\n");
            bodyBuilder.append("Unit: ").append(unitName).append("\n");
            bodyBuilder.append("Topic: ").append(subconceptDesc).append("\n\n");
            bodyBuilder.append("Evaluation Results:\n");
            bodyBuilder.append("Score: ").append(actualScore).append(" out of ").append(maxScore).append("\n\n");
            
            if (remarks != null && !remarks.trim().isEmpty()) {
                bodyBuilder.append("Mentor's Feedback:\n").append(remarks).append("\n\n");
            }
            
            if (hasCorrectedFile) {
                bodyBuilder.append("Your mentor has also provided a corrected version of your assignment. ")
                        .append("You can access it using the link below:\n")
                        .append(correctedFileLink).append("\n\n");
            }
            
            bodyBuilder.append("Keep up the great work! Your dedication to learning is commendable.\n\n");
            bodyBuilder.append("If you have any questions about your evaluation, please feel free to reach out to your mentor.\n\n");
            bodyBuilder.append("Best regards,\n");
            bodyBuilder.append("The Mindfultalk Team");
            
            // Send the email
            sendEmail(userEmail, subject, bodyBuilder.toString());
            logger.info("Assignment correction notification email sent successfully to {}", userEmail);
            
        } catch (Exception e) {
            logger.error("Failed to send assignment correction email for assignment {}: {}", 
                    assignmentId, e.getMessage(), e);
            // Don't throw exception to prevent disrupting the main correction flow
        }
    }
    
}