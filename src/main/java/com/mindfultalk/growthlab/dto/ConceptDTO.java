package com.mindfultalk.growthlab.dto;

public class ConceptDTO {
	
	private String conceptId;
    private String conceptName;
    private String conceptDesc;
    private String conceptSkill1;
    private String conceptSkill2;
    
    private ContentDTO content;
    
    // Getters and Setters
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
	public String getConceptSkill1() {
		return conceptSkill1;
	}
	public void setConceptSkill1(String conceptSkill1) {
		this.conceptSkill1 = conceptSkill1;
	}
	public String getConceptSkill2() {
		return conceptSkill2;
	}
	public void setConceptSkill2(String conceptSkill2) {
		this.conceptSkill2 = conceptSkill2;
	}
	public ContentDTO getContent() {
		return content;
	}
	public void setContent(ContentDTO content) {
		this.content = content;
	}
	
}