package com.chatapp.backend.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.chatapp.backend.dto.ChatMemberDTO;
import com.chatapp.backend.model.User;
import com.chatapp.backend.repository.UserRepository;
import com.chatapp.backend.service.ChatMemberService;

@RestController
@RequestMapping("/api")
public class ChatMemberController {

    private static final Logger log = LoggerFactory.getLogger(ChatMemberController.class);

    private final ChatMemberService chatMemberService;
    private final UserRepository userRepository;

    @Autowired
    public ChatMemberController(ChatMemberService chatMemberService, UserRepository userRepository) {
        this.chatMemberService = chatMemberService;
        this.userRepository = userRepository;
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        String phone = authentication.getName();
        return userRepository.findByPhone(phone)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    @GetMapping("/chats/{chatId}/members")
    public ResponseEntity<List<ChatMemberDTO>> getMembers(@PathVariable Integer chatId) {
        User user = getAuthenticatedUser();
        List<ChatMemberDTO> members = chatMemberService.getMembers(chatId, user.getId());
        return ResponseEntity.ok(members);
    }

    @PostMapping("/chats/{chatId}/add-member")
    public ResponseEntity<Void> addMember(@PathVariable Integer chatId, @RequestParam String phone) {
        getAuthenticatedUser();
        chatMemberService.addMember(chatId, phone);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/chats/{chatId}/remove-member")
    public ResponseEntity<Void> removeMember(@PathVariable Integer chatId, @RequestParam Integer userId) {
        getAuthenticatedUser();
        chatMemberService.removeMember(chatId, userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/chats/{chatId}/block-member")
    public ResponseEntity<Void> blockMember(@PathVariable Integer chatId, @RequestParam Integer userId) {
        getAuthenticatedUser();
        chatMemberService.blockMember(chatId, userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/chats/{chatId}/unblock-member")
    public ResponseEntity<Void> unblockMember(@PathVariable Integer chatId, @RequestParam Integer userId) {
        getAuthenticatedUser();
        chatMemberService.unblockMember(chatId, userId);
        return ResponseEntity.ok().build();
    }
}
