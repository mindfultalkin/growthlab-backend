package com.mindfultalk.growthlab.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
// import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "user_assignments",
uniqueConstraints = @UniqueConstraint(columnNames = {"program_id", "user_id", "unit_id", "subconcept_id"}))
public class UserAssignment {

	@Id
    @Column(name = "assignment_id", nullable = false, unique = true, length = 255)
    private String assignmentId;
	
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "cohort_id", nullable = false)
    private Cohort cohort;
    
    @ManyToOne
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;

    @ManyToOne
    @JoinColumn(name = "stage_id", nullable = false)
    private Stage stage;

    @ManyToOne
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    @ManyToOne
    @JoinColumn(name = "subconcept_id", nullable = false)
    private Subconcept subconcept;

    @ManyToOne
    @JoinColumn(name = "submitted_file_id")
    private MediaFile submittedFile;

    @ManyToOne
    @JoinColumn(name = "corrected_file_id")
    private MediaFile correctedFile;

    @Column(name = "submitted_date", nullable = false)
    @CreationTimestamp
    private OffsetDateTime submittedDate;

    @Column(name = "corrected_date", nullable = true)
    private OffsetDateTime correctedDate;

    @Column(name = "score", nullable = true)
    private Integer score;
    
    @Column(name = "Remarks", nullable = true, length = 5000)
    private String remarks;
    
    @Column(name = "uuid", nullable = false, unique = true)
    private UUID uuid;


    public UserAssignment() {
		}
	public UserAssignment(String assignmentId, User user, Cohort cohort, Program program, Stage stage, Unit unit,
			Subconcept subconcept, MediaFile submittedFile, MediaFile correctedFile, OffsetDateTime submittedDate,
			OffsetDateTime correctedDate, Integer score, String remarks, UUID uuid) {
		super();
		this.assignmentId = assignmentId;
		this.user = user;
		this.cohort = cohort;
		this.program = program;
		this.stage = stage;
		this.unit = unit;
		this.subconcept = subconcept;
		this.submittedFile = submittedFile;
		this.correctedFile = correctedFile;
		this.submittedDate = submittedDate;
		this.correctedDate = correctedDate;
		this.score = score;
		this.remarks = remarks;
		this.uuid = uuid;
	}

// Getters and Setters
public String getAssignmentId() {
		return assignmentId;
	}

	public void setAssignmentId(String assignmentId) {
		this.assignmentId = assignmentId;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
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

	public Stage getStage() {
		return stage;
	}

	public void setStage(Stage stage) {
		this.stage = stage;
	}

	public Unit getUnit() {
		return unit;
	}

	public void setUnit(Unit unit) {
		this.unit = unit;
	}

	public Subconcept getSubconcept() {
		return subconcept;
	}

	public void setSubconcept(Subconcept subconcept) {
		this.subconcept = subconcept;
	}

	public MediaFile getSubmittedFile() {
		return submittedFile;
	}

	public void setSubmittedFile(MediaFile submittedFile) {
		this.submittedFile = submittedFile;
	}

	public MediaFile getCorrectedFile() {
		return correctedFile;
	}

	public void setCorrectedFile(MediaFile correctedFile) {
		this.correctedFile = correctedFile;
	}

	public OffsetDateTime getSubmittedDate() {
		return submittedDate;
	}

	public void setSubmittedDate(OffsetDateTime submittedDate) {
		this.submittedDate = submittedDate;
	}

	public OffsetDateTime getCorrectedDate() {
		return correctedDate;
	}

	public void setCorrectedDate(OffsetDateTime correctedDate) {
		this.correctedDate = correctedDate;
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
	public UUID getUuid() {
		return uuid;
	}

	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}
	

	@Override
	public String toString() {
		return "UserAssignment [assignmentId=" + assignmentId + ", user=" + user + ", cohort=" + cohort + ", program="
				+ program + ", stage=" + stage + ", unit=" + unit + ", subconcept=" + subconcept + ", submittedFile="
				+ submittedFile + ", correctedFile=" + correctedFile + ", submittedDate=" + submittedDate
				+ ", correctedDate=" + correctedDate + ", score=" + score + ", remarks=" + remarks + ", uuid=" + uuid
				+ "]";
	}

	 @PrePersist
	    private void generateAssignmentId() {
	        if (this.uuid == null) {
	            this.uuid = UUID.randomUUID();
	        }
	        this.assignmentId = user.getUserId() + "-A" + UUID.randomUUID().toString().substring(0, 6);
	 }

}
