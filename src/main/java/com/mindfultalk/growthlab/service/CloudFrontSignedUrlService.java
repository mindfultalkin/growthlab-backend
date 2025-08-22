package com.mindfultalk.growthlab.service;

import com.mindfultalk.growthlab.config.CloudFrontConfig;
import com.amazonaws.services.cloudfront.CloudFrontUrlSigner;
import com.amazonaws.services.cloudfront.util.SignerUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.PrivateKey;
import java.util.Date;

@Service
public class CloudFrontSignedUrlService {

    private final CloudFrontConfig config;
    private final PrivateKey privateKey;

    @Autowired
    public CloudFrontSignedUrlService(CloudFrontConfig config, PrivateKey privateKey) {
        this.config = config;
        this.privateKey = privateKey;
    }

    public String generateSignedUrl(String relativePath) {
    	
        try {
            Date expiration = new Date(System.currentTimeMillis() + config.getExpirationSeconds() * 1000);
            
            String fullUrl = config.getDomain() + relativePath;

            return CloudFrontUrlSigner.getSignedURLWithCannedPolicy(
                    fullUrl,
                    config.getKeyPairId(),
                    privateKey,
                    expiration
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate CloudFront signed URL", e);
        }
    }
}