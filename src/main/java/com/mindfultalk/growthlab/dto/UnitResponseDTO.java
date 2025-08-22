package com.mindfultalk.growthlab.dto;

import java.util.Map;

public class UnitResponseDTO {
	 private String unitId;
	    private String unitName;
	    private String unitDesc;
	    private String completionStatus;
		public String getUnitId() {
			return unitId;
		}
		public void setUnitId(String unitId) {
			this.unitId = unitId;
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
		public String getCompletionStatus() {
			return completionStatus;
		}
		public void setCompletionStatus(String completionStatus) {
			this.completionStatus = completionStatus;
		}
		public void setSub_concepts(Map<String, SubconceptResponseDTO> subconcepts) {
		}
}