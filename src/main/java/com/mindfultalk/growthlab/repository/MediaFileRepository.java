package com.mindfultalk.growthlab.repository;

import com.mindfultalk.growthlab.model.*;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MediaFileRepository extends JpaRepository<MediaFile, String> {
}