package com.chatapp.backend.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.chatapp.backend.model.User;
import com.chatapp.backend.repository.UserRepository;

@Component
public class UserStatusTracker{
    private SimpMessagingTemplate messagingTemplate;
    private UserRepository userRepository;
    private Map<String, Long> lastActivity = new ConcurrentHashMap<>();
    private static final long INACTIVITY_TIMEOUT = 5 * 60 * 1000; // 5 minutes

    @Autowired
    public UserStatusTracker(SimpMessagingTemplate messagingTemplate, UserRepository userRepository) {
        this.messagingTemplate = messagingTemplate;
        this.userRepository = userRepository;
    }

    public void updateActivity(String phone) {
        lastActivity.put(phone, System.currentTimeMillis());
        broadcastStatus(phone);
    }

    @Scheduled(fixedRate = 60000) // Every minute
    public void checkStatuses() {
        lastActivity.forEach((userId, timestamp) -> broadcastStatus(userId));
    }

    private void broadcastStatus(String phone) {
        String status = (System.currentTimeMillis() - lastActivity.getOrDefault(phone, 0L) < INACTIVITY_TIMEOUT) ? "active" : "inactive";
        User user = userRepository.findByPhone(phone).orElse(null);
        if (user != null) {
            user.setStatus(status);
            userRepository.save(user); // Update DB (optional)
        }
        messagingTemplate.convertAndSend("/topic/status-updates", Map.of("userId", phone, "status", status));
    }
}