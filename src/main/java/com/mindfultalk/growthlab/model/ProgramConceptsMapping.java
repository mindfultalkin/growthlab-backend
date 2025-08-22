package com.mindfultalk.growthlab.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "program_subconcepts",
    uniqueConstraints = @UniqueConstraint(columnNames = {"unit_id", "subconcept_id"}))
public class ProgramConceptsMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "programconcept_id", nullable = false, unique = true)
    private Long programConceptId;
    

    @Column(name = "program_concept_desc", length = 5000, nullable = false)
    private String programConceptDesc;
    
    @Column(name = "unit_position", nullable = false)
    private Integer position= 0;  // Default value of 0 if position is not provided

    @ManyToOne
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    @ManyToOne
    @JoinColumn(name = "stage_id", nullable = false)
    private Stage stage;

    @ManyToOne
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;

    @ManyToOne
    @JoinColumn(name = "subconcept_id", nullable = false)
    private Subconcept subconcept;

    @Column(name = "uuid", nullable = false, unique = true)
    private UUID uuid;

    public ProgramConceptsMapping() {
    }

    public ProgramConceptsMapping(Long programConceptId, String programConceptDesc, Integer position, Unit unit,
			Stage stage, Program program, Subconcept subconcept, UUID uuid) {
		super();
		this.programConceptId = programConceptId;
		this.programConceptDesc = programConceptDesc;
		this.position = position;
		this.unit = unit;
		this.stage = stage;
		this.program = program;
		this.subconcept = subconcept;
		this.uuid = uuid;
	}


	// Getters & Setters
    
    public Long getProgramConceptId() {
		return programConceptId;
	}

	public void setProgramConceptId(Long programConceptId) {
		this.programConceptId = programConceptId;
	}

	public String getProgramConceptDesc() {
		return programConceptDesc;
	}

	public void setProgramConceptDesc(String programConceptDesc) {
		this.programConceptDesc = programConceptDesc;
	}

	public Integer getPosition() {
		return position;
	}

	public void setPosition(Integer position) {
		this.position = position;
	}

	public Unit getUnit() {
		return unit;
	}

	public void setUnit(Unit unit) {
		this.unit = unit;
	}

	public Stage getStage() {
		return stage;
	}

	public void setStage(Stage stage) {
		this.stage = stage;
	}

	public Program getProgram() {
		return program;
	}

	public void setProgram(Program program) {
		this.program = program;
	}

	public Subconcept getSubconcept() {
		return subconcept;
	}

	public void setSubconcept(Subconcept subconcept) {
		this.subconcept = subconcept;
	}

	public UUID getUuid() {
		return uuid;
	}

	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}

	@Override
	public String toString() {
		return "ProgramConceptsMapping [programConceptId=" + programConceptId + ", programConceptDesc="
				+ programConceptDesc + ", position=" + position + ", unit=" + unit + ", stage=" + stage + ", program="
				+ program + ", subconcept=" + subconcept + ", uuid=" + uuid + "]";
	}

	// Method to ensure UUID and generate programConceptId before persisting
    @PrePersist
    private void ensureUuid() {
        if (this.uuid == null) {
            this.uuid = UUID.randomUUID();
        }
    }
}