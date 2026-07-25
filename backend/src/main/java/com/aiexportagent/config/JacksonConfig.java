package com.aiexportagent.config;

import org.springframework.context.annotation.Configuration;

/**
 * Placeholder for ObjectMapper customization. Spring Boot's autoconfigured
 * ObjectMapper (see spring.jackson.* in application.yml) is sufficient for
 * Sprint 1 — this class exists so future customizations (e.g. custom
 * (de)serializers once JSONB columns move off plain String mapping) have an
 * obvious home.
 */
@Configuration
public class JacksonConfig {
}
