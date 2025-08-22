package com.mindfultalk.growthlab.util;

import java.util.Base64;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class GenerateKey {
    public static void main(String[] args) throws Exception {
        // KeyGenerator for HmacSHA256 (can use other algorithms too)
        KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");
        SecretKey secretKey = keyGen.generateKey();

        // Encode the key to Base64 format
        String encodedKey = Base64.getEncoder().encodeToString(secretKey.getEncoded());
        System.out.println("Base64 Encoded Secret Key: " + encodedKey);
    }
}