package com.chatapp.backend.mapper;

import com.chatapp.backend.dto.ChatMemberDTO;
import com.chatapp.backend.model.ChatMember;
import com.chatapp.backend.model.Chat;
import com.chatapp.backend.model.User;

public class ChatMemberMapper {

    public static ChatMemberDTO mapToDto(ChatMember member) {
        ChatMemberDTO dto = new ChatMemberDTO();
        dto.setId(member.getId());
        dto.setChatId(member.getChat().getId());
        dto.setUserId(member.getUser().getId());
        return dto;
    }

    public static ChatMember mapToEntity(ChatMemberDTO dto) {
        ChatMember member = new ChatMember();
        member.setId(dto.getId());

        Chat chat = new Chat();
        chat.setId(dto.getChatId());
        member.setChat(chat);

        User user = new User();
        user.setId(dto.getUserId());
        member.setUser(user);

        return member;
    }
}
