package com.chatapp.backend.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.chatapp.backend.dto.ChatMessageDto;
import com.chatapp.backend.dto.ConversationDTO;
import com.chatapp.backend.dto.MessageDTO;
import com.chatapp.backend.dto.MessageRequestDTO;
import com.chatapp.backend.dto.MessageResponseDTO;
import com.chatapp.backend.dto.UserDto;
import com.chatapp.backend.service.ConversationService;
import com.chatapp.backend.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class ConversationController {

    private UserService userService;
    private ConversationService conversationService;

    @Autowired
    public ConversationController(UserService userService, ConversationService conversationService) {
        this.userService = userService;
        this.conversationService = conversationService;
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationDTO>> userConversation(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
        String phone = authentication.getName(); 
        System.out.println(phone);
        return ResponseEntity.ok(conversationService.getConversation(phone));
        }
        return new ResponseEntity<>(new ArrayList<>(),HttpStatus.NOT_FOUND);
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<List<MessageDTO>> getConversationMessages(@PathVariable Integer conversationId) {
        try {
            List<MessageDTO> messages = conversationService.getMessagesForConversation(conversationId);
            return ResponseEntity.ok(messages);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(null);
        }
    }

    // @PostMapping("/messages")
    // public ResponseEntity<MessageResponseDTO> sendMessage(@Valid @RequestBody MessageRequestDTO request) {
    //     try {
    //         MessageResponseDTO response = conversationService.sendMessage(request);
    //         return ResponseEntity.ok(response);
    //     } catch (ResponseStatusException e) {
    //         return ResponseEntity.status(e.getStatusCode()).body(null);
    //     }
    // }
}