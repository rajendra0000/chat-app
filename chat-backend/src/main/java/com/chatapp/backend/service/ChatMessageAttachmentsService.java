package com.chatapp.backend.service;

import com.chatapp.backend.dto.ChatMessageAttachmentDto;

public interface ChatMessageAttachmentsService {

    ChatMessageAttachmentDto attachDocumentToMessage(ChatMessageAttachmentDto attachmentDto);

    void deleteAttachment(Integer id);
}
