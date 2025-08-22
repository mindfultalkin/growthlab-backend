package com.mindfultalk.growthlab.model;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;

@Entity
@Table(name = "subscriptions")
public class ProgramSubscription {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long subscriptionId;

    @ManyToOne
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;

    @ManyToOne
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;


    @Column(name = "start_date", nullable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    @CreationTimestamp
    private OffsetDateTime startDate;

    @Column(name = "end_date", nullable = true)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime endDate;
    
   @Column(name = "transaction_id", nullable = false)
   private String transactionId;
   
   @Column(name = "transaction_type", nullable = false)
   private String transactionType;
   
   @Column(name = "transaction_date", nullable = true)
   @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
   private OffsetDateTime transactionDate;
   
   @Column(name = "amount_paid")
   private double amountPaid; 

    @Column(name = "max_cohorts", nullable = true)
    private Integer maxCohorts;
    
    @Column(name = "uuid", unique = true, nullable = false, updatable = false)
    private UUID uuid;
 
    @Column(name = "payment_status", nullable = false)
    private String status;
    
    @Column(name = "user_email", length = 50, nullable = false)
    private String userEmail;
    
    @Column(name = "user_address", length = 1000, nullable = true)
    private String userAddress;
    
    @Column(name = "user_name", length = 100, nullable = false)
    private String userName;

    @Column(name = "user_phone_number", length = 15, nullable = true)
    private String userPhoneNumber;
    
    @Column(name = "user_created")
    private boolean userCreated = false; // default to false
    
    @Column(name = "user_id", length = 255, nullable = true)
    private String userId;
// Default constructor
	public ProgramSubscription() {
	}


	// Parameterized constructor
	public ProgramSubscription(Long subscriptionId, Program program, Organization organization,
			OffsetDateTime startDate, OffsetDateTime endDate, String transactionId, String transactionType,
			OffsetDateTime transactionDate, double amountPaid, Integer maxCohorts, UUID uuid, String status,
			String userEmail, String userAddress, String userName, String userPhoneNumber, boolean userCreated,
			String userId) {
		super();
		this.subscriptionId = subscriptionId;
		this.program = program;
		this.organization = organization;
		this.startDate = startDate;
		this.endDate = endDate;
		this.transactionId = transactionId;
		this.transactionType = transactionType;
		this.transactionDate = transactionDate;
		this.amountPaid = amountPaid;
		this.maxCohorts = maxCohorts;
		this.uuid = uuid;
		this.status = status;
		this.userEmail = userEmail;
		this.userAddress = userAddress;
		this.userName = userName;
		this.userPhoneNumber = userPhoneNumber;
		this.userCreated = userCreated;
		this.userId = userId;
	}



	// Getters and Setters
	public Long getSubscriptionId() {
		return subscriptionId;
	}

	public void setSubscriptionId(Long subscriptionId) {
		this.subscriptionId = subscriptionId;
	}


	public Program getProgram() {
		return program;
	}


	public void setProgram(Program program) {
		this.program = program;
	}


	public Organization getOrganization() {
		return organization;
	}


	public void setOrganization(Organization organization) {
		this.organization = organization;
	}


	public OffsetDateTime getStartDate() {
		return startDate;
	}


	public void setStartDate(OffsetDateTime startDate) {
		this.startDate = startDate;
	}


	public OffsetDateTime getEndDate() {
		return endDate;
	}


	public void setEndDate(OffsetDateTime endDate) {
		this.endDate = endDate;
	}


	public String getTransactionId() {
		return transactionId;
	}


	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}


	public String getTransactionType() {
		return transactionType;
	}


	public void setTransactionType(String transactionType) {
		this.transactionType = transactionType;
	}


	public OffsetDateTime getTransactionDate() {
		return transactionDate;
	}


	public void setTransactionDate(OffsetDateTime transactionDate) {
		this.transactionDate = transactionDate;
	}


	public double getAmountPaid() {
		return amountPaid;
	}


	public void setAmountPaid(double amountPaid) {
		this.amountPaid = amountPaid;
	}


	public Integer getMaxCohorts() {
		return maxCohorts;
	}


	public void setMaxCohorts(Integer maxCohorts) {
		this.maxCohorts = maxCohorts;
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


	public String getUserEmail() {
		return userEmail;
	}


	public void setUserEmail(String userEmail) {
		this.userEmail = userEmail;
	}


	public String getUserAddress() {
		return userAddress;
	}


	public void setUserAddress(String userAddress) {
		this.userAddress = userAddress;
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
	public boolean isUserCreated() {
		return userCreated;
	}

	public void setUserCreated(boolean userCreated) {
		this.userCreated = userCreated;
	}
	public String getUserId() {
	    return userId;
	}

	public void setUserId(String userId) {
	    this.userId = userId;
	}

	@Override
	public String toString() {
		return "ProgramSubscription [subscriptionId=" + subscriptionId + ", program=" + program + ", organization="
				+ organization + ", startDate=" + startDate + ", endDate=" + endDate + ", transactionId="
				+ transactionId + ", transactionType=" + transactionType + ", transactionDate=" + transactionDate
				+ ", amountPaid=" + amountPaid + ", maxCohorts=" + maxCohorts + ", uuid=" + uuid + ", status=" + status
				+ ", userEmail=" + userEmail + ", userAddress=" + userAddress + ", userName=" + userName
				+ ", userPhoneNumber=" + userPhoneNumber + ", userCreated=" + userCreated + ", userId=" + userId + "]";
	}

	// Method to ensure UUID and generate userId before persisting
    @PrePersist
    private void ensureUuid() {
        if (this.uuid == null) {
            this.uuid = UUID.randomUUID();
        }
    }
}
