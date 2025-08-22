package com.mindfultalk.growthlab.repository;

import com.mindfultalk.growthlab.model.*;
import java.util.*;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UnitRepository extends JpaRepository<Unit, String> {
	List<Unit> findByStage_StageId(String stageId);
	Optional<Unit> findByUnitId(String unitId);
	// You can add custom queries here if necessary
}