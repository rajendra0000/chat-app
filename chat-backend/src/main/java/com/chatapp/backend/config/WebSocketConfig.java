package com.chatapp.backend.config;

import com.chatapp.backend.security.WebSocketPrincipalHandshakeHandler;
import com.chatapp.backend.security.WebSocketSecurityInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketSecurityInterceptor securityInterceptor;

    @Value("${cors.allowed-origins:http://localhost:3000}")
    private String allowedOriginsRaw;

    @Autowired
    public WebSocketConfig(WebSocketSecurityInterceptor securityInterceptor) {
        this.securityInterceptor = securityInterceptor;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Split comma-separated origins from env var and allow localhost fallback
        String[] origins = allowedOriginsRaw.split(",");
        registry.addEndpoint("/chat")
                .setHandshakeHandler(new WebSocketPrincipalHandshakeHandler())
                .addInterceptors(securityInterceptor)
                .setAllowedOriginPatterns(origins)
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[]{10000, 10000}) // Server sends heartbeat every 10s, expects client every 10s
                .setTaskScheduler(heartbeatScheduler());
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.setMessageSizeLimit(64 * 1024);       // 64KB max message size
        registration.setSendBufferSizeLimit(512 * 1024);    // 512KB send buffer
        registration.setSendTimeLimit(20 * 1000);           // 20s send timeout — disconnects stale clients
    }

    @org.springframework.context.annotation.Bean
    public org.springframework.scheduling.TaskScheduler heartbeatScheduler() {
        org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler scheduler =
                new org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("ws-heartbeat-");
        scheduler.initialize();
        return scheduler;
    }
}