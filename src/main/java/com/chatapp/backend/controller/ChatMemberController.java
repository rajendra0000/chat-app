package com.chatapp.backend.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat-members")
public class ChatMemberController {

    @PostMapping
    public void addChatMember() {}

    @DeleteMapping("/{id}")
    public void removeChatMember(@PathVariable Long id) {}

    @GetMapping("/{id}")
    public void getChatMember(@PathVariable Long id) {}

    @GetMapping("/chat/{chatId}")
    public void getMembersByChatId(@PathVariable Long chatId) {}
}
