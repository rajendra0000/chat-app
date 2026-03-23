package com.chatapp.backend.ai;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.chatapp.backend.dto.MessageResponseDTO;
import com.chatapp.backend.model.ChatMember;
import com.chatapp.backend.model.ChatMessage;
import com.chatapp.backend.model.User;
import com.chatapp.backend.repository.ChatMemberRepository;
import com.chatapp.backend.repository.ChatMessageRepository;
import com.chatapp.backend.repository.UserRepository;
import com.chatapp.backend.service.ChatMetrics;

/**
 * Core AI service orchestrator.
 *
 * Flow:
 * 1. Idempotency check (skip duplicate jobId)
 * 2. Self-response guard (Kalori won't respond to itself)
 * 3. Rate limit check (20/user/min via Redis)
 * 4. Broadcast "Kalori is thinking..." indicator
 * 5. Fetch last 20 messages as context
 * 6. Call OpenAI via AiClient
 * 7. Save AI response as ChatMessage from Kalori
 * 8. Broadcast real reply, record metrics
 */
@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);
    private static final String RATE_LIMIT_PREFIX = "chat:ai_rate:";
    private static final String IDEMPOTENCY_PREFIX = "chat:ai_job:";
    private static final int IDEMPOTENCY_TTL_MINUTES = 10;

    private final AiClient aiClient;
    private final AiConfig aiConfig;
    private final UserRepository userRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final StringRedisTemplate redisTemplate;
    private final ChatMetrics chatMetrics;

    public AiService(AiClient aiClient, AiConfig aiConfig,
                     UserRepository userRepository,
                     ChatMemberRepository chatMemberRepository,
                     ChatMessageRepository chatMessageRepository,
                     SimpMessagingTemplate messagingTemplate,
                     StringRedisTemplate redisTemplate,
                     ChatMetrics chatMetrics) {
        this.aiClient = aiClient;
        this.aiConfig = aiConfig;
        this.userRepository = userRepository;
        this.chatMemberRepository = chatMemberRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.messagingTemplate = messagingTemplate;
        this.redisTemplate = redisTemplate;
        this.chatMetrics = chatMetrics;
    }

    /**
     * Processes an AI request with full guard chain:
     * idempotency → self-guard → rate-limit → thinking → context → LLM → save → broadcast → metrics
     */
    @Transactional
    public void processAiRequest(String jobId, Integer conversationId, Integer userId, String messageContent) {
        log.info("AI_PROCESS_START jobId={} conversationId={} userId={}", jobId, conversationId, userId);

        // 1. Idempotency check — skip already-processed jobs
        if (jobId != null && isDuplicateJob(jobId)) {
            log.warn("AI_DUPLICATE_JOB jobId={} — skipping", jobId);
            return;
        }

        // 2. Self-response guard — Kalori must not respond to itself
        User kalori = userRepository.findByPhone(AiUserInitializer.KALORI_PHONE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Kalori AI user not found"));

        if (kalori.getId().equals(userId)) {
            log.warn("AI_SELF_GUARD blocked self-response for conversationId={}", conversationId);
            return;
        }

        // 3. Rate limit check
        if (isRateLimited(userId)) {
            log.warn("AI_RATE_LIMITED userId={}", userId);
            broadcastSystemMessage(conversationId,
                    "⚠️ You've reached the AI rate limit (" + aiConfig.getRateLimitPerMinute() + " requests/minute). Please wait.");
            return;
        }

        // 4. Ensure Kalori membership
        ChatMember kaloriMember = ensureKaloriMembership(conversationId, kalori);

        // 5. Broadcast "thinking" indicator
        broadcastThinkingIndicator(conversationId, kalori.getId());

        // 6. Build context from last N messages
        List<Map<String, String>> context = buildContext(conversationId, kalori.getId());

        // 7. Call OpenAI — metrics tracked inside AiClient
        chatMetrics.incrementAiRequests();
        AiClient.AiResponse aiResponse = aiClient.chat(context, userId);

        // 8. Save AI message
        ChatMessage aiMessage = new ChatMessage();
        aiMessage.setChatMember(kaloriMember);
        aiMessage.setContent(aiResponse.getText());
        aiMessage.setMessageUuid(UUID.randomUUID().toString());
        aiMessage.setRead(false);
        aiMessage.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        ChatMessage saved = chatMessageRepository.save(aiMessage);

        // 9. Broadcast real reply
        MessageResponseDTO dto = new MessageResponseDTO();
        dto.setId(saved.getId());
        dto.setConversationId(conversationId);
        dto.setSenderId(kalori.getId());
        dto.setText(saved.getContent());
        dto.setTimestamp(saved.getCreatedAt().toString());
        dto.setRead(false);

        messagingTemplate.convertAndSend("/topic/messages-" + conversationId, dto);
        log.info("AI_REPLY_SENT jobId={} conversationId={} messageId={} tokens={}",
                jobId, conversationId, saved.getId(), aiResponse.getTotalTokens());
    }

    /**
     * Backward-compatible overload (no jobId).
     */
    public void processAiRequest(Integer conversationId, Integer userId, String messageContent) {
        processAiRequest(null, conversationId, userId, messageContent);
    }

    /**
     * Checks whether @kalori is mentioned in the message text.
     */
    public boolean containsKaloriMention(String text) {
        if (text == null) return false;
        return text.toLowerCase().contains("@" + aiConfig.getBotName().toLowerCase());
    }

    /**
     * Strips the @kalori mention from the message text for cleaner prompts.
     */
    public String stripMention(String text) {
        if (text == null) return "";
        return text.replaceAll("(?i)@" + aiConfig.getBotName(), "").trim();
    }

    // ─── Private helpers ─────────────────────────────────────

    private boolean isDuplicateJob(String jobId) {
        try {
            String key = IDEMPOTENCY_PREFIX + jobId;
            Boolean wasSet = redisTemplate.opsForValue().setIfAbsent(key, "1", IDEMPOTENCY_TTL_MINUTES, TimeUnit.MINUTES);
            return wasSet == null || !wasSet; // already existed → duplicate
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis unavailable for idempotency check, allowing job: {}", e.getMessage());
            return false; // allow on Redis failure
        }
    }

    private boolean isRateLimited(Integer userId) {
        try {
            String key = RATE_LIMIT_PREFIX + userId;
            String count = redisTemplate.opsForValue().get(key);
            int current = count != null ? Integer.parseInt(count) : 0;
            if (current >= aiConfig.getRateLimitPerMinute()) {
                return true;
            }
            redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, 1, TimeUnit.MINUTES);
            return false;
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis unavailable for AI rate limiting, allowing request: {}", e.getMessage());
            return false;
        }
    }

    private ChatMember ensureKaloriMembership(Integer conversationId, User kalori) {
        ChatMember existing = chatMemberRepository.findByChat_IdAndUser_Id(conversationId, kalori.getId());
        if (existing != null) {
            return existing;
        }

        var chat = chatMemberRepository.findByChat_Id(conversationId).stream()
                .findFirst()
                .map(ChatMember::getChat)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));

        ChatMember kaloriMember = new ChatMember();
        kaloriMember.setChat(chat);
        kaloriMember.setUser(kalori);
        kaloriMember.setRole("MEMBER");
        kaloriMember.setBlocked(false);
        kaloriMember = chatMemberRepository.save(kaloriMember);
        log.info("AI_JOINED conversationId={}", conversationId);
        return kaloriMember;
    }

    private List<Map<String, String>> buildContext(Integer conversationId, Integer kaloriUserId) {
        var pageable = PageRequest.of(0, aiConfig.getMaxContextMessages(), Sort.by("createdAt").descending());
        var page = chatMessageRepository.findByChatMember_Chat_IdOrderByCreatedAtDesc(conversationId, pageable);

        List<Map<String, String>> messages = new ArrayList<>();

        messages.add(Map.of(
                "role", "system",
                "content", "You are Kalori AI, a helpful assistant in a chat application. " +
                        "Be concise, friendly, and helpful. If asked about yourself, say you're Kalori AI. " +
                        "Respond in the same language the user writes in."
        ));

        List<ChatMessage> orderedMessages = new ArrayList<>(page.getContent());
        java.util.Collections.reverse(orderedMessages);

        for (ChatMessage msg : orderedMessages) {
            if (msg.isDeleted()) continue;
            String role = (msg.getSenderId() != null && msg.getSenderId().equals(kaloriUserId))
                    ? "assistant" : "user";
            String content = msg.getContent();
            if (content != null && !content.isBlank()) {
                messages.add(Map.of("role", role, "content", content));
            }
        }

        return messages;
    }

    private void broadcastThinkingIndicator(Integer conversationId, Integer kaloriUserId) {
        Map<String, Object> thinkingPayload = Map.of(
                "conversationId", conversationId,
                "senderId", kaloriUserId,
                "type", "AI_THINKING",
                "text", "Kalori is thinking..."
        );
        messagingTemplate.convertAndSend("/topic/messages-" + conversationId, thinkingPayload);
    }

    private void broadcastSystemMessage(Integer conversationId, String text) {
        Map<String, Object> payload = Map.of(
                "conversationId", conversationId,
                "type", "SYSTEM",
                "text", text
        );
        messagingTemplate.convertAndSend("/topic/messages-" + conversationId, payload);
    }
}
