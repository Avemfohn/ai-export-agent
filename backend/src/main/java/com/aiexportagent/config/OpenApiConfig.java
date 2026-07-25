package com.aiexportagent.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI aiExportAgentOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI Export Agent API")
                        .description("Sprint 1 scaffolding API — mock data only, no real AI/scraping/email integrations yet.")
                        .version("v0.1"));
    }
}
