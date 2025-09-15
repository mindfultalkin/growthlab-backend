package com.mindfultalk.growthlab.service;

import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
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
    
    public void sendEmail(String to, String subject, String body, boolean isHtml) {
    	logger.info("Attempting to send {} email to: {}", isHtml ? "HTML" : "plain text", to);
        
    	try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, isHtml); // if true → HTML, else plain text

            mailSender.send(message);

            logger.info("Email successfully sent to: {}", to);
        } catch (Exception e) {
            logger.error("Failed to send email to: {}. Error: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send email", e);
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

    
    public void sendUserCreationEmail(String userEmail, String userName, String userId, String plainPassword, List<String> programNames,
    		List<String> cohortNames, String orgAdminEmail, String orgName, String userType) {

        String subject = "🎉 Welcome to Your Learning Journey with Mindfultalk!";

        // Build programs & cohorts
        StringBuilder programCohortDetails = new StringBuilder();
        int maxSize = Math.max(programNames.size(), cohortNames != null ? cohortNames.size() : 0);

        for (int i = 0; i < maxSize; i++) {
            programCohortDetails.append("<p style='margin:5px 0;'>• ");
            if (i < programNames.size()) {
                programCohortDetails.append("<strong>Program:</strong> ").append(programNames.get(i));
            }
            if (cohortNames != null && i < cohortNames.size()) {
                if (i < programNames.size()) {
                    programCohortDetails.append(" | ");
                }
                programCohortDetails.append("<strong>Cohort:</strong> ").append(cohortNames.get(i));
            }
            programCohortDetails.append("</p>");
        }

        // HTML email body
        String body = String.format(
            "<html><body style='font-family: Arial, sans-serif; line-height:1.6; color:#333;'>"
            + "<p>Dear <strong>%s</strong>,</p>"

            + "<p>What an exciting day! 🌟 <strong>Welcome to your transformative learning journey with Mindfultalk!</strong></p>"

            + "<h4 style='color:#2c3e50;'>📚 Your Enrolled Programs</h4>"
            + "%s"

            + "<h4 style='color:#2c3e50;'>🔐 Your Secure Login Credentials</h4>"
            + "<p><em>(Please keep these safe and handy!)</em></p>"
            + "<ul>"
            + "<li><strong>User ID:</strong> %s</li>"
            + "<li><strong>Password:</strong> %s</li>"
            + "<li><strong>User Type:</strong> %s</li>"
            + "</ul>"

            + "<h4 style='color:#2c3e50;'>🚀 Access Your Learning Portal</h4>"
            + "<p>Ready to dive in? Your personal learning hub awaits:<br>"
            + "<a href='https://courses.mindfultalk.in' style='color:#1a73e8; font-weight:bold;'>👉 https://courses.mindfultalk.in</a></p>"
            + "<p><em>💡 Pro Tip: Bookmark this link and add it to your home screen for lightning-fast access!</em></p>"

            + "<h4 style='color:#2c3e50;'>🤝 Your Dedicated Support Team</h4>"
            + "<ul>"
            + "<li><strong>Your Administrator:</strong> %s</li>"
            + "<li><strong>Organization:</strong> %s</li>"
            + "</ul>"

            + "<h4 style='color:#2c3e50;'>✨ Your Exciting Next Steps</h4>"
            + "<ol>"
            + "<li>🎯 <strong>Log into your portal</strong> using the credentials above</li>"
            + "<li>🔍 <strong>Explore your programs</strong> and get familiar with the course materials</li>"
            + "<li>📖 <strong>Start your first module</strong> and begin this amazing transformation</li>"
            + "</ol>"

            + "<hr>"

            + "<p><strong>This isn't just another course</strong> - this is your opportunity to unlock potential you didn't even know you had, build skills that will serve you for life, and achieve goals that once seemed impossible.</p>"

            + "<p>I want you to know that <strong>we're genuinely invested in your success</strong>. Every resource, every lesson, every support interaction has been designed with your growth in mind. Your success truly is our mission, and we can't wait to be part of your journey!</p>"

            + "<p><strong>Here's to your bright future and all the amazing discoveries ahead! 🎊</strong></p>"

            + "<p>With genuine excitement for your journey,</p>"
            + "<p><strong>The Mindfultalk Team</strong><br>"
            + "<em>\"Empowering Growth, One Mind at a Time\"</em></p>"

            + "<hr>"
            + "<p><em>P.S. Keep your eyes peeled for updates, learning tips, and inspiring success stories. You're part of an incredible community now! 💌</em></p>"

            + "</body></html>",
            userName,
            programCohortDetails.toString(),
            userId,
            plainPassword,
            userType,
            orgAdminEmail,
            orgName
        );

        // Make sure sendEmail sets MIME type to HTML
        sendEmail(userEmail, subject, body, true); // true = HTML
    }

    
    public void sendCohortAssignmentEmail(String userEmail, String userName, String cohortName,
            String programName, String orgName) {
            logger.info("Preparing cohort assignment email for user: {}, cohort: {}, program: {}",
            userName, cohortName, programName);
            System.out.println("Preparing cohort assignment email for user: " + userName +
            ", cohort: " + cohortName + ", program: " + programName);

            String subject = "Your Learning Adventure Just Got Even Better! 🌟";

            String body = String.format(
            "<html><body style='font-family: Arial, sans-serif; line-height:1.6; color:#333;'>"
            + "<p>Hi <strong>%s</strong>,</p>"

            + "<p>We’re thrilled to welcome you to the next step in your learning journey at <strong>%s</strong>! 💡</p>"

            + "<h4 style='color:#2c3e50;'>📚 Here’s what’s new for you: </h4>"
            + "<ul>"
            + "<li><strong>Program Name:</strong> %s</li>"
            + "<li><strong>Cohort Name:</strong> %s</li>"
            + "</ul>"

            + "<p>This program is designed to help you grow, connect, and achieve your goals. We're confident that you'll find it both enriching and inspiring. 🎯</p>"

            + "<h4 style='color:#2c3e50;'>🚀 Access Your Learning Portal</h4>"
            + "<p>Ready to get started? Simply log in to your account here:<br>"
            + "<a href='https://courses.mindfultalk.in' style='color:#1a73e8; font-weight:bold;'>👉 Access Your Program</a></p>"

            + "<h4 style='color:#2c3e50;'>✨ Take this opportunity to:</h4>"
            + "<ul>"
            + "<li>✅ Dive into new program content</li>"
            + "<li>✅ Continue building your skills and knowledge</li>"
            + "</ul>"

            + "<p>Your growth matters to us, and we’re here to support you every step of the way. "
            + "If you have any questions, feel free to reach out—we’ve got your back! 💪</p>"

            + "<p>Let’s make this an amazing chapter in your learning journey.</p>"

            + "<p>Warm regards,<br><strong>The Mindfultalk Team</strong></p>"

            + "<hr>"
            + "<p><em>P.S. Remember, every step you take is one closer to achieving your goals. "
            + "Let’s do this together! 🚀</em></p>"

            + "</body></html>",
            userName, orgName, programName, cohortName
            );

            try {
            sendEmail(userEmail, subject, body, true); // true = HTML
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
            sendEmail(userEmail, subject, bodyBuilder.toString(), false);
            logger.info("Assignment correction notification email sent successfully to {}", userEmail);
            
        } catch (Exception e) {
            logger.error("Failed to send assignment correction email for assignment {}: {}", 
                    assignmentId, e.getMessage(), e);
            // Don't throw exception to prevent disrupting the main correction flow
        }
    }
    
}