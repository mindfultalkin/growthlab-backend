package com.mindfultalk.growthlab.service;


import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.csv.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mindfultalk.growthlab.dto.*;
import com.mindfultalk.growthlab.exception.ResourceNotFoundException;
import com.mindfultalk.growthlab.model.*;
import com.mindfultalk.growthlab.repository.*;
import org.springframework.cache.annotation.*;
import java.io.IOException;
import com.itextpdf.io.source.ByteArrayOutputStream;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;


@Service
public class ProgramReportServiceImpl implements ProgramReportService {
	
	@Autowired
	private UserRepository userRepository;

    @Autowired
    private ProgramRepository programRepository;
    
    @Autowired
    private CohortRepository cohortRepository;
    
    @Autowired
    private StageRepository stageRepository;
    
    @Autowired
    private UnitRepository unitRepository;
    
    @Autowired
    private UserSubConceptRepository userSubConceptRepository;
    
    @Autowired
    private ProgramConceptsMappingRepository programConceptsMappingRepository;
    
    @Autowired
    private UserCohortMappingRepository userCohortMappingRepository;
    
    @Autowired
    private UserAttemptsRepository userAttemptsRepository;
    
    @Autowired
    private CacheManagementService cacheManagementService;
    
    private static final Logger logger = LoggerFactory.getLogger(ProgramReportServiceImpl.class);

   
    
    @Override
    @Cacheable(value = "userInfo", key = "#userId")
    public UserDTO getUserInfo(String userId) {
        logger.info("Fetching user information for userId: {}", userId);
        
        try {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
            
            UserDTO userDTO = new UserDTO();
            userDTO.setUserName(user.getUserName());
            userDTO.setUserPhoneNumber(user.getUserPhoneNumber());
            
            logger.debug("Successfully retrieved user info for userId: {}, userName: {}", userId, user.getUserName());
            return userDTO;
        } catch (Exception e) {
            logger.error("Error fetching user information for userId: {}", userId, e);
            throw e;
        }
    }
    
    @Override
    @Cacheable(value = "programReports", key = "#userId + '_' + #programId", unless = "#result == null")
    public ProgramReportDTO generateProgramReport(String userId, String programId) {
        logger.info("Generating program report for userId: {} and programId: {}", userId, programId);
        long startTime = System.currentTimeMillis();
        
        try {
            Program program = programRepository.findById(programId)
                .orElseThrow(() -> new ResourceNotFoundException("Program not found with ID: " + programId));

            logger.debug("Found program: {} for programId: {}", program.getProgramName(), programId);

            ProgramReportDTO report = new ProgramReportDTO();
            report.setProgramId(programId);
            report.setProgramName(program.getProgramName());
            report.setProgramDesc(program.getProgramDesc());

            // Get all stages for the program
            List<Stage> stages = stageRepository.findByProgram_ProgramId(programId);
            logger.debug("Found {} stages for programId: {}", stages.size(), programId);
            
            List<StageReportDTO> stageReports = new ArrayList<>();
            
            // Track overall statistics
            int totalUnits = 0;
            int completedUnits = 0;
            int totalSubconcepts = 0;
            int completedSubconcepts = 0;
            double totalScore = 0;
            int scoreCount = 0;
            
            // Get all user attempts for this program
            List<UserAttempts> allAttempts = userAttemptsRepository
                .findByUser_UserIdAndProgram_ProgramId(userId, programId);
            logger.debug("Found {} attempts for userId: {} and programId: {}", allAttempts.size(), userId, programId);
                
            // Set first and last attempt dates
            if (!allAttempts.isEmpty()) {
                report.setFirstAttemptDate(allAttempts.stream()
                    .min(Comparator.comparing(UserAttempts::getUserAttemptStartTimestamp))
                    .map(UserAttempts::getUserAttemptStartTimestamp)
                    .orElse(null));
                    
                report.setLastAttemptDate(allAttempts.stream()
                    .max(Comparator.comparing(UserAttempts::getUserAttemptEndTimestamp))
                    .map(UserAttempts::getUserAttemptEndTimestamp)
                    .orElse(null));
                    
                logger.debug("Set attempt dates - First: {}, Last: {}", 
                    report.getFirstAttemptDate(), report.getLastAttemptDate());
            }

            // Process each stage
            boolean previousStageCompleted = true;
            for (Stage stage : stages) {
                logger.debug("Processing stage: {} (ID: {})", stage.getStageName(), stage.getStageId());
                
                StageReportDTO stageReport = generateStageReport(userId, stage.getStageId());
                stageReport.setEnabled(previousStageCompleted);
                stageReports.add(stageReport);
                
                // Update statistics
                totalUnits += stageReport.getTotalUnits();
                completedUnits += stageReport.getCompletedUnits();
                
                // Update completion tracking
                previousStageCompleted = "yes".equals(stageReport.getCompletionStatus());
                
                // Process units within stage
                for (UnitReportDTO unitReport : stageReport.getUnits()) {
                    totalSubconcepts += unitReport.getTotalSubconcepts();
                    completedSubconcepts += unitReport.getCompletedSubconcepts();
                    
                    // Accumulate scores
                    if (unitReport.getAverageScore() > 0) {
                        totalScore += unitReport.getAverageScore();
                        scoreCount++;
                    }
                }
            }

            // Set overall statistics
            report.setTotalStages(stages.size());
            report.setCompletedStages((int) stageReports.stream()
                .filter(s -> "yes".equals(s.getCompletionStatus()))
                .count());
            report.setTotalUnits(totalUnits);
            report.setCompletedUnits(completedUnits);
            report.setTotalSubconcepts(totalSubconcepts);
            report.setCompletedSubconcepts(completedSubconcepts);
            
            // Calculate percentages
            report.setStageCompletionPercentage(calculatePercentage(report.getCompletedStages(), report.getTotalStages()));
            report.setUnitCompletionPercentage(calculatePercentage(completedUnits, totalUnits));
            report.setSubconceptCompletionPercentage(calculatePercentage(completedSubconcepts, totalSubconcepts));
            
            // Calculate average score
            report.setAverageScore(scoreCount > 0 ? totalScore / scoreCount : 0);
            
            // Generate score distribution
            report.setScoreDistribution(generateScoreDistribution(allAttempts));
            
            report.setStages(stageReports);
            
            long endTime = System.currentTimeMillis();
            logger.info("Successfully generated program report for userId: {} and programId: {} in {}ms. " +
                    "Stats: {}/{} stages, {}/{} units, {}/{} subconcepts completed", 
                    userId, programId, (endTime - startTime),
                    report.getCompletedStages(), report.getTotalStages(),
                    completedUnits, totalUnits,
                    completedSubconcepts, totalSubconcepts);
            
            return report;
        } catch (Exception e) {
            logger.error("Error generating program report for userId: {} and programId: {}", userId, programId, e);
            throw e;
        }
    }

    @Override
    @Cacheable(value = "stageReports", key = "#userId + '_' + #stageId", unless = "#result == null")
    public StageReportDTO generateStageReport(String userId, String stageId) {
        logger.debug("Generating stage report for userId: {} and stageId: {}", userId, stageId);
        
        try {
            Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new ResourceNotFoundException("Stage not found with ID: " + stageId));
                
            StageReportDTO report = new StageReportDTO();
            report.setStageId(stageId);
            report.setStageName(stage.getStageName());
            report.setStageDesc(stage.getStageDesc());
            
            // Get all units for the stage
            List<Unit> units = unitRepository.findByStage_StageId(stageId);
            logger.debug("Found {} units for stageId: {}", units.size(), stageId);
            
            List<UnitReportDTO> unitReports = new ArrayList<>();
            
            // Process each unit
            boolean previousUnitCompleted = true;
            for (Unit unit : units) {
                UnitReportDTO unitReport = generateUnitReport(userId, unit.getUnitId());
                unitReport.setEnabled(previousUnitCompleted);
                unitReports.add(unitReport);
                
                previousUnitCompleted = "yes".equals(unitReport.getCompletionStatus());
            }
            
            // Calculate stage statistics
            report.setTotalUnits(units.size());
            report.setCompletedUnits((int) unitReports.stream()
                .filter(u -> "yes".equals(u.getCompletionStatus()))
                .count());
            report.setCompletionPercentage(calculatePercentage(report.getCompletedUnits(), report.getTotalUnits()));
            
            // Calculate average score for stage
            double totalScore = unitReports.stream()
                .mapToDouble(UnitReportDTO::getAverageScore)
                .filter(score -> score > 0)
                .average()
                .orElse(0.0);
            report.setAverageScore(totalScore);
            
            report.setUnits(unitReports);
            report.setCompletionStatus(report.getCompletedUnits() == report.getTotalUnits() ? "yes" : "no");
            
            logger.debug("Generated stage report for stageId: {} - {}/{} units completed", 
                stageId, report.getCompletedUnits(), report.getTotalUnits());
            
            return report;
        } catch (Exception e) {
            logger.error("Error generating stage report for userId: {} and stageId: {}", userId, stageId, e);
            throw e;
        }
    }

    @Override
    @Cacheable(value = "unitReports", key = "#userId + '_' + #unitId", unless = "#result == null")
    public UnitReportDTO generateUnitReport(String userId, String unitId) {
        logger.debug("Generating unit report for userId: {} and unitId: {}", userId, unitId);
        
        try {
            Unit unit = unitRepository.findById(unitId)
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found with ID: " + unitId));
                
            UnitReportDTO report = new UnitReportDTO();
            report.setUnitId(unitId);
            report.setUnitName(unit.getUnitName());
            report.setUnitDesc(unit.getUnitDesc());
            
            // Get all subconcepts for the unit
            List<ProgramConceptsMapping> mappings = programConceptsMappingRepository.findByUnit_UnitId(unitId);
            logger.debug("Found {} subconcepts for unitId: {}", mappings.size(), unitId);
            
            List<SubconceptReportDTO> subconceptReports = new ArrayList<>();
            
            // Get all completed subconcepts for this user and unit
            List<UserSubConcept> completedSubconcepts = userSubConceptRepository
                .findByUser_UserIdAndUnit_UnitId(userId, unitId);
                
            // Process each subconcept
            for (ProgramConceptsMapping mapping : mappings) {
                SubconceptReportDTO subconceptReport = new SubconceptReportDTO();
                subconceptReport.setSubconceptId(mapping.getSubconcept().getSubconceptId());
                subconceptReport.setSubconceptDesc(mapping.getSubconcept().getSubconceptDesc());
                
                // Check completion status
                boolean isCompleted = completedSubconcepts.stream()
                    .anyMatch(cs -> cs.getSubconcept().getSubconceptId()
                        .equals(mapping.getSubconcept().getSubconceptId()));
                subconceptReport.setCompleted(isCompleted);
                
                // Get attempts for this subconcept
                List<AttemptDTO> attempts = getUserAttempts(userId, mapping.getSubconcept().getSubconceptId());
                subconceptReport.setAttempts(attempts);
                
                // Calculate statistics
                if (!attempts.isEmpty()) {
                    subconceptReport.setAttemptCount(attempts.size());
                    subconceptReport.setHighestScore(attempts.stream()
                        .mapToInt(AttemptDTO::getScore)
                        .max()
                        .orElse(0));
                    subconceptReport.setLastAttemptDate(attempts.get(attempts.size() - 1).getEndTimestamp());
                }
                
                // Map the Concept data from the subconcept
                if (mapping.getSubconcept().getConcept() != null) {
                    ConceptDTO conceptDTO = new ConceptDTO();
                    conceptDTO.setConceptId(mapping.getSubconcept().getConcept().getConceptId());
                    conceptDTO.setConceptName(mapping.getSubconcept().getConcept().getConceptName());
                    conceptDTO.setConceptDesc(mapping.getSubconcept().getConcept().getConceptDesc());
                    conceptDTO.setConceptSkill1(mapping.getSubconcept().getConcept().getConceptSkill1());
                    conceptDTO.setConceptSkill2(mapping.getSubconcept().getConcept().getConceptSkill2());
                    
                    // If the concept itself has a content object, map it too
                    if (mapping.getSubconcept().getConcept().getContent() != null) {
                        ContentDTO conceptContentDTO = new ContentDTO();
                        conceptContentDTO.setContentId(mapping.getSubconcept().getConcept().getContent().getContentId());
                        conceptContentDTO.setContentName(mapping.getSubconcept().getConcept().getContent().getContentName());
                        conceptContentDTO.setContentDesc(mapping.getSubconcept().getConcept().getContent().getContentDesc());
                        conceptContentDTO.setContentOrigin(mapping.getSubconcept().getConcept().getContent().getContentOrigin());
                        conceptContentDTO.setContentTopic(mapping.getSubconcept().getConcept().getContent().getContentTopic());
                        conceptDTO.setContent(conceptContentDTO);
                    }
                    subconceptReport.setConcept(conceptDTO);
                }
                
                // Add the mapped DTO to your list
                subconceptReports.add(subconceptReport);
            }
            
            // Calculate unit statistics
            report.setTotalSubconcepts(mappings.size());
            report.setCompletedSubconcepts((int) subconceptReports.stream()
                .filter(SubconceptReportDTO::isCompleted)
                .count());
            report.setCompletionPercentage(calculatePercentage(report.getCompletedSubconcepts(), report.getTotalSubconcepts()));
            
            // Calculate average score for unit
            double totalScore = subconceptReports.stream()
                .mapToDouble(SubconceptReportDTO::getHighestScore)
                .filter(score -> score > 0)
                .average()
                .orElse(0.0);
            report.setAverageScore(totalScore);
            
            report.setSubconcepts(subconceptReports);
            report.setCompletionStatus(report.getCompletedSubconcepts() == report.getTotalSubconcepts() ? "yes" : "no");
            
            logger.debug("Generated unit report for unitId: {} - {}/{} subconcepts completed", 
                unitId, report.getCompletedSubconcepts(), report.getTotalSubconcepts());
            
            return report;
        } catch (Exception e) {
            logger.error("Error generating unit report for userId: {} and unitId: {}", userId, unitId, e);
            throw e;
        }
    }
    
    
    @Override
    @Cacheable(value = "userAttempts", key = "#userId + '_' + #subconceptId")
    public List<AttemptDTO> getUserAttempts(String userId, String subconceptId) {
    	 logger.debug("Fetching user attempts for userId: {} and subconceptId: {}", userId, subconceptId);
         
    	 try {
             List<UserAttempts> attempts = userAttemptsRepository
                 .findByUser_UserIdAndSubconcept_SubconceptId(userId, subconceptId);
                 
             List<AttemptDTO> attemptDTOs = attempts.stream()
                 .map(this::mapToAttemptDTO)
                 .sorted(Comparator.comparing(AttemptDTO::getStartTimestamp))
                 .collect(Collectors.toList());
                 
             logger.debug("Found {} attempts for userId: {} and subconceptId: {}", attemptDTOs.size(), userId, subconceptId);
             return attemptDTOs;
         } catch (Exception e) {
             logger.error("Error fetching user attempts for userId: {} and subconceptId: {}", userId, subconceptId, e);
             throw e;
         }
     }

    private AttemptDTO mapToAttemptDTO(UserAttempts attempt) {
        AttemptDTO dto = new AttemptDTO();
        dto.setAttemptId(attempt.getUserAttemptId());
        dto.setStartTimestamp(attempt.getUserAttemptStartTimestamp());
        dto.setEndTimestamp(attempt.getUserAttemptEndTimestamp());
        dto.setScore(attempt.getUserAttemptScore());
        dto.setSuccessful(attempt.isUserAttemptFlag());
        return dto;
    }

    private double calculatePercentage(int completed, int total) {
        return total == 0 ? 0 : (completed * 100.0) / total;
    }

    private Map<String, Integer> generateScoreDistribution(List<UserAttempts> attempts) {
    	logger.debug("Generating score distribution for {} attempts", attempts.size());
    	
        Map<String, Integer> distribution = new HashMap<>();
        distribution.put("0-20", 0);
        distribution.put("21-40", 0);
        distribution.put("41-60", 0);
        distribution.put("61-80", 0);
        distribution.put("81-100", 0);
        
        for (UserAttempts attempt : attempts) {
            int score = attempt.getUserAttemptScore();
            if (score <= 20) distribution.put("0-20", distribution.get("0-20") + 1);
            else if (score <= 40) distribution.put("21-40", distribution.get("21-40") + 1);
            else if (score <= 60) distribution.put("41-60", distribution.get("41-60") + 1);
            else if (score <= 80) distribution.put("61-80", distribution.get("61-80") + 1);
            else distribution.put("81-100", distribution.get("81-100") + 1);
        }
        
        return distribution;
    }
    
    @Override
    @CacheEvict(value = {"programReports", "stageReports", "unitReports", "userAttempts"}, allEntries = true)
    public byte[] generateCsvReport(String userId, String programId) {
        logger.info("Generating CSV report for userId: {} and programId: {}", userId, programId);
        long startTime = System.currentTimeMillis();
        
        try {
            ProgramReportDTO report = generateProgramReport(userId, programId);
            UserDTO user = getUserInfo(userId);

            try (ByteArrayOutputStream out = new ByteArrayOutputStream();
                 CSVPrinter csvPrinter = new CSVPrinter(new PrintWriter(out), CSVFormat.DEFAULT)) {

                // Header
                csvPrinter.printRecord("Learner Name", "Learner ID", "Learner PhoneNumber", "Program ID", 
                                        "Program Name", "Stage Name", "Unit Name", "Subconcept Name", 
                                       "Completion Status", "Average Score", "Attempt Count");

                int recordCount = 0;
                // Data - Include user info for each record
                for (StageReportDTO stageReport : report.getStages()) {
                    for (UnitReportDTO unitReport : stageReport.getUnits()) {
                        for (SubconceptReportDTO subconceptReport : unitReport.getSubconcepts()) {
                            csvPrinter.printRecord(
                                    user.getUserName(),                
                                    userId,                            
                                    user.getUserPhoneNumber(),             
                                    programId,                         
                                    report.getProgramName(),          
                                    stageReport.getStageName(),        
                                    unitReport.getUnitName(),          
                                    subconceptReport.getSubconceptDesc(),  
                                    subconceptReport.isCompleted() ? "Completed" : "Incomplete",  
                                    subconceptReport.getHighestScore(),  
                                    subconceptReport.getAttemptCount()  
                            );
                            recordCount++;
                        }
                    }
                }

                csvPrinter.flush();
                byte[] result = out.toByteArray();
                
                long endTime = System.currentTimeMillis();
                logger.info("Successfully generated CSV report for userId: {} and programId: {} in {}ms. " +
                        "Generated {} records, file size: {} bytes", 
                        userId, programId, (endTime - startTime), recordCount, result.length);
                
                return result;
            }
        } catch (IOException e) {
            logger.error("IOException while generating CSV report for userId: {} and programId: {}", userId, programId, e);
            throw new RuntimeException("Failed to generate CSV", e);
        } catch (Exception e) {
            logger.error("Error generating CSV report for userId: {} and programId: {}", userId, programId, e);
            throw e;
        }
    }

    @Override
    @CacheEvict(value = {"programReports", "stageReports", "unitReports", "userAttempts"}, allEntries = true)
    public byte[] generatePdfReport(String userId, String programId) {
        logger.info("Generating PDF report for userId: {} and programId: {}", userId, programId);
        long startTime = System.currentTimeMillis();
        
        try {
            ProgramReportDTO report = generateProgramReport(userId, programId);
            UserDTO user = getUserInfo(userId);

            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                PdfWriter writer = new PdfWriter(out);
                PdfDocument pdfDoc = new PdfDocument(writer);
                Document document = new Document(pdfDoc);

                // Add title
                document.add(new Paragraph("Program Report").setBold().setFontSize(18));

                // Program and user details
                document.add(new Paragraph("Username: " + user.getUserName()));
                document.add(new Paragraph("User ID: " + userId));
                document.add(new Paragraph("User Phone Number: " + user.getUserPhoneNumber()));
                document.add(new Paragraph("Program ID: " + programId));
                document.add(new Paragraph("Program Name: " + report.getProgramName()));
                document.add(new Paragraph("Total Stages: " + report.getTotalStages()));
                document.add(new Paragraph("Completed Stages: " + report.getCompletedStages()));
                document.add(new Paragraph("Average Score: " + report.getAverageScore()));

                int totalSections = 0;
                // Add stages, units, and subconcepts
                for (StageReportDTO stageReport : report.getStages()) {
                    document.add(new Paragraph("Stage: " + stageReport.getStageName()).setBold());

                    for (UnitReportDTO unitReport : stageReport.getUnits()) {
                        document.add(new Paragraph("  Unit: " + unitReport.getUnitName()));

                        for (SubconceptReportDTO subconceptReport : unitReport.getSubconcepts()) {
                            document.add(new Paragraph("    Subconcept: " + subconceptReport.getSubconceptDesc()
                                    + " (Status: " + (subconceptReport.isCompleted() ? "Completed" : "Incomplete") + ")"));
                            totalSections++;
                        }
                    }
                }

                document.close();
                byte[] result = out.toByteArray();
                
                long endTime = System.currentTimeMillis();
                logger.info("Successfully generated PDF report for userId: {} and programId: {} in {}ms. " +
                        "Generated {} sections, file size: {} bytes", 
                        userId, programId, (endTime - startTime), totalSections, result.length);
                
                return result;
            }
        } catch (IOException e) {
            logger.error("IOException while generating PDF report for userId: {} and programId: {}", userId, programId, e);
            throw new RuntimeException("Failed to generate PDF", e);
        } catch (Exception e) {
            logger.error("Error generating PDF report for userId: {} and programId: {}", userId, programId, e);
            throw e;
        }
    }

    
    @Override
    @Cacheable(value = "cohortProgress", key = "#programId + '_' + #cohortId", unless = "#result == null")
    public CohortProgressDTO getCohortProgress(String programId, String cohortId) {
    	logger.info("Generating cohort progress for programId: {} and cohortId: {}", programId, cohortId);
        long startTime = System.currentTimeMillis();
        
        try {
        // Fetch the program
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new ResourceNotFoundException("Program not found with ID: " + programId));
            
            // Fetch the cohort
            Cohort cohort = cohortRepository.findById(cohortId)
                .orElseThrow(() -> new ResourceNotFoundException("Cohort not found with ID: " + cohortId));
            
            logger.debug("Found program: {} and cohort: {}", program.getProgramName(), cohort.getCohortName());
            
            // Fetch all users in the cohort
            List<UserCohortMapping> userMappings = userCohortMappingRepository.findByCohortCohortId(cohortId);
            List<User> users = userMappings.stream()
                .map(UserCohortMapping::getUser)
                .collect(Collectors.toList());
            
            logger.debug("Found {} users in cohort: {}", users.size(), cohortId);
            
            // Prepare progress data for each user
            List<UserProgressDTO> userProgressList = new ArrayList<>();
            for (User user : users) {
                logger.debug("Processing progress for user: {} (ID: {})", user.getUserName(), user.getUserId());
                
                UserProgressDTO userProgress = new UserProgressDTO();
                userProgress.setUserId(user.getUserId());
                userProgress.setUserName(user.getUserName());
                
                // Fetch stages for the program
                List<Stage> stages = stageRepository.findByProgram_ProgramId(programId);
                int totalStages = stages.size();
                int completedStages = 0;
                int totalUnits = 0;
                int completedUnits = 0;
                int totalSubconcepts = 0;
                int completedSubconcepts = 0;
                
                // Process each stage
                boolean previousStageCompleted = true;
                for (Stage stage : stages) {
                    List<Unit> units = unitRepository.findByStage_StageId(stage.getStageId());
                    totalUnits += units.size();
                    
                    // Process units within stage
                    boolean stageCompleted = true;
                    boolean previousUnitCompleted = true;
                    
                    for (Unit unit : units) {
                        // Get all subconcepts for the unit
                        List<ProgramConceptsMapping> subconcepts = 
                            programConceptsMappingRepository.findByUnit_UnitId(unit.getUnitId());
                        totalSubconcepts += subconcepts.size();
                        
                        // Get completed subconcepts for this user and unit
                        List<UserSubConcept> completedSubconceptsList = userSubConceptRepository
                            .findByUser_UserIdAndUnit_UnitId(user.getUserId(), unit.getUnitId());
                        completedSubconcepts += completedSubconceptsList.size();
                        
                        // Check if unit is completed (all subconcepts completed)
                        boolean unitCompleted = previousUnitCompleted && 
                            completedSubconceptsList.size() == subconcepts.size();
                        
                        if (unitCompleted) {
                            completedUnits++;
                        }
                        
                        // Update completion tracking for next unit
                        previousUnitCompleted = unitCompleted;
                        if (!unitCompleted) {
                            stageCompleted = false;
                        }
                    }
                    
                    // Check if stage is completed (all units completed and previous stage completed)
                    if (stageCompleted && previousStageCompleted) {
                        completedStages++;
                    }
                    
                    // Update completion tracking for next stage
                    previousStageCompleted = stageCompleted;
                }
                
                // Populate progress statistics
                userProgress.setTotalStages(totalStages);
                userProgress.setCompletedStages(completedStages);
                userProgress.setTotalUnits(totalUnits);
                userProgress.setCompletedUnits(completedUnits);
                userProgress.setTotalSubconcepts(totalSubconcepts);
                userProgress.setCompletedSubconcepts(completedSubconcepts);
                
                // Fetch leaderboard score
                UserCohortMapping mapping = userMappings.stream()
                    .filter(um -> um.getUser().getUserId().equals(user.getUserId()))
                    .findFirst()
                    .orElse(null);
            userProgress.setLeaderboardScore(mapping != null ? mapping.getLeaderboardScore() : 0);
            
            userProgressList.add(userProgress);
        }
         // Sort users by leaderboard score in descending order
            userProgressList.sort((u1, u2) -> Integer.compare(u2.getLeaderboardScore(), u1.getLeaderboardScore()));
            
        // Prepare response DTO
        CohortProgressDTO cohortProgress = new CohortProgressDTO();
        cohortProgress.setProgramName(program.getProgramName());
        cohortProgress.setProgramId(program.getProgramId());
        cohortProgress.setProgramDesc(program.getProgramDesc());
        cohortProgress.setCohortId(cohort.getCohortId());
        cohortProgress.setCohortName(cohort.getCohortName());
        cohortProgress.setUsers(userProgressList);
        
        long endTime = System.currentTimeMillis();
        logger.info("Successfully generated cohort progress for programId: {} and cohortId: {} in {}ms. " +
                "Processed {} users", 
                programId, cohortId, (endTime - startTime), userProgressList.size());
        
        return cohortProgress;
        
    } catch (Exception e) {
        logger.error("Error generating cohort progress for programId: {} and cohortId: {}", programId, cohortId, e);
        throw e;
    }
}
    
    
    @Override
    @Cacheable(value = "userProgress", key = "#programId + '_' + #userId", unless = "#result == null")
    public UserProgressDTO getUserProgress(String programId, String userId) {
        logger.info("Generating user progress for programId: {} and userId: {}", programId, userId);
        long startTime = System.currentTimeMillis();
        
        try {
            // Fetch the program
            Program program = programRepository.findById(programId)
                .orElseThrow(() -> new ResourceNotFoundException("Program not found with ID: " + programId));

            // Fetch the user
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

            logger.debug("Found program: {} and user: {}", program.getProgramName(), user.getUserName());

            // Fetch the cohort for this user in the given program
            UserCohortMapping userCohortMapping = userCohortMappingRepository
                .findByUserUserIdAndProgramId(userId, programId)
                .orElseThrow(() -> new ResourceNotFoundException("User not enrolled in any cohort for this program"));
            
            // Fetch stages for the program
            List<Stage> stages = stageRepository.findByProgram_ProgramId(programId);
            int totalStages = stages.size();
            int completedStages = 0;
            int totalUnits = 0;
            int completedUnits = 0;
            int totalSubconcepts = 0;
            int completedSubconcepts = 0;

            logger.debug("Found {} stages for programId: {}", stages.size(), programId);

            // Process each stage - FIXED LOGIC to match generateProgramReport()
            for (Stage stage : stages) {
                List<Unit> units = unitRepository.findByStage_StageId(stage.getStageId());
                totalUnits += units.size();

                int stageCompletedUnits = 0;
                int stageTotalUnits = units.size();

                for (Unit unit : units) {
                    List<ProgramConceptsMapping> subconcepts = 
                        programConceptsMappingRepository.findByUnit_UnitId(unit.getUnitId());
                    totalSubconcepts += subconcepts.size();

                    List<UserSubConcept> completedSubconceptsList = userSubConceptRepository
                        .findByUser_UserIdAndUnit_UnitId(userId, unit.getUnitId());
                    completedSubconcepts += completedSubconceptsList.size();

                    // Check if unit is completed (all subconcepts completed)
                    boolean unitCompleted = completedSubconceptsList.size() == subconcepts.size();

                    if (unitCompleted) {
                        completedUnits++;
                        stageCompletedUnits++;
                    }
                }

                // FIXED: Stage is completed when ALL its units are completed
                // No dependency on previous stages
                if (stageCompletedUnits == stageTotalUnits && stageTotalUnits > 0) {
                    completedStages++;
                }
            }

            // Prepare response DTO
            UserProgressDTO userProgress = new UserProgressDTO();
            userProgress.setUserId(user.getUserId());
            userProgress.setUserName(user.getUserName());
            userProgress.setTotalStages(totalStages);
            userProgress.setCompletedStages(completedStages);
            userProgress.setTotalUnits(totalUnits);
            userProgress.setCompletedUnits(completedUnits);
            userProgress.setTotalSubconcepts(totalSubconcepts);
            userProgress.setCompletedSubconcepts(completedSubconcepts);
            userProgress.setLeaderboardScore(userCohortMapping.getLeaderboardScore());

            long endTime = System.currentTimeMillis();
            logger.info("Successfully generated user progress for programId: {} and userId: {} in {}ms. " +
                    "Stats: {}/{} stages, {}/{} units, {}/{} subconcepts completed", 
                    programId, userId, (endTime - startTime),
                    completedStages, totalStages, completedUnits, totalUnits, 
                    completedSubconcepts, totalSubconcepts);

            return userProgress;
            
        } catch (Exception e) {
            logger.error("Error generating user progress for programId: {} and userId: {}", programId, userId, e);
            throw e;
        }
    }

    // Additional method to clear user progress cache when progress is updated
    @CacheEvict(value = {"userProgress", "cohortProgress"}, allEntries = true)
    public void clearProgressCache() {
        logger.info("Clearing progress cache for all users and cohorts");
    }

    // Method to clear specific user progress cache
    @CacheEvict(value = "userProgress", key = "#programId + '_' + #userId")
    public void clearUserProgressCache(String programId, String userId) {
        logger.info("Clearing user progress cache for programId: {} and userId: {}", programId, userId);
    }

    // Method to clear specific cohort progress cache
    @CacheEvict(value = "cohortProgress", key = "#programId + '_' + #cohortId")
    public void clearCohortProgressCache(String programId, String cohortId) {
        logger.info("Clearing cohort progress cache for programId: {} and cohortId: {}", programId, cohortId);
    }
    
    /**
     * Method to manually evict report caches when called externally
     * This can be called from your UserAttempts creation logic
     */
    @CacheEvict(value = {"programReports", "stageReports", "unitReports", "userAttempts"}, allEntries = false)
    public void evictUserReportCaches(String userId, String programId, String stageId, String unitId, String subconceptId) {
        logger.info("Manually evicting report caches for userId: {}, programId: {}", userId, programId);
        
        // The @CacheEvict annotation above will handle the eviction,
        // but we can also use the CacheManagementService for more granular control
        try {
            // Get all related IDs for comprehensive cache eviction
            List<String> stageIds = Collections.singletonList(stageId);
            List<String> unitIds = Collections.singletonList(unitId);
            
            // Use the cache management service for comprehensive eviction
            cacheManagementService.evictReportCaches(userId, programId, stageIds, unitIds);
            
        } catch (Exception e) {
            logger.error("Error in manual cache eviction for userId: {}, programId: {}", userId, programId, e);
        }
    }
}