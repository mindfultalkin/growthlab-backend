package com.mindfultalk.growthlab.repository;

import com.mindfultalk.growthlab.model.*;

import java.util.*;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StageRepository extends JpaRepository<Stage, String> {
	List<Stage> findByProgram_ProgramId(String programId);
	Optional<Stage> findByStageId(String stageId);
	
	@Query("SELECT s.stageId FROM Stage s WHERE s.program.programId = :programId")
	List<String> findStageIdsByProgramId(@Param("programId") String programId);
	// Custom query methods can be added here if necessary
}