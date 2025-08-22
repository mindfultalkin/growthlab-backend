package com.mindfultalk.growthlab.dto;

import java.util.List;

public class CohortProgressDTO {
    private String programName;
    private String programId;
    private String programDesc;
    private String cohortId;
    private String cohortName;
    private List<UserProgressDTO> users;
    
    // Getters and Setters
	public String getProgramName() {
		return programName;
	}
	public void setProgramName(String programName) {
		this.programName = programName;
	}
	public String getProgramId() {
		return programId;
	}
	public void setProgramId(String programId) {
		this.programId = programId;
	}
	public String getProgramDesc() {
		return programDesc;
	}
	public void setProgramDesc(String programDesc) {
		this.programDesc = programDesc;
	}
	public String getCohortId() {
		return cohortId;
	}
	public void setCohortId(String cohortId) {
		this.cohortId = cohortId;
	}
	public String getCohortName() {
		return cohortName;
	}
	public void setCohortName(String cohortName) {
		this.cohortName = cohortName;
	}
	public List<UserProgressDTO> getUsers() {
		return users;
	}
	public void setUsers(List<UserProgressDTO> users) {
		this.users = users;
	}
}