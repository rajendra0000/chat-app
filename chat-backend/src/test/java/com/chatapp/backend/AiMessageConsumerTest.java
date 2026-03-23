package com.chatapp.backend;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

import com.chatapp.backend.ai.AiConfig;
import com.chatapp.backend.ai.AiJobDTO;
import com.chatapp.backend.ai.AiMessageConsumer;
import com.chatapp.backend.ai.AiService;
import com.chatapp.backend.service.ChatMetrics;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AiMessageConsumer: job delegation, disabled discard, error → DLQ, metrics.
 */
@ExtendWith(MockitoExtension.class)
class AiMessageConsumerTest {

    @Mock private AiService aiService;
    @Mock private ChatMetrics chatMetrics;

    private AiConfig aiConfig;
    private AiMessageConsumer consumer;

    @BeforeEach
    void setUp() throws Exception {
        aiConfig = new AiConfig();
        var enabledField = AiConfig.class.getDeclaredField("enabled");
        enabledField.setAccessible(true);
        enabledField.set(aiConfig, true);

        consumer = new AiMessageConsumer(aiService, aiConfig, chatMetrics);
    }

    @Test
    @DisplayName("Delegates job to AiService with jobId")
    void handleAiJob_delegatesToService() {
        AiJobDTO job = new AiJobDTO(100, 1, "explain Redis");

        consumer.handleAiJob(job);

        verify(aiService).processAiRequest(job.getJobId(), 100, 1, "explain Redis");
    }

    @Test
    @DisplayName("AI disabled: job is discarded")
    void handleAiJob_disabledDiscards() throws Exception {
        var enabledField = AiConfig.class.getDeclaredField("enabled");
        enabledField.setAccessible(true);
        enabledField.set(aiConfig, false);

        AiJobDTO job = new AiJobDTO(100, 1, "explain Redis");
        consumer.handleAiJob(job);

        verify(aiService, never()).processAiRequest(anyString(), anyInt(), anyInt(), anyString());
    }

    @Test
    @DisplayName("Exception causes AmqpRejectAndDontRequeueException for DLQ routing")
    void handleAiJob_errorGoesToDlq() {
        doThrow(new RuntimeException("API error"))
                .when(aiService).processAiRequest(anyString(), anyInt(), anyInt(), anyString());

        AiJobDTO job = new AiJobDTO(100, 1, "explain Redis");

        assertThrows(AmqpRejectAndDontRequeueException.class, () -> consumer.handleAiJob(job));
        verify(chatMetrics).incrementAiFailures();
    }
}
