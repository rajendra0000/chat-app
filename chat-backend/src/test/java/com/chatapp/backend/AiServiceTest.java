package com.chatapp.backend;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.chatapp.backend.ai.AiClient;
import com.chatapp.backend.ai.AiConfig;
import com.chatapp.backend.ai.AiService;
import com.chatapp.backend.model.Chat;
import com.chatapp.backend.model.ChatMember;
import com.chatapp.backend.model.ChatMessage;
import com.chatapp.backend.model.User;
import com.chatapp.backend.repository.ChatMemberRepository;
import com.chatapp.backend.repository.ChatMessageRepository;
import com.chatapp.backend.repository.UserRepository;
import com.chatapp.backend.service.ChatMetrics;

/**
 * Tests for AiService: idempotency, self-guard, rate limiting, LLM timeout, full flow.
 */
@ExtendWith(MockitoExtension.class)
class AiServiceTest {

    @Mock private AiClient aiClient;
    @Mock private UserRepository userRepository;
    @Mock private ChatMemberRepository chatMemberRepository;
    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private ChatMetrics chatMetrics;

    private AiConfig aiConfig;
    private AiService aiService;

    private User kaloriUser;
    private User regularUser;
    private Chat chat;
    private ChatMember kaloriMember;

    @BeforeEach
    void setUp() throws Exception {
        aiConfig = new AiConfig();
        setField(aiConfig, "enabled", true);
        setField(aiConfig, "botName", "kalori");
        setField(aiConfig, "model", "grok-4-latest");
        setField(aiConfig, "maxContextMessages", 20);
        setField(aiConfig, "rateLimitPerMinute", 20);
        setField(aiConfig, "timeoutSeconds", 30);

        aiService = new AiService(aiClient, aiConfig, userRepository, chatMemberRepository,
                chatMessageRepository, messagingTemplate, redisTemplate, chatMetrics);

        kaloriUser = new User();
        kaloriUser.setId(999);
        kaloriUser.setPhone("AI_KALORI");
        kaloriUser.setName("Kalori AI");

        regularUser = new User();
        regularUser.setId(1);
        regularUser.setPhone("1234567890");
        regularUser.setName("Test User");

        chat = new Chat();
        chat.setId(100);

        kaloriMember = new ChatMember();
        kaloriMember.setChat(chat);
        kaloriMember.setUser(kaloriUser);
        kaloriMember.setRole("MEMBER");
    }

    @Test
    @DisplayName("processAiRequest: full flow saves AI message and broadcasts")
    void processAiRequest_fullFlow() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(true);
        when(valueOperations.get(startsWith("chat:ai_rate:"))).thenReturn("0");
        when(userRepository.findByPhone("AI_KALORI")).thenReturn(Optional.of(kaloriUser));
        when(chatMemberRepository.findByChat_IdAndUser_Id(100, 999)).thenReturn(kaloriMember);

        ChatMessage prevMsg = new ChatMessage();
        prevMsg.setContent("Hello");
        prevMsg.setDeleted(false);
        ChatMember userMember = new ChatMember();
        userMember.setUser(regularUser);
        prevMsg.setChatMember(userMember);
        Page<ChatMessage> page = new PageImpl<>(List.of(prevMsg));
        when(chatMessageRepository.findByChatMember_Chat_IdOrderByCreatedAtDesc(eq(100), any(Pageable.class)))
                .thenReturn(page);

        when(aiClient.chat(anyList(), eq(1)))
                .thenReturn(new AiClient.AiResponse("Redis is an in-memory data store.", 200, false));

        ChatMessage savedMsg = new ChatMessage();
        savedMsg.setId(500);
        savedMsg.setChatMember(kaloriMember);
        savedMsg.setContent("Redis is an in-memory data store.");
        savedMsg.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        savedMsg.setRead(false);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(savedMsg);

        aiService.processAiRequest("job-123", 100, 1, "explain Redis");

        verify(chatMessageRepository).save(any(ChatMessage.class));
        verify(messagingTemplate, atLeast(1)).convertAndSend(eq("/topic/messages-100"), any(Map.class));
        verify(chatMetrics).incrementAiRequests();
    }

    @Test
    @DisplayName("processAiRequest: duplicate jobId is skipped")
    void processAiRequest_idempotency() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        // setIfAbsent returns false → key already existed → duplicate
        when(valueOperations.setIfAbsent(eq("chat:ai_job:dup-123"), anyString(), anyLong(), any()))
                .thenReturn(false);

        aiService.processAiRequest("dup-123", 100, 1, "explain Redis");

        // Should NOT call AI or save anything
        verify(aiClient, never()).chat(anyList(), anyInt());
        verify(chatMessageRepository, never()).save(any());
    }

    @Test
    @DisplayName("processAiRequest: self-response guard blocks Kalori user")
    void processAiRequest_selfGuard() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(true);
        when(userRepository.findByPhone("AI_KALORI")).thenReturn(Optional.of(kaloriUser));

        // userId = Kalori's ID (999)
        aiService.processAiRequest("job-456", 100, 999, "I should not talk to myself");

        verify(aiClient, never()).chat(anyList(), anyInt());
        verify(chatMessageRepository, never()).save(any());
    }

    @Test
    @DisplayName("processAiRequest: rate limited user gets system message")
    void processAiRequest_rateLimited() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(true);
        when(valueOperations.get(startsWith("chat:ai_rate:"))).thenReturn("20");
        when(userRepository.findByPhone("AI_KALORI")).thenReturn(Optional.of(kaloriUser));

        aiService.processAiRequest("job-789", 100, 1, "explain Redis");

        verify(aiClient, never()).chat(anyList(), anyInt());
        verify(messagingTemplate).convertAndSend(eq("/topic/messages-100"), anyMap());
    }

    @Test
    @DisplayName("processAiRequest: LLM timeout returns graceful fallback")
    void processAiRequest_llmTimeout() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(true);
        when(valueOperations.get(startsWith("chat:ai_rate:"))).thenReturn("0");
        when(userRepository.findByPhone("AI_KALORI")).thenReturn(Optional.of(kaloriUser));
        when(chatMemberRepository.findByChat_IdAndUser_Id(100, 999)).thenReturn(kaloriMember);

        Page<ChatMessage> emptyPage = new PageImpl<>(List.of());
        when(chatMessageRepository.findByChatMember_Chat_IdOrderByCreatedAtDesc(eq(100), any(Pageable.class)))
                .thenReturn(emptyPage);

        // Simulate timeout
        when(aiClient.chat(anyList(), eq(1)))
                .thenReturn(new AiClient.AiResponse(
                        "I'm sorry, I couldn't process your request right now. Please try again.",
                        0, true));

        ChatMessage savedMsg = new ChatMessage();
        savedMsg.setId(501);
        savedMsg.setChatMember(kaloriMember);
        savedMsg.setContent("I'm sorry, I couldn't process your request right now. Please try again.");
        savedMsg.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        savedMsg.setRead(false);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(savedMsg);

        aiService.processAiRequest("job-timeout", 100, 1, "slow question");

        // Should still save the timeout fallback message
        verify(chatMessageRepository).save(any(ChatMessage.class));
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
