package com.mindfultalk.growthlab.service;

import com.mindfultalk.growthlab.model.*;

import org.springframework.core.io.Resource;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public interface UserAssignmentService {

   // UserAssignment createAssignment(UserAssignment userAssignment);

    List<UserAssignment> getAssignmentsByUserId(String userId);

    List<UserAssignment> getAssignmentsByCohortId(String cohortId);

    List<UserAssignment> getAssignmentsByCohortIdAndUserId(String cohortId, String userId);

    UserAssignment submitNewAssignment(String userId, String cohortId, String programId, 
            String stageId, String unitId, String subconceptId, 
            MultipartFile file) throws IOException;

    UserAssignment submitCorrectedAssignment(String assignmentId, Integer score, 
            MultipartFile correctedFile, 
            String remarks, OffsetDateTime correctedDate) throws IOException;
    UserAssignment getAssignmentById(String assignmentId);
    MediaFile getSubmittedFile(String assignmentId); // Add this line
    MediaFile getCorrectedFile(String assignmentId);
    UserAssignment getAssignmentByUserIdAndSubconceptId(String userId, String subconceptId);
    
    Resource downloadAllAssignments(String cohortId) throws IOException;
    Resource downloadAllAssignmentsSendEmail(String cohortId) throws IOException;
    void uploadCorrectedAssignments(List<MultipartFile> files, List<Integer> scores, List<String> remarks,
                                     List<String> assignmentIds) throws IOException;

    Map<String, Object> getSubmittedFileDetails(String assignmentId);

    Map<String, Object> getCorrectedFileDetails(String assignmentId);
    void generateAndEmailAssignmentsCSV(String cohortId) throws IOException;
}