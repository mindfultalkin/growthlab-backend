package com.mindfultalk.growthlab.dto;

import java.util.List;

public class StageReportStageDTO {
	private String stageId;
    private String stageName;
    private String stageDesc;
    private int totalUnits;
    private int completedUnits;
    private double completionPercentage;
    private double averageScore;
    private List<UnitReportStageDTO> units;
    private boolean isEnabled;
    private String completionStatus;
	public String getStageId() {
		return stageId;
	}
	public void setStageId(String stageId) {
		this.stageId = stageId;
	}
	public String getStageName() {
		return stageName;
	}
	public void setStageName(String stageName) {
		this.stageName = stageName;
	}
	public String getStageDesc() {
		return stageDesc;
	}
	public void setStageDesc(String stageDesc) {
		this.stageDesc = stageDesc;
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
	public double getCompletionPercentage() {
		return completionPercentage;
	}
	public void setCompletionPercentage(double completionPercentage) {
		this.completionPercentage = completionPercentage;
	}
	public double getAverageScore() {
		return averageScore;
	}
	public void setAverageScore(double averageScore) {
		this.averageScore = averageScore;
	}
	public List<UnitReportStageDTO> getUnits() {
		return units;
	}
	public void setUnits(List<UnitReportStageDTO> units) {
		this.units = units;
	}
	public boolean isEnabled() {
		return isEnabled;
	}
	public void setEnabled(boolean isEnabled) {
		this.isEnabled = isEnabled;
	}
	public String getCompletionStatus() {
		return completionStatus;
	}
	public void setCompletionStatus(String completionStatus) {
		this.completionStatus = completionStatus;
	}

    
}