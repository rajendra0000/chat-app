package com.chatapp.backend.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.chatapp.backend.dto.FileUploadResponseDTO;
import com.chatapp.backend.model.Chat;
import com.chatapp.backend.model.User;
import com.chatapp.backend.repository.ChatMemberRepository;
import com.chatapp.backend.repository.ChatRepository;
import com.chatapp.backend.repository.UserRepository;
import com.chatapp.backend.service.ConversationService;
import com.chatapp.backend.service.FileStorageService;
import com.chatapp.backend.service.UserService;

@RestController
@RequestMapping("/api")
public class FileController {

    private static final Logger log = LoggerFactory.getLogger(FileController.class);

    private final UserService userService;
    private final ConversationService conversationService;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;
    private final ChatRepository chatRepository;
    private final ChatMemberRepository chatMemberRepository;

    @Autowired
    public FileController(UserService userService, ConversationService conversationService,
                          FileStorageService fileStorageService, UserRepository userRepository,
                          ChatRepository chatRepository, ChatMemberRepository chatMemberRepository) {
        this.userService = userService;
        this.conversationService = conversationService;
        this.fileStorageService = fileStorageService;
        this.userRepository = userRepository;
        this.chatRepository = chatRepository;
        this.chatMemberRepository = chatMemberRepository;
    }

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        String phone = auth.getName();
        return userRepository.findByPhone(phone)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    @PostMapping(value = "/upload", consumes = {"multipart/form-data"})
    public ResponseEntity<FileUploadResponseDTO> uploadFile(@RequestPart("file") MultipartFile file) {
        try {
            getAuthenticatedUser();
            FileUploadResponseDTO response = fileStorageService.uploadFile(file);
            log.info("File uploaded successfully: {}", response.getFileName());
            return ResponseEntity.ok(response);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "File upload failed", e);
        }
    }

    @PutMapping(value = "/profile/upload-pic", consumes = {"multipart/form-data"})
    public ResponseEntity<FileUploadResponseDTO> uploadUserProfilePic(@RequestPart("file") MultipartFile file) {
        User user = getAuthenticatedUser();
        try {
            FileUploadResponseDTO response = fileStorageService.uploadFile(file);
            log.info("Profile pic uploaded for user: {}", user.getPhone());
            user.setProfilePicId(response.getDocumentId());
            userRepository.save(user);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "File upload failed", e);
        }
    }

    @GetMapping("/profile/pic-url")
    public ResponseEntity<String> getUserProfilePicUrl() {
        User user = getAuthenticatedUser();
        if (user.getProfilePicId() == null) {
            return ResponseEntity.ok(null);
        }
        try {
            String url = fileStorageService.getPresignedUrl(user.getProfilePicId());
            return ResponseEntity.ok(url);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to get profile pic URL", e);
        }
    }

    @GetMapping("/users/{userId}/pic-url")
    public ResponseEntity<String> getProfilePicUrl(@PathVariable Integer userId) {
        getAuthenticatedUser(); // Ensure caller is authenticated
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getProfilePicId() == null) {
            return ResponseEntity.ok(null);
        }
        try {
            String url = fileStorageService.getPresignedUrl(user.getProfilePicId());
            return ResponseEntity.ok(url);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to get profile pic URL", e);
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<User> getCurrentUser() {
        User user = getAuthenticatedUser();
        return ResponseEntity.ok(user);
    }

    @PostMapping(value = "/chats/{chatId}/upload-profile", consumes = {"multipart/form-data"})
    public ResponseEntity<FileUploadResponseDTO> uploadChatProfile(@PathVariable Integer chatId, @RequestPart("file") MultipartFile file) {
        User user = getAuthenticatedUser();
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat not found"));

        // Verify user is a member of the chat
        if (!chatMemberRepository.existsByChat_IdAndUser_Id(chatId, user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member of this chat");
        }
        if (!conversationService.isAdmin(chatId, user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not an admin");
        }
        try {
            FileUploadResponseDTO response = fileStorageService.uploadFile(file);
            chat.setProfilePicId(response.getDocumentId());
            chatRepository.save(chat);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "File upload failed", e);
        }
    }

    @GetMapping("/chats/{chatId}/pic-url")
    public ResponseEntity<String> getChatProfilePicUrl(@PathVariable Integer chatId) {
        User user = getAuthenticatedUser();
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat not found"));

        // Verify user is a member of the chat
        if (!chatMemberRepository.existsByChat_IdAndUser_Id(chatId, user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member of this chat");
        }
        if (chat.getProfilePicId() == null) {
            return ResponseEntity.ok(null);
        }
        try {
            String url = fileStorageService.getPresignedUrl(chat.getProfilePicId());
            return ResponseEntity.ok(url);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to get chat pic URL", e);
        }
    }

    @GetMapping("/chats/{chatId}/is-admin")
    public ResponseEntity<Boolean> isChatAdmin(@PathVariable Integer chatId) {
        User user = getAuthenticatedUser();
        boolean isAdmin = conversationService.isAdmin(chatId, user.getId());
        return ResponseEntity.ok(isAdmin);
    }
}
