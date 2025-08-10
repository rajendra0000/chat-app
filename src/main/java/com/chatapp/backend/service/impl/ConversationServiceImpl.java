package com.chatapp.backend.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.chatapp.backend.dto.ConversationDTO;
import com.chatapp.backend.dto.MessageDTO;
import com.chatapp.backend.dto.MessageRequestDTO;
import com.chatapp.backend.dto.MessageResponseDTO;
import com.chatapp.backend.model.Chat;
import com.chatapp.backend.model.ChatMember;
import com.chatapp.backend.model.ChatMessage;
import com.chatapp.backend.model.User;
import com.chatapp.backend.repository.ChatMemberRepository;
import com.chatapp.backend.repository.ChatMessageRepository;
import com.chatapp.backend.repository.ChatRepository;
import com.chatapp.backend.repository.UserRepository;
import com.chatapp.backend.service.ConversationService;
import com.chatapp.backend.service.GroupPresenceTracker;
import com.chatapp.backend.service.UserStatusTracker;

import jakarta.transaction.Transactional;

@Service
public class ConversationServiceImpl implements ConversationService {

    private UserRepository userRepository;
    private ChatMemberRepository chatMemberRepository;
    private ChatMessageRepository chatMessageRepository;
    private SimpMessagingTemplate messagingTemplate;
    private GroupPresenceTracker groupPresenceTracker;
    private UserStatusTracker userStatusTracker;
    private ChatRepository chatRepository;
    private SimpUserRegistry simpUserRegistry;

    @Autowired
    public ConversationServiceImpl(UserRepository userRepository, ChatMemberRepository chatMemberRepository,
            ChatMessageRepository chatMessageRepository, SimpMessagingTemplate messagingTemplate,
            GroupPresenceTracker groupPresenceTracker, UserStatusTracker userStatusTracker, 
            ChatRepository chatRepository, SimpUserRegistry simpUserRegistry) {
        this.userRepository = userRepository;
        this.chatMemberRepository = chatMemberRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.messagingTemplate = messagingTemplate;
        this.groupPresenceTracker = groupPresenceTracker;
        this.userStatusTracker = userStatusTracker;
        this.chatRepository = chatRepository;
        this.simpUserRegistry = simpUserRegistry;
    }

    @Override
    @Transactional
    public List<ConversationDTO> getConversation(String phone) {
        User user = userRepository.findByPhone(phone)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Integer userId = user.getId();
        List<ChatMember> members = chatMemberRepository.findByUserId(userId);
        List<ConversationDTO> dtos = new ArrayList<>();
        for (ChatMember member : members) {
            Chat chat = member.getChat();
            List<ChatMember> otherMembers = chatMemberRepository.findByChat_IdAndUser_IdNot(chat.getId(), userId);
            String otherUserName = otherMembers.isEmpty() ? "Unknown" : otherMembers.get(0).getUser().getName();
            String otherUserStatus = otherMembers.isEmpty() ? "Unknown" : otherMembers.get(0).getUser().getStatus();
            ChatMessage latestMessage = chatMessageRepository.findFirstByChatMember_ChatOrderByCreatedAtDesc(chat);
            String lastMessageContent = latestMessage != null ? latestMessage.getContent() : null;
            String timestamp = latestMessage != null ? latestMessage.getCreatedAt().toString() : null;
            long unreadCount = chatMessageRepository.countByChatMember_ChatAndChatMember_UserIdNotAndReadFalse(chat, userId);
            ConversationDTO dto = new ConversationDTO();
            dto.setId(chat.getId());
            dto.setName(otherUserName);
            dto.setLastMessage(lastMessageContent);
            dto.setTimestamp(timestamp);
            dto.setUnreadCount((int) unreadCount);
            dto.setStatus(otherUserStatus);
            dtos.add(dto);
        }
        return dtos;
    }

    @Override
    public List<MessageDTO> getMessagesForConversation(Integer conversationId) {
        String phone = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByPhone(phone)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Integer userId = user.getId();
        ChatMember member = chatMemberRepository.findByChat_IdAndUser_Id(conversationId, userId);
        if (member == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User not a member of this conversation");
        }
        List<ChatMessage> messages = chatMessageRepository.findByChatMember_Chat_IdOrderByCreatedAtAsc(conversationId);
        List<MessageDTO> dtos = new ArrayList<>();
        for (ChatMessage message : messages) {
            MessageDTO dto = new MessageDTO();
            dto.setId(message.getId());
            dto.setSenderId(message.getChatMember().getUser().getId());
            dto.setText(message.getContent());
            dto.setTimestamp(message.getCreatedAt().toString());
            dto.setRead(message.getRead());
            dtos.add(dto);
        }
        return dtos;
    }

    @Override
    @Transactional
    public MessageResponseDTO sendMessage(MessageRequestDTO request, String phone) {
        if (phone.isEmpty()) {
            phone = SecurityContextHolder.getContext().getAuthentication().getName();
        }
        User user = userRepository.findByPhone(phone)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Integer userId = user.getId();
        ChatMember member = chatMemberRepository.findByChat_IdAndUser_Id(request.getConversationId(), userId);
        if (member == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User not a member of this conversation");
        }
        ChatMessage message = new ChatMessage();
        message.setChatMember(member);
        message.setContent(request.getText());
        ChatMessage savedMessage = chatMessageRepository.save(message);
        userStatusTracker.updateActivity(phone); // Update user activity
        MessageResponseDTO dto = new MessageResponseDTO();
        dto.setId(savedMessage.getId());
        dto.setConversationId(savedMessage.getChatMember().getChat().getId());
        dto.setSenderId(savedMessage.getChatMember().getUser().getId());
        dto.setText(savedMessage.getContent());
        dto.setTimestamp(savedMessage.getCreatedAt().toString());
        Chat chat = member.getChat();
        messagingTemplate.convertAndSend("/topic/messages-" + chat.getId(), dto);
        if (!isUserSubscribed(phone, chat.getId())) {
            chat.setUnreadCount(chat.getUnreadCount() + 1); // Simplified; see note
            chatRepository.save(chat);
            ChatMessage latestMessage = chatMessageRepository.findFirstByChatMember_ChatOrderByCreatedAtDesc(chat);
            messagingTemplate.convertAndSend("/queue/notifications-" + phone, 
                Map.of("chatId", chat.getId(), "unreadCount", chat.getUnreadCount(), 
                       "latestMessage", latestMessage.getContent(), "timestamp", latestMessage.getCreatedAt()));
            if ("GROUP".equals(chat.getChatType().name())) {
                groupPresenceTracker.broadcastGroupStatus(chat.getId());
            }
        }
        return dto;
    }

    @Override
    @Transactional
    public void markMessagesAsRead(Integer chatId, String phone) {
        User user = userRepository.findByPhone(phone)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Integer userId = user.getId();
        ChatMember member = chatMemberRepository.findByChat_IdAndUser_Id(chatId, userId);
        if (member == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User not a member of this conversation");
        }
        Chat chat = member.getChat();
        List<ChatMessage> messages = chatMessageRepository.findByChatMember_ChatAndChatMember_UserIdNotAndReadFalse(chat, userId);
        for (ChatMessage message : messages) {
            message.setRead(true);
            chatMessageRepository.save(message);
        }
        chat.setUnreadCount(0); // Reset for this user (simplified)
        chatRepository.save(chat);
        messagingTemplate.convertAndSend("/queue/read-" + phone, 
            Map.of("chatId", chatId, "unreadCount", 0));
    }

    @Override
    @Transactional
    public boolean isUserSubscribed(String phone, Integer chatId) {
        if(phone == null || phone.trim().isEmpty() || chatId == null) {
            return false;
        }

        // Get the user from SimpUserRegistry
        SimpUser user = simpUserRegistry.getUser(phone);
        if (user == null) {
            return false; // User is not connected
        }

        // Check if the user is subscribed to the destination containing the chatId
        String destination = "/topic/chat/" + chatId;
        return user.getSessions().stream()
                .flatMap(session -> session.getSubscriptions().stream())
                .anyMatch(subscription -> subscription.getDestination().equals(destination));
    }


    @Override
    @Transactional
    public List<Integer> getUserGroupIds(String phone) {
        User user = userRepository.findByPhone(phone)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return chatMemberRepository.findByUserId(user.getId()).stream()
            .filter(m -> "GROUP".equals(m.getChat().getChatType().name()))
            .map(m -> m.getChat().getId())
            .collect(Collectors.toList());
    }
}