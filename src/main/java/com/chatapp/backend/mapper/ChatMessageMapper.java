package com.chatapp.backend.mapper;

import com.chatapp.backend.dto.ChatMessageDto;
import com.chatapp.backend.model.ChatMessage;
import com.chatapp.backend.model.ChatMember;

public class ChatMessageMapper {

    public static ChatMessageDto mapToDto(ChatMessage msg) {
        ChatMessageDto dto = new ChatMessageDto();
        dto.setId(msg.getId());
        dto.setChatMemberId(msg.getChatMember().getId());
        dto.setContent(msg.getContent());
        dto.setIsEdited(msg.getIsEdited());
        dto.setCreatedAt(msg.getCreatedAt());
        dto.setUpdatedAt(msg.getUpdatedAt());
        return dto;
    }

    public static ChatMessage mapToEntity(ChatMessageDto dto) {
        ChatMessage msg = new ChatMessage();
        msg.setId(dto.getId());
        msg.setContent(dto.getContent());
        msg.setIsEdited(dto.getIsEdited());

        ChatMember member = new ChatMember();
        member.setId(dto.getChatMemberId());
        msg.setChatMember(member);

        return msg;
    }
}
