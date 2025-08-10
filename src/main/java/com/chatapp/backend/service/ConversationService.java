package com.chatapp.backend.service;

import java.util.List;


import com.chatapp.backend.dto.ConversationDTO;
import com.chatapp.backend.dto.MessageDTO;
import com.chatapp.backend.dto.MessageRequestDTO;
import com.chatapp.backend.dto.MessageResponseDTO;

public interface ConversationService {

    List<ConversationDTO> getConversation(String phone);

    List<MessageDTO>getMessagesForConversation(Integer conversationId);

    MessageResponseDTO sendMessage(MessageRequestDTO request , String phone);

    void markMessagesAsRead(Integer chatId, String phone);

    boolean isUserSubscribed(String phone, Integer chatId);

    List<Integer> getUserGroupIds(String phone);
} 