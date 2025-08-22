package com.mindfultalk.growthlab.service;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.mindfultalk.growthlab.model.*;
import com.mindfultalk.growthlab.repository.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.transaction.Transactional;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class BuddhaPurnimaGreetingService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private JavaMailSender mailSender;
    
    private static final Logger logger = LoggerFactory.getLogger(BuddhaPurnimaGreetingService.class);
    
    // Define image paths as constants
    private static final String BUDDHA_PURNIMA_IMAGE = "images/buddha.png";
    private static final String LOGO_IMAGE = "images/ChipperSageLogo.png";

    @Transactional
    public void sendBuddhaPurnimaGreetings() {
        logger.info("Starting Buddha Purnima greeting email process...");
        long startTime = System.currentTimeMillis();
        // Get all users with email addresses
        List<User> usersWithEmails = userRepository.findAll().stream()
            .filter(user -> user.getUserEmail() != null && !user.getUserEmail().isEmpty())
            .collect(Collectors.toList());
        
        logger.info("Found {} users with valid emails", usersWithEmails.size());
        
        // Track successful emails
        int sentEmails = 0;
        long dbFetchTime = System.currentTimeMillis() - startTime;
        logger.info("Time taken to fetch users from database: {} ms", dbFetchTime);
        
        // Send Buddha Purnima greetings to all users
        for (User user : usersWithEmails) {
        	long emailStartTime = System.currentTimeMillis();
            try {
                sendBuddhaPurnimaGreeting(user);
                sentEmails++;
                long emailSendTime = System.currentTimeMillis() - emailStartTime;
                logger.info("Buddha Purnima greeting sent to user: {}, time taken: {} ms", 
                        user.getUserEmail(), emailSendTime);
                logger.info("Buddha Purnima greeting sent to user: {}", user.getUserEmail());
            } catch (Exception e) {
                logger.error("Failed to send Buddha Purnima greeting to user: {}. Error: {}", 
                            user.getUserEmail(), e.getMessage(), e);
                sendPlainTextFallbackEmail(user);
            }
        }
        long totalTime = System.currentTimeMillis() - startTime;
        logger.info("Completed Buddha Purnima greeting process. Sent emails to {} users in {} ms (avg {} ms per email).", 
                    sentEmails, totalTime, sentEmails > 0 ? totalTime/sentEmails : 0);
        logger.info("Completed Buddha Purnima greeting process. Sent emails to {} users.", sentEmails);
    }
    
    private void sendBuddhaPurnimaGreeting(User user) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        helper.setTo(user.getUserEmail());
        helper.setSubject("Celebrating Buddha Purnima 🌸 A Path to Enlightenment and Peace");
        
        StringBuilder emailBody = new StringBuilder()
            .append("<html><body style='font-family: Arial, sans-serif; color: #333333;'>")
            .append("<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>")
            
            // Greeting header
            .append("<div style='text-align: center; margin-bottom: 20px;'>")
            .append("<h1 style='color: #9b7653;'>Buddha Purnima Celebrations</h1>")
            .append("<h2 style='color: #b08968;'>🌸 Embracing Wisdom, Compassion & Inner Peace 🕉️</h2>")
            .append("</div>")
            
            // Personalized greeting
            .append("<p>Dear ").append(user.getUserName()).append(",</p>")
            
            // Main message body
            .append("<p>As the full moon illuminates the sky this Buddha Purnima, the ChipperSage family extends warm wishes to you on this sacred occasion. ")
            .append("This auspicious day marks the birth, enlightenment, and nirvana of Lord Buddha, offering us a moment to reflect on his timeless teachings of peace, compassion, and mindfulness.</p>")
            
            // Insert Buddha Purnima image
            .append("<div style='text-align: center; margin: 20px 0;'>")
            .append("<img src='cid:buddhaPurnimaImage' alt='Buddha Purnima' style='max-width: 100%; height: auto; border-radius: 8px;'/>")
            .append("</div>")
            
            // Fun facts about Buddha
            .append("<div style='margin: 20px 0; padding: 15px; background-color: #f7f3ee; border-radius: 8px;'>")
            .append("<h3 style='color: #9b7653; margin-top: 0;'>Did You Know? 🌟</h3>")
            .append("<ul style='color: #5a4636;'>")
            .append("<li>Buddha's given name was Siddhartha Gautama, and he was a prince who gave up royal luxuries to seek enlightenment.</li>")
            .append("<li>The Bodhi Tree under which Buddha attained enlightenment still exists in Bodh Gaya, India — it's a descendant of the original tree!</li>")
            .append("<li>Buddha's footprints, called Buddhapada, are among the earliest symbols of Buddhism before human representations were created.</li>")
            .append("<li>The word \"Buddha\" is not a name but a title meaning \"the awakened one\" or \"the enlightened one.\"</li>")
            .append("</ul>")
            .append("<p style='font-style: italic; text-align: center; color: #9b7653; margin-top: 10px;'>Just like words have different forms (like \"fish\" and \"fishes\"), Buddha's teachings have many forms but one essence: compassion.</p>")
            .append("</div>")
            
            // Inspirational message
            .append("<div style='margin: 20px 0; padding: 15px; background-color: #f1ebe5; border-radius: 8px;'>")
            .append("<h3 style='color: #9b7653; margin-top: 0;'>The Wisdom of Buddha</h3>")
            .append("<p>Buddha Purnima reminds us that the path to true happiness lies not in material possessions but in cultivating inner peace and compassion. ")
            .append("As Buddha taught, \"Peace comes from within. Do not seek it without.\"</p>")
            .append("<p style='font-style: italic; text-align: center; color: #b08968;'>\"Three things cannot be long hidden: the sun, the moon, and the truth.\" - Buddha</p>")
            .append("</div>")
            
            // Message for students and teachers
            .append("<div style='text-align: center; margin: 20px 0; padding: 15px; background-color: #e9e1d8; border-radius: 8px;'>")
            .append("<p style='font-size: 16px; color: #9b7653;'><b>To Our Dedicated Language Learners</b></p>")
            .append("<p style='font-size: 16px; color: #b08968;'>Like Buddha's journey to enlightenment, your path to English fluency requires patience, practice, and perseverance.")
            .append(" Just as he taught that every journey begins with a single step, remember that every new word and phrase you learn ")
            .append("brings you closer to your goal.</p>")
            .append("<p style='font-size: 16px; color: #b08968;'>As Buddha said, \"Drop by drop is the water pot filled.\" Similarly, day by day, your knowledge grows.</p>")
            .append("<p style='font-size: 16px; color: #b08968;'><b>May your learning journey be filled with clarity and understanding!</b></p>")
            .append("</div>")
            
            // Learning connection
            .append("<p>On this Buddha Purnima, we invite you to embrace the Buddhist value of continuous learning and growth. ")
            .append("Just as Buddha encouraged his followers to question, learn, and discover truths for themselves, ")
            .append("we encourage you to approach your language learning with the same curious and dedicated spirit.</p>")
            
            // Mindful activity suggestion
            .append("<div style='margin: 20px 0; padding: 15px; background-color: #f5f2ee; border-radius: 8px;'>")
            .append("<h3 style='color: #9b7653; margin-top: 0;'>Mindful Learning Activity 🍃</h3>")
            .append("<p>Try this mindful language practice inspired by Buddha's teachings: Take 5 minutes to focus solely on reading a short English passage. ")
            .append("Notice the shape of each word, its meaning, and how the words connect. When your mind wanders (and it will!), ")
            .append("gently bring your attention back to the text without judgment. This mindfulness practice can deepen both your concentration and comprehension.</p>")
            .append("</div>")
            
            // CTA Button
            .append("<div style='text-align: center; margin: 25px 0;'>")
            .append("<a href='https://flowofenglish.thechippersage.com' ")
            .append("style='display: inline-block; padding: 12px 25px; background: linear-gradient(135deg, #9b7653, #b08968); ")
            .append("color: #ffffff; text-decoration: none; border-radius: 5px; font-weight: bold; box-shadow: 0 4px 8px rgba(0,0,0,0.1);'>")
            .append("Continue Your Learning Path</a>")
            .append("</div>")
            
            // Support Information
            .append("<p>For any assistance during this festive period, please contact us at ")
            .append("<a href='mailto:support@thechippersage.com'>support@thechippersage.com</a>.</p>")
            
            // Sign-off
            .append("<p>Wishing you peace and enlightenment,</p>")
            .append("<p><b>The ChipperSage Team 🌟</b></p>")
            
            // Footer with Logo
            .append("<div style='margin-top: 30px; border-top: 1px solid #dddddd; padding-top: 20px; text-align: center;'>")
            .append("<img src='cid:logoImage' alt='ChipperSage Logo' style='max-width: 150px; height: auto;'/>")
            .append("<p style='color: #777777; font-size: 12px;'>© 2025 ChipperSage. All rights reserved.</p>")
            .append("</div>")
            
            .append("</div></body></html>");

        helper.setText(emailBody.toString(), true);
        
        // Add images as inline attachments
        helper.addInline("buddhaPurnimaImage", new ClassPathResource(BUDDHA_PURNIMA_IMAGE));
        helper.addInline("logoImage", new ClassPathResource(LOGO_IMAGE));
        
        mailSender.send(mimeMessage);
    }
    
    // Fallback method to send plain text emails if HTML email fails
    private void sendPlainTextFallbackEmail(User user) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(user.getUserEmail());
            message.setSubject("Celebrating Buddha Purnima - A Path to Enlightenment and Peace");
            
            StringBuilder emailBody = new StringBuilder();
            
            emailBody.append("Dear ").append(user.getUserName()).append(",\n\n")
                .append("As the full moon illuminates the sky this Buddha Purnima, the ChipperSage family extends warm wishes to you on this sacred occasion. ")
                .append("This auspicious day marks the birth, enlightenment, and nirvana of Lord Buddha, offering us a moment to reflect on his timeless teachings of peace, compassion, and mindfulness.\n\n")
                .append("DID YOU KNOW?\n")
                .append("- Buddha's given name was Siddhartha Gautama, and he was a prince who gave up royal luxuries to seek enlightenment.\n")
                .append("- The Bodhi Tree under which Buddha attained enlightenment still exists in Bodh Gaya, India.\n")
                .append("- The word \"Buddha\" is not a name but a title meaning \"the awakened one\" or \"the enlightened one\".\n\n")
                .append("Buddha Purnima reminds us that the path to true happiness lies not in material possessions but in cultivating inner peace and compassion. ")
                .append("As Buddha taught, \"Peace comes from within. Do not seek it without.\"\n\n")
                .append("To Our Dedicated Language Learners: Like Buddha's journey to enlightenment, your path to English fluency requires patience, practice, and perseverance. ")
                .append("As Buddha said, \"Drop by drop is the water pot filled.\" Similarly, day by day, your knowledge grows.\n\n")
                .append("Continue your learning journey: https://flowofenglish.thechippersage.com\n\n")
                .append("For any assistance, please contact us at support@thechippersage.com.\n\n")
                .append("Wishing you peace and enlightenment,\n")
                .append("The ChipperSage Team");
            
            message.setText(emailBody.toString());
            mailSender.send(message);
            
            logger.info("Fallback plain text email sent to user: {}", user.getUserEmail());
        } catch (Exception e) {
            logger.error("Failed to send even fallback email to user: {}. Error: {}", user.getUserEmail(), e.getMessage(), e);
        }
    }
    
    // Method to send a test email to a specific email address
    public void sendTestBuddhaPurnimaEmail(String emailAddress) {
        try {
            // Create a dummy user for testing
            User testUser = new User();
            testUser.setUserName("Test User");
            testUser.setUserEmail(emailAddress);
            
            // Send the Buddha Purnima email
            sendBuddhaPurnimaGreeting(testUser);
            logger.info("Test Buddha Purnima email sent to: {}", emailAddress);
        } catch (Exception e) {
            logger.error("Failed to send test Buddha Purnima email to: {}. Error: {}", emailAddress, e.getMessage(), e);
            
            // Try fallback email
            User testUser = new User();
            testUser.setUserName("Test User");
            testUser.setUserEmail(emailAddress);
            sendPlainTextFallbackEmail(testUser);
        }
    }
}