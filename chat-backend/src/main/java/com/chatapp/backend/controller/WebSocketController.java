package com.chatapp.backend.controller;

import java.security.Principal;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;

import com.chatapp.backend.dto.MessageRequestDTO;
import com.chatapp.backend.dto.MessageResponseDTO;
import com.chatapp.backend.model.Chat;
import com.chatapp.backend.repository.ChatMemberRepository;
import com.chatapp.backend.repository.UserRepository;
import com.chatapp.backend.service.ConversationService;
import com.chatapp.backend.service.GroupPresenceTracker;
import com.chatapp.backend.service.UserStatusTracker;

@Controller
public class WebSocketController {

    private static final Logger log = LoggerFactory.getLogger(WebSocketController.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final ConversationService conversationService;
    private final GroupPresenceTracker groupPresenceTracker;
    private final UserStatusTracker userStatusTracker;
    private final RedisTemplate<String, String> redisTemplate;
    private final ChatMemberRepository chatMemberRepository;
    private final UserRepository userRepository;

    @Autowired
    public WebSocketController(SimpMessagingTemplate messagingTemplate, ConversationService conversationService,
            GroupPresenceTracker groupPresenceTracker, UserStatusTracker userStatusTracker,
            RedisTemplate<String, String> redisTemplate, ChatMemberRepository chatMemberRepository,
            UserRepository userRepository) {
        this.messagingTemplate = messagingTemplate;
        this.conversationService = conversationService;
        this.groupPresenceTracker = groupPresenceTracker;
        this.userStatusTracker = userStatusTracker;
        this.redisTemplate = redisTemplate;
        this.chatMemberRepository = chatMemberRepository;
        this.userRepository = userRepository;
    }

    @MessageMapping("/sendMessage")
    public void sendMessage(@Payload MessageRequestDTO request, Principal principal) {
        String phone = principal.getName();

        // Validate message length
        if (request.getText() != null && request.getText().length() > 4000) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Message must not exceed 4000 characters");
        }

        MessageResponseDTO response = conversationService.sendMessage(request, phone);
        userStatusTracker.updateActivity(phone);

        userRepository.findByPhone(phone).ifPresent(user -> {
            var member = chatMemberRepository.findByChat_IdAndUser_Id(request.getConversationId(), user.getId());
            if (member != null && member.getChat() != null
                    && "GROUP".equals(member.getChat().getChatType().name())) {
                groupPresenceTracker.markUserActive(phone, member.getChat().getId());
            }
        });
    }

    @MessageMapping("/markRead")
    public void markRead(@Payload Map<String, Integer> payload, Principal principal) {
        String phone = principal.getName();
        Integer chatId = payload.get("chatId");
        log.debug("markRead: phone={}, chatId={}", phone, chatId);
        conversationService.markMessagesAsRead(chatId, phone);
    }

    @MessageMapping("/ping")
    public void handlePing(@Payload Map<String, Object> payload, Principal principal) {
        String phone = principal.getName();
        log.debug("Ping received from: {}", phone);
        userStatusTracker.updateActivity(phone);
        conversationService.getUserGroupIds(phone).forEach(groupId ->
                groupPresenceTracker.markUserActive(phone, groupId));
    }

    @MessageMapping("/typing")
    public void handleTyping(@Payload Map<String, Object> payload, Principal principal) {
        String userId = principal.getName();
        Integer groupId = (Integer) payload.get("groupId");
        redisTemplate.opsForSet().add("group:" + groupId + ":typing_users", userId);
        redisTemplate.expire("group:" + groupId + ":typing_users", 3, TimeUnit.SECONDS);
        broadcastTypingStatus(groupId);
    }

    @MessageMapping("/typing-stop")
    public void handleTypingStop(@Payload Map<String, Object> payload, Principal principal) {
        String userId = principal.getName();
        Integer groupId = (Integer) payload.get("groupId");
        redisTemplate.opsForSet().remove("group:" + groupId + ":typing_users", userId);
        broadcastTypingStatus(groupId);
    }

    private void broadcastTypingStatus(Integer groupId) {
        Set<String> typingUsers = redisTemplate.opsForSet().members("group:" + groupId + ":typing_users");
        String typingMessage = (typingUsers == null || typingUsers.isEmpty()) ? "" :
                (typingUsers.size() == 1 ? typingUsers.iterator().next() + " is typing..." : "Multiple users are typing...");
        messagingTemplate.convertAndSend("/topic/typing-" + groupId,
                Map.of("groupId", groupId, "typingMessage", typingMessage));
    }

    @MessageExceptionHandler
    public void handleException(ResponseStatusException ex, Principal principal) {
        log.warn("WebSocket error for user {}: {}", principal != null ? principal.getName() : "unknown", ex.getReason());
        String errorMessage = ex.getReason();

        if (principal != null && errorMessage != null) {
            Map<String, String> errorPayload = Map.of("error", errorMessage);
            messagingTemplate.convertAndSendToUser(
                    principal.getName(),
                    "/queue/errors",
                    errorPayload
            );
        }
    }
}