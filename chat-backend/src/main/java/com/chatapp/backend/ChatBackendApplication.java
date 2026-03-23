package com.chatapp.backend;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class ChatBackendApplication {
    public static void main(String[] args) {
        // Fix JVM timezone: PostgreSQL 15 rejects deprecated "Asia/Calcutta", expects "Asia/Kolkata"
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));

        Dotenv dotenv = Dotenv.configure().load();
        dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));
        SpringApplication.run(ChatBackendApplication.class, args);
    }
}
