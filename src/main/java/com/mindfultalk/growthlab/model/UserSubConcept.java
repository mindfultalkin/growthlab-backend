package com.mindfultalk.growthlab.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import com.fasterxml.jackson.annotation.JsonFormat;

@Entity 
@Table(name = "user_subconcept_completion",
	    uniqueConstraints = @UniqueConstraint(columnNames = {"program_id", "user_id", "unit_id", "subconcept_id"}))
public class UserSubConcept {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_subconcept_id", nullable = false, unique = true)
    private Long userSubconceptId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

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
    
    @Column(name = "completion_date")
    @CreationTimestamp
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime completionDate; // New field to track the date and time of completion
    
    // A transient field to represent completion status without persisting it
    @Transient
    private boolean completionStatus;

    @Column(name = "uuid", nullable = false, unique = true)
    private UUID uuid;

    // Default constructor
    public UserSubConcept() { 
    }

    public UserSubConcept(Long userSubconceptId, User user, Program program, Stage stage, Unit unit,
			Subconcept subconcept, OffsetDateTime completionDate, boolean completionStatus, UUID uuid) {
		super();
		this.userSubconceptId = userSubconceptId;
		this.user = user;
		this.program = program;
		this.stage = stage;
		this.unit = unit;
		this.subconcept = subconcept;
		this.completionDate = completionDate;
		this.completionStatus = completionStatus;
		this.uuid = uuid;
	}

	// Getters and Setters
    public Long getUserSubconceptId() {
		return userSubconceptId;
	}

	public void setUserSubconceptId(Long userSubconceptId) {
		this.userSubconceptId = userSubconceptId;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
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

	public OffsetDateTime getCompletionDate() {
		return completionDate;
	}

	public void setCompletionDate(OffsetDateTime completionDate) {
		this.completionDate = completionDate;
	}

	public UUID getUuid() {
		return uuid;
	}

	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}

	// Setters and Getters for completionStatus (non-persistent)
	/**
	 * Gets the completion status (transient field)
	 * @return true if completed, false otherwise
	 */
	public boolean isCompletionStatus() {
	    return completionStatus;
	}

	/**
	 * Sets the completion status (transient field)
	 * @param completionStatus the completion status to set
	 */
	public void setCompletionStatus(boolean completionStatus) {
	    this.completionStatus = completionStatus;
	}

	/**
	 * Alternative getter method name for completionStatus
	 * @return true if completed, false otherwise
	 */
	public boolean getCompletionStatus() {
	    return completionStatus;
	}
    
    @Override
	public String toString() {
		return "UserSubConcept [userSubconceptId=" + userSubconceptId + ", user=" + user + ", program=" + program
				+ ", stage=" + stage + ", unit=" + unit + ", subconcept=" + subconcept + ", completionDate="
				+ completionDate + ", completionStatus=" + completionStatus + ", uuid=" + uuid + "]";
	}

	@PrePersist
    private void ensureUuid() {
        if (this.uuid == null) {
            this.uuid = UUID.randomUUID();
        }
    }

 
}