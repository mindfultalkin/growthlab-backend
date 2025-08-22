package com.mindfultalk.growthlab.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "user_cohort_mapping",
        uniqueConstraints = @UniqueConstraint(columnNames = {"cohort_id", "user_id"}))
public class UserCohortMapping  {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_cohort_id", nullable = false, unique = true)
    private int userCohortId;
	
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
	
	@Column(name = "leaderboard_score", nullable = false)
    private int leaderboardScore;
	
	
    @Column(name = "uuid", nullable = false, unique = true)
    private UUID uuid;

    @ManyToOne
    @JoinColumn(name = "cohort_id", nullable = false)
    private Cohort cohort;
    
    @Column(name = "status", length = 20, nullable = false)
    private String status = "ACTIVE"; // Default value: ACTIVE
    
    @Column(name = "deactivated_at")
    private OffsetDateTime deactivatedAt;
    
    @Column(name = "deactivated_reason", length = 500, nullable = true)
    private String deactivatedReason;
    
    @Column(name = "created_at", nullable = false)
    @CreationTimestamp
    private OffsetDateTime createdAt;
    
    // Default constructor
    public UserCohortMapping() {
        
    }

	public UserCohortMapping(int userCohortId, User user, int leaderboardScore, UUID uuid, Cohort cohort,
			String status, OffsetDateTime deactivatedAt, String deactivatedReason) {
		super();
		this.userCohortId = userCohortId;
		this.user = user;
		this.leaderboardScore = 0;
		this.uuid = uuid;
		this.cohort = cohort;
		this.status = status;
		this.deactivatedAt = deactivatedAt;
		this.deactivatedReason = deactivatedReason;
		this.createdAt = OffsetDateTime.now();
	}


	// Getters and Setters
    public int getUserCohortId() {
		return userCohortId;
	}

	public void setUserCohortId(int userCohortId) {
		this.userCohortId = userCohortId;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public int getLeaderboardScore() {
		return leaderboardScore;
	}

	public void setLeaderboardScore(int leaderboardScore) {
		this.leaderboardScore = leaderboardScore;
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

	public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

	public OffsetDateTime getDeactivatedAt() {
		return deactivatedAt;
	}

	public void setDeactivatedAt(OffsetDateTime deactivatedAt) {
		this.deactivatedAt = deactivatedAt;
	}

	public String getDeactivatedReason() {
		return deactivatedReason;
	}

	public void setDeactivatedReason(String deactivatedReason) {
		this.deactivatedReason = deactivatedReason;
	}
	
	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(OffsetDateTime createdAt) {
		this.createdAt = createdAt;
	}

   // To String
	@Override
	public String toString() {
		return "UserCohortMapping [userCohortId=" + userCohortId + ", user=" + user + ", leaderboardScore="
				+ leaderboardScore + ", uuid=" + uuid + ", cohort=" + cohort + ", status=" + status
				+ ", deactivatedAt=" + deactivatedAt + ", deactivatedReason=" + deactivatedReason 
				+ ", createdAt=" + createdAt + "]";
	}

	/**
    * Check if user is active
    * @return true if user is active, false otherwise
    */
   public boolean isActive() {
       return "ACTIVE".equals(this.status);
   }
   
   /**
    * Disable user with reason
    * @param reason Reason for disabling the user
    */
   public void disable(String reason) {
       this.status = "DISABLED";
       this.deactivatedAt = OffsetDateTime.now();
       this.deactivatedReason = reason;
   }
   
   public String getDeactivationDetails() {
       if (!isActive() && deactivatedAt != null) {
           DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
           return String.format("Program access deactivated on %s. Reason: %s", 
               deactivatedAt.format(formatter), 
               deactivatedReason != null ? deactivatedReason : "Not specified");
       }
       return "";
   }
	// Method to ensure UUID and generate leaderboardScore before persisting
    @PrePersist
    private void ensureUuid() {
        if (this.uuid == null) {
            this.uuid = UUID.randomUUID();
        }
        if (this.status == null) {
            this.status = "ACTIVE";
        }
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now();
        }
    }
    public String getOrganizationId() {
        return this.cohort.getOrganization().getOrganizationId();
    }
    
    public String getCohortId() {
        return this.cohort.getCohortId();
    }
}