package com.mindfultalk.growthlab.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "stage")
public class Stage {

    @Id
    @Column(name = "stage_id", nullable = false, unique = true)
    private String stageId;

    @Column(name = "stage_name", nullable = false)
    private String stageName;

    @Column(name = "stage_desc")
    private String stageDesc;

    @ManyToOne
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;
    
    @Column(name = "uuid", nullable = false, unique = true)
    private UUID uuid;

    // Default constructor
    public Stage() {
    }

    public Stage(String stageId, String stageName, String stageDesc, Program program, UUID uuid) {
        this.stageId = stageId;
        this.stageName = stageName;
        this.stageDesc = stageDesc;
        this.program = program;
        this.uuid = uuid;
    }

    // Getters and Setters
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

    public Program getProgram() {
        return program;
    }

    public void setProgram(Program program) {
        this.program = program;
    }

    public UUID getUuid() {
		return uuid;
	}

	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}

	@Override
	public String toString() {
		return "Stage [stageId=" + stageId + ", stageName=" + stageName + ", stageDesc=" + stageDesc + ", program="
				+ program + ", uuid=" + uuid + "]";
	}
	// Method to ensure UUID and generate stageId before persisting
    @PrePersist
    private void ensureUuid() {
        if (this.uuid == null) {
            this.uuid = UUID.randomUUID();
        }
    }

	public String getStageCompletionStatus() {
		// TODO Auto-generated method stub
		return null;
	}
	
}