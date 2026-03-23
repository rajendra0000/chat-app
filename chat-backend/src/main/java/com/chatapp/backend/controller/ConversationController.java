package com.chatapp.backend.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.chatapp.backend.dto.ConversationDTO;
import com.chatapp.backend.dto.MessageResponseDTO;
import com.chatapp.backend.dto.UserSearchDTO;
import com.chatapp.backend.model.User;
import com.chatapp.backend.repository.ChatMemberRepository;
import com.chatapp.backend.repository.UserRepository;
import com.chatapp.backend.service.ConversationService;
import com.chatapp.backend.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class ConversationController {

    private static final Logger log = LoggerFactory.getLogger(ConversationController.class);

    private final UserService userService;
    private final ConversationService conversationService;
    private final UserRepository userRepository;
    private final ChatMemberRepository chatMemberRepository;

    @Autowired
    public ConversationController(UserService userService, ConversationService conversationService,
                                  UserRepository userRepository, ChatMemberRepository chatMemberRepository) {
        this.userService = userService;
        this.conversationService = conversationService;
        this.userRepository = userRepository;
        this.chatMemberRepository = chatMemberRepository;
    }

    private String getAuthenticatedPhone() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        return authentication.getName();
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationDTO>> userConversation() {
        String phone = getAuthenticatedPhone();
        log.debug("Fetching conversations for user: {}", phone);
        return ResponseEntity.ok(conversationService.getConversation(phone));
    }

    @PostMapping("/conversations")
    public ResponseEntity<ConversationDTO> createConversation(@Valid @RequestBody UserSearchDTO request) {
        String phone = getAuthenticatedPhone();
        ConversationDTO newConversation = conversationService.createConversation(phone, request.getUserId());
        return new ResponseEntity<>(newConversation, HttpStatus.CREATED);
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<Page<MessageResponseDTO>> getConversationMessages(
            @PathVariable Integer conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String phone = getAuthenticatedPhone();
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        if (!chatMemberRepository.existsByChat_IdAndUser_Id(conversationId, user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this conversation");
        }
        log.debug("Fetching messages for conversation {} (page={}, size={})", conversationId, page, size);
        Page<MessageResponseDTO> messages = conversationService.getMessagesForConversation(conversationId, page, size);
        return ResponseEntity.ok(messages);
    }

    /** #4 — Group rename (admin only) */
    @PatchMapping("/conversations/{conversationId}/rename")
    public ResponseEntity<Void> renameGroup(
            @PathVariable Integer conversationId,
            @RequestBody java.util.Map<String, String> body) {
        String phone = getAuthenticatedPhone();
        String name = body.getOrDefault("name", "").trim();
        if (name.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name cannot be blank");
        conversationService.renameGroup(phone, conversationId, name);
        return ResponseEntity.ok().build();
    }

    /** #5 — Delete group (only allowed when caller is the last/only member) */
    @DeleteMapping("/conversations/{conversationId}/group")
    public ResponseEntity<Void> deleteGroup(@PathVariable Integer conversationId) {
        String phone = getAuthenticatedPhone();
        conversationService.deleteGroup(phone, conversationId);
        return ResponseEntity.noContent().build();
    }

    /** #7 — Delete private chat for caller only (hidden_for_user = true) */
    @DeleteMapping("/conversations/{conversationId}")
    public ResponseEntity<Void> hideConversation(@PathVariable Integer conversationId) {
        String phone = getAuthenticatedPhone();
        conversationService.hideConversationForUser(phone, conversationId);
        return ResponseEntity.noContent().build();
    }
}
