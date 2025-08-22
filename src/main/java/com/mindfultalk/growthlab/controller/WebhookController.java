package com.mindfultalk.growthlab.controller;

import com.mindfultalk.growthlab.model.PaymentEvent;
import com.mindfultalk.growthlab.repository.PaymentEventRepository;
import com.mindfultalk.growthlab.service.WebhookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.apache.commons.codec.binary.Hex;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/v1/webhook")
public class WebhookController {
    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);
    
    @Autowired
    private WebhookService webhookService;
    
    @Autowired
    private PaymentEventRepository paymentEventRepository;
    
    @Value("${razorpay.webhookSecret}")
    private String webhookSecret;
    
    @PostMapping("/razorpay")
    public ResponseEntity<String> handleRazorpayWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature) {
        logger.info("Received Razorpay webhook");
        
        try {
            // Verify webhook signature
            if (!verifySignature(payload, signature)) {
                logger.error("Webhook signature verification failed");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
            }
            
            // Process the webhook event
            JSONObject payloadJson = new JSONObject(payload);
            String eventType = payloadJson.getString("event");
            logger.info("Processing webhook event: {}", eventType);
            
            // Store the event in the database
            PaymentEvent paymentEvent = createPaymentEvent(payloadJson);
            paymentEventRepository.save(paymentEvent);
            
            // Process the event
            webhookService.processWebhookEvent(eventType, payloadJson);
            
            return ResponseEntity.ok("Webhook processed successfully");
        } catch (Exception e) {
            logger.error("Error processing webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                   .body("Error processing webhook: " + e.getMessage());
        }
    }
    
    private boolean verifySignature(String payload, String signature) {
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(webhookSecret.getBytes(), "HmacSHA256");
            sha256_HMAC.init(secretKey);
            
            String computedSignature = Hex.encodeHexString(sha256_HMAC.doFinal(payload.getBytes()));
            return signature.equals(computedSignature);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            logger.error("Error verifying webhook signature: {}", e.getMessage(), e);
            return false;
        }
    }
    
    private PaymentEvent createPaymentEvent(JSONObject payloadJson) {
        PaymentEvent event = new PaymentEvent();
        event.setEventType(payloadJson.getString("event"));
        
        try {
            // Extract payment details
            if (payloadJson.has("payload") && payloadJson.getJSONObject("payload").has("payment")) {
                JSONObject payment = payloadJson.getJSONObject("payload").getJSONObject("payment").getJSONObject("entity");
                event.setPaymentId(payment.getString("id"));
                
                if (payment.has("order_id")) {
                    event.setOrderId(payment.getString("order_id"));
                }
                
                if (payment.has("amount")) {
                    event.setAmount(payment.getDouble("amount") / 100.0);
                }
                
                if (payment.has("status")) {
                    event.setStatus(payment.getString("status"));
                }
                
                // Fix for error_code - check if it exists and is a string before getting it
                if (payment.has("error_code") && !payment.isNull("error_code")) {
                    Object errorCode = payment.get("error_code");
                    if (errorCode instanceof String) {
                        event.setErrorCode((String) errorCode);
                    } else {
                        // Convert to string if it's not null but another type
                        event.setErrorCode(String.valueOf(errorCode));
                    }
                }
                
             // Fix for error_description - check if it exists and is a string
                if (payment.has("error_description") && !payment.isNull("error_description")) {
                    Object errorDesc = payment.get("error_description");
                    if (errorDesc instanceof String) {
                        event.setErrorDescription((String) errorDesc);
                    } else {
                        // Convert to string if it's not null but another type
                        event.setErrorDescription(String.valueOf(errorDesc));
                    }
                }
            }
            
            // Extract order details if present
            if (payloadJson.has("payload") && payloadJson.getJSONObject("payload").has("order")) {
                JSONObject order = payloadJson.getJSONObject("payload").getJSONObject("order").getJSONObject("entity");
                if (event.getOrderId() == null && order.has("id")) {
                    event.setOrderId(order.getString("id"));
                }
            }
            
            // Store the full raw payload
            event.setRawPayload(payloadJson.toString());
            
        } catch (Exception e) {
            logger.error("Error extracting webhook data: {}", e.getMessage(), e);
        }
        
        return event;
    }
    
    // Endpoint to get payment events by subscription ID
    @GetMapping("/events/subscription/{subscriptionId}")
    public ResponseEntity<?> getEventsBySubscription(@PathVariable Long subscriptionId) {
        try {
            List<PaymentEvent> events = paymentEventRepository.findBySubscriptionId(subscriptionId);
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                   .body("Error retrieving events: " + e.getMessage());
        }
    }
    
    // Endpoint to get payment events by payment ID
    @GetMapping("/events/payment/{paymentId}")
    public ResponseEntity<?> getEventsByPayment(@PathVariable String paymentId) {
        try {
            List<PaymentEvent> events = paymentEventRepository.findByPaymentId(paymentId);
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                   .body("Error retrieving events: " + e.getMessage());
        }
    }
}