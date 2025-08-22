package com.mindfultalk.growthlab.service;

import com.mindfultalk.growthlab.model.ProgramSubscription;
import com.mindfultalk.growthlab.repository.ProgramSubscriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class ProgramSubscriptionServiceImpl implements ProgramSubscriptionService {

    @Autowired
    private ProgramSubscriptionRepository subscriptionRepository;

    @Override
    public ProgramSubscription createProgramSubscription(ProgramSubscription subscription) {
        return subscriptionRepository.save(subscription);
    }

    @Override
    public ProgramSubscription updateProgramSubscription(Long subscriptionId, ProgramSubscription subscriptionDetails) {
        Optional<ProgramSubscription> existingSubscription = subscriptionRepository.findById(subscriptionId);
        if (existingSubscription.isPresent()) {
            ProgramSubscription subscription = existingSubscription.get();
            subscription.setProgram(subscriptionDetails.getProgram());
            subscription.setOrganization(subscriptionDetails.getOrganization());
            subscription.setStartDate(subscriptionDetails.getStartDate());
            subscription.setEndDate(subscriptionDetails.getEndDate());
            subscription.setMaxCohorts(subscriptionDetails.getMaxCohorts());
            subscription.setAmountPaid(subscriptionDetails.getAmountPaid());
            subscription.setUserName(subscriptionDetails.getUserName());
            subscription.setTransactionDate(subscriptionDetails.getTransactionDate());
            subscription.setStatus(subscriptionDetails.getStatus());
            subscription.setUserAddress(subscriptionDetails.getUserAddress());
            subscription.setUserEmail(subscriptionDetails.getUserEmail());
            subscription.setTransactionType(subscriptionDetails.getTransactionType());
            subscription.setUserPhoneNumber(subscriptionDetails.getUserPhoneNumber());
            return subscriptionRepository.save(subscription);
        }
        return null; // Or throw an exception
    }

    @Override
    public Optional<ProgramSubscription> getProgramSubscription(Long subscriptionId) {
        return subscriptionRepository.findById(subscriptionId);
    }

    @Override
    public List<ProgramSubscription> getAllProgramSubscriptions() {
        return subscriptionRepository.findAll();
    }

    @Override
    public List<ProgramSubscription> getProgramSubscriptionsByOrganization(String organizationId) {
        return subscriptionRepository.findByOrganization_OrganizationId(organizationId);
    }

    @Override
    public void deleteProgramSubscription(Long subscriptionId) {
        subscriptionRepository.deleteById(subscriptionId);
    }
}