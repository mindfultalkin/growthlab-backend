package com.mindfultalk.growthlab.model;

import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonFormat;

@Entity
@Table(name = "user_attempts")
public class UserAttempts {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_attempt_id")
    private Long userAttemptId;

    @Column(name = "user_attempt_end_timestamp")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime userAttemptEndTimestamp;

    @Column(name = "user_attempt_flag", nullable = false)
    private boolean userAttemptFlag;

    @Column(name = "user_attempt_score", nullable = false)
    private int userAttemptScore;

    @Column(name = "user_attempt_start_timestamp", nullable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime userAttemptStartTimestamp;

    @ManyToOne
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    @ManyToOne
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;

    @ManyToOne
    @JoinColumn(name = "stage_id", nullable = false)
    private Stage stage;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "session_id", nullable = false)
    private UserSessionMapping session;

    @ManyToOne
    @JoinColumn(name = "subconcept_id", nullable = false)
    private Subconcept subconcept;

    @Column(name = "uuid", nullable = false, unique = true)
    private UUID uuid;

    // Default constructor
    public UserAttempts() {}

    public UserAttempts(Long userAttemptId, OffsetDateTime userAttemptEndTimestamp, boolean userAttemptFlag,
			int userAttemptScore, OffsetDateTime userAttemptStartTimestamp, Unit unit, Program program, Stage stage,
			User user, UserSessionMapping session, Subconcept subconcept, UUID uuid) {
		super();
		this.userAttemptId = userAttemptId;
		this.userAttemptEndTimestamp = userAttemptEndTimestamp;
		this.userAttemptFlag = userAttemptFlag;
		this.userAttemptScore = userAttemptScore;
		this.userAttemptStartTimestamp = userAttemptStartTimestamp;
		this.unit = unit;
		this.program = program;
		this.stage = stage;
		this.user = user;
		this.session = session;
		this.subconcept = subconcept;
		this.uuid = uuid;
	}

	// Getters and Setters

    public Long getUserAttemptId() {
        return userAttemptId;
    }

    public void setUserAttemptId(Long userAttemptId) {
        this.userAttemptId = userAttemptId;
    }

    public OffsetDateTime getUserAttemptEndTimestamp() {
        return userAttemptEndTimestamp;
    }

    public void setUserAttemptEndTimestamp(OffsetDateTime userAttemptEndTimestamp) {
        this.userAttemptEndTimestamp = userAttemptEndTimestamp;
    }

    public boolean isUserAttemptFlag() {
		return userAttemptFlag;
	}

	public void setUserAttemptFlag(boolean userAttemptFlag) {
		this.userAttemptFlag = userAttemptFlag;
	}

	public int getUserAttemptScore() {
        return userAttemptScore;
    }

    public void setUserAttemptScore(int userAttemptScore) {
        this.userAttemptScore = userAttemptScore;
    }

    public OffsetDateTime getUserAttemptStartTimestamp() {
        return userAttemptStartTimestamp;
    }

    public void setUserAttemptStartTimestamp(OffsetDateTime userAttemptStartTimestamp) {
        this.userAttemptStartTimestamp = userAttemptStartTimestamp;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Unit getUnit() {
        return unit;
    }

    public void setUnit(Unit unit) {
        this.unit = unit;
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

    public UserSessionMapping getSession() {
        return session;
    }

    public void setSession(UserSessionMapping session) {
        this.session = session;
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
		return "UserAttempts [userAttemptId=" + userAttemptId + ", userAttemptEndTimestamp=" + userAttemptEndTimestamp
				+ ", userAttemptFlag=" + userAttemptFlag + ", userAttemptScore=" + userAttemptScore
				+ ", userAttemptStartTimestamp=" + userAttemptStartTimestamp + ", unit=" + unit + ", program=" + program
				+ ", stage=" + stage + ", user=" + user + ", session=" + session + ", subconcept=" + subconcept
				+ ", uuid=" + uuid + "]";
	}

	// Ensure UUID and default timestamps to UTC
    @PrePersist
    @PreUpdate
    private void initializeOrAdjustTimestamps() {
        if (this.uuid == null) {
            this.uuid = UUID.randomUUID();
        }

        ZoneOffset utcZoneOffset = ZoneOffset.UTC;

        if (this.userAttemptStartTimestamp == null) {
            this.userAttemptStartTimestamp = OffsetDateTime.now(utcZoneOffset);
        }

        if (this.userAttemptEndTimestamp == null) {
            this.userAttemptEndTimestamp = OffsetDateTime.now(utcZoneOffset);
        }
    }
}