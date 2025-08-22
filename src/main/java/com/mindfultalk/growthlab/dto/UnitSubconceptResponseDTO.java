package com.mindfultalk.growthlab.dto;

import java.util.Map;

public class UnitSubconceptResponseDTO {
	private String userId;
    private String cohortId;
    private String programId;
    private String stageId;
    private String unitId;
    private Map<String, SubconceptResponseDTO> subConcepts;
    private String unitCompletionStatus;
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public String getCohortId() {
		return cohortId;
	}
	public void setCohortId(String cohortId) {
		this.cohortId = cohortId;
	}
	public String getProgramId() {
		return programId;
	}
	public void setProgramId(String programId) {
		this.programId = programId;
	}
	public String getStageId() {
		return stageId;
	}
	public void setStageId(String stageId) {
		this.stageId = stageId;
	}
	public String getUnitId() {
		return unitId;
	}
	public void setUnitId(String unitId) {
		this.unitId = unitId;
	}
	public Map<String, SubconceptResponseDTO> getSubConcepts() {
		return subConcepts;
	}
	public void setSubConcepts(Map<String, SubconceptResponseDTO> subConcepts) {
		this.subConcepts = subConcepts;
	}
	public String getUnitCompletionStatus() {
		return unitCompletionStatus;
	}
	public void setUnitCompletionStatus(String unitCompletionStatus) {
		this.unitCompletionStatus = unitCompletionStatus;
	}

}