package com.mindfultalk.growthlab.dto;

import java.time.OffsetDateTime;
import java.util.List;

public class SubconceptReportDTO {
	private String subconceptId;
    private String subconceptDesc;
    private boolean isCompleted;
    private double highestScore;
    private int attemptCount;
    private OffsetDateTime lastAttemptDate;
    private String completionStatus;
    private List<AttemptDTO> attempts;
    
    private ConceptDTO concept;
 //   private ContentDTO content;
    
 // Getters and Setters
    
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
	public boolean isCompleted() {
		return isCompleted;
	}
	public void setCompleted(boolean isCompleted) {
		this.isCompleted = isCompleted;
	}
	public double getHighestScore() {
		return highestScore;
	}
	public void setHighestScore(double highestScore) {
		this.highestScore = highestScore;
	}
	public int getAttemptCount() {
		return attemptCount;
	}
	public void setAttemptCount(int attemptCount) {
		this.attemptCount = attemptCount;
	}
	public OffsetDateTime getLastAttemptDate() {
		return lastAttemptDate;
	}
	public void setLastAttemptDate(OffsetDateTime lastAttemptDate) {
		this.lastAttemptDate = lastAttemptDate;
	}
	public String getCompletionStatus() {
		return completionStatus;
	}
	public void setCompletionStatus(String completionStatus) {
		this.completionStatus = completionStatus;
	}
	public List<AttemptDTO> getAttempts() {
		return attempts;
	}
	public void setAttempts(List<AttemptDTO> attempts) {
		this.attempts = attempts;
	}
	public ConceptDTO getConcept() {
		return concept;
	}
	public void setConcept(ConceptDTO concept) {
		this.concept = concept;
	}

}