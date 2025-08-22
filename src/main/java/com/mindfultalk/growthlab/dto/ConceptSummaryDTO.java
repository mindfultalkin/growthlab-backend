package com.mindfultalk.growthlab.dto;

import java.util.List;

public class ConceptSummaryDTO {
	private String conceptId;
    private String conceptName;
    private String conceptDesc;
    private List<String> conceptSkills;
    private ContentDTO content;
    private int totalSubconcepts;
    private int completedSubconcepts;
    private double averageScore;
    private List<SubconceptReportDTO> subconcepts;
 
    // Getters and setters...
	public String getConceptId() {
		return conceptId;
	}
	public void setConceptId(String conceptId) {
		this.conceptId = conceptId;
	}
	public String getConceptName() {
		return conceptName;
	}
	public void setConceptName(String conceptName) {
		this.conceptName = conceptName;
	}
	public String getConceptDesc() {
		return conceptDesc;
	}
	public void setConceptDesc(String conceptDesc) {
		this.conceptDesc = conceptDesc;
	}
	public List<String> getConceptSkills() {
		return conceptSkills;
	}
	public void setConceptSkills(List<String> conceptSkills) {
		this.conceptSkills = conceptSkills;
	}
	public ContentDTO getContent() {
		return content;
	}
	public void setContent(ContentDTO content) {
		this.content = content;
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
	public double getAverageScore() {
		return averageScore;
	}
	public void setAverageScore(double averageScore) {
		this.averageScore = averageScore;
	}
	public List<SubconceptReportDTO> getSubconcepts() {
		return subconcepts;
	}
	public void setSubconcepts(List<SubconceptReportDTO> subconcepts) {
		this.subconcepts = subconcepts;
	}
    

}