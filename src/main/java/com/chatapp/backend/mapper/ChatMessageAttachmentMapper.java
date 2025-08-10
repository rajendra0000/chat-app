package com.chatapp.backend.mapper;

import com.chatapp.backend.dto.ChatMessageAttachmentDto;
import com.chatapp.backend.model.ChatMessageAttachment;
import com.chatapp.backend.model.ChatMessage;
import com.chatapp.backend.model.Document;

public class ChatMessageAttachmentMapper {

    public static ChatMessageAttachmentDto mapToDto(ChatMessageAttachment attach) {
        ChatMessageAttachmentDto dto = new ChatMessageAttachmentDto();
        dto.setId(attach.getId());
        dto.setChatMessageId(attach.getChatMessage().getId());
        dto.setDocumentId(attach.getDocument().getId());
        return dto;
    }

    public static ChatMessageAttachment mapToEntity(ChatMessageAttachmentDto dto) {
        ChatMessageAttachment attach = new ChatMessageAttachment();
        attach.setId(dto.getId());

        ChatMessage msg = new ChatMessage();
        msg.setId(dto.getChatMessageId());
        attach.setChatMessage(msg);

        Document doc = new Document();
        doc.setId(dto.getDocumentId());
        attach.setDocument(doc);

        return attach;
    }
}
