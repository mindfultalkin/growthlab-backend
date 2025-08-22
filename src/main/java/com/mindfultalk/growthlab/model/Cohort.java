package com.mindfultalk.growthlab.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import com.fasterxml.jackson.annotation.JsonFormat;

@Entity
@Table(name = "cohorts")
public class Cohort {

    @Id
    @Column(name = "cohort_id", length = 255, nullable = false)
    private String cohortId;

    @Column(name = "cohort_name", length = 255, nullable = false)
    private String cohortName;

    @Column(name = "cohort_end_date", nullable = true)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime cohortEndDate;

    @Column(name = "cohort_start_date", nullable = false, updatable = false)
    @CreationTimestamp
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime cohortStartDate;

    @Column(name = "show_leaderboard", nullable = false)
    private boolean showLeaderboard = true; // Default is true

    @Column(name = "delayed_stage_unlock", nullable = false)
    private boolean delayedStageUnlock = false; // Default is false

    @Column(name = "delay_in_days", nullable = false)
    private Integer delayInDays = 0; // Default is 0 (no delay)
    
    @Column(name = "uuid", nullable = false, unique = true, updatable = false)
    private UUID uuid;

    @ManyToOne
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    // Default constructor
    public Cohort() {
        
    }

    // Parameterized constructor
	public Cohort(String cohortId, String cohortName, OffsetDateTime cohortEndDate, OffsetDateTime cohortStartDate,
			boolean showLeaderboard, boolean delayedStageUnlock, Integer delayInDays, UUID uuid,
			Organization organization) {
		super();
		this.cohortId = cohortId;
		this.cohortName = cohortName;
		this.cohortEndDate = cohortEndDate;
		this.cohortStartDate = cohortStartDate;
		this.showLeaderboard = showLeaderboard;
		this.delayedStageUnlock = delayedStageUnlock;
		this.delayInDays = delayInDays;
		this.uuid = uuid;
		this.organization = organization;
	}
// Getters and Setters 
	public String getCohortId() {
		return cohortId;
	}

	public void setCohortId(String cohortId) {
		this.cohortId = cohortId;
	}

	public String getCohortName() {
		return cohortName;
	}

	public void setCohortName(String cohortName) {
		this.cohortName = cohortName;
	}

	public OffsetDateTime getCohortEndDate() {
		return cohortEndDate;
	}

	public void setCohortEndDate(OffsetDateTime cohortEndDate) {
		this.cohortEndDate = cohortEndDate;
	}

	public OffsetDateTime getCohortStartDate() {
		return cohortStartDate;
	}

	public void setCohortStartDate(OffsetDateTime cohortStartDate) {
		this.cohortStartDate = cohortStartDate;
	}

	public boolean isShowLeaderboard() {
		return showLeaderboard;
	}

	public void setShowLeaderboard(boolean showLeaderboard) {
		this.showLeaderboard = showLeaderboard;
	}

	public boolean isDelayedStageUnlock() {
		return delayedStageUnlock;
	}

	public void setDelayedStageUnlock(boolean delayedStageUnlock) {
		this.delayedStageUnlock = delayedStageUnlock;
	}

	public Integer getDelayInDays() {
		return delayInDays;
	}

	public void setDelayInDays(Integer delayInDays) {
		this.delayInDays = delayInDays;
	}

	public UUID getUuid() {
		return uuid;
	}

	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}

	public Organization getOrganization() {
		return organization;
	}

	public void setOrganization(Organization organization) {
		this.organization = organization;
	}

	@Override
	public String toString() {
		return "Cohort [cohortId=" + cohortId + ", cohortName=" + cohortName + ", cohortEndDate=" + cohortEndDate
				+ ", cohortStartDate=" + cohortStartDate + ", showLeaderboard=" + showLeaderboard
				+ ", delayedStageUnlock=" + delayedStageUnlock + ", delayInDays=" + delayInDays + ", uuid=" + uuid
				+ ", organization=" + organization + "]";
	}

	// Method to ensure UUID generate before persisting
	@PrePersist
    private void ensureUuid() {
        if (this.uuid == null) {
            this.uuid = UUID.randomUUID();
        }
    }
          
}