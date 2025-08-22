package com.mindfultalk.growthlab.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.io.*;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

@Configuration
public class CloudFrontConfig {

    @Value("${cloudfront.domain}")
    private String domain;

    @Value("${cloudfront.keyPairId}")
    private String keyPairId;

    @Value("${cloudfront.private-key-path:}")
    private String privateKeyPath;

    @Value("${cloudfront.private-key-content:}")
    private String privateKeyContent;

    @Value("${cloudfront.url.expiration.seconds}")
    private long expirationSeconds;

    private final ResourceLoader resourceLoader;

    public CloudFrontConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public String getDomain() {
        return domain;
    }

    public String getKeyPairId() {
        return keyPairId;
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    @Bean
    public PrivateKey cloudFrontPrivateKey() {
        try {
            String pemContent;

            if (privateKeyPath != null && !privateKeyPath.isBlank()) {
                // Load from file (classpath or absolute)
                Resource resource = resourceLoader.getResource(privateKeyPath);
                try (InputStream is = resource.getInputStream()) {
                    pemContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }
            } else if (privateKeyContent != null && !privateKeyContent.isBlank()) {
                // Load from raw secure string (e.g., from SSM or SecretsManager)
                pemContent = privateKeyContent.replace("\\n", "\n");
            } else {
                throw new IllegalArgumentException("CloudFront private key not provided via path or content.");
            }

            // Strip PEM markers
            pemContent = pemContent
                    .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                    .replace("-----END RSA PRIVATE KEY-----", "")
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");

            byte[] decoded = Base64.getDecoder().decode(pemContent);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
            return KeyFactory.getInstance("RSA").generatePrivate(spec);

        } catch (Exception e) {
            throw new RuntimeException("Failed to load CloudFront private key", e);
        }
    }
}