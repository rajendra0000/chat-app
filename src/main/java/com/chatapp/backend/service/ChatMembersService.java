package com.chatapp.backend.service;

import com.chatapp.backend.dto.ChatMemberDto;
import java.util.List;

public interface ChatMembersService {

    ChatMemberDto addMemberToChat(ChatMemberDto chatMemberDto);

    List<ChatMemberDto> getMembersByChatId(Integer chatId);

    void removeMember(Integer id);
}
