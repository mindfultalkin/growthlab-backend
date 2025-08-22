package com.mindfultalk.growthlab.service;

import com.mindfultalk.growthlab.model.UserAttempts;
import java.util.*;

public interface UserAttemptsService {
    List<UserAttempts> getAllUserAttempts();
    Optional<UserAttempts> getUserAttemptById(Long userAttemptId);
    UserAttempts saveUserAttempt(UserAttempts userAttempt); 
    UserAttempts createUserAttempt(UserAttempts userAttempt, String cohortId);
    UserAttempts updateUserAttempt(Long userAttemptId, UserAttempts userAttempt);
    void deleteUserAttempt(Long userAttemptId);
}