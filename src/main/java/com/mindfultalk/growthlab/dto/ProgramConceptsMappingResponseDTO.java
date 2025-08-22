package com.mindfultalk.growthlab.dto;

import java.util.Map;

public class ProgramConceptsMappingResponseDTO {
    private String programId;
    private String programName;
    private String unitId;
    private String unitName;
    private String unitDesc;
    private String stageId;
    private String stageName;
    private String programConceptDesc;
    private Map<String, SubconceptResponseDTO> subConcepts; 
    private String unitCompletionStatus; 
    private int subconceptCount;

  
    // Getters and Setters
    public String getProgramId() {
        return programId;
    }

    public void setProgramId(String programId) {
        this.programId = programId;
    }

    public String getUnitId() {
        return unitId;
    }

    public void setUnitId(String unitId) {
        this.unitId = unitId;
    }

    public String getStageId() {
        return stageId;
    }

    public void setStageId(String stageId) {
        this.stageId = stageId;
    }

    
    public String getProgramName() {
		return programName;
	}

	public void setProgramName(String programName) {
		this.programName = programName;
	}

	public String getUnitName() {
		return unitName;
	}

	public void setUnitName(String unitName) {
		this.unitName = unitName;
	}

	
	public String getUnitDesc() {
		return unitDesc;
	}

	public void setUnitDesc(String unitDesc) {
		this.unitDesc = unitDesc;
	}

	public String getStageName() {
		return stageName;
	}

	public void setStageName(String stageName) {
		this.stageName = stageName;
	}

	public Map<String, SubconceptResponseDTO> getSubConcepts() {
        return subConcepts;
    }

    public void setSubConcepts(Map<String, SubconceptResponseDTO> subConcepts) {
        this.subConcepts = subConcepts;
    }

    public String getUnitCompletionStatus() {
        return unitCompletionStatus;
    }

    public void setUnitCompletionStatus(String unitCompletionStatus) {
        this.unitCompletionStatus = unitCompletionStatus;
    }

    public int getSubconceptCount() {
        return subconceptCount;
    }

    public void setSubconceptCount(int subconceptCount) {
        this.subconceptCount = subconceptCount;
    }

	public String getProgramConceptDesc() {
		return programConceptDesc;
	}

	public void setProgramConceptDesc(String programConceptDesc) {
		this.programConceptDesc = programConceptDesc;
	}
    
    
}