package com.mindfultalk.growthlab.service;

import com.mindfultalk.growthlab.dto.*;
import com.mindfultalk.growthlab.model.Unit;

import java.util.*;

import org.springframework.web.multipart.MultipartFile;

public interface UnitService {
   
	ProgramDTO getProgramWithStagesAndUnits(String userid, String programId); 
	Unit createUnit(Unit unit);
    Unit updateUnit(String unitId, Unit unit);
    Unit getUnitById(String unitId);
    void deleteUnit(String unitId);
    void deleteUnits(List<String> unitIds);
    List<UnitResponseDTO> getAllUnits(); 
    Map<String, Object> bulkUploadUnits(MultipartFile file);
	Optional<Unit> findByUnitId(String unitId);
}