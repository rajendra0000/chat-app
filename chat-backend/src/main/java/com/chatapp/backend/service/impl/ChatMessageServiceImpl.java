package com.chatapp.backend.service.impl;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.HtmlUtils;

import com.chatapp.backend.dto.ChatMessageDto;
import com.chatapp.backend.dto.ConversationDTO;
import com.chatapp.backend.enums.ChatType;
import com.chatapp.backend.mapper.ChatMessageMapper;
import com.chatapp.backend.model.Chat;
import com.chatapp.backend.model.ChatMember;
import com.chatapp.backend.model.ChatMessage;
import com.chatapp.backend.model.MessageReaction;
import com.chatapp.backend.model.User;
import com.chatapp.backend.repository.ChatMemberRepository;
import com.chatapp.backend.repository.ChatMessageRepository;
import com.chatapp.backend.repository.ChatRepository;
import com.chatapp.backend.repository.MessageReactionRepository;
import com.chatapp.backend.repository.UserRepository;
import com.chatapp.backend.service.ChatMessageService;

import jakarta.transaction.Transactional;

@Service
public class ChatMessageServiceImpl implements ChatMessageService {

    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final MessageReactionRepository messageReactionRepository;

    public ChatMessageServiceImpl(ChatRepository chatRepository, UserRepository userRepository,
                                  ChatMemberRepository chatMembersRepository, ChatMessageRepository chatMessagesRepository,
                                  SimpMessagingTemplate messagingTemplate,
                                  MessageReactionRepository messageReactionRepository) {
        this.chatRepository = chatRepository;
        this.userRepository = userRepository;
        this.chatMemberRepository = chatMembersRepository;
        this.chatMessageRepository = chatMessagesRepository;
        this.messagingTemplate = messagingTemplate;
        this.messageReactionRepository = messageReactionRepository;
    }

    @Override
    @Transactional
    public ConversationDTO blockUserInSingleChat(Integer chatId, Integer targetUserId) {
        // 1. Fetch the current user from the security context
        String phone = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByPhone(phone)
                .orElseThrow(() -> new IllegalArgumentException("Current user not found"));

        // 2. Fetch the chat and validate it's a private chat
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Chat not found with ID: " + chatId));

        if (!ChatType.PRIVATE.equals(chat.getChatType())) {
            throw new IllegalArgumentException("Blocking is only allowed in private chats");
        }

        // 3. Find the target member to block
        ChatMember targetMember = chatMemberRepository.findByChat_IdAndUser_Id(chatId, targetUserId);
        if (targetMember == null) {
            throw new IllegalArgumentException("Target user is not a member of the chat");
        }

        // 4. Update the block status if not already blocked
        if (!targetMember.isBlocked()) {
            targetMember.setBlocked(true);
            chatMemberRepository.save(targetMember);
        }

        // 5. Build and return the updated DTO with the new state
        return buildPrivateConversationDTO(chat, currentUser);
    }

    @Override
    @Transactional
    public ConversationDTO unblockUserInSingleChat(Integer chatId, Integer targetUserId) {
        // 1. Fetch the current user from the security context
        String phone = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByPhone(phone)
                .orElseThrow(() -> new IllegalArgumentException("Current user not found"));

        // 2. Fetch the chat and validate it's a private chat
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Chat not found with ID: " + chatId));

        if (!ChatType.PRIVATE.equals(chat.getChatType())) {
            throw new IllegalArgumentException("Unblocking is only allowed in private chats");
        }

        // 3. Find the target member to unblock
        ChatMember targetMember = chatMemberRepository.findByChat_IdAndUser_Id(chatId, targetUserId);
        if (targetMember == null) {
            throw new IllegalArgumentException("Target user is not a member of the chat");
        }

        // 4. Update the block status if they are currently blocked
        if (targetMember.isBlocked()) {
            targetMember.setBlocked(false);
            chatMemberRepository.save(targetMember);
        }

        List<ChatMember> members = chatMemberRepository.findByChat_Id(chatId);
        for (ChatMember member : members) {
            String userPhone = member.getUser().getPhone();
            messagingTemplate.convertAndSendToUser(userPhone, "/queue/refresh", Map.of("chatId", chatId));
        }
        
        // 5. Build and return the updated DTO with the new state
        return buildPrivateConversationDTO(chat, currentUser);
    }

    /**
     * Helper method to build a ConversationDTO for a private chat.
     * This ensures the block statuses are set correctly from the current user's perspective.
     */
    private ConversationDTO buildPrivateConversationDTO(Chat chat, User currentUser) {
        // Find the two members of the private chat
        List<ChatMember> members = chatMemberRepository.findByChat_Id(chat.getId());
        
        ChatMember currentUserMember = members.stream()
                .filter(m -> m.getUser().getId().equals(currentUser.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Current user is not a member of chat " + chat.getId()));

        ChatMember otherUserMember = members.stream()
                .filter(m -> !m.getUser().getId().equals(currentUser.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Could not find the other member of chat " + chat.getId()));

        ConversationDTO dto = new ConversationDTO();
        dto.setId(chat.getId());
        dto.setName(otherUserMember.getUser().getName()); // Name is the other person's name
        dto.setChatType(chat.getChatType().name());
        // ... set other fields like lastMessage, timestamp, etc. as needed

        // --- This is the most important part ---
        // Is the other user blocked by me? Check the 'isBlocked' flag on the OTHER user's member object.
        dto.setBlockedByCurrentUser(otherUserMember.isBlocked());
        
        // Am I blocked by the other user? Check the 'isBlocked' flag on MY OWN member object.
        dto.setCurrentUserBlocked(currentUserMember.isBlocked());
        
        dto.setParticipants(members.stream().map(m -> m.getUser().getId()).collect(Collectors.toList()));

        return dto;
    }

    @Override
    @Transactional
    public ChatMessageDto editMessage(Integer messageId, String newText) {
        if (newText == null || newText.trim().isEmpty()) {
            throw new IllegalArgumentException("Message content cannot be empty");
        }
        if (newText.length() > 500) {
            throw new IllegalArgumentException("Message content exceeds maximum length of 500 characters");
        }

        // Use EntityGraph to eagerly load attachments — prevents LazyInitializationException in mapper
        ChatMessage message = chatMessageRepository.findByIdWithAttachments(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found with ID: " + messageId));
        if (message.isDeleted()) {
            throw new IllegalStateException("Cannot edit a deleted message");
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String phone = auth.getName();
        User currentUser = userRepository.findByPhone(phone)
                .orElseThrow(() -> new IllegalArgumentException("Current user not found"));

        if (!message.getSenderId().equals(currentUser.getId())) {
            throw new IllegalStateException("Only the sender can edit the message");
        }

        message.setContent(HtmlUtils.htmlEscape(newText)); // Sanitize input to prevent XSS
        message.setEdited(true);
        message.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
        chatMessageRepository.save(message);

        ChatMessageDto messageDto = ChatMessageMapper.mapToDto(message);
        messageDto.setSenderId(message.getSenderId());

        // Notify clients via WebSocket
        messagingTemplate.convertAndSend("/topic/messages-" + message.getChatMember().getChat().getId(), messageDto);

        return messageDto;
    }

    @Override
    @Transactional
    public void deleteMessage(Integer messageId) {
        ChatMessage message = chatMessageRepository.findByIdWithAttachments(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found with ID: " + messageId));
        if (message.isDeleted()) {
            throw new IllegalStateException("Message is already deleted");
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String phone = auth.getName();
        User currentUser = userRepository.findByPhone(phone)
                .orElseThrow(() -> new IllegalArgumentException("Current user not found"));

        if (!message.getSenderId().equals(currentUser.getId())) {
            throw new IllegalStateException("Only the sender can delete the message");
        }

        message.setDeleted(true);
        message.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
        chatMessageRepository.save(message);

        // Notify clients via WebSocket
        ChatMessageDto messageDto = ChatMessageMapper.mapToDto(message);
        messageDto.setSenderId(message.getSenderId());
        messagingTemplate.convertAndSend("/topic/messages-" + message.getChatMember().getChat().getId(), messageDto);
    }

    // ─── Reaction ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public Map<String, Long> reactToMessage(Integer messageId, String emoji, String userPhone) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found: " + messageId));
        User user = userRepository.findByPhone(userPhone)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Enforce one reaction per user per message (WhatsApp/Telegram-style):
        // Remove any existing reaction by this user, regardless of emoji.
        Optional<MessageReaction> existingAny =
                messageReactionRepository.findByMessage_IdAndReactor_Id(messageId, user.getId());

        String previousEmoji = existingAny.map(MessageReaction::getEmoji).orElse(null);
        existingAny.ifPresent(messageReactionRepository::delete);

        // If the user tapped the same emoji they already had → toggle it off (already deleted above)
        // Otherwise add the new emoji
        if (!emoji.equals(previousEmoji)) {
            MessageReaction reaction = new MessageReaction();
            reaction.setMessage(message);
            reaction.setReactor(user);
            reaction.setEmoji(emoji);
            messageReactionRepository.save(reaction);
        }

        // Build updated emoji → count map
        Map<String, Long> reactionCounts = messageReactionRepository
                .countByEmojiForMessage(messageId).stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Long) row[1]
                ));

        // Broadcast to the room so all clients update in real-time
        int chatId = message.getChatMember().getChat().getId();
        messagingTemplate.convertAndSend("/topic/messages-" + chatId, Map.of(
                "type", "REACTION_UPDATE",
                "messageId", messageId,
                "reactions", reactionCounts
        ));

        return reactionCounts;
    }

    // ─── Pin ───────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public boolean togglePin(Integer messageId, String userPhone) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found: " + messageId));
        // Only members of the chat can pin (any member, not just admins for now)
        int chatId = message.getChatMember().getChat().getId();
        User user = userRepository.findByPhone(userPhone)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (chatMemberRepository.findByChat_IdAndUser_Id(chatId, user.getId()) == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this chat");
        }

        boolean newState = !message.isPinned();
        message.setPinned(newState);
        chatMessageRepository.save(message);

        // Broadcast pin event
        messagingTemplate.convertAndSend("/topic/messages-" + chatId, Map.of(
                "type", "PIN_TOGGLED",
                "messageId", messageId,
                "pinned", newState
        ));

        return newState;
    }

    // ─── Seen / Blue ticks ─────────────────────────────────────────────────────

    @Override
    @Transactional
    public void markSeen(Integer messageId, String userPhone) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found: " + messageId));
        if (Boolean.TRUE.equals(message.getRead())) return; // already read, skip

        User user = userRepository.findByPhone(userPhone)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        int chatId = message.getChatMember().getChat().getId();
        // Only the recipient (not the sender) marks a message as seen
        if (message.getSenderId().equals(user.getId())) return;

        message.setRead(true);
        chatMessageRepository.save(message);

        // Broadcast SEEN receipt so the sender's client can update to blue ticks
        int senderId = message.getSenderId();
        messagingTemplate.convertAndSendToUser(
                String.valueOf(senderId),
                "/queue/read-receipt",
                Map.of(
                        "type", "SEEN",
                        "messageId", messageId,
                        "chatId", chatId,
                        "seenBy", user.getId()
                )
        );
    }
}