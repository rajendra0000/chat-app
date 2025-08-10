package com.chatapp.backend.service;

import com.chatapp.backend.dto.ChatDto;
import java.util.List;

public interface ChatsService {

    ChatDto createChat(ChatDto chatDto);

    ChatDto getChatById(Integer id);

    List<ChatDto> getAllChats();

    void deleteChat(Integer id);
}
