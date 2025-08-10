package com.chatapp.backend.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chats")
public class ChatController {

    @GetMapping
    public void getAllChats() {}

    @GetMapping("/{id}")
    public void getChatById(@PathVariable Long id) {}

    @PostMapping
    public void createChat() {}

    @PutMapping("/{id}")
    public void updateChat(@PathVariable Long id) {}

    @DeleteMapping("/{id}")
    public void deleteChat(@PathVariable Long id) {}

    @GetMapping("/{id}/messages")
    public void getChatMessages(@PathVariable Long id) {}

    @GetMapping("/{id}/members")
    public void getChatMembers(@PathVariable Long id) {}
}
