package com.mindfultalk.growthlab.dto;

import java.time.LocalDateTime;

public class UserAttemptRequestDTO {
    private LocalDateTime userAttemptEndTimestamp;
    private boolean userAttemptFlag;
    private int userAttemptScore;
    private LocalDateTime userAttemptStartTimestamp;
    private String userId;
    private String unitId;
    private String programId;
    private String stageId;
    private String sessionId;
    private String subconceptId;
    private String cohortId;

    // Getters and Setters

    public LocalDateTime getUserAttemptEndTimestamp() {
        return userAttemptEndTimestamp;
    }

    public void setUserAttemptEndTimestamp(LocalDateTime userAttemptEndTimestamp) {
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

    public LocalDateTime getUserAttemptStartTimestamp() {
        return userAttemptStartTimestamp;
    }

    public void setUserAttemptStartTimestamp(LocalDateTime userAttemptStartTimestamp) {
        this.userAttemptStartTimestamp = userAttemptStartTimestamp;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUnitId() {
        return unitId;
    }

    public void setUnitId(String unitId) {
        this.unitId = unitId;
    }

    public String getProgramId() {
        return programId;
    }

    public void setProgramId(String programId) {
        this.programId = programId;
    }

    public String getStageId() {
        return stageId;
    }

    public void setStageId(String stageId) {
        this.stageId = stageId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSubconceptId() {
        return subconceptId;
    }

    public void setSubconceptId(String subconceptId) {
        this.subconceptId = subconceptId;
    }

    public String getCohortId() {
        return cohortId;
    }

    public void setCohortId(String cohortId) {
        this.cohortId = cohortId;
    }
}