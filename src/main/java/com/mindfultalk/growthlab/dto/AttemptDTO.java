package com.mindfultalk.growthlab.dto;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public class AttemptDTO {
    private Long attemptId;
    private OffsetDateTime startTimestamp; 
    private OffsetDateTime endTimestamp;
    private int score;
    private boolean isSuccessful;

    public Long getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(Long attemptId) {
        this.attemptId = attemptId;
    }

    public OffsetDateTime getStartTimestamp() {
        return startTimestamp;
    }

    public void setStartTimestamp(OffsetDateTime startTimestamp) {
        this.startTimestamp = startTimestamp;
    }

    public OffsetDateTime getEndTimestamp() {
        return endTimestamp;
    }

    public void setEndTimestamp(OffsetDateTime endTimestamp) {
        this.endTimestamp = endTimestamp;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public boolean isSuccessful() {
        return isSuccessful;
    }

    public void setSuccessful(boolean isSuccessful) {
        this.isSuccessful = isSuccessful;
    }

    // Helper method to convert LocalDateTime to OffsetDateTime if needed
    public void setStartTimestampFromLocal(LocalDateTime localStartTimestamp) {
        this.startTimestamp = localStartTimestamp.atOffset(ZoneOffset.UTC);
    }

    public void setEndTimestampFromLocal(LocalDateTime localEndTimestamp) {
        this.endTimestamp = localEndTimestamp.atOffset(ZoneOffset.UTC);
    }
}