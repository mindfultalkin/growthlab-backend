package com.mindfultalk.growthlab.dto;

public class OrganizationDTO {

    private String organizationId;
    private String organizationName;
    private String organizationAdminName;
    private String organizationAdminEmail;
    private String organizationAdminPhone;

    // Getters and Setters

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getOrganizationAdminName() {
        return organizationAdminName;
    }

    public void setOrganizationAdminName(String organizationAdminName) {
        this.organizationAdminName = organizationAdminName;
    }

    public String getOrganizationAdminEmail() {
        return organizationAdminEmail;
    }

    public void setOrganizationAdminEmail(String organizationAdminEmail) {
        this.organizationAdminEmail = organizationAdminEmail;
    }

    public String getOrganizationAdminPhone() {
        return organizationAdminPhone;
    }

    public void setOrganizationAdminPhone(String organizationAdminPhone) {
        this.organizationAdminPhone = organizationAdminPhone;
    }
}