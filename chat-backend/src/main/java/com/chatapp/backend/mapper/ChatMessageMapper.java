package com.chatapp.backend.mapper;

import com.chatapp.backend.dto.ChatMessageAttachmentDto;
import com.chatapp.backend.dto.ChatMessageDto;
import com.chatapp.backend.model.ChatMessage;


import com.chatapp.backend.model.ChatMember;
import java.util.stream.Collectors;


public class ChatMessageMapper {

    public static ChatMessageDto mapToDto(ChatMessage msg) {
        if (msg == null) {
            return null;
        }
        ChatMessageDto dto = new ChatMessageDto();
        dto.setId(msg.getId());
        dto.setChatMemberId(msg.getChatMember() != null ? msg.getChatMember().getId() : null);
        dto.setText(msg.isDeleted() ? "This message was deleted" : msg.getContent());
        dto.setEdited(msg.isEdited());
        dto.setDeleted(msg.isDeleted());
        dto.setIsRead(msg.getRead());
        dto.setCreatedAt(msg.getCreatedAt());
        dto.setUpdatedAt(msg.getUpdatedAt());
        // Map attachments if present
        if (msg.getAttachments() != null) {
            dto.setAttachments(msg.getAttachments().stream()
                    .map(ChatMessageAttachmentMapper::mapToDto) // Assume a mapper for attachments
                    .collect(Collectors.toList()));
        }
        return dto;
    }

    public static ChatMessage mapToEntity(ChatMessageDto dto) {
        if (dto == null) {
            return null;
        }
        ChatMessage msg = new ChatMessage();
        msg.setId(dto.getId());
        msg.setContent(dto.getText());
        msg.setEdited(dto.getEdited() != null ? dto.getEdited() : false);
        msg.setDeleted(dto.getDeleted() != null ? dto.getDeleted() : false);
        msg.setRead(dto.getIsRead() != null ? dto.getIsRead() : false);
        if (dto.getChatMemberId() != null) {
            ChatMember member = new ChatMember();
            member.setId(dto.getChatMemberId());
            msg.setChatMember(member);
        }
        if (dto.getCreatedAt() != null) {
            msg.setCreatedAt(dto.getCreatedAt());
        }
        if (dto.getUpdatedAt() != null) {
            msg.setUpdatedAt(dto.getUpdatedAt());
        }
        if (dto.getAttachments() != null) {
            msg.setAttachments(dto.getAttachments().stream()
                    .map(ChatMessageAttachmentMapper::mapToEntity) // Assume a mapper for attachments
                    .collect(Collectors.toList()));
        }
        return msg;
    }
}