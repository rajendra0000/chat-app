package com.chatapp.backend.config;

import com.cloudinary.Cloudinary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class CloudinaryHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryHealthIndicator.class);

    @Autowired
    private Cloudinary cloudinary;

    @Override
    public Health health() {
        try {
            // Ping Cloudinary by requesting account usage info — lightweight
            cloudinary.api().usage(null);
            return Health.up()
                    .withDetail("storage", "cloudinary")
                    .withDetail("status", "connected")
                    .build();
        } catch (Exception e) {
            log.error("Cloudinary health check failed: {}", e.getMessage());
            return Health.down()
                    .withDetail("storage", "cloudinary")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
