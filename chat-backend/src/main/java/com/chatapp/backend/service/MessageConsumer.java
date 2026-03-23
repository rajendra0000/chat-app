package com.chatapp.backend.service;

import com.chatapp.backend.model.Chat;
import com.chatapp.backend.repository.ChatRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class MessageConsumer {

    private static final Logger log = LoggerFactory.getLogger(MessageConsumer.class);

    @Autowired
    private ChatRepository chatRepository;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @RabbitListener(queues = "${spring.rabbitmq.queue}")
    public void receiveMessage(Map<String, Object> message) {
        log.debug("Received message from RabbitMQ: {}", message);
        try {
            Integer chatId = (Integer) message.get("chatId");
            Integer senderId = (Integer) message.get("senderId");
            String content = (String) message.get("content");
            List<?> documents = (List<?>) message.get("documents");
            Long timestamp = (Long) message.get("timestamp");

            Chat chat = chatRepository.findById(chatId)
                    .orElseThrow(() -> new RuntimeException("Chat not found"));

            messagingTemplate.convertAndSend("/topic/messages-" + chatId,
                    Map.of("chatId", chatId, "senderId", senderId, "content", content, "documents", documents, "timestamp", timestamp));
        } catch (Exception e) {
            log.error("Error processing RabbitMQ message: {}", e.getMessage(), e);
        }
    }
}