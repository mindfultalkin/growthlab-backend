package com.mindfultalk.growthlab.repository;

import com.mindfultalk.growthlab.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public interface ProgramSubscriptionRepository extends JpaRepository<ProgramSubscription, Long> {
    // Custom query to find subscriptions by organization ID
    List<ProgramSubscription> findByOrganization_OrganizationId(String organizationId);
    Optional<ProgramSubscription> findByTransactionId(String transactionId);
    List<ProgramSubscription> findAllByTransactionIdStartingWith(String transactionIdPrefix);
    List<ProgramSubscription> findAllByTransactionId(String transactionId);
}