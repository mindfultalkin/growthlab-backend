package com.mindfultalk.growthlab.controller;

import com.mindfultalk.growthlab.dto.*;
import com.mindfultalk.growthlab.service.*;
import org.springframework.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reports")
public class ProgramReportController {

    @Autowired
    private ProgramReportService programReportService;
    
   
  @GetMapping("/program/{userId}/{programId}")
  public ProgramReportDTO generateProgramReport(
        @PathVariable String userId,
        @PathVariable String programId) {
    return programReportService.generateProgramReport(userId, programId);
  }
  @GetMapping("/program/{programId}/cohort/{cohortId}/progress")
  public ResponseEntity<CohortProgressDTO> getCohortProgress(
      @PathVariable String programId, 
      @PathVariable String cohortId) {
      CohortProgressDTO progress = programReportService.getCohortProgress(programId, cohortId);
      return ResponseEntity.ok(progress);
  }
  @GetMapping("/program/{programId}/user/{userId}/progress")
  public ResponseEntity<UserProgressDTO> getUserProgress(
      @PathVariable String programId,
      @PathVariable String userId) {
      
      UserProgressDTO progress = programReportService.getUserProgress(programId, userId);
      return ResponseEntity.ok(progress);
  }

    @GetMapping("/program/{userId}/{programId}/download")
    public ResponseEntity<?> downloadProgramReport(
            @PathVariable String userId,
            @PathVariable String programId,
            @RequestParam String format) {
        if ("csv".equalsIgnoreCase(format)) {
            byte[] csvData = programReportService.generateCsvReport(userId, programId);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report.csv")
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(csvData);
        } else if ("pdf".equalsIgnoreCase(format)) {
            byte[] pdfData = programReportService.generatePdfReport(userId, programId);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfData);
        } else {
            return ResponseEntity.badRequest().body("Invalid format");
        }
    }
}