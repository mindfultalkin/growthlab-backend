package com.mindfultalk.growthlab.repository;

import com.mindfultalk.growthlab.model.*;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProgramConceptsMappingRepository extends JpaRepository<ProgramConceptsMapping, Long> {
	// Custom query method to find mappings by unitId
    List<ProgramConceptsMapping> findByUnit_UnitId(String unitId);
    List<ProgramConceptsMapping> findByProgram_ProgramId(String programId);
	
	// Custom query methods can be added here
    @Query("SELECT pcm FROM ProgramConceptsMapping pcm WHERE pcm.unit.unitId = :unitId ORDER BY pcm.position ASC")
    List<ProgramConceptsMapping> findByUnit_UnitIdOrdered(@Param("unitId") String unitId);

    @Query("SELECT DISTINCT pcm.unit.unitId FROM ProgramConceptsMapping pcm WHERE pcm.program.programId = :programId")
    List<String> findDistinctUnitIdsByProgramId(@Param("programId") String programId);
    
    @Query("SELECT DISTINCT pcm.subconcept.subconceptId FROM ProgramConceptsMapping pcm WHERE pcm.program.programId = :programId")
    List<String> findDistinctSubconceptIdsByProgramId(@Param("programId") String programId);

    @Query("SELECT pcm.subconcept.subconceptId FROM ProgramConceptsMapping pcm WHERE pcm.unit.unitId = :unitId")
    List<String> findSubconceptIdsByUnitId(@Param("unitId") String unitId);
}