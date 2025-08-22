package com.mindfultalk.growthlab.dto;

import java.util.Map;

public class StageDTO {
    private String stageId;
    private String stageName;
    private String stageDesc;
    private String stageCompletionStatus;
    private boolean stageEnabled;
    private Integer daysUntilNextStageEnabled;
    private String stageAvailableDate;
    private Map<String, UnitResponseDTO> units; // Use a Map for dynamic keys
    
    // Getters and Setters  
    public Map<String, UnitResponseDTO> getUnits() {
		return units;
	}
	public void setUnits(Map<String, UnitResponseDTO> units) {
		this.units = units;
	}
	public String getStageCompletionStatus() {
		return stageCompletionStatus;
	}
	public void setStageCompletionStatus(String stageCompletionStatus) {
		this.stageCompletionStatus = stageCompletionStatus;
	}
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
	public boolean isStageEnabled() {
		return stageEnabled;
	}
	public void setStageEnabled(boolean stageEnabled) {
		this.stageEnabled = stageEnabled;
	}
	public Integer getDaysUntilNextStageEnabled() {
		return daysUntilNextStageEnabled;
	}
	public void setDaysUntilNextStageEnabled(Integer daysUntilNextStageEnabled) {
		this.daysUntilNextStageEnabled = daysUntilNextStageEnabled;
	}
	public String getStageAvailableDate() {
		return stageAvailableDate;
	}
	public void setStageAvailableDate(String stageAvailableDate) {
		this.stageAvailableDate = stageAvailableDate;
	}
}