package com.mindfultalk.growthlab.service;


import com.mindfultalk.growthlab.model.*;
import com.mindfultalk.growthlab.repository.*;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WebhookService {
    
    private static final Logger logger = LoggerFactory.getLogger(WebhookService.class);
    
    @Autowired
    private ProgramSubscriptionRepository subscriptionRepository;
    
    @Autowired
    private ProgramRepository programRepository;
    
    @Autowired
    private OrganizationRepository organizationRepository;
    
    @Autowired
    private PaymentEventRepository paymentEventRepository;
    
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CohortProgramRepository cohortProgramRepository;

    @Autowired
    private UserCohortMappingRepository userCohortMappingRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    // Default password for new users
    private static final String DEFAULT_PASSWORD = "Welcome123";
    
 // Payment status constants
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_AUTHORIZED = "AUTHORIZED";
    private static final String STATUS_PAID = "PAID";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_DISPUTED = "DISPUTED";
    
    @Transactional
    public void processWebhookEvent(String eventType, JSONObject payload) {
        logger.info("Processing event type: {}", eventType);
        
        try {
            JSONObject paymentEntity = null;
            JSONObject orderEntity = null;
            String paymentId = null;
            String orderId = null;
            
         // Extract the entities based on event type
            switch (eventType) {
                case "payment.authorized":
                case "payment.captured":
                case "payment.failed":
                    paymentEntity = payload.getJSONObject("payload").getJSONObject("payment").getJSONObject("entity");
                    paymentId = paymentEntity.getString("id");
                    orderId = paymentEntity.has("order_id") ? paymentEntity.getString("order_id") : null;
                    break;
                    
                case "order.paid":
                    orderEntity = payload.getJSONObject("payload").getJSONObject("order").getJSONObject("entity");
                    paymentEntity = payload.getJSONObject("payload").getJSONObject("payment").getJSONObject("entity");
                    paymentId = paymentEntity.getString("id");
                    orderId = orderEntity.getString("id");
                    break;
                    
                case "payment.dispute.created":
                case "payment.dispute.won":
                case "payment.dispute.lost":
                    paymentEntity = payload.getJSONObject("payload").getJSONObject("payment").getJSONObject("entity");
                    paymentId = paymentEntity.getString("id");
                    handlePaymentDispute(paymentEntity, eventType);
                    break;
                    
                case "refund.created":
                case "refund.processed":
                case "refund.failed":
                    JSONObject refundEntity = payload.getJSONObject("payload").getJSONObject("refund").getJSONObject("entity");
                    paymentId = refundEntity.getString("payment_id");
                    handleRefund(refundEntity, eventType);
                    break;
                    
                default:
                    logger.info("Unhandled event type: {}", eventType);
//                 // Don't record payment event here, it's already recorded in WebhookController
//                    recordPaymentEvent(eventType, payload, null, null);
                    return;
            }
         // Find associated subscription
            Optional<ProgramSubscription> subscriptionOpt;
            Long subscriptionId = null;
            
            if (paymentId != null) {
                subscriptionOpt = subscriptionRepository.findByTransactionId(paymentId);
                if (!subscriptionOpt.isPresent() && orderId != null) {
                    subscriptionOpt = subscriptionRepository.findByTransactionId(orderId);
                }
            } else if (orderId != null) {
                subscriptionOpt = subscriptionRepository.findByTransactionId(orderId);
            } else {
                subscriptionOpt = Optional.empty();
            }
            
            if (subscriptionOpt.isPresent()) {
                subscriptionId = subscriptionOpt.get().getSubscriptionId();
                
                // Update the payment event that was already created in WebhookController
                updateExistingPaymentEvent(eventType, paymentId, orderId, subscriptionId);
            }
            
//            // Record the payment event
//            PaymentEvent event = recordPaymentEvent(eventType, payload, paymentId, orderId);
//            
//            // Fix for NullPointerException - check if event is null
//            if (event != null && subscriptionId != null) {
//                event.setSubscriptionId(subscriptionId);
//                paymentEventRepository.save(event);
//            }
            
            // Process different event types
            switch (eventType) {
            case "payment.captured":
                handlePaymentCaptured(paymentEntity);
                break;
            case "payment.failed":
                handlePaymentFailed(paymentEntity);
                break;
            case "order.paid":
                handleOrderPaid(orderEntity, paymentEntity);
                break;
        }
        
    } catch (Exception e) {
        logger.error("Error processing webhook: {}", e.getMessage(), e);
        throw new RuntimeException("Error processing webhook event", e);
    }
}
 // New method to update the existing payment event with subscription ID
    private void updateExistingPaymentEvent(String eventType, String paymentId, String orderId, Long subscriptionId) {
        if (subscriptionId == null) return;
        
        try {
            List<PaymentEvent> events;
            if (paymentId != null) {
                events = paymentEventRepository.findByPaymentId(paymentId);
            } else if (orderId != null) {
                events = paymentEventRepository.findByOrderId(orderId);
            } else {
                return;
            }
            
            // Find the most recent event of this type
            Optional<PaymentEvent> eventOpt = events.stream()
                .filter(e -> e.getEventType().equals(eventType))
                .findFirst();
                
            if (eventOpt.isPresent()) {
                PaymentEvent event = eventOpt.get();
                event.setSubscriptionId(subscriptionId);
                paymentEventRepository.save(event);
                logger.info("Updated payment event with subscription ID: {}", subscriptionId);
            }
        } catch (Exception e) {
            logger.error("Error updating payment event with subscription ID: {}", e.getMessage(), e);
        }
    }
    
    private PaymentEvent recordPaymentEvent(String eventType, JSONObject payload, String paymentId, String orderId) {
        try {
            PaymentEvent event = new PaymentEvent();
            event.setEventType(eventType);
            event.setPaymentId(paymentId);
            event.setOrderId(orderId);
            
            // Extract amount and error details if available
            Double amount = null;
            String status = null;
            String errorCode = null;
            String errorDescription = null;
            
            if (payload.has("payload")) {
                JSONObject payloadObj = payload.getJSONObject("payload");
                
                if (payloadObj.has("payment") && payloadObj.getJSONObject("payment").has("entity")) {
                    JSONObject payment = payloadObj.getJSONObject("payment").getJSONObject("entity");
                    if (payment.has("amount")) {
                        amount = payment.getDouble("amount") / 100.0;
                    }
                    if (payment.has("status")) {
                        status = payment.getString("status");
                    }
                    
                    // error_code handling
                    if (payment.has("error_code") && !payment.isNull("error_code")) {
                        Object error = payment.get("error_code");
                        errorCode = String.valueOf(error);
                    }
                    
                    // error_description handling
                    if (payment.has("error_description") && !payment.isNull("error_description")) {
                        Object error = payment.get("error_description");
                        errorDescription = String.valueOf(error);
                    }
                }
            }
            
            event.setAmount(amount);
            event.setStatus(status);
            event.setErrorCode(errorCode);
            event.setErrorDescription(errorDescription);
            event.setRawPayload(payload.toString());
            
            return paymentEventRepository.save(event);
        } catch (Exception e) {
            logger.error("Error recording payment event: {}", e.getMessage(), e);
            return null;
        }
    }
    
    private void handlePaymentAuthorized(JSONObject paymentEntity) {
        String paymentId = paymentEntity.getString("id");
        String orderId = paymentEntity.has("order_id") ? paymentEntity.getString("order_id") : null;
        double amount = paymentEntity.getDouble("amount") / 100.0; // Convert from paise to rupees
        
        logger.info("Payment authorized: Payment ID={}, Order ID={}, Amount={}", paymentId, orderId, amount);
        
        // Create subscription with AUTHORIZED status
        try {
            createOrUpdateSubscription(paymentEntity, STATUS_AUTHORIZED);
        } catch (Exception e) {
            logger.error("Error creating subscription: {}", e.getMessage(), e);
        }
        
        // Send email notification logic here if needed
    }
     
    private void handlePaymentCaptured(JSONObject paymentEntity) {
        String paymentId = paymentEntity.getString("id");
        String orderId = paymentEntity.getString("order_id");
        double amount = paymentEntity.getDouble("amount") / 100.0; // Convert from paise to rupees

        logger.info("Payment captured: Payment ID={}, Order ID={}, Amount={}", paymentId, orderId, amount);
     // Update existing subscription or create new one with PAID status
        try {
            Optional<ProgramSubscription> existingSubscription = subscriptionRepository.findByTransactionId(paymentId);

            if (existingSubscription.isPresent()) {
                ProgramSubscription subscription = existingSubscription.get();
                subscription.setStatus(STATUS_PAID);
                subscription.setTransactionDate(OffsetDateTime.now());
                subscriptionRepository.save(subscription);
                logger.info("Updated subscription status to PAID: {}", subscription.getSubscriptionId());
            } else {
                createOrUpdateSubscription(paymentEntity, STATUS_PAID);
            }

            // ✅ Create user only if payment succeeded
            createUserFromPaidSubscription(paymentEntity);

        } catch (Exception e) {
            logger.error("Error updating subscription: {}", e.getMessage(), e);
        }

     // Send email notification logic here if needed
    }

    private void handlePaymentFailed(JSONObject paymentEntity) {
        String paymentId = paymentEntity.getString("id");
        String orderId = paymentEntity.has("order_id") ? paymentEntity.getString("order_id") : "N/A";
        double amount = paymentEntity.getDouble("amount") / 100.0; // Convert from paise to rupees
        String errorCode = paymentEntity.has("error_code") ? paymentEntity.getString("error_code") : "unknown";
        String errorDescription = paymentEntity.has("error_description") ? paymentEntity.getString("error_description") : "Unknown error";
        
        logger.info("Payment failed: Payment ID={}, Order ID={}, Amount={}, Error={}", paymentId, orderId, amount, errorCode);
        
        // Create or update subscription with FAILED status
        try {
            Optional<ProgramSubscription> existingSubscription = subscriptionRepository.findByTransactionId(paymentId);
            
            if (existingSubscription.isPresent()) {
                ProgramSubscription subscription = existingSubscription.get();
                subscription.setStatus(STATUS_FAILED);
                subscription.setTransactionDate(OffsetDateTime.now());
                subscriptionRepository.save(subscription);
                logger.info("Updated subscription status to FAILED: {}", subscription.getSubscriptionId());
            } else {
                createOrUpdateSubscription(paymentEntity, STATUS_FAILED);
            }
        } catch (Exception e) {
            logger.error("Error updating subscription: {}", e.getMessage(), e);
        }
        
        // Send email notification logic here if needed
    }
    
    private void handleOrderPaid(JSONObject orderEntity, JSONObject paymentEntity) {
        String paymentId = paymentEntity.getString("id");
        String orderId = orderEntity.getString("id");
        double amount = paymentEntity.getDouble("amount") / 100.0; // Convert from paise to rupees
        
        logger.info("Order paid: Order ID={}, Amount={}", orderId, amount);
        
        try {
            Optional<ProgramSubscription> existingSubscription = subscriptionRepository.findByTransactionId(paymentId);
            if (existingSubscription.isPresent()) {
                ProgramSubscription subscription = existingSubscription.get();
                subscription.setStatus(STATUS_PAID);
                subscription.setTransactionDate(OffsetDateTime.now());
                subscriptionRepository.save(subscription);
                logger.info("Updated subscription status to PAID: {}", subscription.getSubscriptionId());
            } else {
                createOrUpdateSubscription(paymentEntity, STATUS_PAID);
            }

//            // ✅ Create user only if payment succeeded
//            createUserFromPaidSubscription(paymentEntity);

        } catch (Exception e) {
            logger.error("Error creating subscription: {}", e.getMessage(), e);
        }
        
        // Send email notification logic here if needed
    }
    
    private void handlePaymentDispute(JSONObject paymentEntity, String eventType) {
        String paymentId = paymentEntity.getString("id");
        logger.info("Payment dispute event: {}, Payment ID={}", eventType, paymentId);
        
        // Update subscription status based on dispute event
        Optional<ProgramSubscription> existingSubscription = subscriptionRepository.findByTransactionId(paymentId);
        if (existingSubscription.isPresent()) {
            ProgramSubscription subscription = existingSubscription.get();
            subscription.setStatus(STATUS_DISPUTED);
            subscriptionRepository.save(subscription);
            logger.info("Updated subscription status to DISPUTED: {}", subscription.getSubscriptionId());
        }
    }
    
    private void handleRefund(JSONObject refundEntity, String eventType) {
        String paymentId = refundEntity.getString("payment_id");
        logger.info("Refund event: {}, Payment ID={}", eventType, paymentId);
        
        // You can implement specific logic for handling refunds based on your business requirements
        // For example, you might want to mark the subscription as "REFUNDED" or reduce the subscription period
    }
    
    
//    @Transactional
//    private void createOrUpdateSubscription(JSONObject paymentEntity, String status) {
//        // Only proceed if status is PAID or FAILED
//        if (!STATUS_PAID.equals(status) && !STATUS_FAILED.equals(status)) {
//            logger.info("Skipping subscription update for status: {}", status);
//            return;
//        }
//        
//        // Extract the necessary data from the payment entity
//        String paymentId = paymentEntity.getString("id");
//        double totalAmount = paymentEntity.getDouble("amount") / 100.0; // Total payment amount
//        
//        // Extract user information
//        JSONObject notes = paymentEntity.has("notes") ? paymentEntity.getJSONObject("notes") : new JSONObject();
//        String userEmail = notes.has("user_email") ? notes.getString("user_email") : extractEmailFromPayment(paymentEntity);
//        String userName = notes.has("user_name") ? notes.getString("user_name") : "Unknown User";
//        String userPhone = notes.has("user_phone") ? notes.getString("user_phone") : null;
//        String userAddress = notes.has("user_address") ? notes.getString("user_address") : null;
//        
//        // Check if this is a multi-program enrollment
//        JSONArray enrollments = null;
//        if (notes.has("enrollments")) {
//            try {
//                // First try to parse as a JSONArray directly
//                try {
//                    enrollments = notes.getJSONArray("enrollments");
//                } catch (Exception e) {
//                    // If that fails, try parsing as a string
//                    String enrollmentsStr = notes.getString("enrollments");
//                    enrollments = new JSONArray(enrollmentsStr);
//                    logger.info("Successfully parsed enrollments string: {}", enrollmentsStr);
//                }
//            } catch (Exception e) {
//                logger.error("Error parsing enrollments array or string: {}", e.getMessage());
//            }
//        }
//        boolean processedEnrollment = false;
//        
//        if (enrollments != null && enrollments.length() > 0) {
//            // Process multiple enrollments
//            processedEnrollment = true;
//            logger.info("Processing multi-program enrollment with {} programs", enrollments.length());
//            for (int i = 0; i < enrollments.length(); i++) {
//                JSONObject enrollment = enrollments.getJSONObject(i);
//                String programId = enrollment.getString("program_id");
//                String organizationId = enrollment.getString("organization_id");
//                double amount = enrollment.has("amount") ? enrollment.getDouble("amount") : (totalAmount / enrollments.length());
//                Integer maxCohorts = enrollment.has("max_cohorts") ? enrollment.getInt("max_cohorts") : 1;
//                
//                // Generate a unique transaction ID for this program subscription
//                String subscriptionTransactionId = paymentId + "_" + programId;
//                
//                // Create or update subscription for this program
//                createSingleSubscription(
//                    subscriptionTransactionId, status, programId, organizationId, amount, 
//                    userEmail, userName, userPhone, userAddress, maxCohorts
//                );
//            }
//        } else if (notes.has("program_id") && notes.has("organization_id")) {
//        	// Legacy path for single enrollment
//            processedEnrollment = true;
//            String programId = notes.getString("program_id");
//            String organizationId = notes.getString("organization_id");
//            Integer maxCohorts = notes.has("max_cohorts") ? notes.getInt("max_cohorts") : 1;
//            
//            createSingleSubscription(
//                paymentId, status, programId, organizationId, totalAmount,
//                userEmail, userName, userPhone, userAddress, maxCohorts
//            );
//        } else {
//            // Try to find flattened enrollments like "enrollments[0].program_id"
//            int index = 0;
//            boolean foundFlattened = false;
//            
//            while (notes.has("enrollments[" + index + "].program_id")) {
//                foundFlattened = true;
//                processedEnrollment = true;
//                
//                String programId = notes.getString("enrollments[" + index + "].program_id");
//                String organizationId = notes.getString("enrollments[" + index + "].organization_id");
//                double amount = notes.has("enrollments[" + index + "].amount") ? 
//                              notes.getDouble("enrollments[" + index + "].amount") : totalAmount;
//                Integer maxCohorts = notes.has("enrollments[" + index + "].max_cohorts") ? 
//                                   notes.getInt("enrollments[" + index + "].max_cohorts") : 1;
//                    
//                // Generate a unique transaction ID for this program subscription
//                String subscriptionTransactionId = paymentId + "_" + programId;
//                    
//                // Create or update subscription for this program
//                createSingleSubscription(
//                    subscriptionTransactionId, status, programId, organizationId, amount, 
//                    userEmail, userName, userPhone, userAddress, maxCohorts
//                );
//                
//                index++;
//            }
//            
//            if (foundFlattened) {
//                logger.info("Processed {} flattened enrollments", index);
//            }
//        }
//        
//        if (!processedEnrollment) {
//            logger.error("Missing program enrollment information in payment notes. Notes: {}", notes.toString());
//        }
//    }
//
//    private void createSingleSubscription(
//            String transactionId, String status, String programId, String organizationId, 
//            double amount, String userEmail, String userName, String userPhone, 
//            String userAddress, Integer maxCohorts) {
//        
//        // Fetch program and organization
//        Optional<Program> programOpt = programRepository.findByProgramId(programId);
//        Optional<Organization> orgOpt = organizationRepository.findById(organizationId);
//        
//        if (!programOpt.isPresent() || !orgOpt.isPresent()) {
//            logger.error("Program or Organization not found. Program ID: {}, Org ID: {}", programId, organizationId);
//            return;
//        }
//        
//        Program program = programOpt.get();
//        Organization organization = orgOpt.get();
//        
//        // Check if subscription already exists for this transaction ID
//        Optional<ProgramSubscription> existingSubscription = subscriptionRepository.findByTransactionId(transactionId);
//        ProgramSubscription subscription;
//        
//        if (existingSubscription.isPresent()) {
//            // Update existing subscription
//            subscription = existingSubscription.get();
//            subscription.setTransactionDate(OffsetDateTime.now());
//            subscription.setStatus(status);
//        } else {
//            // Create new subscription
//            subscription = new ProgramSubscription();
//            subscription.setProgram(program);
//            subscription.setOrganization(organization);
//            subscription.setStartDate(OffsetDateTime.now());
//            
//            // Set end date based on program duration or a default period
//            OffsetDateTime endDate = OffsetDateTime.now().plus(365, ChronoUnit.DAYS); // Default to one year
//            subscription.setEndDate(endDate);
//            
//            subscription.setTransactionId(transactionId);
//            subscription.setTransactionType("RAZORPAY");
//            subscription.setTransactionDate(OffsetDateTime.now());
//            subscription.setAmountPaid(amount);
//            subscription.setMaxCohorts(maxCohorts);
//            subscription.setStatus(status);
//            
//            // Set user information
//            subscription.setUserEmail(userEmail);
//            subscription.setUserName(userName);
//            subscription.setUserPhoneNumber(userPhone);
//            subscription.setUserAddress(userAddress);
//            subscription.setUserCreated(false);
//        }
//        
//        // Save the subscription
//        subscriptionRepository.save(subscription);
//        logger.info("Subscription created/updated for program {}: {}", programId, subscription);
//    }
//
//    @Transactional
//    private void createUserFromPaidSubscription(JSONObject paymentEntity) {
//        String paymentId = paymentEntity.getString("id");
//        
//        // Find all subscriptions related to this payment
//        List<ProgramSubscription> subscriptions = findAllSubscriptionsForPayment(paymentId);
//        
//        if (subscriptions.isEmpty()) {
//            logger.error("No subscriptions found for payment ID: {}", paymentId);
//            return;
//        }
//        
//        // Process each subscription for the same payment
//        logger.info("Found {} subscriptions for payment ID: {}", subscriptions.size(), paymentId);
//        
//        // Get user information from the first subscription
//        ProgramSubscription firstSubscription = subscriptions.get(0);
//        
//        // Only process if the subscription is in PAID status
//        if (!STATUS_PAID.equals(firstSubscription.getStatus())) {
//            logger.info("Skipping user creation for subscription with status: {}", firstSubscription.getStatus());
//            return;
//        }
//        
//        String userEmail = firstSubscription.getUserEmail();
//        String userName = firstSubscription.getUserName();
//        String userPhone = firstSubscription.getUserPhoneNumber();
//        String userAddress = firstSubscription.getUserAddress();
//        Organization organization = firstSubscription.getOrganization();
//        
//        // Check if user already exists by email
//        Optional<User> existingUserOpt = userRepository.findByUserEmail(userEmail);
//        User user;
//        boolean isNewUser = false;
//        
//        if (existingUserOpt.isPresent()) {
//            // Use existing user
//            user = existingUserOpt.get();
//            logger.info("Using existing user: {}", user.getUserId());
//        } else {
//            // Create new user
//            user = new User();
//            isNewUser = true;
//            
//            // Generate userId from userName
//            String userId = generateUserIdFromName(userName);
//            
//            // Set user details
//            user.setUserId(userId);
//            user.setUserName(userName);
//            user.setUserEmail(userEmail);
//            user.setUserPhoneNumber(userPhone);
//            user.setUserAddress(userAddress);
//            user.setUserType("learner"); // Default user type
//            user.setUserPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
//            user.setOrganization(organization);
//            user.setUuid(UUID.randomUUID().toString());
//            
//            // Save the user
//            user = userRepository.save(user);
//            logger.info("Created new user: {}", user.getUserId());
//        }
//        
//        // Lists to track programs and cohorts for welcome email
//        List<String> programNames = new ArrayList<>();
//        List<String> cohortNames = new ArrayList<>();
//        
//        // Process each subscription to enroll the user in the appropriate cohorts
//        for (ProgramSubscription subscription : subscriptions) {
//            // Skip if this subscription is not PAID or already processed
//            if (!STATUS_PAID.equals(subscription.getStatus()) || subscription.isUserCreated()) {
//                logger.info("Skipping subscription: ID={}, Status={}, UserCreated={}", 
//                    subscription.getSubscriptionId(), subscription.getStatus(), subscription.isUserCreated());
//                continue;
//            }
//            
//            try {
//                Program program = subscription.getProgram();
//                
//                // Find default cohort for this program
//                Optional<CohortProgram> defaultCohortProgram = 
//                    findDefaultCohortForProgram(program.getProgramId(), organization.getOrganizationId());
//                
//                if (!defaultCohortProgram.isPresent()) {
//                    logger.error("No default cohort found for program: {} and organization: {}", 
//                            program.getProgramId(), organization.getOrganizationId());
//                    continue;
//                }
//                
//                Cohort cohort = defaultCohortProgram.get().getCohort();
//                
//                // Verify cohort organization matches user organization
//                if (!cohort.getOrganization().getOrganizationId().equals(organization.getOrganizationId())) {
//                    logger.error("Organization mismatch for program {}! User org: {}, Cohort org: {}", 
//                            program.getProgramId(), organization.getOrganizationId(), 
//                            cohort.getOrganization().getOrganizationId());
//                    continue;
//                }
//                
//                // Check if user is already enrolled in this cohort
//                boolean userExistsInCohort = 
//                    checkUserExistsInCohort(user.getUserId(), cohort.getCohortId());
//                
//                if (!userExistsInCohort) {
//                    // Create user-cohort mapping
//                    UserCohortMapping mapping = new UserCohortMapping();
//                    mapping.setUser(user);
//                    mapping.setCohort(cohort);
//                    mapping.setLeaderboardScore(0);
//                    mapping.setUuid(UUID.randomUUID().toString());
//                    
//                    // Save the mapping
//                    userCohortMappingRepository.save(mapping);
//                    logger.info("User {} enrolled in cohort: {}", user.getUserId(), cohort.getCohortName());
//                    
//                    // Add to lists for welcome email
//                    programNames.add(program.getProgramName());
//                    cohortNames.add(cohort.getCohortName());
//                } else {
//                    logger.info("User {} already enrolled in cohort: {}", user.getUserId(), cohort.getCohortName());
//                }
//                
//                // Mark this subscription as processed
//                subscription.setUserCreated(true);
//                subscriptionRepository.save(subscription);
//                logger.info("Marked subscription {} as processed", subscription.getSubscriptionId());
//                
//            } catch (Exception e) {
//                logger.error("Error enrolling user in program: {}, Error: {}", 
//                        subscription.getProgram().getProgramId(), e.getMessage(), e);
//            }
//        }
//        
//        // Send welcome email with all enrolled programs and cohorts
//        if (!programNames.isEmpty()) {
//            sendWelcomeEmail(user, isNewUser ? DEFAULT_PASSWORD : null, programNames, cohortNames);
//            logger.info("Sent welcome email to user {} with {} programs", user.getUserId(), programNames.size());
//        }
//    }
//    
//    private List<ProgramSubscription> findAllSubscriptionsForPayment(String paymentId) {
//        // Find direct matches
//        List<ProgramSubscription> direct = subscriptionRepository.findAllByTransactionId(paymentId);
//        
//        // Find subscriptions with derived transaction IDs (paymentId_programId format)
//        List<ProgramSubscription> derived = subscriptionRepository
//            .findAllByTransactionIdStartingWith(paymentId + "_");
//        
//        // Combine all results
//        Set<ProgramSubscription> uniqueSubscriptions = new HashSet<>();
//        uniqueSubscriptions.addAll(direct);
//        uniqueSubscriptions.addAll(derived);
//        
//        return new ArrayList<>(uniqueSubscriptions);
//    }

    @Transactional
    private void createOrUpdateSubscription(JSONObject paymentEntity, String status) {
    	// Only proceed if status is PAID or FAILED
        if (!STATUS_PAID.equals(status) && !STATUS_FAILED.equals(status)) {
            logger.info("Skipping subscription update for status: {}", status);
            return;
        }
        // Extract the necessary data from the payment entity
        String paymentId = paymentEntity.getString("id");
        double amount = paymentEntity.getDouble("amount") / 100.0; // Convert from paise to rupees
        
        // Extract program and organization IDs from notes
        JSONObject notes = paymentEntity.has("notes") ? paymentEntity.getJSONObject("notes") : new JSONObject();
        
        if (!notes.has("program_id") || !notes.has("organization_id")) {
            logger.error("Missing program_id or organization_id in payment notes");
            return;
        }
        
        String programId = notes.getString("program_id");
        String organizationId = notes.getString("organization_id");
        Integer maxCohorts = notes.has("max_cohorts") ? notes.getInt("max_cohorts") : 1;
        
        // Extract user information
        String userEmail = notes.has("email") ? notes.getString("email") : extractEmailFromPayment(paymentEntity);
        String userName = notes.has("name") ? notes.getString("name") : "Unknown User";
        String userPhone = notes.has("contact") ? notes.getString("contact") : null;
        String userAddress = notes.has("address") ? notes.getString("address") : null;
        
        // Fetch program and organization
        Optional<Program> programOpt = programRepository.findByProgramId(programId);
        Optional<Organization> orgOpt = organizationRepository.findById(organizationId);
        
        if (!programOpt.isPresent() || !orgOpt.isPresent()) {
            logger.error("Program or Organization not found. Program ID: {}, Org ID: {}", programId, organizationId);
            return;
        }
        
        Program program = programOpt.get();
        Organization organization = orgOpt.get();
        
        // Check if subscription already exists
        Optional<ProgramSubscription> existingSubscription = subscriptionRepository.findByTransactionId(paymentId);
        ProgramSubscription subscription;
        
        if (existingSubscription.isPresent()) {
            // Update existing subscription
            subscription = existingSubscription.get();
            subscription.setTransactionDate(OffsetDateTime.now());
            subscription.setStatus(status);
        } else {
            // Create new subscription
            subscription = new ProgramSubscription();
            subscription.setProgram(program);
            subscription.setOrganization(organization);
            subscription.setStartDate(OffsetDateTime.now());
            
            // Set end date based on program duration or a default period
            // Adjust according to your business logic
            OffsetDateTime endDate = OffsetDateTime.now().plus(365, ChronoUnit.DAYS); // Default to one year
            subscription.setEndDate(endDate);
            
            subscription.setTransactionId(paymentId);
            subscription.setTransactionType("RAZORPAY");
            subscription.setTransactionDate(OffsetDateTime.now());
            subscription.setAmountPaid(amount);
            subscription.setMaxCohorts(maxCohorts);
            subscription.setStatus(status);
            
            // Set user information
            subscription.setUserEmail(userEmail);
            subscription.setUserName(userName);
            subscription.setUserPhoneNumber(userPhone);
            subscription.setUserAddress(userAddress);
        }
        
        // Save the subscription
        subscriptionRepository.save(subscription);
        
        logger.info("Subscription created/updated: {}", subscription);
    }
    
    private String extractEmailFromPayment(JSONObject paymentEntity) {
        // Try to extract email from various possible locations in the payment entity
        
        // Check in notes
        if (paymentEntity.has("notes")) {
            JSONObject notes = paymentEntity.getJSONObject("notes");
            if (notes.has("email")) {
                return notes.getString("email");
            }
        }
        
        // Check in customer details if available
        if (paymentEntity.has("customer_details")) {
            JSONObject customerDetails = paymentEntity.getJSONObject("customer_details");
            if (customerDetails.has("email")) {
                return customerDetails.getString("email");
            }
        }
        
        // If using RazorpayX, check in contact
        if (paymentEntity.has("contact")) {
            JSONObject contact = paymentEntity.getJSONObject("contact");
            if (contact.has("email")) {
                return contact.getString("email");
            }
        }
        
        // If cannot find email, return null
        return null;
    }
       
    // create the User after payment success
    @Transactional
    private void createUserFromPaidSubscription(JSONObject paymentEntity) {
    String paymentId = paymentEntity.getString("id");
    
    // Find the subscription by transaction ID
    Optional<ProgramSubscription> subscriptionOpt = subscriptionRepository.findByTransactionId(paymentId);
    
    if (!subscriptionOpt.isPresent()) {
        logger.error("No subscription found for payment ID: {}", paymentId);
        return;
    }
    
    ProgramSubscription subscription = subscriptionOpt.get();
    
    // Only create user if subscription status is PAID
    if (!STATUS_PAID.equals(subscription.getStatus())) {
        logger.info("Skipping user creation for subscription with status: {}", subscription.getStatus());
        return;
    }
    if (subscription.isUserCreated()) {
        logger.info("User already created for subscription: {}", subscription.getSubscriptionId());
        return;
    }
    
    try {
        // Create new user
        User user = new User();
        
        // Generate userId from userName (remove spaces)
        String userId = generateUserIdFromName(subscription.getUserName());
        
        // Set user details from subscription
        user.setUserId(userId);
        user.setUserName(subscription.getUserName());
        user.setUserEmail(subscription.getUserEmail());
        user.setUserPhoneNumber(subscription.getUserPhoneNumber());
        user.setUserAddress(subscription.getUserAddress());
        user.setUserType("learner"); // Default user type
        user.setUserPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        Organization organization = subscription.getOrganization();
        user.setOrganization(organization);
        user.setUuid(UUID.randomUUID()); // Set UUID for the user
        
        // Save the user
        User savedUser = userRepository.save(user);
        logger.info("Created new user: {}", savedUser.getUserId());
        
        // Update the subscription with the userId
        subscription.setUserId(savedUser.getUserId());
        
        // Find default cohort for the program ensuring org match
        Program program = subscription.getProgram();
        Optional<CohortProgram> defaultCohortProgram = findDefaultCohortForProgram(program.getProgramId(), organization.getOrganizationId());
        
        if (!defaultCohortProgram.isPresent()) {
            logger.error("No default cohort found for program: {} and organization: {}", 
                    program.getProgramId(), organization.getOrganizationId());
            return;
        }
        
        Cohort cohort = defaultCohortProgram.get().getCohort();
        
        // Verify that cohort organization matches user organization
        if (!cohort.getOrganization().getOrganizationId().equals(organization.getOrganizationId())) {
            logger.error("Organization mismatch! User org: {}, Cohort org: {}", 
                    organization.getOrganizationId(), cohort.getOrganization().getOrganizationId());
            return;
        }
        
        // Check if this userId already exists in this specific cohort
        boolean userExistsInCohort = checkUserExistsInCohort(userId, cohort.getCohortId());
        
        if (userExistsInCohort) {
            logger.warn("User with ID {} already exists in cohort {}. Generating new userId.", 
                    userId, cohort.getCohortId());
            
            // Generate a new unique userId with a suffix
            String newUserId = userId + "_" + System.currentTimeMillis();
            user.setUserId(newUserId);
            
            // Update the saved user with new ID
            savedUser = userRepository.save(user);
            
            // Make sure to update the subscription with the new user ID
            subscription.setUserId(savedUser.getUserId());
            logger.info("Created user with new ID: {}", savedUser.getUserId());
        }
        
        // Create user-cohort mapping
        UserCohortMapping userCohortMapping = new UserCohortMapping();
        userCohortMapping.setUser(savedUser);
        userCohortMapping.setCohort(cohort);
        userCohortMapping.setLeaderboardScore(0);
        userCohortMapping.setUuid(UUID.randomUUID());
        // Save the mapping
        userCohortMappingRepository.save(userCohortMapping);
        
        // ✅ Mark user as created and save the subscription with userId
        subscription.setUserCreated(true);
        subscriptionRepository.save(subscription);
        
        logger.info("User added to cohort: {}", cohort.getCohortName());
        logger.info("Subscription updated with userId: {}", subscription.getUserId());
        
        // Prepare data for welcome email
        List<String> programNames = new ArrayList<>();
        programNames.add(program.getProgramName());
        
        List<String> cohortNames = new ArrayList<>();
        cohortNames.add(cohort.getCohortName());
        
        // Send welcome email
        sendWelcomeEmail(savedUser, DEFAULT_PASSWORD, programNames, cohortNames);
        
    } catch (Exception e) {
        logger.error("Error creating user from subscription: {}", e.getMessage(), e);
    }
}
    
    /**
     * Find default cohort for a program with organization validation
     */
    private Optional<CohortProgram> findDefaultCohortForProgram(String programId, String organizationId) {
        // Find all cohorts for the program
        List<CohortProgram> cohortPrograms = cohortProgramRepository.findByProgramProgramId(programId);
        
        if (cohortPrograms.isEmpty()) {
            return Optional.empty();
        }
        
        // Filter cohorts by organization ID
        List<CohortProgram> filteredByOrg = cohortPrograms.stream()
            .filter(cp -> cp.getCohort().getOrganization().getOrganizationId().equals(organizationId))
            .collect(Collectors.toList());
        
        if (filteredByOrg.isEmpty()) {
            return Optional.empty();
        }
        
        // Return the first matching cohort
        return Optional.of(filteredByOrg.get(0));
    }
    
    // Generate a userId based on name and make sure it's unique
    private String generateUserIdFromName(String userName) {
        if (userName == null || userName.trim().isEmpty()) {
            return "user" + System.currentTimeMillis();
        }
        
        // Remove spaces and special characters
        String baseUserId = userName.replaceAll("[^a-zA-Z0-9]", "");
        
        // Make sure it's not empty after cleaning
        if (baseUserId.isEmpty()) {
            baseUserId = "user";
        }
        
        String userId = baseUserId;
        int suffix = 1;
        
        // Check if userId already exists and generate a unique one
        while (userRepository.existsById(userId)) {
            userId = baseUserId + suffix;
            suffix++;
        }
        
        return userId;
    }

    // Check if a user with this ID already exists in the specified cohort
    private boolean checkUserExistsInCohort(String userId, String cohortId) {
        return userCohortMappingRepository.findByUser_UserIdAndCohort_CohortId(userId, cohortId).isPresent();
    }

    private Optional<CohortProgram> findDefaultCohortForProgram(String programId) {
        // Find all cohorts for the program
        List<CohortProgram> cohortPrograms = cohortProgramRepository.findByProgramProgramId(programId);
        
        if (cohortPrograms.isEmpty()) {
            return Optional.empty();
        }
        
        // Find the default cohort (you might need to add a "isDefault" flag to your CohortProgram entity)
        // For now, we'll just use the first one found or you can implement your own logic
        return Optional.of(cohortPrograms.get(0));
    }

    // Helper method for sending welcome email (this is already in your code)
    private void sendWelcomeEmail(User user, String plainPassword, List<String> programNames, List<String> cohortNames) {
        try {
            Organization organization = user.getOrganization();
            emailService.sendUserCreationEmail(
                user.getUserEmail(),
                user.getUserName(),
                user.getUserId(),
                plainPassword,
                programNames,
                cohortNames,
                organization.getOrganizationAdminEmail(),
                organization.getOrganizationName(),
                user.getUserType()
            );
        } catch (Exception e) {
            logger.error("Failed to send welcome email for user: {}", user.getUserId(), e);
        }
    }
    
}