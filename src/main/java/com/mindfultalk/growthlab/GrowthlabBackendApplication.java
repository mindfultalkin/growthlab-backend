package com.mindfultalk.growthlab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(exclude = {RedisRepositoriesAutoConfiguration.class})
@EnableJpaRepositories(basePackages = "com.mindfultalk.growthlab.repository")
@EnableTransactionManagement
public class GrowthlabBackendApplication {
	public static void main(String[] args) {
		SpringApplication.run(GrowthlabBackendApplication.class, args);
	}
}
