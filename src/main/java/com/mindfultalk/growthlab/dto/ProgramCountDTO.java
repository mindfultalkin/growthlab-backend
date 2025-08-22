package com.mindfultalk.growthlab.dto;

public class ProgramCountDTO {
    private String programId;
    private String programName;
    private int totalStages;
    private int totalUnits;
    private int totalSubconcepts;
    
 // Getters and Setters
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
	public int getTotalStages() {
		return totalStages;
	}
	public void setTotalStages(int totalStages) {
		this.totalStages = totalStages;
	}
	public int getTotalUnits() {
		return totalUnits;
	}
	public void setTotalUnits(int totalUnits) {
		this.totalUnits = totalUnits;
	}
	public int getTotalSubconcepts() {
		return totalSubconcepts;
	}
	public void setTotalSubconcepts(int totalSubconcepts) {
		this.totalSubconcepts = totalSubconcepts;
	}

}