package com.mindfultalk.growthlab.service;

import com.mindfultalk.growthlab.model.UserSubConcept;
import java.util.*;

public interface UserSubConceptService {
    
    UserSubConcept getUserSubConceptById(Long userSubconceptId);
    List<UserSubConcept> getAllUserSubConcepts();
    List<UserSubConcept> getAllUserSubConceptsByUserId(String userId);
    void deleteUserSubConcept(Long userSubconceptId);
    UserSubConcept updateUserSubConcept(Long userSubconceptId, UserSubConcept userSubConcept);
    
    UserSubConcept createUserSubConcept(UserSubConcept userSubConcept);
	Optional<UserSubConcept> findByUser_UserIdAndProgram_ProgramIdAndStage_StageIdAndUnit_UnitIdAndSubconcept_SubconceptId(
			String userId, String programId, String stageId, String unitId, String subconceptId);
	UserSubConcept updateUserSubConcept(UserSubConcept userSubConcept);
}