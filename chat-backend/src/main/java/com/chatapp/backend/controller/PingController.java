package com.chatapp.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lightweight keep-alive endpoint.
 * Cron-Job.org hits GET /ping every 10 minutes to prevent Render from sleeping.
 */
@RestController
public class PingController {

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }
}
