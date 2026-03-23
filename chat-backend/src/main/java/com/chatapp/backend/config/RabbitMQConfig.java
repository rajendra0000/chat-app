package com.chatapp.backend.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class RabbitMQConfig {

    @Value("${spring.rabbitmq.queue}")
    private String queueName;

    @Value("${spring.rabbitmq.uri:amqp://guest:guest@localhost:5672}")
    private String rabbitmqUri;

    private static final String DLX_EXCHANGE = "chat-dlx-exchange";
    private static final String DLQ_NAME = "chat-queue-dlq";
    private static final String AI_QUEUE_NAME = "ai-chat-queue";
    private static final String AI_DLQ_NAME = "ai-chat-queue-dlq";

    @Bean
    public Queue queue() {
        return QueueBuilder.durable(queueName)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_NAME)
                .build();
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX_EXCHANGE);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ_NAME).build();
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with(DLQ_NAME);
    }

    // ─── AI Chat Queue ───────────────────────────────────────

    @Bean
    public Queue aiChatQueue() {
        return QueueBuilder.durable(AI_QUEUE_NAME)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", AI_DLQ_NAME)
                .build();
    }

    @Bean
    public Queue aiDeadLetterQueue() {
        return QueueBuilder.durable(AI_DLQ_NAME).build();
    }

    @Bean
    public Binding aiDeadLetterBinding() {
        return BindingBuilder.bind(aiDeadLetterQueue())
                .to(deadLetterExchange())
                .with(AI_DLQ_NAME);
    }

    /**
     * Single shared AMQP connection with multiple virtual channels.
     * CloudAMQP Little Lemur free tier allows only 20 connections total —
     * CacheMode.CHANNEL multiplexes all listeners/templates over one TCP connection.
     */
    @Bean
    @Primary
    public CachingConnectionFactory connectionFactory() throws Exception {
        CachingConnectionFactory factory = new CachingConnectionFactory();
        factory.setUri(rabbitmqUri);
        factory.setCacheMode(CachingConnectionFactory.CacheMode.CHANNEL);
        factory.setChannelCacheSize(5);
        return factory;
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}