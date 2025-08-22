package com.mindfultalk.growthlab.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "concepts")
public class Concept {

    @Id
    @Column(name = "concept_id", length = 255)
    private String conceptId;
    
    @Column(name = "concept_name", length = 5000, nullable = true)
    private String conceptName;


	@Column(name = "concept_desc", length = 5000, nullable = true)
    private String conceptDesc;

    @Column(name = "concept_skill_1", length = 5000, nullable = true)
    private String conceptSkill1;

    @Column(name = "concept_skill_2", length = 500, nullable = true)
    private String conceptSkill2;

    @Column(name = "uuid", nullable = false, unique = true)
    private UUID uuid;

    @ManyToOne
    @JoinColumn(name = "content_id", nullable = false)
    private ContentMaster content;

	public Concept() {
	}
	
	public Concept(String conceptId, String conceptName, String conceptDesc, String conceptSkill1, String conceptSkill2, UUID uuid,
			ContentMaster content) {
		super();
		this.conceptId = conceptId;
		this.conceptName = conceptName;
		this.conceptDesc = conceptDesc;
		this.conceptSkill1 = conceptSkill1;
		this.conceptSkill2 = conceptSkill2;
		this.uuid = uuid;
		this.content = content;
	}


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

	public UUID getUuid() {
		return uuid;
	}

	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}

	public ContentMaster getContent() {
		return content;
	}

	public void setContent(ContentMaster content) {
		this.content = content;
	}

	@Override
	public String toString() {
		return "Concept [conceptId=" + conceptId + ", conceptName=" + conceptName + ", conceptDesc=" + conceptDesc
				+ ", conceptSkill1=" + conceptSkill1 + ", conceptSkill2=" + conceptSkill2 + ", uuid=" + uuid
				+ ", content=" + content + "]";
	}

	// Method to ensure UUID and generate conceptId before persisting
    @PrePersist
    private void ensureUuid() {
        if (this.uuid == null) {
            this.uuid = UUID.randomUUID();
        }

    }
}