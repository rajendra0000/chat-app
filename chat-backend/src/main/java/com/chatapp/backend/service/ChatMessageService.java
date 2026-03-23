package com.chatapp.backend.service;

import com.chatapp.backend.dto.ChatMessageDto;
import com.chatapp.backend.dto.ConversationDTO;

import java.util.Map;

public interface ChatMessageService {

    ConversationDTO blockUserInSingleChat(Integer chatId, Integer targetUserId);

    ConversationDTO unblockUserInSingleChat(Integer chatId, Integer targetUserId);

    ChatMessageDto editMessage(Integer messageId, String newText);

    void deleteMessage(Integer messageId);

    /** Toggle an emoji reaction for the current user on a message.
     *  Returns the updated emoji → count map for that message. */
    Map<String, Long> reactToMessage(Integer messageId, String emoji, String userPhone);

    /** Toggle the pinned flag on a message.
     *  Returns the new pinned state. */
    boolean togglePin(Integer messageId, String userPhone);

    /** Mark a message as seen (read) by the current user.
     *  Broadcasts a blue-tick receipt over WebSocket. */
    void markSeen(Integer messageId, String userPhone);
}
