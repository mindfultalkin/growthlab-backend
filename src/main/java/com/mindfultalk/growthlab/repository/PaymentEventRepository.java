package com.mindfultalk.growthlab.repository;

import com.mindfultalk.growthlab.model.*;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public interface PaymentEventRepository extends JpaRepository<PaymentEvent, Long> {
    List<PaymentEvent> findByPaymentId(String paymentId);
    List<PaymentEvent> findBySubscriptionId(Long subscriptionId);
    List<PaymentEvent> findByEventType(String eventType);
    List<PaymentEvent> findByOrderId(String orderId);
}