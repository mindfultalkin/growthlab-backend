package com.mindfultalk.growthlab.dto;

import com.mindfultalk.growthlab.model.User;

public class UsercreateDTO {
	private User user;
    private String cohortId;

    // Getters and Setters
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getCohortId() {
        return cohortId;
    }

    public void setCohortId(String cohortId) {
        this.cohortId = cohortId;
    }

}