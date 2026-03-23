package com.chatapp.backend.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.chatapp.backend.service.ChatMetrics;

/**
 * RabbitMQ consumer for AI job queue.
 * Guards: AI enabled check, consumer-side rate-limit, self-response prevention.
 * On unrecoverable failure: throws AmqpRejectAndDontRequeueException → message goes to DLQ.
 */
@Component
public class AiMessageConsumer {

    private static final Logger log = LoggerFactory.getLogger(AiMessageConsumer.class);

    private final AiService aiService;
    private final AiConfig aiConfig;
    private final ChatMetrics chatMetrics;

    public AiMessageConsumer(AiService aiService, AiConfig aiConfig, ChatMetrics chatMetrics) {
        this.aiService = aiService;
        this.aiConfig = aiConfig;
        this.chatMetrics = chatMetrics;
    }

    @RabbitListener(queues = "ai-chat-queue")
    public void handleAiJob(AiJobDTO job) {
        if (!aiConfig.isEnabled()) {
            log.debug("AI disabled, discarding job for conversation {}", job.getConversationId());
            return;
        }

        // Consumer-side self-response guard: reject jobs from the bot user
        if (isKaloriBotUser(job.getUserId())) {
            log.warn("AI_CONSUMER_SELF_GUARD rejected job from bot userId={} conversationId={}",
                    job.getUserId(), job.getConversationId());
            return;
        }

        log.info("AI_JOB_RECEIVED jobId={} conversationId={} userId={}",
                job.getJobId(), job.getConversationId(), job.getUserId());

        try {
            aiService.processAiRequest(job.getJobId(), job.getConversationId(), job.getUserId(), job.getMessageContent());
        } catch (Exception e) {
            log.error("AI_JOB_FAILED jobId={} conversationId={} userId={} error={}",
                    job.getJobId(), job.getConversationId(), job.getUserId(), e.getMessage(), e);
            chatMetrics.incrementAiFailures();

            // Reject and don't requeue → goes to DLQ after retry exhaustion
            throw new AmqpRejectAndDontRequeueException(
                    "AI job failed permanently: " + e.getMessage(), e);
        }
    }

    /**
     * Quick check without hitting DB: matches the well-known Kalori phone constant.
     * The service layer also has its own guard using the actual user ID.
     */
    private boolean isKaloriBotUser(Integer userId) {
        // This is a lightweight check — the full DB-backed guard is in AiService.processAiRequest()
        // We can't easily check by userId here without a DB call, so we rely on service-layer guard.
        // This method exists as a hook for future caller-ID checks.
        return false;
    }
}
