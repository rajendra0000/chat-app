package com.chatapp.backend.ai;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.chatapp.backend.service.ChatMetrics;

import reactor.core.publisher.Mono;

/**
 * HTTP client wrapping an OpenAI-compatible Chat Completions API (e.g. Groq, OpenAI).
 * Enforces connection + response timeouts via Netty and Reactor.
 * Redacts prompt content in logs. Records token usage metrics.
 */
@Component
public class AiClient {

    private static final Logger log = LoggerFactory.getLogger(AiClient.class);
    private static final int MAX_LOG_LENGTH = 80;

    private final WebClient aiWebClient;
    private final AiConfig aiConfig;
    private final ChatMetrics chatMetrics;

    public AiClient(WebClient aiWebClient, AiConfig aiConfig, ChatMetrics chatMetrics) {
        this.aiWebClient = aiWebClient;
        this.aiConfig = aiConfig;
        this.chatMetrics = chatMetrics;
    }

    /**
     * Result wrapper to expose token usage to callers.
     */
    public static class AiResponse {
        private final String text;
        private final int totalTokens;
        private final boolean timeout;

        public AiResponse(String text, int totalTokens, boolean timeout) {
            this.text = text;
            this.totalTokens = totalTokens;
            this.timeout = timeout;
        }

        public String getText() { return text; }
        public int getTotalTokens() { return totalTokens; }
        public boolean isTimeout() { return timeout; }
    }

    /**
     * Calls the LLM Chat Completions API with conversation context.
     * Enforces both connection timeout (Netty) and response timeout (Reactor).
     * Logs redacted prompts (first 80 chars only) and token usage.
     *
     * @param messages list of {role, content} maps representing the conversation
     * @param userId   requesting user ID for logging
     * @return AiResponse with text and token usage
     */
    public AiResponse chat(List<Map<String, String>> messages, Integer userId) {
        // Log redacted prompt (first message after system prompt, truncated)
        if (messages.size() > 1) {
            String lastUserMsg = messages.get(messages.size() - 1).getOrDefault("content", "");
            log.info("AI_PROMPT user={} prompt=\"{}\"", userId, redact(lastUserMsg));
        }

        Map<String, Object> requestBody = Map.of(
                "model", aiConfig.getModel(),
                "messages", messages,
                "max_tokens", 1000,
                "temperature", 0.7
        );

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = aiWebClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(java.time.Duration.ofSeconds(aiConfig.getTimeoutSeconds()))
                    .onErrorResume(TimeoutException.class, e -> {
                        log.error("AI_TIMEOUT user={} timeout={}s", userId, aiConfig.getTimeoutSeconds());
                        chatMetrics.incrementAiFailures();
                        return Mono.empty();
                    })
                    .onErrorResume(WebClientResponseException.class, e -> {
                        log.error("AI_API_ERROR user={} status={}", userId, e.getStatusCode());
                        chatMetrics.incrementAiFailures();
                        return Mono.empty();
                    })
                    .onErrorResume(io.netty.channel.ConnectTimeoutException.class, e -> {
                        log.error("AI_CONNECT_TIMEOUT user={} connectTimeout={}ms", userId, aiConfig.getConnectTimeoutMs());
                        chatMetrics.incrementAiFailures();
                        return Mono.empty();
                    })
                    .block();

            if (response == null) {
                return new AiResponse(
                        "I'm sorry, I couldn't process your request right now. Please try again.", 0, true);
            }

            // Extract response text
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices == null || choices.isEmpty()) {
                log.warn("AI_EMPTY_RESPONSE user={}", userId);
                chatMetrics.incrementAiFailures();
                return new AiResponse(
                        "I'm sorry, I didn't get a response. Please try again.", 0, false);
            }

            @SuppressWarnings("unchecked")
            Map<String, String> messageContent = (Map<String, String>) choices.get(0).get("message");
            String reply = messageContent != null ? messageContent.get("content") : "";

            // Parse token usage
            int totalTokens = 0;
            @SuppressWarnings("unchecked")
            Map<String, Object> usage = (Map<String, Object>) response.get("usage");
            if (usage != null) {
                totalTokens = ((Number) usage.getOrDefault("total_tokens", 0)).intValue();
                log.info("AI_REQUEST user={} model={} prompt_tokens={} completion_tokens={} total_tokens={}",
                        userId, aiConfig.getModel(),
                        usage.get("prompt_tokens"),
                        usage.get("completion_tokens"),
                        totalTokens);
                chatMetrics.recordAiTokens(totalTokens);
            } else {
                log.info("AI_REQUEST user={} model={} tokens=unknown", userId, aiConfig.getModel());
            }

            return new AiResponse(
                    reply != null ? reply.trim() : "No response generated.", totalTokens, false);

        } catch (Exception e) {
            log.error("AI_CALL_FAILED user={} error={}", userId, e.getMessage());
            chatMetrics.incrementAiFailures();
            return new AiResponse(
                    "I'm sorry, something went wrong. Please try again later.", 0, false);
        }
    }

    /**
     * Redacts prompt for logging: truncates to MAX_LOG_LENGTH chars, replaces rest with "...".
     */
    private String redact(String text) {
        if (text == null) return "[null]";
        if (text.length() <= MAX_LOG_LENGTH) return text;
        return text.substring(0, MAX_LOG_LENGTH) + "...[redacted]";
    }
}
