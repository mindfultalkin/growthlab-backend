package com.mindfultalk.growthlab.repository;

import com.mindfultalk.growthlab.model.*;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProgramRepository extends JpaRepository<Program, String> {
	Optional<Program> findByProgramId(String programId);
    // You can define custom queries here if needed
}
