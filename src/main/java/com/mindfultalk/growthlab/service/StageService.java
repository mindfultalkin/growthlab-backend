package com.mindfultalk.growthlab.service;

import com.mindfultalk.growthlab.model.Stage;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.web.multipart.MultipartFile;

public interface StageService {
    List<Stage> getAllStages();
    Optional<Stage> getStageById(String stageId);
    Optional<Stage> findByStageId(String stageId);
    Stage createStage(Stage stage);
    Stage updateStage(String stageId, Stage stage);
    void deleteStage(String stageId);
    Map<String, Object> uploadStagesCSV(MultipartFile file);
}