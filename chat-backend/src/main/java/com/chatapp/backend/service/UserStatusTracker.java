package com.chatapp.backend.service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.chatapp.backend.model.User;
import com.chatapp.backend.repository.UserRepository;

/**
 * Tracks user online/offline status using Redis.
 * Uses standardized key prefix: chat:presence:{phone}
 * Degrades gracefully on Redis failures.
 */
@Component
public class UserStatusTracker {

    private static final Logger log = LoggerFactory.getLogger(UserStatusTracker.class);
    private static final String STATUS_KEY_PREFIX = "chat:presence:";
    private static final long INACTIVITY_TIMEOUT_SECONDS = 5 * 60; // 5 minutes

    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;

    @Autowired
    public UserStatusTracker(SimpMessagingTemplate messagingTemplate, UserRepository userRepository,
                             StringRedisTemplate redisTemplate) {
        this.messagingTemplate = messagingTemplate;
        this.userRepository = userRepository;
        this.redisTemplate = redisTemplate;
    }

    public void updateActivity(String phone) {
        try {
            String key = STATUS_KEY_PREFIX + phone;
            redisTemplate.opsForValue().set(key, String.valueOf(System.currentTimeMillis()),
                    INACTIVITY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis unavailable for presence update, degrading gracefully: {}", e.getMessage());
        }
        broadcastStatus(phone, "active");
    }

    @Scheduled(fixedRate = 60000)
    public void checkStatuses() {
        try {
            Set<String> keys = redisTemplate.keys(STATUS_KEY_PREFIX + "*");
            if (keys != null) {
                for (String key : keys) {
                    String phone = key.substring(STATUS_KEY_PREFIX.length());
                    String timestamp = redisTemplate.opsForValue().get(key);
                    if (timestamp != null) {
                        long lastActive = Long.parseLong(timestamp);
                        String status = (System.currentTimeMillis() - lastActive < INACTIVITY_TIMEOUT_SECONDS * 1000)
                                ? "active" : "inactive";
                        broadcastStatus(phone, status);
                    }
                }
            }
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis unavailable for status check, skipping cycle: {}", e.getMessage());
        }
    }

    private void broadcastStatus(String phone, String status) {
        User user = userRepository.findByPhone(phone).orElse(null);
        if (user != null && !status.equals(user.getStatus())) {
            user.setStatus(status);
            userRepository.save(user);
        }
        messagingTemplate.convertAndSend("/topic/status-updates",
                Map.of("userId", phone, "status", status));
    }
}