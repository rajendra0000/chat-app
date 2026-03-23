package com.chatapp.backend.mapper;

import com.chatapp.backend.dto.ChatDto;
import com.chatapp.backend.model.Chat;

public class ChatMapper {

    public static ChatDto mapToDto(Chat chat) {
        ChatDto dto = new ChatDto();
        dto.setId(chat.getId());
        dto.setTitle(chat.getTitle());
        dto.setChatType(chat.getChatType());
        dto.setInitiatedBy(chat.getInitiatedBy());
        dto.setCreatedAt(chat.getCreatedAt());
        dto.setUpdatedAt(chat.getUpdatedAt());
        return dto;
    }

    public static Chat mapToEntity(ChatDto dto) {
        Chat chat = new Chat();
        chat.setId(dto.getId());
        chat.setTitle(dto.getTitle());
        chat.setChatType(dto.getChatType());
        chat.setInitiatedBy(dto.getInitiatedBy());
        return chat;
    }
}
