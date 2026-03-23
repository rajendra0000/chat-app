package com.chatapp.backend.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * Publishes AI job requests to ai-chat-queue.
 */
@Service
public class AiMessageProducer {

    private static final Logger log = LoggerFactory.getLogger(AiMessageProducer.class);
    private static final String AI_QUEUE = "ai-chat-queue";

    private final RabbitTemplate rabbitTemplate;
    private final AiConfig aiConfig;

    public AiMessageProducer(RabbitTemplate rabbitTemplate, AiConfig aiConfig) {
        this.rabbitTemplate = rabbitTemplate;
        this.aiConfig = aiConfig;
    }

    /**
     * Publishes an AI job to the queue if AI is enabled.
     */
    public void publishAiJob(Integer conversationId, Integer userId, String messageContent) {
        if (!aiConfig.isEnabled()) {
            log.debug("AI disabled, skipping job for conversation {}", conversationId);
            return;
        }

        AiJobDTO job = new AiJobDTO(conversationId, userId, messageContent);
        rabbitTemplate.convertAndSend(AI_QUEUE, job);
        log.info("AI_JOB_PUBLISHED conversationId={} userId={}", conversationId, userId);
    }
}
