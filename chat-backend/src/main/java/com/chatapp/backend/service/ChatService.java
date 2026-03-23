package com.chatapp.backend.service;

import com.chatapp.backend.dto.ChatDto;
import com.chatapp.backend.dto.ConversationDTO;

import java.util.List;

public interface ChatService {

    ConversationDTO createGroup(String title,  List<Integer> memberIds);

    void deletePrivateConversation(Integer chatId);
    
    void leaveGroup(Integer chatId);

}
