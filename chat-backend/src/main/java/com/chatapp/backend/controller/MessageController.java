package com.chatapp.backend.controller;

import com.chatapp.backend.service.ChatMessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Handles the three "modern" message-level actions surfaced by the frontend:
 *  POST  /api/messages/{id}/react  — toggle emoji reaction
 *  PATCH /api/messages/{id}/pin    — toggle pin flag
 *  POST  /api/messages/{id}/seen   — mark message as seen (blue ticks)
 */
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final ChatMessageService chatMessageService;

    public MessageController(ChatMessageService chatMessageService) {
        this.chatMessageService = chatMessageService;
    }

    private String currentUserPhone() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        return auth.getName();
    }

    /** Toggle emoji reaction. Returns updated emoji → count map. */
    @PostMapping("/{messageId}/react")
    public ResponseEntity<Map<String, Long>> react(
            @PathVariable Integer messageId,
            @RequestBody Map<String, String> body) {
        String emoji = body.get("emoji");
        if (emoji == null || emoji.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "emoji is required");
        }
        Map<String, Long> reactions = chatMessageService.reactToMessage(messageId, emoji, currentUserPhone());
        return ResponseEntity.ok(reactions);
    }

    /** Toggle pin flag on a message. Returns {"pinned": true/false}. */
    @PatchMapping("/{messageId}/pin")
    public ResponseEntity<Map<String, Boolean>> pin(@PathVariable Integer messageId) {
        boolean pinned = chatMessageService.togglePin(messageId, currentUserPhone());
        return ResponseEntity.ok(Map.of("pinned", pinned));
    }

    /** Mark a message as seen (read) by the current user. */
    @PostMapping("/{messageId}/seen")
    public ResponseEntity<Void> seen(@PathVariable Integer messageId) {
        chatMessageService.markSeen(messageId, currentUserPhone());
        return ResponseEntity.ok().build();
    }
}
