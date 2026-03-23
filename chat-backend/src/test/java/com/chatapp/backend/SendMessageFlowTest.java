package com.chatapp.backend;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;

import com.chatapp.backend.dto.MessageRequestDTO;
import com.chatapp.backend.dto.MessageResponseDTO;
import com.chatapp.backend.enums.ChatType;
import com.chatapp.backend.model.*;
import com.chatapp.backend.repository.*;
import com.chatapp.backend.service.ChatMetrics;
import com.chatapp.backend.service.FileStorageService;
import com.chatapp.backend.service.GroupPresenceTracker;
import com.chatapp.backend.service.MessageProducer;
import com.chatapp.backend.service.UserStatusTracker;
import com.chatapp.backend.ai.AiMessageProducer;
import com.chatapp.backend.ai.AiService;
import com.chatapp.backend.service.impl.ConversationServiceImpl;

/**
 * Integration-style unit tests for sendMessage flow.
 * Tests the complete message lifecycle: validation → save → broadcast → notification.
 */
@ExtendWith(MockitoExtension.class)
class SendMessageFlowTest {

    @Mock private UserRepository userRepository;
    @Mock private ChatMemberRepository chatMemberRepository;
    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private GroupPresenceTracker groupPresenceTracker;
    @Mock private UserStatusTracker userStatusTracker;
    @Mock private ChatRepository chatRepository;
    @Mock private SimpUserRegistry simpUserRegistry;
    @Mock private MessageProducer messageProducer;
    @Mock private FileStorageService fileStorageService;
    @Mock private DocumentRepository documentRepository;
    @Mock private ChatMessageAttachmentRepository chatMessageAttachmentRepository;
    @Mock private ChatMetrics chatMetrics;
    @Mock private AiMessageProducer aiMessageProducer;
    @Mock private AiService aiService;

    @InjectMocks
    private ConversationServiceImpl conversationService;

    private User sender;
    private User recipient;
    private Chat chat;
    private ChatMember senderMember;
    private ChatMember recipientMember;

    @BeforeEach
    void setUp() {
        sender = new User();
        sender.setId(1);
        sender.setPhone("1234567890");
        sender.setName("Sender");
        sender.setStatus("active");

        recipient = new User();
        recipient.setId(2);
        recipient.setPhone("0987654321");
        recipient.setName("Recipient");
        recipient.setStatus("active");

        chat = new Chat();
        chat.setId(100);
        chat.setChatType(ChatType.PRIVATE);
        chat.setCreatedAt(new Timestamp(System.currentTimeMillis()));

        senderMember = new ChatMember();
        senderMember.setChat(chat);
        senderMember.setUser(sender);
        senderMember.setBlocked(false);

        recipientMember = new ChatMember();
        recipientMember.setChat(chat);
        recipientMember.setUser(recipient);
        recipientMember.setBlocked(false);
    }

    @Test
    @DisplayName("sendMessage: saves message, broadcasts to topic, sends notification, increments metric")
    void sendMessage_fullFlow() {
        when(userRepository.findByPhone("1234567890")).thenReturn(Optional.of(sender));
        when(chatMemberRepository.findByChat_IdAndUser_Id(100, 1)).thenReturn(senderMember);
        when(chatMemberRepository.findByChat_Id(100)).thenReturn(List.of(senderMember, recipientMember));
        when(chatMessageRepository.countByChatMember_ChatAndChatMember_UserIdNotAndReadFalse(chat, 2)).thenReturn(1L);

        ChatMessage savedMessage = new ChatMessage();
        savedMessage.setId(500);
        savedMessage.setChatMember(senderMember);
        savedMessage.setContent("Hello World");
        savedMessage.setRead(false);
        savedMessage.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(savedMessage);

        MessageRequestDTO request = new MessageRequestDTO();
        request.setConversationId(100);
        request.setText("Hello World");

        MessageResponseDTO result = conversationService.sendMessage(request, "1234567890");

        // Verify message saved
        assertNotNull(result);
        assertEquals(500, result.getId());
        assertEquals("Hello World", result.getText());
        assertEquals(100, result.getConversationId());
        assertEquals(1, result.getSenderId());

        // Verify broadcast to chat topic
        verify(messagingTemplate).convertAndSend(eq("/topic/messages-100"), any(MessageResponseDTO.class));

        // Verify notification sent to recipient
        verify(messagingTemplate).convertAndSendToUser(eq("0987654321"), eq("/queue/notifications"), anyMap());

        // Verify metric incremented
        verify(chatMetrics).incrementMessagesSent();

        // Verify user activity updated
        verify(userStatusTracker).updateActivity("1234567890");
    }

    @Test
    @DisplayName("sendMessage: blocked chat prevents message sending")
    void sendMessage_blockedChat_throwsForbidden() {
        recipientMember.setBlocked(true);
        when(userRepository.findByPhone("1234567890")).thenReturn(Optional.of(sender));
        when(chatMemberRepository.findByChat_IdAndUser_Id(100, 1)).thenReturn(senderMember);
        when(chatMemberRepository.findByChat_Id(100)).thenReturn(List.of(senderMember, recipientMember));

        MessageRequestDTO request = new MessageRequestDTO();
        request.setConversationId(100);
        request.setText("Should fail");

        assertThrows(Exception.class, () -> conversationService.sendMessage(request, "1234567890"));
        verify(chatMessageRepository, never()).save(any());
    }

    @Test
    @DisplayName("sendMessage: with attachment saves document reference")
    void sendMessage_withAttachment() throws Exception {
        when(userRepository.findByPhone("1234567890")).thenReturn(Optional.of(sender));
        when(chatMemberRepository.findByChat_IdAndUser_Id(100, 1)).thenReturn(senderMember);
        when(chatMemberRepository.findByChat_Id(100)).thenReturn(List.of(senderMember, recipientMember));
        when(chatMessageRepository.countByChatMember_ChatAndChatMember_UserIdNotAndReadFalse(chat, 2)).thenReturn(1L);

        Document doc = new Document();
        doc.setId(10);
        doc.setPath("abc-test.png");
        when(documentRepository.findById(10)).thenReturn(Optional.of(doc));
        when(fileStorageService.getPresignedUrl(10)).thenReturn("https://minio/test.png");

        ChatMessage savedMessage = new ChatMessage();
        savedMessage.setId(501);
        savedMessage.setChatMember(senderMember);
        savedMessage.setContent("Check this file");
        savedMessage.setRead(false);
        savedMessage.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(savedMessage);

        MessageRequestDTO request = new MessageRequestDTO();
        request.setConversationId(100);
        request.setText("Check this file");
        request.setDocumentId(10);

        MessageResponseDTO result = conversationService.sendMessage(request, "1234567890");

        assertNotNull(result);
        assertNotNull(result.getDocuments());
        assertEquals(1, result.getDocuments().size());
        assertEquals("https://minio/test.png", result.getDocuments().get(0).getUrl());
        verify(chatMessageAttachmentRepository).save(any(ChatMessageAttachment.class));
    }

    @Test
    @DisplayName("sendMessage: non-member gets forbidden")
    void sendMessage_nonMember_throwsForbidden() {
        when(userRepository.findByPhone("1234567890")).thenReturn(Optional.of(sender));
        when(chatMemberRepository.findByChat_IdAndUser_Id(100, 1)).thenReturn(null);

        MessageRequestDTO request = new MessageRequestDTO();
        request.setConversationId(100);
        request.setText("Should fail");

        assertThrows(Exception.class, () -> conversationService.sendMessage(request, "1234567890"));
    }
}
