package com.mindfultalk.growthlab.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public class ProgramReportDTO {
    private String programId;
    private String programName;
    private String programDesc;
    private int totalStages;
    private int completedStages;
    private int totalUnits;
    private int completedUnits;
    private int totalSubconcepts;
    private int completedSubconcepts;
    private double stageCompletionPercentage;
    private double unitCompletionPercentage;
    private double subconceptCompletionPercentage;
    private double averageScore;
    private OffsetDateTime firstAttemptDate;  
    private OffsetDateTime lastAttemptDate;  
    private List<StageReportDTO> stages;
    private Map<String, Integer> scoreDistribution;
	public String getProgramId() {
		return programId;
	}
	public void setProgramId(String programId) {
		this.programId = programId;
	}
	public String getProgramName() {
		return programName;
	}
	public void setProgramName(String programName) {
		this.programName = programName;
	}
	public String getProgramDesc() {
		return programDesc;
	}
	public void setProgramDesc(String programDesc) {
		this.programDesc = programDesc;
	}
	public int getTotalStages() {
		return totalStages;
	}
	public void setTotalStages(int totalStages) {
		this.totalStages = totalStages;
	}
	public int getCompletedStages() {
		return completedStages;
	}
	public void setCompletedStages(int completedStages) {
		this.completedStages = completedStages;
	}
	public int getTotalUnits() {
		return totalUnits;
	}
	public void setTotalUnits(int totalUnits) {
		this.totalUnits = totalUnits;
	}
	public int getCompletedUnits() {
		return completedUnits;
	}
	public void setCompletedUnits(int completedUnits) {
		this.completedUnits = completedUnits;
	}
	public int getTotalSubconcepts() {
		return totalSubconcepts;
	}
	public void setTotalSubconcepts(int totalSubconcepts) {
		this.totalSubconcepts = totalSubconcepts;
	}
	public int getCompletedSubconcepts() {
		return completedSubconcepts;
	}
	public void setCompletedSubconcepts(int completedSubconcepts) {
		this.completedSubconcepts = completedSubconcepts;
	}
	public double getStageCompletionPercentage() {
		return stageCompletionPercentage;
	}
	public void setStageCompletionPercentage(double stageCompletionPercentage) {
		this.stageCompletionPercentage = stageCompletionPercentage;
	}
	public double getUnitCompletionPercentage() {
		return unitCompletionPercentage;
	}
	public void setUnitCompletionPercentage(double unitCompletionPercentage) {
		this.unitCompletionPercentage = unitCompletionPercentage;
	}
	public double getSubconceptCompletionPercentage() {
		return subconceptCompletionPercentage;
	}
	public void setSubconceptCompletionPercentage(double subconceptCompletionPercentage) {
		this.subconceptCompletionPercentage = subconceptCompletionPercentage;
	}
	public double getAverageScore() {
		return averageScore;
	}
	public void setAverageScore(double averageScore) {
		this.averageScore = averageScore;
	}
	
	public OffsetDateTime getFirstAttemptDate() {
		return firstAttemptDate;
	}
	public void setFirstAttemptDate(OffsetDateTime firstAttemptDate) {
		this.firstAttemptDate = firstAttemptDate;
	}
	public OffsetDateTime getLastAttemptDate() {
		return lastAttemptDate;
	}
	public void setLastAttemptDate(OffsetDateTime lastAttemptDate) {
		this.lastAttemptDate = lastAttemptDate;
	}
	public List<StageReportDTO> getStages() {
		return stages;
	}
	public void setStages(List<StageReportDTO> stages) {
		this.stages = stages;
	}
	public Map<String, Integer> getScoreDistribution() {
		return scoreDistribution;
	}
	public void setScoreDistribution(Map<String, Integer> scoreDistribution) {
		this.scoreDistribution = scoreDistribution;
	}
}