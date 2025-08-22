package com.mindfultalk.growthlab.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.mindfultalk.growthlab.model.*;
import com.mindfultalk.growthlab.repository.*;
import com.mindfultalk.growthlab.service.*;
import org.slf4j.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/v1/test")
public class TestEmailController {

    private static final Logger logger = LoggerFactory.getLogger(TestEmailController.class);
    
    @Autowired
    private BuddhaPurnimaGreetingService buddhaPurnimaGreetingService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private WeeklyReportService weeklyReportService;
    
    @GetMapping("/weekly-report/email/{userId}")
    public String sendTestWeeklyReportEmail(@PathVariable String userId) {
        logger.info("Received request to send test Weekly Report email to user ID: {}", userId);

        Optional<User> userOptional = userRepository.findByUserId(userId);
        if (!userOptional.isPresent()) {
            logger.error("User not found with ID: {}", userId);
            return "User not found with ID: " + userId;
        }

        User user = userOptional.get();
        if (user.getUserEmail() == null || user.getUserEmail().isEmpty()) {
            logger.error("User with ID: {} has no email address", userId);
            return "User with ID: " + userId + " has no email address";
        }

        try {
            // This will only send to this one user (no admin report)
            List<User> singleUserList = Collections.singletonList(user);
            int successCount = weeklyReportService.weeklyReportServiceTestSingleUser(singleUserList);

            logger.info("Test Weekly Report email sent to {} ({}), Success count: {}", 
                        user.getUserName(), user.getUserEmail(), successCount);

            return "Test Weekly Report email sent to " + user.getUserName() +
                   " (" + user.getUserEmail() + "). Success count: " + successCount;

        } catch (Exception e) {
            logger.error("Error sending test Weekly Report email to user ID: {}, Error: {}", userId, e.getMessage(), e);
            return "Error sending test Weekly Report email: " + e.getMessage();
        }
    }

    @GetMapping("/good-friday/email/{userId}")
    public String sendTestGoodFridayEmail(@PathVariable String userId) {
        logger.info("Received request to send test Good Friday email to user ID: {}", userId);
        
        // Find user by userId - using Optional since that's what the repository returns
        Optional<User> userOptional = userRepository.findByUserId(userId);
        
        if (!userOptional.isPresent()) {
            logger.error("User not found with ID: {}", userId);
            return "User not found with ID: " + userId;
        }
        
        // Get the actual User object from the Optional
        User user = userOptional.get();
        
        if (user.getUserEmail() == null || user.getUserEmail().isEmpty()) {
            logger.error("User with ID: {} has no email address", userId);
            return "User with ID: " + userId + " has no email address";
        }
        
        try {
            // Current timestamp
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String timestamp = now.format(formatter);
            
            // Send test email to the actual user
            buddhaPurnimaGreetingService.sendTestBuddhaPurnimaEmail(user.getUserEmail());
            
            logger.info("Test Good Friday email sent to user: {}, email: {} at {}", 
                        user.getUserName(), user.getUserEmail(), timestamp);
                        
            return "Test Good Friday email sent to " + user.getUserName() + 
                   " (" + user.getUserEmail() + ") at " + timestamp;
                   
        } catch (Exception e) {
            logger.error("Error sending test Good Friday email to user ID: {}, Error: {}", 
                        userId, e.getMessage(), e);
            return "Error sending test email: " + e.getMessage();
        }
    }
    
    @GetMapping("/good-friday/email/manual")
    public String sendManualTestEmail() {
        // For the specific user mentioned in the requirement
        String userId = "Harikrishna05";
        String username = "Harikrishna";
        String email = "harikrishna055hari@gmail.com";
        
        logger.info("Sending manual test Good Friday email to: {}", email);
        
        try {
            // Create timestamp for current time
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
            String timestamp = now.format(formatter);
            
            // Use the test method from the service
            buddhaPurnimaGreetingService.sendTestBuddhaPurnimaEmail(email);
            
            logger.info("Manual test Good Friday email sent to {} at {}", email, timestamp);
            
            return "Manual test Good Friday email sent to " + username + 
                   " (" + email + ") at " + timestamp;
                   
        } catch (Exception e) {
            logger.error("Error sending manual test Good Friday email: {}", e.getMessage(), e);
            return "Error sending manual test email: " + e.getMessage();
        }
    }
}