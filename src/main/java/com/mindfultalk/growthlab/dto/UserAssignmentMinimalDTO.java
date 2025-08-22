package com.mindfultalk.growthlab.dto;

import java.time.OffsetDateTime;

import lombok.Data;

@Data
public class UserAssignmentMinimalDTO {
    private String assignmentId;

    private String userId;
    private String userName;

    private String cohortId;
    private String cohortName;

    private String programId;
    private String programName;

    private String stageId;
    private String stageName;

    private String unitId;
    private String unitName;

    private String subconceptId;
    private String subconceptDesc;
    private String subconceptLink;

    private String fileName;
    private String filePath;
    private Long fileSize;

    private OffsetDateTime submittedDate;
    private Integer score;
    private String remarks;
	public String getAssignmentId() {
		return assignmentId;
	}
	public void setAssignmentId(String assignmentId) {
		this.assignmentId = assignmentId;
	}
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
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
	public String getUnitId() {
		return unitId;
	}
	public void setUnitId(String unitId) {
		this.unitId = unitId;
	}
	public String getUnitName() {
		return unitName;
	}
	public void setUnitName(String unitName) {
		this.unitName = unitName;
	}
	public String getSubconceptId() {
		return subconceptId;
	}
	public void setSubconceptId(String subconceptId) {
		this.subconceptId = subconceptId;
	}
	public String getSubconceptDesc() {
		return subconceptDesc;
	}
	public void setSubconceptDesc(String subconceptDesc) {
		this.subconceptDesc = subconceptDesc;
	}
	public String getSubconceptLink() {
		return subconceptLink;
	}
	public void setSubconceptLink(String subconceptLink) {
		this.subconceptLink = subconceptLink;
	}
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public String getFilePath() {
		return filePath;
	}
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
	public Long getFileSize() {
		return fileSize;
	}
	public void setFileSize(Long fileSize) {
		this.fileSize = fileSize;
	}
	public OffsetDateTime getSubmittedDate() {
		return submittedDate;
	}
	public void setSubmittedDate(OffsetDateTime offsetDateTime) {
		this.submittedDate = offsetDateTime;
	}
	public Integer getScore() {
		return score;
	}
	public void setScore(Integer score) {
		this.score = score;
	}
	public String getRemarks() {
		return remarks;
	}
	public void setRemarks(String remarks) {
		this.remarks = remarks;
	}
    
}