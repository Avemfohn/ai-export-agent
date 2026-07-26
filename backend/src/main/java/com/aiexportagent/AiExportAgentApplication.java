package com.aiexportagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * {@code @EnableScheduling} powers the automated outreach pipeline
 * ({@code OutreachQueueingScheduler}/{@code OutreachSendingScheduler}) — the
 * first {@code @Scheduled} usage in this codebase. Spring Boot's default
 * scheduler is a single-threaded {@code ThreadPoolTaskScheduler}, so these
 * jobs never run concurrently with each other or themselves.
 */
@EnableScheduling
@SpringBootApplication
public class AiExportAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiExportAgentApplication.class, args);
    }
}
