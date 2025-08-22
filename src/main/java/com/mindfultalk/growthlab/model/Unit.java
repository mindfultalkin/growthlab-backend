package com.mindfultalk.growthlab.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "unit")
public class Unit {

    @Id
    @Column(name = "unit_id", length = 500)
    private String unitId;
    
    @ManyToOne
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;

    @ManyToOne
    @JoinColumn(name = "stage_id", nullable = false)
    private Stage stage;

    @Column(name = "unit_desc", length = 1000)
    private String unitDesc;

    @Column(name = "unit_name", length = 255, nullable = false)
    private String unitName;

    @Column(name = "uuid", nullable = false, unique = true)
    private UUID uuid;

    public Unit() {
		
	}


	public Unit(String unitId, Program program, Stage stage, String unitDesc, String unitName, UUID uuid) {
		super();
		this.unitId = unitId;
		this.program = program;
		this.stage = stage;
		this.unitDesc = unitDesc;
		this.unitName = unitName;
		this.uuid = uuid;
	}


	// Getters and Setters

	public String getUnitId() {
		return unitId;
	}

	public void setUnitId(String unitId) {
		this.unitId = unitId;
	}

	public Program getProgram() {
		return program;
	}

	public void setProgram(Program program) {
		this.program = program;
	}

	public Stage getStage() {
		return stage;
	}

	public void setStage(Stage stage) {
		this.stage = stage;
	}

	public String getUnitDesc() {
		return unitDesc;
	}

	public void setUnitDesc(String unitDesc) {
		this.unitDesc = unitDesc;
	}

	public String getUnitName() {
		return unitName;
	}

	public void setUnitName(String unitName) {
		this.unitName = unitName;
	}

	public UUID getUuid() {
		return uuid;
	}

	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}

	@Override
	public String toString() {
		return "Unit [unitId=" + unitId + ", program=" + program + ", stage=" + stage + ", unitDesc=" + unitDesc
				+ ", unitName=" + unitName + ", uuid=" + uuid + "]";
	}

	// Method to ensure UUID and generate unitId before persisting
    @PrePersist
    private void ensureUuid() {
        if (this.uuid == null) {
            this.uuid = UUID.randomUUID();
        }
    }
    
}