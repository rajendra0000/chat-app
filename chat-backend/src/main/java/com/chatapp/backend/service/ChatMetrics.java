package com.chatapp.backend.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Custom Actuator metrics for observability.
 * Tracks: message sends, WebSocket connects/disconnects, OTP requests, AI usage.
 * Exposed via /actuator/metrics/chat.* endpoints.
 */
@Component
public class ChatMetrics {

    private final Counter messageSendCounter;
    private final Counter websocketConnectCounter;
    private final Counter websocketDisconnectCounter;
    private final Counter otpRequestCounter;
    private final Counter otpVerifyCounter;
    private final Counter aiRequestCounter;
    private final Counter aiFailureCounter;
    private final Counter aiTokenCounter;

    public ChatMetrics(MeterRegistry registry) {
        this.messageSendCounter = Counter.builder("chat.messages.sent")
                .description("Total messages sent")
                .register(registry);

        this.websocketConnectCounter = Counter.builder("chat.websocket.connections")
                .description("Total WebSocket connections established")
                .register(registry);

        this.websocketDisconnectCounter = Counter.builder("chat.websocket.disconnections")
                .description("Total WebSocket disconnections")
                .register(registry);

        this.otpRequestCounter = Counter.builder("chat.otp.requests")
                .description("Total OTP send requests")
                .register(registry);

        this.otpVerifyCounter = Counter.builder("chat.otp.verifications")
                .description("Total OTP verification attempts")
                .register(registry);

        this.aiRequestCounter = Counter.builder("chat.ai.requests")
                .description("Total AI requests processed")
                .register(registry);

        this.aiFailureCounter = Counter.builder("chat.ai.failures")
                .description("Total AI request failures")
                .register(registry);

        this.aiTokenCounter = Counter.builder("chat.ai.tokens")
                .description("Total AI tokens consumed")
                .register(registry);
    }

    public void incrementMessagesSent() { messageSendCounter.increment(); }
    public void incrementWebSocketConnections() { websocketConnectCounter.increment(); }
    public void incrementWebSocketDisconnections() { websocketDisconnectCounter.increment(); }
    public void incrementOtpRequests() { otpRequestCounter.increment(); }
    public void incrementOtpVerifications() { otpVerifyCounter.increment(); }
    public void incrementAiRequests() { aiRequestCounter.increment(); }
    public void incrementAiFailures() { aiFailureCounter.increment(); }
    public void recordAiTokens(double count) { aiTokenCounter.increment(count); }
}
