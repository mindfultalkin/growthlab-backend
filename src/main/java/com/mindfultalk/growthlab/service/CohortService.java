package com.mindfultalk.growthlab.service;

import com.mindfultalk.growthlab.dto.*;
import com.mindfultalk.growthlab.model.*;
import java.util.*;

public interface CohortService {

    List<Cohort> getAllCohorts();

    Optional<Cohort> getCohortById(String cohortId);

    List<Cohort> getCohortsByOrganizationId(String organizationId);

    Cohort createCohort(Cohort cohort);

    Cohort updateCohort(String cohortId, Cohort cohort);

    void deleteCohort(String cohortId);

	CohortDTO convertToDTO(Cohort cohort);
}