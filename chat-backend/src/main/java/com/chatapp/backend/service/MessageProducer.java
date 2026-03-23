package com.chatapp.backend.service;

import com.chatapp.backend.dto.MessageResponseDTO;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class MessageProducer {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${spring.rabbitmq.queue}")
    private String queueName;

    public void sendMessage(Integer chatId, Integer userId, String content, List<MessageResponseDTO.DocumentDTO> documents) {
        Map<String, Object> message = Map.of(
                "chatId", chatId,
                "userId", userId,
                "content", content != null ? content : "",
                "documents", documents != null ? documents : List.of(),
                "timestamp", System.currentTimeMillis()
        );
        rabbitTemplate.convertAndSend(queueName, message);
    }
}