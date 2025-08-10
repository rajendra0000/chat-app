package com.chatapp.backend.service;

import com.chatapp.backend.dto.ChatMessageDto;
import java.util.List;

public interface ChatMessagesService {

    ChatMessageDto sendMessage(ChatMessageDto chatMessageDto);

    List<ChatMessageDto> getMessagesByChatId(Integer chatId);

    void deleteMessage(Integer id);
}
