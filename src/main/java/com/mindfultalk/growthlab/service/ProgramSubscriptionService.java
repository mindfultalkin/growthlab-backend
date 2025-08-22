package com.mindfultalk.growthlab.service;

import com.mindfultalk.growthlab.model.*;
import java.util.*;

public interface ProgramSubscriptionService {
	
    ProgramSubscription createProgramSubscription(ProgramSubscription subscription);
    
    ProgramSubscription updateProgramSubscription(Long subscriptionId, ProgramSubscription subscriptionDetails);
    
    Optional<ProgramSubscription> getProgramSubscription(Long subscriptionId);
    
    List<ProgramSubscription> getAllProgramSubscriptions();
    
    List<ProgramSubscription> getProgramSubscriptionsByOrganization(String organizationId);
    
    void deleteProgramSubscription(Long subscriptionId);
}