package com.chatapp.backend.service.impl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.chatapp.backend.dto.ChatMemberDTO;
import com.chatapp.backend.enums.ChatType;
import com.chatapp.backend.model.Chat;
import com.chatapp.backend.model.ChatMember;
import com.chatapp.backend.model.User;
import com.chatapp.backend.repository.ChatMemberRepository;
import com.chatapp.backend.repository.ChatRepository;
import com.chatapp.backend.repository.UserRepository;
import com.chatapp.backend.service.ChatMemberService;

@Service
public class ChatMemberServiceImpl implements ChatMemberService {

    private static final Logger log = LoggerFactory.getLogger(ChatMemberServiceImpl.class);

    private final ChatMemberRepository chatMemberRepository;
    private final ChatRepository chatRepository;
    private final UserRepository userRepository;

    @Autowired
    public ChatMemberServiceImpl(ChatMemberRepository chatMemberRepository, ChatRepository chatRepository,
            UserRepository userRepository) {
        this.chatMemberRepository = chatMemberRepository;
        this.chatRepository = chatRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMemberDTO> getMembers(Integer chatId, Integer requestingUserId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat not found"));
        if (!chatMemberRepository.existsByChat_IdAndUser_Id(chatId, requestingUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not a member of this chat");
        }
        if (!ChatType.GROUP.equals(chat.getChatType())) {
            return List.of();
        }
        ChatMember chatmember = Optional.ofNullable(chatMemberRepository.findByChat_IdAndUser_Id(chatId, requestingUserId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not a member of this chat"));

        if (chatmember.isBlocked()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are blocked in this chat");
        }
        List<ChatMember> members = chatMemberRepository.findByChat_Id(chatId);
        return members.stream().map(member -> {
            ChatMemberDTO dto = new ChatMemberDTO();
            dto.setUserId(member.getUser().getId());
            dto.setName(member.getUser().getName());
            dto.setRole(member.getRole());
            dto.setBlocked(member.isBlocked());
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void addMember(Integer chatId, String phone) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat not found"));
        if (!ChatType.GROUP.equals(chat.getChatType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only group chats can add members");
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String adminPhone = auth.getName();
        User currentUser = userRepository.findByPhone(adminPhone)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        ChatMember adminMember = Optional.ofNullable(chatMemberRepository.findByChat_IdAndUser_Id(chatId, currentUser.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member"));
        if (!"ADMIN".equals(adminMember.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can add members");
        }
        User newUser = userRepository.findByPhone(phone)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (Optional.ofNullable(chatMemberRepository.findByChat_IdAndUser_Id(chatId, newUser.getId())).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already in group");
        }
        ChatMember member = new ChatMember();
        member.setChat(chat);
        member.setUser(newUser);
        member.setRole("MEMBER");
        member.setBlocked(false);
        chatMemberRepository.save(member);
        log.info("Added member {} to chat {}", phone, chatId);
    }

    @Override
    @Transactional
    public void removeMember(Integer chatId, Integer userId) {
        chatRepository.findById(chatId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat not found"));
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String phone = auth.getName();
        User currentUser = userRepository.findByPhone(phone)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        ChatMember adminMember = Optional.ofNullable(chatMemberRepository.findByChat_IdAndUser_Id(chatId, currentUser.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member"));
        if (!"ADMIN".equals(adminMember.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can remove members");
        }
        ChatMember targetMember = Optional.ofNullable(chatMemberRepository.findByChat_IdAndUser_Id(chatId, userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));
        if ("ADMIN".equals(targetMember.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot remove admins");
        }
        chatMemberRepository.delete(targetMember);
        log.info("Removed member {} from chat {}", userId, chatId);
    }

    @Override
    @Transactional
    public void unblockMember(Integer chatId, Integer userId) {
        chatRepository.findById(chatId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat not found"));
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String phone = auth.getName();
        User currentUser = userRepository.findByPhone(phone)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        ChatMember adminMember = Optional.ofNullable(chatMemberRepository.findByChat_IdAndUser_Id(chatId, currentUser.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member"));
        if (!"ADMIN".equals(adminMember.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can unblock members");
        }
        ChatMember targetMember = Optional.ofNullable(chatMemberRepository.findByChat_IdAndUser_Id(chatId, userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));
        targetMember.setBlocked(false);
        chatMemberRepository.save(targetMember);
        log.info("Unblocked member {} in chat {}", userId, chatId);
    }

    @Override
    @Transactional
    public void blockMember(Integer chatId, Integer userId) {
        chatRepository.findById(chatId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat not found"));
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String phone = auth.getName();
        User currentUser = userRepository.findByPhone(phone)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        ChatMember adminMember = Optional.ofNullable(chatMemberRepository.findByChat_IdAndUser_Id(chatId, currentUser.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member"));
        if (!"ADMIN".equals(adminMember.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can block members");
        }
        ChatMember targetMember = Optional.ofNullable(chatMemberRepository.findByChat_IdAndUser_Id(chatId, userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));
        targetMember.setBlocked(true);
        chatMemberRepository.save(targetMember);
        log.info("Blocked member {} in chat {}", userId, chatId);
    }
}
