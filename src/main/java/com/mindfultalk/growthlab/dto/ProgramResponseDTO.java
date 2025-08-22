package com.mindfultalk.growthlab.dto;

import java.util.List;

public class ProgramResponseDTO {
	    private String programId;
	    private String programName;
	    private String programDesc;
	    private List<CohortDTO> cohorts;

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

		public List<CohortDTO> getCohorts() {
	        return cohorts;
	    }

	    public void setCohorts(List<CohortDTO> cohorts) {
	        this.cohorts = cohorts;
	    }
	}