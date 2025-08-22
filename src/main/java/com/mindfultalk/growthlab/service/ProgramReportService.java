package com.mindfultalk.growthlab.service;

import java.util.List;
import com.mindfultalk.growthlab.dto.*;

public interface ProgramReportService {
	ProgramReportDTO generateProgramReport(String userId, String programId);
    StageReportDTO generateStageReport(String userId, String stageId);
    UnitReportDTO generateUnitReport(String userId, String unitId);
    List<AttemptDTO> getUserAttempts(String userId, String subconceptId);
    CohortProgressDTO getCohortProgress(String programId, String cohortId);
    
    byte[] generatePdfReport(String userId, String programId); 
    byte[] generateCsvReport(String userId, String programId);
	UserProgressDTO getUserProgress(String programId, String userId);
	UserDTO getUserInfo(String userId);
}