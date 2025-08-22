package com.mindfultalk.growthlab.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Base64;
import java.util.Date;

@Service
public class TokenService {

    @Value("${jwt.secret.key}") // Read from application.properties or env variable
    private String secretKey;

    public String generateToken(String userId) {
        // Convert the secret key into a byte array and then into a Key object
        byte[] secretKeyBytes = Base64.getDecoder().decode(secretKey);
        Key key = new SecretKeySpec(secretKeyBytes, SignatureAlgorithm.HS256.getJcaName());

        return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10)) // 10 hours expiry
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}