package com.chatapp.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


// @SpringBootApplication(exclude = {
//         org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
//         org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
//        // org.springframework.ai.model.mistralai.autoconfigure.MistralAiChatAutoConfiguration.class,
//        // org.springframework.ai.model.mistralai.autoconfigure.MistralAiEmbeddingAutoConfiguration.class
// })

@SpringBootApplication
public class ChatBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatBackendApplication.class, args);
    }

}
