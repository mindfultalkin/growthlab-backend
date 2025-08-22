package com.mindfultalk.growthlab.service;

import com.mindfultalk.growthlab.model.*;
import com.mindfultalk.growthlab.repository.*;
import com.opencsv.CSVReader;
import jakarta.transaction.Transactional;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import org.slf4j.*;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ContentMasterServiceImpl implements ContentMasterService {

	private static final Logger logger = LoggerFactory.getLogger(ContentMasterServiceImpl.class);
	
    @Autowired
    private ContentMasterRepository contentMasterRepository;

    @Override
    @Cacheable(value = "contents", key = "'all'")
    public List<ContentMaster> getAllContents() {
        logger.info("Cache miss for 'all' contents - fetching from DB");
        return contentMasterRepository.findAll();
    }

    @Override
    @Cacheable(value = "contents", key = "#id")
    public Optional<ContentMaster> getContentById(int id) {
        logger.info("Cache miss for content ID {} - fetching from DB", id);
        return contentMasterRepository.findById(id);
    }

    @Override
    @CacheEvict(value = "contents", allEntries = true)
    public ContentMaster createContent(ContentMaster content) {
        content.setUuid(UUID.randomUUID());
        logger.info("Creating new content with ID {}", content.getContentId());
        return contentMasterRepository.save(content);
    }

    @Override
    @Transactional
    @CacheEvict(value = "contents", allEntries = true)
    public Map<String, Object> uploadContents(MultipartFile file) throws Exception {
        List<String> insertedIds = new ArrayList<>();
        List<String> duplicateIdsInCsv = new ArrayList<>();
        List<String> duplicateIdsInDatabase = new ArrayList<>();
        List<String> errorIds = new ArrayList<>();
        Set<Integer> seenIds = new HashSet<>(); // To track duplicate IDs in the CSV file itself
        List<ContentMaster> contentMasters = new ArrayList<>();
        
        logger.info("Starting content upload from CSV");

        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            String[] line;
            
            while ((line = reader.readNext()) != null) {
                try {
                    int contentId = Integer.parseInt(line[0]);
                    
                    // Skip if contentId is duplicate in CSV file itself
                    if (seenIds.contains(contentId)) {
                        duplicateIdsInCsv.add(String.valueOf(contentId));
                        continue;
                    }
                    seenIds.add(contentId);
                    
                    // Check if contentId already exists in the database
                    if (contentMasterRepository.existsById(contentId)) {
                        duplicateIdsInDatabase.add(String.valueOf(contentId));
                        continue;
                    }

                    // Create ContentMaster object
                    ContentMaster content = new ContentMaster();
                    content.setContentId(contentId);
                    content.setContentName(line[1]);
                    content.setContentDesc(line[2]);
                    content.setContentOrigin(line[3]);
                    content.setContentTopic(line[4]);
                    content.setUuid(UUID.randomUUID());
                    contentMasters.add(content);

                } catch (Exception e) {
                    logger.error("Error parsing line: {}, exception: {}", Arrays.toString(line), e.getMessage());
                    errorIds.add(line[0]);
                }
            }
        }

        try {
            List<ContentMaster> savedContents = contentMasterRepository.saveAll(contentMasters);
            insertedIds = savedContents.stream().map(c -> String.valueOf(c.getContentId())).collect(Collectors.toList());
            logger.info("Inserted {} new content records", insertedIds.size());
        } catch (DataIntegrityViolationException e) {
            logger.error("Database constraint violation: {}", e.getMessage());
            errorIds.add("Database constraint violation occurred");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("insertedIds", insertedIds);
        response.put("duplicateIdsInCsv", duplicateIdsInCsv);
        response.put("duplicateIdsInDatabase", duplicateIdsInDatabase);
        response.put("errorIds", errorIds);
        response.put("successfulInsertCount", insertedIds.size());
        response.put("failedInsertCount", duplicateIdsInCsv.size() + duplicateIdsInDatabase.size() + errorIds.size());

        return response;
    }
    
    @Override
    @CachePut(value = "contents", key = "#id")
    @CacheEvict(value = "contents", key = "'all'")
    public ContentMaster updateContent(int id, ContentMaster updatedContent) {
        Optional<ContentMaster> existingContent = contentMasterRepository.findById(id);
        if (existingContent.isPresent()) {
            ContentMaster content = existingContent.get();
            content.setContentName(updatedContent.getContentName());
            content.setContentDesc(updatedContent.getContentDesc());
            content.setContentOrigin(updatedContent.getContentOrigin());
            content.setContentTopic(updatedContent.getContentTopic());
            return contentMasterRepository.save(content);
        }
        return null;
    }

    @Override
    @CacheEvict(value = "contents", allEntries = true)
    public void deleteContent(int id) {
        contentMasterRepository.deleteById(id);
    }
}