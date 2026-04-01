package com.chatapp.backend.service.impl;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.chatapp.backend.dto.ChatMemberDTO;
import com.chatapp.backend.dto.ChatMessageDto;
import com.chatapp.backend.dto.ConversationDTO;
import com.chatapp.backend.dto.FileUploadResponseDTO;
import com.chatapp.backend.dto.MessageDTO;
import com.chatapp.backend.dto.MessageRequestDTO;
import com.chatapp.backend.dto.MessageResponseDTO;
import com.chatapp.backend.dto.MessageResponseDTO.DocumentDTO;
import com.chatapp.backend.enums.ChatType;
import com.chatapp.backend.mapper.ChatMessageMapper;
import com.chatapp.backend.model.Chat;
import com.chatapp.backend.model.ChatMember;
import com.chatapp.backend.model.ChatMessage;
import com.chatapp.backend.model.ChatMessageAttachment;
import com.chatapp.backend.model.Document;
import com.chatapp.backend.model.User;
import com.chatapp.backend.repository.ChatMemberRepository;
import com.chatapp.backend.repository.ChatMessageAttachmentRepository;
import com.chatapp.backend.repository.ChatMessageRepository;
import com.chatapp.backend.repository.ChatRepository;
import com.chatapp.backend.repository.DocumentRepository;
import com.chatapp.backend.repository.UserRepository;
import com.chatapp.backend.service.ConversationService;
import com.chatapp.backend.service.FileStorageService;
import com.chatapp.backend.service.GroupPresenceTracker;
import com.chatapp.backend.service.MessageProducer;
import com.chatapp.backend.service.UserStatusTracker;
import com.chatapp.backend.service.ChatMetrics;
import com.chatapp.backend.ai.AiMessageProducer;
import com.chatapp.backend.ai.AiService;
import com.chatapp.backend.ai.AiUserInitializer;

import jakarta.transaction.Transactional;

@Service
public class ConversationServiceImpl implements ConversationService {

    private static final Logger logger = LoggerFactory.getLogger(ConversationService.class);

    private UserRepository userRepository;
    private ChatMemberRepository chatMemberRepository;
    private ChatMessageRepository chatMessageRepository;
    private SimpMessagingTemplate messagingTemplate;
    private GroupPresenceTracker groupPresenceTracker;
    private UserStatusTracker userStatusTracker;
    private ChatRepository chatRepository;
    private SimpUserRegistry simpUserRegistry;
    private MessageProducer messageProducer;
    private FileStorageService fileStorageService;
    private DocumentRepository documentRepository;
    private ChatMessageAttachmentRepository chatMessageAttachmentRepository;
    private ChatMetrics chatMetrics;
    private AiMessageProducer aiMessageProducer;
    private AiService aiService;

    @Autowired
    public ConversationServiceImpl(UserRepository userRepository, ChatMemberRepository chatMemberRepository,
            ChatMessageRepository chatMessageRepository, SimpMessagingTemplate messagingTemplate,
            GroupPresenceTracker groupPresenceTracker, UserStatusTracker userStatusTracker, 
            ChatRepository chatRepository, SimpUserRegistry simpUserRegistry,
            MessageProducer messageProducer, FileStorageService fileStorageService,
            DocumentRepository documentRepository, ChatMessageAttachmentRepository chatMessageAttachmentRepository,
            ChatMetrics chatMetrics, AiMessageProducer aiMessageProducer, AiService aiService) {
        this.userRepository = userRepository;
        this.chatMemberRepository = chatMemberRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.messagingTemplate = messagingTemplate;
        this.groupPresenceTracker = groupPresenceTracker;
        this.userStatusTracker = userStatusTracker;
        this.chatRepository = chatRepository;
        this.simpUserRegistry = simpUserRegistry;
        this.messageProducer = messageProducer;
        this.fileStorageService = fileStorageService;
        this.documentRepository = documentRepository;
        this.chatMessageAttachmentRepository = chatMessageAttachmentRepository;
        this.chatMetrics = chatMetrics;
        this.aiMessageProducer = aiMessageProducer;
        this.aiService = aiService;
    }

    @Value("${spring.rabbitmq.queue}")
    private String queueName;

    @Override
    @Transactional
    public List<ConversationDTO> getConversation(String phone) {
        User user = userRepository.findByPhone(phone)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Integer userId = user.getId();
        List<ChatMember> members = chatMemberRepository.findByUserId(userId);
        List<ConversationDTO> dtos = new ArrayList<>();
        for (ChatMember member : members) {
            // #7: Skip chats hidden by this user (delete for me)
            if (member.isHiddenForUser()) continue;

            Chat chat = member.getChat();
            List<ChatMember> otherMembers = chatMemberRepository.findByChat_IdAndUser_IdNot(chat.getId(), userId);
            String otherUserName = otherMembers.isEmpty() ? "Unknown" : otherMembers.get(0).getUser().getName();
            String otherUserStatus = otherMembers.isEmpty() ? "Unknown" : otherMembers.get(0).getUser().getStatus();
            ChatMessage latestMessage = chatMessageRepository.findFirstByChatMember_ChatOrderByCreatedAtDesc(chat);

            // #6: Hide empty PRIVATE conversations (no messages) but show empty GROUP chats
            // Newly created groups have no messages yet and must still appear in the sidebar.
            if (latestMessage == null && chat.getChatType() != ChatType.GROUP) continue;

            String lastMessageContent = latestMessage != null ? latestMessage.getContent() : "";
            String timestamp = latestMessage != null
                    ? latestMessage.getCreatedAt().toString()
                    : chat.getCreatedAt().toString();
            long unreadCount = chatMessageRepository.countByChatMember_ChatAndChatMember_UserIdNotAndReadFalse(chat, userId);
            ConversationDTO dto = new ConversationDTO();
            dto.setId(chat.getId());
            dto.setName(otherUserName);
            dto.setLastMessage(latestMessage != null && latestMessage.isDeleted() ? "This message was deleted" : lastMessageContent);
            dto.setTimestamp(timestamp);
            dto.setDeleted(latestMessage != null && latestMessage.isDeleted());
            dto.setUnreadCount((int) unreadCount);
            dto.setStatus(otherUserStatus);
            List<Integer> participantIds = chatMemberRepository.findByChat_Id(chat.getId())
            .stream()
            .map(cm -> cm.getUser().getId())
            .collect(Collectors.toList());
            dto.setParticipants(participantIds);
            if (ChatType.GROUP.equals(chat.getChatType())) {
                dto.setChatType("GROUP");
                dto.setName(chat.getTitle() != null ? chat.getTitle() : "Unnamed Group");
                // Resolve group profile picture
                if (chat.getProfilePicId() != null) {
                    try {
                        dto.setAvatarUrl(fileStorageService.getPresignedUrl(chat.getProfilePicId()));
                    } catch (Exception e) {
                        logger.warn("Failed to resolve group avatar for chat {}: {}", chat.getId(), e.getMessage());
                    }
                }
            } else { // Assume PRIVATE
                dto.setChatType("PRIVATE");
                if (!otherMembers.isEmpty()) {
                    ChatMember otherMember = otherMembers.get(0);
                    dto.setName(otherUserName);
                    dto.setStatus(otherUserStatus);
                    dto.setBlockedByCurrentUser(otherMember.isBlocked());
                    dto.setCurrentUserBlocked(member.isBlocked());
                    logger.debug("isBlockedByCurrentUser: {}, isCurrentUserBlocked: {}", dto.isBlockedByCurrentUser(), dto.isCurrentUserBlocked());
                    // Resolve other user's profile picture
                    Integer picId = otherMember.getUser().getProfilePicId();
                    if (picId != null) {
                        try {
                            dto.setAvatarUrl(fileStorageService.getPresignedUrl(picId));
                        } catch (Exception e) {
                            logger.warn("Failed to resolve avatar for user {} in chat {}: {}", otherMember.getUser().getId(), chat.getId(), e.getMessage());
                        }
                    }
                } else {
                    dto.setName("Unknown");
                    dto.setStatus("active");
                }
            }
            dtos.add(dto);
        }
        return dtos;
    }


    @Override
    @Transactional
    public ConversationDTO createConversation(String currentUserPhone, int otherUserId) {
        User currentUser = userRepository.findByPhone(currentUserPhone)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Current user not found"));
        User otherUser = userRepository.findById(otherUserId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Other user not found"));

        // Check for existing private conversation between these two users
        List<ChatMember> currentUserMemberships = chatMemberRepository.findByUserId(currentUser.getId());
        for (ChatMember membership : currentUserMemberships) {
            Chat existingChat = membership.getChat();
            if (existingChat.getChatType() == ChatType.PRIVATE) {
                ChatMember otherMember = chatMemberRepository.findByChat_IdAndUser_Id(existingChat.getId(), otherUserId);
                if (otherMember != null) {
                    logger.info("Private conversation already exists between {} and user {}", currentUserPhone, otherUserId);
                    ConversationDTO dto = new ConversationDTO();
                    dto.setId(existingChat.getId());
                    dto.setName(otherUser.getName());
                    dto.setLastMessage(null);
                    dto.setTimestamp(existingChat.getCreatedAt().toString());
                    dto.setUnreadCount(0);
                    dto.setStatus(otherUser.getStatus());
                    return dto;
                }
            }
        }

        // Create a new chat
        Chat chat = new Chat();
        chat.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        chat.setChatType(ChatType.PRIVATE);
        chat.setUnreadCount(0);
        chat.setTitle(currentUser.getName() + " & " + otherUser.getName());
        chat = chatRepository.save(chat);

        // Add members to the chat
        ChatMember currentMember = new ChatMember();
        currentMember.setChat(chat);
        currentMember.setUser(currentUser);
        chatMemberRepository.save(currentMember);

        ChatMember otherMember = new ChatMember();
        otherMember.setChat(chat);
        otherMember.setUser(otherUser);
        chatMemberRepository.save(otherMember);

        // Prepare the ConversationDTO for the new chat
        ConversationDTO dto = new ConversationDTO();
        dto.setId(chat.getId());
        dto.setName(otherUser.getName());
        dto.setLastMessage(null); // No messages yet
        dto.setTimestamp(chat.getCreatedAt().toString());
        dto.setUnreadCount(0);
        dto.setStatus(otherUser.getStatus());

        return dto;
    }

    @Override
    @Transactional
    public Page<MessageResponseDTO> getMessagesForConversation(Integer conversationId, int page, int size) {
        logger.debug("Fetching messages for conversationId: {}, page: {}, size: {}", conversationId, page, size);

        // Authentication
        String phone = SecurityContextHolder.getContext().getAuthentication().getName();
        if (phone == null) {
            logger.error("No authenticated user found in SecurityContext");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No authenticated user found");
        }
        logger.debug("Authenticated user phone: {}", phone);

        // Find user
        User user = userRepository.findByPhone(phone)
            .orElseThrow(() -> {
                logger.error("User not found for phone: {}", phone);
                return new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
            });
        Integer userId = user.getId();
        logger.debug("User found: id={}", userId);

        // Check membership
        ChatMember member = chatMemberRepository.findByChat_IdAndUser_Id(conversationId, userId);
        if (member == null) {
            logger.error("User {} is not a member of conversation {}", userId, conversationId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User not a member of this conversation");
        }
        logger.debug("User {} is a member of conversation {}", userId, conversationId);

        // Step 1: Paginate WITHOUT @EntityGraph (avoids HHH90003004 full-table-in-memory fetch)
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ChatMessage> messages = chatMessageRepository.findByChatMember_Chat_IdOrderByCreatedAtDesc(conversationId, pageable);
        logger.debug("Fetched {} messages for conversation {}", messages.getTotalElements(), conversationId);

        // Step 2: Batch-load attachments only for this page's message IDs — avoids N+1 and the full-table fetch
        List<Integer> pageIds = messages.getContent().stream().map(ChatMessage::getId).collect(Collectors.toList());
        Map<Integer, ChatMessage> hydratedById = pageIds.isEmpty()
                ? java.util.Collections.emptyMap()
                : chatMessageRepository.findByIdsWithAttachments(pageIds).stream()
                        .collect(Collectors.toMap(ChatMessage::getId, m -> m));

        // Map to DTOs — use hydrated entity (with attachments) when available
        return messages.map(msg -> {
            ChatMessage message = hydratedById.getOrDefault(msg.getId(), msg);
            MessageResponseDTO dto = new MessageResponseDTO();
            try {
                dto.setId(message.getId());
                dto.setConversationId(message.getChatMember().getChat().getId());
                dto.setSenderId(message.getChatMember().getUser().getId());
                if(message.isDeleted()) {
                    dto.setText("This message was deleted");
                } else {
                    dto.setText(message.getContent());
                }
                dto.setTimestamp(message.getCreatedAt().toString());
                dto.setRead(message.getRead());
                dto.setEdited(message.isEdited());
                dto.setDeleted(message.isDeleted());

                if (message.getAttachments() != null && !message.getAttachments().isEmpty()) {
                    List<DocumentDTO> documentDtos = message.getAttachments().stream()
                        .map(attachment -> {
                            Document doc = attachment.getDocument();
                            if (doc != null) {
                                DocumentDTO docDto = new DocumentDTO();
                                docDto.setId(doc.getId());
                                docDto.setFileName(doc.getFileName() != null ? doc.getFileName()
                                        : doc.getPath().contains("-") ? doc.getPath().substring(doc.getPath().indexOf("-") + 1) : doc.getPath());
                                docDto.setFileType(doc.getMimeType() != null ? doc.getMimeType() : "application/octet-stream");
                                try {
                                    docDto.setUrl(fileStorageService.getPresignedUrl(doc.getId()));
                                } catch (Exception e) {
                                    logger.warn("Failed to generate presigned URL for document {} — skipping URL", doc.getId(), e);
                                    docDto.setUrl(null); // Gracefully skip — don't crash the entire message list
                                }
                                return docDto;
                            }
                            return null;
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                    dto.setDocuments(documentDtos);
                }
            } catch (Exception e) {
                logger.error("Error mapping message {} to DTO", message.getId(), e);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error mapping message", e);
            }
            return dto;
        });
    }

    @Override
    @Transactional
    public MessageResponseDTO sendMessage(MessageRequestDTO request, String phone) {
        // 1. Find the sender and their membership (your existing logic is correct)
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
        Chat chat = member.getChat();

        // 2. Check for blocking in private chats (your existing logic is correct)
        if (chat.getChatType() == ChatType.PRIVATE) {
            List<ChatMember> allMembersInChat = chatMemberRepository.findByChat_Id(chat.getId());
            if (allMembersInChat.stream().anyMatch(ChatMember::isBlocked)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot send message; a user in this chat is blocked.");
            }
        }

        // 3. Idempotency check: if messageUuid is provided, check for duplicate
        if (request.getMessageUuid() != null && !request.getMessageUuid().isBlank()) {
            ChatMessage existing = chatMessageRepository.findByMessageUuid(request.getMessageUuid()).orElse(null);
            if (existing != null) {
                logger.info("Duplicate message detected with UUID: {}", request.getMessageUuid());
                MessageResponseDTO dupDto = new MessageResponseDTO();
                dupDto.setId(existing.getId());
                dupDto.setConversationId(existing.getChatMember().getChat().getId());
                dupDto.setSenderId(existing.getChatMember().getUser().getId());
                dupDto.setText(existing.getContent());
                dupDto.setTimestamp(existing.getCreatedAt().toString());
                dupDto.setRead(existing.getRead());
                return dupDto;
            }
        }

        // 4. Create and save the message
        ChatMessage message = new ChatMessage();
        message.setChatMember(member);
        message.setContent(request.getText());
        message.setMessageUuid(request.getMessageUuid());
        message.setRead(false);
        message.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        ChatMessage savedMessage = chatMessageRepository.save(message);

        // Guard: block file/voice uploads in a direct AI chat — Kalori is text-only
        boolean isDirectAiChatEarly = chatMemberRepository.findByChat_Id(chat.getId()).stream()
                .anyMatch(m -> AiUserInitializer.KALORI_PHONE.equals(m.getUser().getPhone()));
        if (isDirectAiChatEarly && chat.getChatType() == ChatType.PRIVATE && request.getDocumentId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Kalori AI only supports text messages. File and voice uploads are not allowed in an AI chat.");
        }

        // 4. Handle attachments (your existing logic is correct)
        List<MessageResponseDTO.DocumentDTO> documentDTOs = new ArrayList<>();
            if (request.getDocumentId() != null) {
            Document document = documentRepository.findById(request.getDocumentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

            ChatMessageAttachment attachment = new ChatMessageAttachment();
            attachment.setChatMessage(savedMessage);
            attachment.setDocument(document);
            chatMessageAttachmentRepository.save(attachment);

            MessageResponseDTO.DocumentDTO docDTO = new MessageResponseDTO.DocumentDTO();
            docDTO.setId(document.getId());
            try {
                docDTO.setUrl(fileStorageService.getPresignedUrl(document.getId()));
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate presigned URL", e);
            }
            docDTO.setFileName(document.getFileName() != null ? document.getFileName()
                    : document.getPath().contains("-") ? document.getPath().substring(document.getPath().indexOf("-") + 1) : document.getPath());
            docDTO.setFileType(document.getMimeType() != null ? document.getMimeType() : "application/octet-stream");
            documentDTOs.add(docDTO);
        }

        // 5. Create the DTO for the real-time broadcast (your existing logic is correct)
        userStatusTracker.updateActivity(phone);
        MessageResponseDTO dto = new MessageResponseDTO();
        dto.setId(savedMessage.getId());
        dto.setConversationId(savedMessage.getChatMember().getChat().getId());
        dto.setSenderId(savedMessage.getChatMember().getUser().getId());
        dto.setText(savedMessage.getContent());
        dto.setTimestamp(savedMessage.getCreatedAt().toString());
        dto.setRead(savedMessage.getRead());
        dto.setDocuments(documentDTOs);

        // 6. Broadcast the message to all ACTIVE subscribers
        messagingTemplate.convertAndSend("/topic/messages-" + chat.getId(), dto);
        chatMetrics.incrementMessagesSent();

        // This can be removed if the WebSocket is the primary delivery mechanism
        // messageProducer.sendMessage(chat.getId(), userId, request.getText(), documentDTOs);


        // --- THIS IS THE CORRECTED NOTIFICATION LOGIC ---
        // 7. Send private notifications to all INACTIVE recipients
        List<ChatMember> allMembers = chatMemberRepository.findByChat_Id(chat.getId());
        ChatMessage latestMessage = savedMessage; // The message we just sent is the latest

        for (ChatMember recipientMember : allMembers) {
            // Don't send a notification to the person who sent the message
            if (recipientMember.getUser().getId().equals(userId)) {
                continue;
            }

            String recipientPhone = recipientMember.getUser().getPhone();
            
            // Check if this specific recipient is active in the chat
            //if (!isUserSubscribed(recipientPhone, chat.getId())) {
                // This recipient is not active, so send them a private notification
                // A proper unread count is per-user, per-chat. Let's calculate it for this recipient.
                long unreadCount = chatMessageRepository.countByChatMember_ChatAndChatMember_UserIdNotAndReadFalse(chat, recipientMember.getUser().getId());

                messagingTemplate.convertAndSendToUser(
                    recipientPhone, // Send the notification to the RECIPIENT
                    "/queue/notifications",
                    Map.of("chatId", chat.getId(),
                        "unreadCount", unreadCount,
                        "latestMessage", latestMessage.getContent(),
                        "timestamp", latestMessage.getCreatedAt().toInstant().toString())
                );
           // }
        }
        // --- END OF CORRECTION ---

        // 8. AI Trigger — async via RabbitMQ
        // Guard: Kalori must not respond to its own messages
        // Guard: skip entirely if there is no text to send (attachment-only or voice-only message)
        String messageText = request.getText();
        boolean hasText = messageText != null && !messageText.isBlank();
        if (!AiUserInitializer.KALORI_PHONE.equals(phone) && hasText) {
            boolean isDirectAiChat = allMembers.stream()
                    .anyMatch(m -> AiUserInitializer.KALORI_PHONE.equals(m.getUser().getPhone()));
            boolean hasMention = aiService.containsKaloriMention(messageText);

            if (isDirectAiChat || hasMention) {
                String prompt = hasMention ? aiService.stripMention(messageText) : messageText;
                aiMessageProducer.publishAiJob(chat.getId(), userId, prompt);
                logger.info("AI job published for conversation {} (direct={}, mention={})", chat.getId(), isDirectAiChat, hasMention);
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
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat not found"));

        // Find all messages in this chat sent by OTHERS that are currently unread
        List<ChatMessage> messagesToMarkAsRead = chatMessageRepository.findByChatMember_ChatAndChatMember_UserIdNotAndReadFalse(chat, userId);

        if (messagesToMarkAsRead.isEmpty()) {
            return; // Nothing to do, exit early
        }

        // --- THIS IS THE FIX ---
        for (ChatMessage message : messagesToMarkAsRead) {
            message.setRead(true);
            ChatMessage savedMessage = chatMessageRepository.save(message);

            // For each message we mark as read, broadcast the updated message object
            // back to the main chat topic. This notifies everyone, including the sender.
            MessageResponseDTO messageDto = new MessageResponseDTO(); // Or use your mapper
            // Manually map the DTO for simplicity
            messageDto.setId(savedMessage.getId());
            messageDto.setConversationId(chatId);
            messageDto.setSenderId(savedMessage.getSenderId());
            messageDto.setText(savedMessage.getContent());
            messageDto.setTimestamp(savedMessage.getCreatedAt().toString());
            messageDto.setRead(savedMessage.getRead()); // This will now be true
            messageDto.setEdited(savedMessage.isEdited());
            messageDto.setDeleted(savedMessage.isDeleted());

            messagingTemplate.convertAndSend("/topic/messages-" + chatId, messageDto);
        }
        
        // Notify the reader's sidebar: unread count is now 0. Must match the frontend subscription path.
        messagingTemplate.convertAndSendToUser(phone, "/queue/read-receipt",
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
        //String destination = "/topic/chat/" + chatId;
        String destination = "/topic/messages-" + chatId;
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

    @Override
    public boolean isAdmin(Integer chatId, Integer userId) {
        ChatMember chatMember = chatMemberRepository.findByChat_IdAndUser_Id(chatId, userId);
        if (chatMember == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not a member of this chat");
        }
        return chatMember.isAdmin();
    }

    // ─── #4 Group rename ─────────────────────────────────────────────────────
    @Override
    @Transactional
    public void renameGroup(String phone, Integer chatId, String newName) {
        User user = userRepository.findByPhone(phone)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Chat chat = chatRepository.findById(chatId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat not found"));
        if (!ChatType.GROUP.equals(chat.getChatType()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a group chat");
        ChatMember member = chatMemberRepository.findByChat_IdAndUser_Id(chatId, user.getId());
        if (member == null || !member.isAdmin())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can rename the group");
        chat.setTitle(newName);
        chatRepository.save(chat);
        // Broadcast so all members update the name in real-time
        messagingTemplate.convertAndSend("/topic/messages-" + chatId,
            Map.of("type", "GROUP_RENAMED", "chatId", chatId, "name", newName));
        logger.info("Group {} renamed to '{}' by {}", chatId, newName, phone);
    }

    // ─── #5 Delete group ─────────────────────────────────────────────────────
    @Override
    @Transactional
    public void deleteGroup(String phone, Integer chatId) {
        User user = userRepository.findByPhone(phone)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Chat chat = chatRepository.findById(chatId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat not found"));
        if (!ChatType.GROUP.equals(chat.getChatType()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a group chat");
        List<ChatMember> members = chatMemberRepository.findByChat_Id(chatId);
        boolean isMember = members.stream().anyMatch(m -> m.getUser().getId().equals(user.getId()));
        if (!isMember) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member");
        if (members.size() > 1)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot delete group while other members exist");
        // Cascade: delete all messages (attachments cascade from ChatMessage entity)
        List<ChatMessage> messages = chatMessageRepository.findByChatMember_Chat_IdOrderByCreatedAtAsc(chatId);
        chatMessageRepository.deleteAll(messages);
        chatMemberRepository.deleteAll(members);
        chatRepository.delete(chat);
        logger.info("Group {} deleted by {}", chatId, phone);
    }

    // ─── #7 Hide conversation for current user ────────────────────────────────
    @Override
    @Transactional
    public void hideConversationForUser(String phone, Integer chatId) {
        User user = userRepository.findByPhone(phone)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        ChatMember member = chatMemberRepository.findByChat_IdAndUser_Id(chatId, user.getId());
        if (member == null)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member of this chat");
        member.setHiddenForUser(true);
        chatMemberRepository.save(member);
        logger.info("Conversation {} hidden for user {}", chatId, phone);
    }
}