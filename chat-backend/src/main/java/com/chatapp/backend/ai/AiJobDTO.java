package com.chatapp.backend.ai;

import java.io.Serializable;
import java.util.UUID;

/**
 * Payload for AI job messages on RabbitMQ ai-chat-queue.
 * Includes jobId for idempotency tracking.
 */
public class AiJobDTO implements Serializable {

    private static final long serialVersionUID = 2L;

    private String jobId;
    private Integer conversationId;
    private Integer userId;
    private String messageContent;

    public AiJobDTO() {}

    public AiJobDTO(Integer conversationId, Integer userId, String messageContent) {
        this.jobId = UUID.randomUUID().toString();
        this.conversationId = conversationId;
        this.userId = userId;
        this.messageContent = messageContent;
    }

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }

    public Integer getConversationId() { return conversationId; }
    public void setConversationId(Integer conversationId) { this.conversationId = conversationId; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getMessageContent() { return messageContent; }
    public void setMessageContent(String messageContent) { this.messageContent = messageContent; }
}
