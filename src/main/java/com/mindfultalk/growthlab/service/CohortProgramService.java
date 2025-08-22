package com.mindfultalk.growthlab.service;

import com.mindfultalk.growthlab.model.*;
import java.util.*;

public interface CohortProgramService {
    List<CohortProgram> getAllCohortPrograms();
    Optional<CohortProgram> getCohortProgram(Long cohortProgramId);
    CohortProgram createCohortProgram(CohortProgram cohortProgram);
    void deleteCohortProgram(Long cohortProgramId);
}