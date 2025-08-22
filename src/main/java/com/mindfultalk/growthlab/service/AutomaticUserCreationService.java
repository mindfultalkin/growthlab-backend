package com.mindfultalk.growthlab.service;

//import com.FlowofEnglish.model.*;
//import com.FlowofEnglish.repository.*;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.Optional;
//import java.util.UUID;
//
//@Service
//public class AutomaticUserCreationService {
//
//private static final Logger logger =
//LoggerFactory.getLogger(AutomaticUserCreationService.class);
//
//@Autowired
//private UserRepository userRepository;
//
//@Autowired
//private CohortRepository cohortRepository;
//
//@Autowired
//private UserCohortMappingRepository userCohortMappingRepository;
//
//@Autowired
//private PasswordEncoder passwordEncoder;
//
//@Autowired
//private EmailService emailService;
//
//// The default password that every new user is assigned
//private static final String DEFAULT_PASSWORD = "Welcome123";
//
//// Payment status constants
//private static final String STATUS_PAID = "PAID";
//private static final String STATUS_FAILED = "FAILED";
//
///**
//* Creates a user after payment is confirmed
//* @param subscription The subscription with payment details
//*/
//@Transactional
//public void createUserFromSubscription(ProgramSubscription subscription) {
//// Check if payment status is PAID
//if (!STATUS_PAID.equals(subscription.getStatus())) {
//logger.info("Skipping user creation for subscription ID: {} as payment status
//is: {}",
//subscription.getSubscriptionId(), subscription.getStatus());
//return;
//}
//
//try {
//// Check if user already exists with the email
//Optional<User> existingUser =
//userRepository.findByUserEmail(subscription.getUserEmail());
//if (existingUser.isPresent()) {
//logger.info("User already exists with email: {}",
//subscription.getUserEmail());
//
//// Optionally, you might want to update existing user or enroll them in the
//new program
//enrollUserInDefaultCohort(existingUser.get(), subscription.getProgram());
//return;
//}
//
//// Create new user
//User user = new User();
//
//// Generate userId from userName (remove spaces)
//String userId = generateUserIdFromName(subscription.getUserName());
//user.setUserId(userId);
//
//user.setUserName(subscription.getUserName());
//user.setUserEmail(subscription.getUserEmail());
//user.setUserPhoneNumber(subscription.getUserPhoneNumber());
//user.setUserAddress(subscription.getUserAddress());
//user.setOrganization(subscription.getOrganization());
//user.setUserType("learner"); // Default user type
//user.setUserPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
//
//// Save the user
//User savedUser = userRepository.save(user);
//logger.info("User created successfully: {}", savedUser.getUserId());
//
//// Enroll user in default cohort for the program
//enrollUserInDefaultCohort(savedUser, subscription.getProgram());
//
//// Send welcome email with credentials
//sendWelcomeEmail(savedUser, subscription.getProgram().getProgramName());
//
//} catch (Exception e) {
//logger.error("Error creating user from subscription: {}", e.getMessage(), e);
//throw new RuntimeException("Failed to create user after payment", e);
//}
//}
//
///**
//* Generates a userId from the user's name by removing spaces
//*/
//private String generateUserIdFromName(String userName) {
//if (userName == null || userName.trim().isEmpty()) {
//return "user_" + UUID.randomUUID().toString().substring(0, 8);
//}
//
//// Remove spaces and special characters
//String sanitizedId = userName.replaceAll("\\s+",
//"").replaceAll("[^a-zA-Z0-9]", "");
//
//// Check if userId already exists
//if (userRepository.existsById(sanitizedId)) {
//// Append random string to make it unique
//sanitizedId += "_" + UUID.randomUUID().toString().substring(0, 5);
//}
//
//return sanitizedId;
//}
//
///**
//* Enrolls a user in the default cohort for a program
//*/
//private void enrollUserInDefaultCohort(User user, Program program) {
//// Find default cohort for the program
//Optional<Cohort> defaultCohortOpt = findDefaultCohort(program);
//
//if (defaultCohortOpt.isPresent()) {
//Cohort defaultCohort = defaultCohortOpt.get();
//
//// Check if mapping already exists
//boolean mappingExists =
//userCohortMappingRepository.existsByUserUserIdAndCohortCohortId(
//user.getUserId(), defaultCohort.getCohortId());
//
//if (!mappingExists) {
//// Create UserCohortMapping
//UserCohortMapping mapping = new UserCohortMapping();
//mapping.setUser(user);
//mapping.setCohort(defaultCohort);
//mapping.setLeaderboardScore(0);
//mapping.setUuid(UUID.randomUUID().toString());
//
//userCohortMappingRepository.save(mapping);
//logger.info("User {} enrolled in cohort {}", user.getUserId(),
//defaultCohort.getCohortId());
//} else {
//logger.info("User {} is already enrolled in cohort {}", user.getUserId(),
//defaultCohort.getCohortId());
//}
//} else {
//logger.warn("No default cohort found for program {}",
//program.getProgramId());
//}
//}
//
///**
//* Finds the default cohort for a program
//* Strategy: Get the first active cohort for the program
//*/
//private Optional<Cohort> findDefaultCohort(Program program) {
//// Find cohorts associated with the program that are active
//return cohortRepository.findByProgramIdAndActive(program.getProgramId(),
//true)
//.stream()
//.findFirst();
//}
//
///**
//* Sends a welcome email with login credentials
//*/
//private void sendWelcomeEmail(User user, String programName) {
//try {
//// Compose email content
//String subject = "Welcome to Flow of English Program";
//
//StringBuilder content = new StringBuilder();
//content.append("Dear ").append(user.getUserName()).append(",\n\n");
//content.append("Welcome to the Flow of English program:
//").append(programName).append(".\n\n");
//content.append("Your account has been created and you can now access our
//learning platform.\n\n");
//content.append("Your login details:\n");
//content.append("Username: ").append(user.getUserId()).append("\n");
//content.append("Password: ").append(DEFAULT_PASSWORD).append(" (Please change
//after first login)\n\n");
//content.append("Thank you for joining us!\n\n");
//content.append("Best regards,\n");
//content.append("The Chippersage Team");
//
//// Send email
//emailService.sendSimpleMessage(user.getUserEmail(), subject,
//content.toString());
//logger.info("Welcome email sent to user: {}", user.getUserEmail());
//} catch (Exception e) {
//logger.error("Failed to send welcome email to user {}: {}",
//user.getUserEmail(), e.getMessage());
//// Don't throw exception here, as user creation should still succeed even if
//email fails
//}
//}
//}