package com.mindfultalk.growthlab.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;

import com.mindfultalk.growthlab.filter.SwaggerAuthFilter;

import java.util.Arrays;
import java.util.List;

@Configuration
public class SecurityConfig {

	@Autowired
    private SwaggerAuthFilter swaggerAuthFilter;
	
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, Environment env) throws Exception {
        boolean isDev = Arrays.asList(env.getActiveProfiles()).contains("dev");

        http
            .cors(cors -> cors
                .configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();
                    config.setAllowedOrigins(List.of(
                        "http://localhost:3000",
                        "http://localhost:8080",
                        "http://13.234.42.153:8080",
                        "http://13.234.42.153:3000",
                        "https://flowofenglish.thechippersage.com",
                        "https://flowofenglish.thechippersage.com/admin",
                        "https://flowofenglish-user.thechippersage.com",
                        "https://flowofenglish-admin.thechippersage.com",
                        "https://flowofenglish-old.thechippersage.com/",
                        "https://flowofenglish-backend.thechippersage.com",
                        "http://localhost:5173",
                        "http://10.12.131.110:5173/",
                        "http://127.0.0.1:5501",
                        "http://10.12.127.175:5173/",
                        "http://127.0.0.1:5501/index.html",
                        "https://chippersageblr.s3.ap-south-1.amazonaws.com", // s3 bucket
                        "https://d1pb9z6a4vrmi3.cloudfront.net/", // CloudFront URL chippersageblr
                        "https://d1kq2q5oc3pn5i.cloudfront.net/", // CloudFront Signed-in URL
                        "https://d27ig5p8mdmxxx.cloudfront.net/", // CloudFront Mindful Frontend
                        "https://paymentpage-nine.vercel.app/" // Razorpay payment page
                    ));
                    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                    config.setAllowedHeaders(List.of("*"));
                    config.setAllowCredentials(true);
                    return config;
                })
            );

     // Add Swagger authentication filter
        http.addFilterBefore(swaggerAuthFilter, UsernamePasswordAuthenticationFilter.class);

        http.authorizeHttpRequests(authorize -> {
            authorize
                // Allow Swagger authentication endpoints
                .requestMatchers("/actuator/health", "/api/v1/swagger/login", "/api/v1/swagger/authenticate",
                                "/api/v1/swagger/logout", "/api/v1/swagger/unauthorized")
                .permitAll()
                
                // Allow API docs and Swagger UI (these will be filtered by SwaggerAuthFilter)
                .requestMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**",
                                "/webjars/swagger-ui/**")
                .permitAll()
                
                // Allow static resources
                .requestMatchers("/css/**", "/js/**", "/images/**", "/", "/index.html", "/static/**")
                .permitAll();
            

            authorize
                .requestMatchers(
                    "/api/v1/superadmin/**",
                    "/api/v1/organizations/login",
                    "/api/v1/organizations/forgotorgpassword",
                    "/api/v1/organizations/resetorgpassword",
                    "/api/v1/organizations/create",
                    "/api/v1/users/create",
                    "/api/v1/users/login",
                    "/api/v1/users/**",
                    "/api/v1/content-masters/**",
                    "/api/v1/concepts/**",
                    "/api/v1/subconcepts/**",
                    "/api/v1/cohorts/**",
                    "/api/v1/programs/**",
                    "/api/v1/user-cohort-mappings/**",
                    "/api/v1/cohortprogram/**",
                    "/api/v1/organizations/**",
                    "/api/v1/cohorts/create",
                    "/api/v1/user-cohort-mappings/create",
                    "/api/v1/programs/create",
                    "/api/v1/stages/**",
                    "/api/v1/units/**",
                    "/api/v1/user-attempts/**",
                    "/api/v1/user-session-mappings/**",
                    "/api/v1/userSubConceptsCompletion/**",
                    "/api/v1/programconceptsmappings/**",
                    "/api/v1/assignment-with-attempt/**",
                    "/api/v1/subscriptions/**",
                    "/api/v1/reports/**",
                    "/api/v1/assignments/**",
                    "/api/v1/payments/**",
                    "/api/v1/webhook/**",
                    "/api/v1/webhooks/**",
                    "/api/v1/test/**",
                    "/api/v1/cache/**",
                    "/", "/index.html", "/static/**",
                    "/css/**", "/js/**", "/images/**"
                )
                .permitAll()
                .anyRequest()
                .authenticated();
        });

        http.csrf().disable();

        return http.build();
    }
}