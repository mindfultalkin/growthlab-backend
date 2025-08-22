package com.mindfultalk.growthlab.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI flowOfEnglishOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Flow of English API")
                        .description("API documentation for Flow of English backend - SuperAdmin Access Required")
                        .version("v1.0")
                        .contact(new Contact()
                                .name("Support")
                                .email("harikrishna.kuruva@mindfultalk.in"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://springdoc.org")))
                .externalDocs(new ExternalDocumentation()
                        .description("ChipperSage Docs")
                        .url("https://thechippersage.com"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("SuperAdmin authentication token")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    @Bean
    public GroupedOpenApi allEndpointsApi() {
        return GroupedOpenApi.builder()
                .group("all-endpoints")
                .pathsToMatch("/**")
                .build();
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public")
                .pathsToMatch("/**")
                .pathsToExclude("/swagger/**") // Exclude swagger auth endpoints from API docs
                .build();
    }

    @Bean
    public GroupedOpenApi superAdminApi() {
        return GroupedOpenApi.builder()
                .group("super-admin")
                .pathsToMatch("/api/v1/superadmin/**")
                .build();
    }

    @Bean
    public GroupedOpenApi userManagementApi() {
        return GroupedOpenApi.builder()
                .group("user-management")
                .pathsToMatch("/api/v1/users/**", "/api/v1/organizations/**")
                .build();
    }

    @Bean
    public GroupedOpenApi contentManagementApi() {
        return GroupedOpenApi.builder()
                .group("content-management")
                .pathsToMatch("/api/v1/content-masters/**", "/api/v1/concepts/**", 
                             "/api/v1/subconcepts/**", "/api/v1/stages/**", "/api/v1/units/**")
                .build();
    }
}
