package com.mindfultalk.growthlab.service;

import com.mindfultalk.growthlab.config.RazorpayConfig;
import com.mindfultalk.growthlab.model.*;
import com.mindfultalk.growthlab.repository.*;
import com.mindfultalk.growthlab.util.Utils;
import com.razorpay.*;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class PaymentService {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    @Autowired
    private RazorpayClient razorpayClient;

    @Autowired
    private RazorpayConfig razorpayConfig;
    
    @Autowired
    private ProgramRepository programRepository;
    
    @Autowired
    private OrganizationRepository organizationRepository;
    
    @Autowired
    private ProgramSubscriptionRepository subscriptionRepository;
    
    @Autowired
    private PaymentEventRepository paymentEventRepository;
    
    private static final String STATUS_PENDING = "PENDING";

    public String createOrder(double amount, String currency, Map<String, Object> metadata) throws RazorpayException {
        try {
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", (int)(amount * 100)); // Convert to paise and ensure it's an integer
            orderRequest.put("currency", currency);
            orderRequest.put("payment_capture", 1);
            
            // Add metadata as notes
            if (metadata != null && !metadata.isEmpty()) {
                JSONObject notes = new JSONObject();
                for (Map.Entry<String, Object> entry : metadata.entrySet()) {
                    notes.put(entry.getKey(), entry.getValue().toString());
                }
                orderRequest.put("notes", notes);
            }

            Order order = razorpayClient.orders.create(orderRequest);
            
            // Create a pending subscription if we have program and organization info
            if (metadata != null && metadata.containsKey("program_id") && metadata.containsKey("organization_id")) {
                createPendingSubscription(order.toJson(), metadata);
            }
            
            return order.toString();
        } catch (RazorpayException e) {
            logger.error("Error creating order: {}", e.getMessage());
            throw new RazorpayException("Error creating order: " + e.getMessage());
        }
    }
    
    @Transactional
    private void createPendingSubscription(JSONObject orderData, Map<String, Object> metadata) {
        try {
            String orderId = orderData.getString("id");
            double amount = orderData.getDouble("amount") / 100.0;
            
            // Extract required fields
            String programId = metadata.get("program_id").toString();
            String organizationId = metadata.get("organization_id").toString();
            Integer maxCohorts = metadata.containsKey("max_cohorts") ? 
                    Integer.parseInt(metadata.get("max_cohorts").toString()) : 1;
            
            // Get user details
            String userEmail = metadata.containsKey("email") ? 
                    metadata.get("email").toString() : null;
            String userName = metadata.containsKey("name") ? 
                    metadata.get("name").toString() : null;
            String userPhone = metadata.containsKey("contact") ? 
                    metadata.get("contact").toString() : null;
            String userAddress = metadata.containsKey("address") ? 
                    metadata.get("address").toString() : null;
            
            // Check for required fields
            if (userName == null || userEmail == null) {
                logger.error("Required user details missing. Name: {}, Email: {}", userName, userEmail);
                throw new RuntimeException("Required user details missing");
            }
            // Fetch program and organization
            Optional<Program> programOpt = programRepository.findByProgramId(programId);
            Optional<Organization> orgOpt = organizationRepository.findById(organizationId);
            
            if (!programOpt.isPresent() || !orgOpt.isPresent()) {
                logger.error("Program or Organization not found. Program ID: {}, Org ID: {}", programId, organizationId);
                return;
            }
            
            Program program = programOpt.get();
            Organization organization = orgOpt.get();
            
            // Create pending subscription
            ProgramSubscription subscription = new ProgramSubscription();
            subscription.setProgram(program);
            subscription.setOrganization(organization);
            subscription.setStartDate(OffsetDateTime.now());
            
            // Set end date (default to one year)
            OffsetDateTime endDate = OffsetDateTime.now().plus(365, ChronoUnit.DAYS);
            subscription.setEndDate(endDate);
            
            subscription.setTransactionId(orderId); // Using order ID initially
            subscription.setTransactionType("RAZORPAY");
            subscription.setTransactionDate(OffsetDateTime.now());
            subscription.setAmountPaid(amount);
            subscription.setMaxCohorts(maxCohorts);
            subscription.setStatus(STATUS_PENDING);
            
            // Set user information if available
            if (userEmail != null) subscription.setUserEmail(userEmail);
            if (userName != null) subscription.setUserName(userName);
            if (userPhone != null) subscription.setUserPhoneNumber(userPhone);
            if (userAddress != null) subscription.setUserAddress(userAddress);
            
            // Save the subscription
            ProgramSubscription savedSubscription = subscriptionRepository.save(subscription);
            
            // Create payment event
            PaymentEvent event = new PaymentEvent();
            event.setEventType("order.created");
            event.setOrderId(orderId);
         // Set a placeholder value for payment_id since it cannot be null
            event.setPaymentId("pending_" + orderId);
            event.setAmount(amount);
            event.setStatus(STATUS_PENDING);
            event.setSubscriptionId(savedSubscription.getSubscriptionId());
            event.setRawPayload(orderData.toString());
            event.setUuid(UUID.randomUUID());
            event.setCreatedAt(OffsetDateTime.now());
            
            paymentEventRepository.save(event);
            
            logger.info("Created pending subscription: {}", savedSubscription);
            
        } catch (Exception e) {
            logger.error("Error creating pending subscription: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public boolean verifyPayment(String orderId, String paymentId, String razorpaySignature) {
        try {
            String payload = orderId + "|" + paymentId;
            boolean isValid = Utils.verifySignature(payload, razorpaySignature, razorpayConfig.getSecret());
            
            if (isValid) {
                // Update subscription status if it exists with order ID
                Optional<ProgramSubscription> subscriptionOpt = subscriptionRepository.findByTransactionId(orderId);
                if (subscriptionOpt.isPresent()) {
                    ProgramSubscription subscription = subscriptionOpt.get();
                    
                    // Update transaction ID to payment ID
                    subscription.setTransactionId(paymentId);
                    subscription.setStatus("VERIFIED");
                    subscription.setTransactionDate(OffsetDateTime.now());
                    
                    subscriptionRepository.save(subscription);
                    
                    // Create payment verification event
                    PaymentEvent event = new PaymentEvent();
                    event.setEventType("payment.verified");
                    event.setPaymentId(paymentId);
                    event.setOrderId(orderId);
                    event.setStatus("VERIFIED");
                    event.setSubscriptionId(subscription.getSubscriptionId());
                    
                    paymentEventRepository.save(event);
                    
                    logger.info("Updated subscription with payment ID: {}", paymentId);
                }
            }
            
            return isValid;
        } catch (Exception e) {
            logger.error("Error verifying payment: {}", e.getMessage(), e);
            return false;
        }
    }
    
 // Add this method to your PaymentService class

    @Transactional(readOnly = true)
    public Map<String, Object> getSubscriptionStatus(Long subscriptionId) {
        Optional<ProgramSubscription> subscriptionOpt = subscriptionRepository.findById(subscriptionId);
        
        if (!subscriptionOpt.isPresent()) {
            throw new RuntimeException("Subscription not found with ID: " + subscriptionId);
        }
        
        ProgramSubscription subscription = subscriptionOpt.get();
        
        Map<String, Object> status = new HashMap<>();
        status.put("subscriptionId", subscription.getSubscriptionId());
        status.put("programId", subscription.getProgram().getProgramId());
        status.put("organizationId", subscription.getOrganization().getOrganizationId());
        status.put("status", subscription.getStatus());
        status.put("startDate", subscription.getStartDate());
        status.put("endDate", subscription.getEndDate());
        status.put("transactionId", subscription.getTransactionId());
        status.put("amountPaid", subscription.getAmountPaid());
        status.put("maxCohorts", subscription.getMaxCohorts());
        
        // Get payment events related to this subscription
        List<PaymentEvent> events = paymentEventRepository.findBySubscriptionId(subscriptionId);
        status.put("paymentEvents", events);
        
        return status;
    }
}