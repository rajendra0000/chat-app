package com.chatapp.backend.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayRepairConfig {

    @Bean
    public FlywayMigrationStrategy repairFlywayStrategy() {
        return flyway -> {
            flyway.repair(); // 👈 Repairs the schema history (checksum mismatch fix)
            flyway.migrate();
        };
    }
}
