package com.chatapp.backend.service;

import com.chatapp.backend.dto.ChatMemberDTO;
import java.util.List;

public interface ChatMemberService {

    List<ChatMemberDTO> getMembers(Integer chatId, Integer requestingUserId);

    void addMember(Integer chatId, String phone);

    void removeMember(Integer chatId, Integer userId);
    
    void blockMember(Integer chatId, Integer userId);

    void unblockMember(Integer chatId, Integer userId);
}
