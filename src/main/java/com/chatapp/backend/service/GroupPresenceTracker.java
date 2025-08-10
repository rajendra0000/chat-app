package com.chatapp.backend.service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class GroupPresenceTracker {
    
    private RedisTemplate<String, String> redisTemplate;
    private SimpMessagingTemplate messagingTemplate;
    private static final long EXPIRY_SECONDS = 300; // 5 minutes

    @Autowired
    public GroupPresenceTracker(RedisTemplate<String, String> redisTemplate, SimpMessagingTemplate messagingTemplate) {
        this.redisTemplate = redisTemplate;
        this.messagingTemplate = messagingTemplate;
    }

    public void markUserActive(String userId, Integer groupId) {
        redisTemplate.opsForSet().add("group:" + groupId + ":active_users", userId);
        redisTemplate.expire("group:" + groupId + ":active_users", EXPIRY_SECONDS, TimeUnit.SECONDS);
    }

    public long getActiveCount(Integer groupId) {
        return redisTemplate.opsForSet().size("group:" + groupId + ":active_users");
    }

    public void broadcastGroupStatus(Integer groupId) {
        long activeCount = getActiveCount(groupId);
        messagingTemplate.convertAndSend("/topic/status-" + groupId, 
            Map.of("groupId", groupId, "activeCount", activeCount));
    }
}