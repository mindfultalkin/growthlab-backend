package com.mindfultalk.growthlab.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "subconcepts")
public class Subconcept {

    @Id
    @Column(name = "subconcept_id", length = 255, nullable = false)
    private String subconceptId;

    @Column(name = "dependency", length = 1000, nullable = true)
    private String dependency;

    @Column(name = "show_to", length = 1000, nullable = true)
    private String showTo;

    @Column(name = "subconcept_desc", length = 1000, nullable = true)
    private String subconceptDesc;
    
    @Column(name = "subconcept_desc_2", length = 1000, nullable = true)
    private String subconceptDesc2;
    
    @Column(name = "subconcept_group", length = 1000, nullable = true)
    private String subconceptGroup;

    @Column(name = "subconcept_link")
    private String subconceptLink;

    @Column(name = "subconcept_type", length = 1000, nullable = false)
    private String subconceptType;

    @Column(name = "num_questions", length = 100, nullable = true)
    private Integer numQuestions;

    
    @Column(name = "subconcept_maxscore", nullable = true)
    private Integer subconceptMaxscore;

    @Column(name = "subconcept_duration", nullable = false)
    private Integer subconceptDuration;
    
    @Column(name = "uuid", nullable = false, unique = true)
    private UUID uuid;

    @ManyToOne
    @JoinColumn(name = "concept_id")
    private Concept concept;

    @ManyToOne
    @JoinColumn(name = "content_id")
    private ContentMaster content;

	public Subconcept() {
		
	}

	public Subconcept(String subconceptId, String dependency, String showTo, String subconceptDesc,
			String subconceptDesc2, String subconceptGroup, String subconceptLink, String subconceptType,
			Integer numQuestions, Integer subconceptMaxscore, Integer subconceptDuration, UUID uuid, Concept concept,
			ContentMaster content) {
		super();
		this.subconceptId = subconceptId;
		this.dependency = dependency;
		this.showTo = showTo;
		this.subconceptDesc = subconceptDesc;
		this.subconceptDesc2 = subconceptDesc2;
		this.subconceptGroup = subconceptGroup;
		this.subconceptLink = subconceptLink;
		this.subconceptType = subconceptType;
		this.numQuestions = numQuestions;
		this.subconceptMaxscore = subconceptMaxscore;
		this.subconceptDuration = subconceptDuration;
		this.uuid = uuid;
		this.concept = concept;
		this.content = content;
	}

	// Getters and Setters
	
	public String getSubconceptId() {
		return subconceptId;
	}

	public void setSubconceptId(String subconceptId) {
		this.subconceptId = subconceptId;
	}

	public String getDependency() {
		return dependency;
	}

	public void setDependency(String dependency) {
		this.dependency = dependency;
	}

	public String getShowTo() {
		return showTo;
	}

	public void setShowTo(String showTo) {
		this.showTo = showTo;
	}

	public String getSubconceptDesc() {
		return subconceptDesc;
	}

	public void setSubconceptDesc(String subconceptDesc) {
		this.subconceptDesc = subconceptDesc;
	}

	public String getSubconceptDesc2() {
		return subconceptDesc2;
	}

	public void setSubconceptDesc2(String subconceptDesc2) {
		this.subconceptDesc2 = subconceptDesc2;
	}

	public String getSubconceptGroup() {
		return subconceptGroup;
	}

	public void setSubconceptGroup(String subconceptGroup) {
		this.subconceptGroup = subconceptGroup;
	}

	public String getSubconceptLink() {
		return subconceptLink;
	}

	public void setSubconceptLink(String subconceptLink) {
		this.subconceptLink = subconceptLink;
	}

	public String getSubconceptType() {
		return subconceptType;
	}

	public void setSubconceptType(String subconceptType) {
		this.subconceptType = subconceptType;
	}

	public Integer getNumQuestions() {
		return numQuestions;
	}

	public void setNumQuestions(Integer numQuestions) {
		this.numQuestions = numQuestions;
	}

	public Integer getSubconceptMaxscore() {
		return subconceptMaxscore;
	}

	public void setSubconceptMaxscore(Integer subconceptMaxscore) {
		this.subconceptMaxscore = subconceptMaxscore;
	}

	public Integer getSubconceptDuration() {
		return subconceptDuration;
	}

	public void setSubconceptDuration(Integer subconceptDuration) {
		this.subconceptDuration = subconceptDuration;
	}

	public UUID getUuid() {
		return uuid;
	}

	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}

	public Concept getConcept() {
		return concept;
	}

	public void setConcept(Concept concept) {
		this.concept = concept;
	}

	public ContentMaster getContent() {
		return content;
	}

	public void setContent(ContentMaster content) {
		this.content = content;
	}

	@Override
	public String toString() {
		return "Subconcept [subconceptId=" + subconceptId + ", dependency=" + dependency + ", showTo=" + showTo
				+ ", subconceptDesc=" + subconceptDesc + ", subconceptDesc2=" + subconceptDesc2 + ", subconceptGroup="
				+ subconceptGroup + ", subconceptLink=" + subconceptLink + ", subconceptType=" + subconceptType
				+ ", numQuestions=" + numQuestions + ", subconceptMaxscore=" + subconceptMaxscore
				+ ", subconceptDuration=" + subconceptDuration + ", uuid=" + uuid + ", concept=" + concept
				+ ", content=" + content + "]";
	}

	// Method to ensure UUID and generate subconceptId before persisting
    @PrePersist
    private void ensureUuid() {
        if (this.uuid == null) {
            this.uuid = UUID.randomUUID();
        }
    }
    
    
}