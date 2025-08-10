package com.chatapp.backend.controller;

import java.security.Principal;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

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
    
    private SimpMessagingTemplate messagingTemplate;
    
    private ConversationService conversationService;

    private GroupPresenceTracker groupPresenceTracker;
    
    private UserStatusTracker userStatusTracker;
    
    private RedisTemplate<String, String> redisTemplate;

    private ChatMemberRepository chatMemberRepository;

    private UserRepository userRepository;

    

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
        MessageResponseDTO response = conversationService.sendMessage(request, phone);
        userStatusTracker.updateActivity(phone);
        Chat chat = chatMemberRepository.findByChat_IdAndUser_Id(request.getConversationId(), 
            userRepository.findByPhone(phone).get().getId()).getChat();
        if ("group".equals(chat.getChatType())) {
            groupPresenceTracker.markUserActive(phone, chat.getId());
        }
    }

    @MessageMapping("/markRead")
    public void markRead(@Payload Map<String, Integer> payload, Principal principal) {
        String phone = principal.getName();
        Integer chatId = payload.get("chatId");
        System.out.println("value of phone : " + phone);        
        System.out.println("value of principal : " + principal);
        conversationService.markMessagesAsRead(chatId, phone);
    }

    @MessageMapping("/ping")
    public void handlePing(@Payload Map<String, Object> payload, Principal principal) {
        System.out.println("here is the principle:" + principal);
        String phone = principal.getName();
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
        String typingMessage = typingUsers.isEmpty() ? "" : 
            (typingUsers.size() == 1 ? typingUsers.iterator().next() + " is typing..." : "Multiple users are typing...");
        messagingTemplate.convertAndSend("/topic/typing-" + groupId, 
            Map.of("groupId", groupId, "typingMessage", typingMessage));
    }
}