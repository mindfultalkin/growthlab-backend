package com.mindfultalk.growthlab.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "users")
public class User {

    @Id
    @Column(name = "user_id", length = 255)
    private String userId;

    @Column(name = "user_address", length = 1000, nullable = true)
    private String userAddress;

    @Column(name = "user_email", length = 50, nullable = true)
    private String userEmail;

    @Column(name = "user_name", length = 100)
    private String userName;

    @Column(name = "user_phone_number", length = 15, nullable = true)
    private String userPhoneNumber;

    @Column(name = "user_password", length = 255, nullable = false)
    private String userPassword;

    @Column(name = "user_type", length = 100, nullable = false)
    private String userType;

    @Column(name = "user_dateofbirth", nullable = true)
    private OffsetDateTime userDateOfBirth;
    
    @Column(name = "created_by", length = 255, nullable = true)
    private String createdBy; // admin/self signup
    
    @Column(name = "uuid", nullable = false, unique = true)
    private UUID uuid;
    
    @Column(name = "status", length = 20, nullable = false)
    private String status = "ACTIVE"; // Default value: ACTIVE
    
    @Column(name = "created_at", nullable = false)
    @CreationTimestamp
    private OffsetDateTime createdAt;
    
    @Column(name = "updated_at")
    @UpdateTimestamp
    private OffsetDateTime updatedAt;
    
    @Column(name = "deactivated_at")
    private OffsetDateTime deactivatedAt;
    
    @Column(name = "deactivated_reason", length = 500, nullable = true)
    private String deactivatedReason;

    @ManyToOne
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserAttempts> userAttempts = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<UserCohortMapping> userCohortMappings = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserSessionMapping> userSessions = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserSubConcept> userSubConcept = new ArrayList<>();
    
    public User() { }

	public User(String userId, String userAddress, String userEmail, String userName, String userPhoneNumber,
			String userPassword, String userType, OffsetDateTime userDateOfBirth, String createdBy, UUID uuid,
			String status, OffsetDateTime createdAt, OffsetDateTime updatedAt, OffsetDateTime deactivatedAt,
			String deactivatedReason, Organization organization) {
		super();
		this.userId = userId;
		this.userAddress = userAddress;
		this.userEmail = userEmail;
		this.userName = userName;
		this.userPhoneNumber = userPhoneNumber;
		this.userPassword = userPassword;
		this.userType = userType;
		this.userDateOfBirth = userDateOfBirth;
		this.createdBy = createdBy;
		this.uuid = uuid;
		this.status = status;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
		this.deactivatedAt = deactivatedAt;
		this.deactivatedReason = deactivatedReason;
		this.organization = organization;
	}

	// Getters and Setters
	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getUserAddress() {
		return userAddress;
	}

	public void setUserAddress(String userAddress) {
		this.userAddress = userAddress;
	}

	public String getUserEmail() {
		return userEmail;
	}

	public void setUserEmail(String userEmail) {
		this.userEmail = userEmail;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getUserPhoneNumber() {
		return userPhoneNumber;
	}

	public void setUserPhoneNumber(String userPhoneNumber) {
		this.userPhoneNumber = userPhoneNumber;
	}

	public String getUserPassword() {
		return userPassword;
	}

	public void setUserPassword(String userPassword) {
		this.userPassword = userPassword;
	}

	public String getUserType() {
		return userType;
	}

	public void setUserType(String userType) {
		this.userType = userType;
	}

	public UUID getUuid() {
		return uuid;
	}

	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(OffsetDateTime createdAt) {
		this.createdAt = createdAt;
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

	public Organization getOrganization() {
		return organization;
	}

	public void setOrganization(Organization organization) {
		this.organization = organization;
	}
	
	 public OffsetDateTime getUserDateOfBirth() {
		return userDateOfBirth;
	}

	public void setUserDateOfBirth(OffsetDateTime userDateOfBirth) {
		this.userDateOfBirth = userDateOfBirth;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(OffsetDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

	public List<UserCohortMapping> getUserCohortMappings() {
			return userCohortMappings;
		}


		public void setUserCohortMappings(List<UserCohortMapping> userCohortMappings) {
			this.userCohortMappings = userCohortMappings;
		}
	
	@Override
	public String toString() {
		return "User [userId=" + userId + ", userAddress=" + userAddress + ", userEmail=" + userEmail + ", userName="
				+ userName + ", userPhoneNumber=" + userPhoneNumber + ", userPassword=" + userPassword + ", userType="
				+ userType + ", userDateOfBirth=" + userDateOfBirth + ", createdBy=" + createdBy + ", uuid=" + uuid
				+ ", status=" + status + ", createdAt=" + createdAt + ", updatedAt=" + updatedAt + ", deactivatedAt="
				+ deactivatedAt + ", deactivatedReason=" + deactivatedReason + ", organization=" + organization + "]";
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
        this.deactivatedAt = java.time.OffsetDateTime.now();
        this.deactivatedReason = reason;
    }
    public String getDeactivationDetails() {
        if (!isActive() && deactivatedAt != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
            return String.format("Deactivated on %s. Reason: %s", 
                deactivatedAt.format(formatter), 
                deactivatedReason != null ? deactivatedReason : "Not specified");
        }
        return "";
    }
    // Method to ensure UUID and set default values before persisting
    @PrePersist
    private void prePersist() {
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
}
