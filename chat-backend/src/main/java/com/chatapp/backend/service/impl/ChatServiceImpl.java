package com.chatapp.backend.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.chatapp.backend.dto.ConversationDTO;
import com.chatapp.backend.enums.ChatType;
import com.chatapp.backend.model.Chat;
import com.chatapp.backend.model.ChatMember;
import com.chatapp.backend.model.User;
import com.chatapp.backend.repository.ChatMemberRepository;
import com.chatapp.backend.repository.ChatRepository;
import com.chatapp.backend.repository.UserRepository;
import com.chatapp.backend.service.ChatService;

import jakarta.transaction.Transactional;

@Service
public class ChatServiceImpl implements ChatService {

    // Assume necessary repositories are autowired here
    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final ChatMemberRepository chatMemberRepository;

    @Autowired
    public ChatServiceImpl(ChatRepository chatRepository, UserRepository userRepository, ChatMemberRepository chatMemberRepository) {
        this.chatRepository = chatRepository;
        this.userRepository = userRepository;
        this.chatMemberRepository = chatMemberRepository;
    }

@Transactional
@Override
public ConversationDTO createGroup(String title, List<Integer> memberIds) {
    String phone = SecurityContextHolder.getContext().getAuthentication().getName();
    User creator = userRepository.findByPhone(phone).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

    // Create and save the chat
    Chat chat = new Chat();
    chat.setTitle(title);
    chat.setChatType(ChatType.GROUP);
    Chat savedChat = chatRepository.save(chat);

    // Add the creator as an ADMIN
    ChatMember creatorMember = new ChatMember();
    creatorMember.setChat(savedChat);
    creatorMember.setUser(creator);
    creatorMember.setAdmin(true);
    chatMemberRepository.save(creatorMember);

    // Add other members
    for (Integer memberId : memberIds) {
        if (!memberId.equals(creator.getId())) {
            User memberUser = userRepository.findById(memberId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member user not found"));
            ChatMember member = new ChatMember();
            member.setChat(savedChat);
            member.setUser(memberUser);
            member.setAdmin(false);
            chatMemberRepository.save(member);
        }
    }
    // You need a mapper to convert the 'Chat' entity to a 'ConversationDTO'
    // This is a simplified example; you'll need to implement the mapping fully.
    ConversationDTO dto = new ConversationDTO();
    dto.setId(savedChat.getId());
    dto.setName(savedChat.getTitle());
    dto.setChatType(savedChat.getChatType().name());
    return dto;
}

@Transactional
@Override
public void deletePrivateConversation(Integer chatId) {
    String phone = SecurityContextHolder.getContext().getAuthentication().getName();
    User currentUser = userRepository.findByPhone(phone).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    
    Chat chat = chatRepository.findById(chatId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat not found"));

    if (chat.getChatType() != ChatType.PRIVATE) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only private conversations can be deleted");
    }

    // This will delete all memberships, effectively deleting the chat for all users
    //chatMemberRepository.deleteAllByChat_Id(chatId);
    chatMemberRepository.deleteByChat_IdAndUser_Id(chatId, currentUser.getId());
    // Optionally delete messages if desired
    // chatMessageRepository.deleteAllByChatId(chatId);
    chatRepository.delete(chat);
}

@Transactional
@Override
public void leaveGroup(Integer chatId) {
    String phone = SecurityContextHolder.getContext().getAuthentication().getName();
    User currentUser = userRepository.findByPhone(phone).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

    ChatMember member = chatMemberRepository.findByChat_IdAndUser_Id(chatId, currentUser.getId());
    if (member == null) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "You are not a member of this group");
    }

    // Basic check to prevent the last admin from leaving.
    // A more robust solution would be to transfer admin rights.
    if (member.isAdmin()) {
        // long adminCount = chatMemberRepository.countByChat_IdAndRole(chatId, "ADMIN");
        // if (adminCount <= 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You are the last admin and cannot leave the group.");
        // }
    }
    chatMemberRepository.delete(member);
}


}
