package com.mindfultalk.growthlab.dto;

public class UserProgressDTO {
    private String userId;
    private String userName;
    private int totalStages;
    private int completedStages;
    private int totalUnits;
    private int completedUnits;
    private int totalSubconcepts;
    private int completedSubconcepts;
    private int leaderboardScore;
    
    // Getters and Setters
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
	public int getTotalStages() {
		return totalStages;
	}
	public void setTotalStages(int totalStages) {
		this.totalStages = totalStages;
	}
	public int getCompletedStages() {
		return completedStages;
	}
	public void setCompletedStages(int completedStages) {
		this.completedStages = completedStages;
	}
	public int getTotalUnits() {
		return totalUnits;
	}
	public void setTotalUnits(int totalUnits) {
		this.totalUnits = totalUnits;
	}
	public int getCompletedUnits() {
		return completedUnits;
	}
	public void setCompletedUnits(int completedUnits) {
		this.completedUnits = completedUnits;
	}
	public int getTotalSubconcepts() {
		return totalSubconcepts;
	}
	public void setTotalSubconcepts(int totalSubconcepts) {
		this.totalSubconcepts = totalSubconcepts;
	}
	public int getCompletedSubconcepts() {
		return completedSubconcepts;
	}
	public void setCompletedSubconcepts(int completedSubconcepts) {
		this.completedSubconcepts = completedSubconcepts;
	}
	public int getLeaderboardScore() {
		return leaderboardScore;
	}
	public void setLeaderboardScore(int leaderboardScore) {
		this.leaderboardScore = leaderboardScore;
	}
}