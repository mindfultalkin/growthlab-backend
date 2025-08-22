package com.mindfultalk.growthlab.config;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class RazorpayConfig {
	
  @Value("${razorpay.keyId}")
  private String keyId;
  
  @Value("${razorpay.keySecret}")
  private String keySecret;
  
  @Bean
  public RazorpayClient razorpayClient() throws RazorpayException {
    return new RazorpayClient(keyId, keySecret);
  }
    public String getSecret() {
        return keySecret;
      }
    
    
}