package com.mindfultalk.growthlab.dto;

import java.util.Map;

public class ProgramDTO {
    
    private String programId;
    private String programName;
    private String programDesc;
    private int stagesCount; 
    private int unitCount;
    private Map<String, StageDTO> stages;
    private String programCompletionStatus;
    
    // Getters and Setters

    public String getProgramId() {
        return programId;
    }

    public void setProgramId(String programId) {
        this.programId = programId;
    }

    public String getProgramName() {
        return programName;
    }

    public void setProgramName(String programName) {
        this.programName = programName;
    }

    public String getProgramDesc() {
        return programDesc;
    }

    public void setProgramDesc(String programDesc) {
        this.programDesc = programDesc;
    }

    public int getStagesCount() {
        return stagesCount;
    }

    public void setStagesCount(int stagesCount) { 
        this.stagesCount = stagesCount;
    }

    public int getUnitCount() {
        return unitCount;
    }

    public void setUnitCount(int unitCount) {
        this.unitCount = unitCount;
    }

    public Map<String, StageDTO> getStages() {
        return stages;
    }

    public void setStages(Map<String, StageDTO> stages) {
        this.stages = stages;
    }
    
    public String getProgramCompletionStatus() {
        return programCompletionStatus;
    }

    public void setProgramCompletionStatus(String programCompletionStatus) {
        this.programCompletionStatus = programCompletionStatus;
    }

}