package com.chatapp.backend.ai;

import java.time.Duration;

import io.netty.resolver.DefaultAddressResolverGroup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import reactor.netty.http.client.HttpClient;

/**
 * AI feature configuration.
 * All AI functionality is gated behind ai.enabled=true.
 * WebClient enforces both connection timeout and response timeout.
 * Supports any OpenAI-compatible API (default: xAI Grok).
 */
@Configuration
public class AiConfig {

    @Value("${ai.enabled:true}")
    private boolean enabled;

    @Value("${ai.bot-name:kalori}")
    private String botName;

    @Value("${ai.base-url:https://api.groq.com/openai/v1}")
    private String baseUrl;

    @Value("${ai.model:llama-3.3-70b-versatile}")
    private String model;

    @Value("${ai.max-context-messages:20}")
    private int maxContextMessages;

    @Value("${ai.api-key:}")
    private String apiKey;

    @Value("${ai.rate-limit-per-minute:20}")
    private int rateLimitPerMinute;

    @Value("${ai.timeout-seconds:30}")
    private int timeoutSeconds;

    @Value("${ai.connect-timeout-ms:5000}")
    private int connectTimeoutMs;

    @Bean
    public WebClient aiWebClient() {
        HttpClient httpClient = HttpClient.create()
                .resolver(DefaultAddressResolverGroup.INSTANCE)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
                .responseTimeout(Duration.ofSeconds(timeoutSeconds));

        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
    }

    public boolean isEnabled() { return enabled; }
    public String getBotName() { return botName; }
    public String getModel() { return model; }
    public int getMaxContextMessages() { return maxContextMessages; }
    public String getBaseUrl() { return baseUrl; }
    // Note: no getter for apiKey — intentionally kept private to prevent accidental logging
    public int getRateLimitPerMinute() { return rateLimitPerMinute; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public int getConnectTimeoutMs() { return connectTimeoutMs; }
}
