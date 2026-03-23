package com.chatapp.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.chatapp.backend.dto.ConversationDTO;
import com.chatapp.backend.dto.CreateGroupRequestDTO;
import com.chatapp.backend.service.ChatService;

@RestController
@RequestMapping("/api/chats")
public class ChatController {

    private ChatService chatService;

    @Autowired
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }


    @PostMapping("/groups")
    public ResponseEntity<ConversationDTO> createGroup(@RequestBody CreateGroupRequestDTO request) {
        ConversationDTO newGroup = chatService.createGroup(request.getTitle(), request.getMemberIds());
        return ResponseEntity.ok(newGroup);
    }

    @DeleteMapping("/conversations/{chatId}")
    public ResponseEntity<Void> deletePrivateConversation(@PathVariable Integer chatId) {
        chatService.deletePrivateConversation(chatId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/groups/{chatId}/leave")
    public ResponseEntity<Void> leaveGroup(@PathVariable Integer chatId) {
        chatService.leaveGroup(chatId);
        return ResponseEntity.ok().build();
    }

}
