package com.chatapp.backend.controller;

import com.chatapp.backend.dto.ChatMessageDto;
import com.chatapp.backend.dto.ConversationDTO;
import com.chatapp.backend.service.ChatMessageService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.apache.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat-message")
@Validated
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

    public ChatMessageController(ChatMessageService chatMessageService) {
        this.chatMessageService = chatMessageService;
    }

   @PutMapping("/chats/{chatId}/block-user")
    public ResponseEntity<ConversationDTO> blockUserInSingleChat(
            @PathVariable @NotNull Integer chatId,
            @RequestParam @NotNull Integer targetUserId) {
        // Modify your service to return the updated ConversationDTO
        ConversationDTO updatedConversation = chatMessageService.blockUserInSingleChat(chatId, targetUserId);
        return ResponseEntity.ok(updatedConversation);
    }

    @PutMapping("/chats/{chatId}/unblock-user")
    public ResponseEntity<ConversationDTO> unblocUserInSingleChat(
            @PathVariable @NotNull Integer chatId,
            @RequestParam @NotNull Integer targetUserId) {
        // Modify your service to return the updated ConversationDTO
        ConversationDTO updatedConversation = chatMessageService.unblockUserInSingleChat(chatId, targetUserId);
        return ResponseEntity.ok(updatedConversation);
    }

    @PutMapping("/messages/{messageId}/edit")
    public ResponseEntity<ChatMessageDto> editMessage(
            @PathVariable @NotNull Integer messageId,
            @Valid @RequestBody Map<String, @NotBlank String> request) {
        String newText = request.get("text");
        ChatMessageDto updatedMessage = chatMessageService.editMessage(messageId, newText);
        return ResponseEntity.ok(updatedMessage);
    }

    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(@PathVariable @NotNull Integer messageId) {
        chatMessageService.deleteMessage(messageId);
        return ResponseEntity.ok().build();
    }
}