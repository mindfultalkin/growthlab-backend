package com.mindfultalk.growthlab.dto;

import java.time.OffsetDateTime;
import java.util.List;


public class UserDetailsWithCohortsAndProgramsDTO {
	private String userId;
    private String userName;
    private String userEmail;
    private String userPhoneNumber;
    private String userAddress;
    private String userType;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime deactivatedAt;
    private String deactivatedReason;
    private OrganizationDTO organization;
    private List<CohortProgramDTO> allCohortsWithPrograms;
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public String getUserEmail() {
		return userEmail;
	}
	public void setUserEmail(String userEmail) {
		this.userEmail = userEmail;
	}
	public String getUserPhoneNumber() {
		return userPhoneNumber;
	}
	public void setUserPhoneNumber(String userPhoneNumber) {
		this.userPhoneNumber = userPhoneNumber;
	}
	public String getUserAddress() {
		return userAddress;
	}
	public void setUserAddress(String userAddress) {
		this.userAddress = userAddress;
	}
	public String getUserType() {
		return userType;
	}
	public void setUserType(String userType) {
		this.userType = userType;
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
	public OrganizationDTO getOrganization() {
		return organization;
	}
	public void setOrganization(OrganizationDTO organization) {
		this.organization = organization;
	}
	public List<CohortProgramDTO> getAllCohortsWithPrograms() {
		return allCohortsWithPrograms;
	}
	public void setAllCohortsWithPrograms(List<CohortProgramDTO> allCohortsWithPrograms) {
		this.allCohortsWithPrograms = allCohortsWithPrograms;
	}

}