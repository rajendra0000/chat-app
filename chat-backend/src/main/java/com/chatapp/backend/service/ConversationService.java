package com.chatapp.backend.service;

import java.util.List;

import org.springframework.data.domain.Page;

import com.chatapp.backend.dto.ConversationDTO;
import com.chatapp.backend.dto.MessageDTO;
import com.chatapp.backend.dto.MessageRequestDTO;
import com.chatapp.backend.dto.MessageResponseDTO;

public interface ConversationService {

    List<ConversationDTO> getConversation(String phone);

    ConversationDTO createConversation(String currentUserPhone, int otherUserId);

    Page<MessageResponseDTO> getMessagesForConversation(Integer conversationId, int page, int size);

    MessageResponseDTO sendMessage(MessageRequestDTO request , String phone);

    void markMessagesAsRead(Integer chatId, String phone);

    boolean isUserSubscribed(String phone, Integer chatId);

    List<Integer> getUserGroupIds(String phone);

    boolean isAdmin(Integer chatId, Integer userId);

    /** #4 — Rename a group (admin only) */
    void renameGroup(String phone, Integer chatId, String newName);

    /** #5 — Delete a group chat entirely (only if caller is the sole member) */
    void deleteGroup(String phone, Integer chatId);

    /** #7 — Mark a conversation as hidden for the calling user ("delete for me") */
    void hideConversationForUser(String phone, Integer chatId);
} 