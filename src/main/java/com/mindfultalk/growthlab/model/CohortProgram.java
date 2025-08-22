package com.mindfultalk.growthlab.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "cohort_program", uniqueConstraints = @UniqueConstraint(columnNames = {"cohort_id", "program_id"}))
public class CohortProgram {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cohort_program_id")
    private long cohortProgramId;

    @Column(name = "uuid", nullable = false, unique = true)
    private UUID uuid;

    @ManyToOne
    @JoinColumn(name = "cohort_id", nullable = false, unique = true)
    private Cohort cohort;

    @ManyToOne
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;
    
	public CohortProgram() {
	}
	
	public CohortProgram(long cohortProgramId, UUID uuid, Cohort cohort, Program program) {
		super();
		this.cohortProgramId = cohortProgramId;
		this.uuid = uuid;
		this.cohort = cohort;
		this.program = program;
	}

	public long getCohortProgramId() {
		return cohortProgramId;
	}

	public void setCohortProgramId(long cohortProgramId) {
		this.cohortProgramId = cohortProgramId;
	}

	public UUID getUuid() {
		return uuid;
	}

	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}

	public Cohort getCohort() {
		return cohort;
	}

	public void setCohort(Cohort cohort) {
		this.cohort = cohort;
	}

	public Program getProgram() {
		return program;
	}

	public void setProgram(Program program) {
		this.program = program;
	}

	@Override
	public String toString() {
		return "CohortProgram [cohortProgramId=" + cohortProgramId + ", uuid=" + uuid + ", cohort=" + cohort
				+ ", program=" + program + "]";
	}

	// Method to ensure UUID and generate cohort_program_id before persisting
    @PrePersist
    private void ensureUuid() {
        if (this.uuid == null) {
            this.uuid = UUID.randomUUID();
        }
    } 
}