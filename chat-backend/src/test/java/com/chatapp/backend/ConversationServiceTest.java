package com.chatapp.backend;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;

import com.chatapp.backend.dto.ConversationDTO;
import com.chatapp.backend.dto.MessageRequestDTO;
import com.chatapp.backend.dto.MessageResponseDTO;
import com.chatapp.backend.enums.ChatType;
import com.chatapp.backend.model.*;
import com.chatapp.backend.repository.*;
import com.chatapp.backend.service.FileStorageService;
import com.chatapp.backend.service.GroupPresenceTracker;
import com.chatapp.backend.service.MessageProducer;
import com.chatapp.backend.service.UserStatusTracker;
import com.chatapp.backend.service.ChatMetrics;
import com.chatapp.backend.ai.AiMessageProducer;
import com.chatapp.backend.ai.AiService;
import com.chatapp.backend.service.impl.ConversationServiceImpl;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

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

    private User testUser;
    private User otherUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1);
        testUser.setPhone("1234567890");
        testUser.setName("Test User");
        testUser.setStatus("active");

        otherUser = new User();
        otherUser.setId(2);
        otherUser.setPhone("0987654321");
        otherUser.setName("Other User");
        otherUser.setStatus("active");
    }

    @Test
    void getConversation_returnsConversationList() {
        Chat chat = new Chat();
        chat.setId(1);
        chat.setChatType(ChatType.PRIVATE);
        chat.setCreatedAt(new Timestamp(System.currentTimeMillis()));

        ChatMember member = new ChatMember();
        member.setChat(chat);
        member.setUser(testUser);

        ChatMember otherMember = new ChatMember();
        otherMember.setChat(chat);
        otherMember.setUser(otherUser);

        when(userRepository.findByPhone("1234567890")).thenReturn(Optional.of(testUser));
        when(chatMemberRepository.findByUserId(1)).thenReturn(List.of(member));
        when(chatMemberRepository.findByChat_IdAndUser_IdNot(1, 1)).thenReturn(List.of(otherMember));
        when(chatMemberRepository.findByChat_Id(1)).thenReturn(List.of(member, otherMember));
        when(chatMessageRepository.findFirstByChatMember_ChatOrderByCreatedAtDesc(chat)).thenReturn(null);
        when(chatMessageRepository.countByChatMember_ChatAndChatMember_UserIdNotAndReadFalse(chat, 1)).thenReturn(0L);

        List<ConversationDTO> result = conversationService.getConversation("1234567890");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Other User", result.get(0).getName());
    }

    @Test
    void createConversation_preventsDuplicatePrivateChat() {
        Chat existingChat = new Chat();
        existingChat.setId(10);
        existingChat.setChatType(ChatType.PRIVATE);
        existingChat.setCreatedAt(new Timestamp(System.currentTimeMillis()));

        ChatMember existingMembership = new ChatMember();
        existingMembership.setChat(existingChat);
        existingMembership.setUser(testUser);

        ChatMember existingOtherMembership = new ChatMember();
        existingOtherMembership.setChat(existingChat);
        existingOtherMembership.setUser(otherUser);

        when(userRepository.findByPhone("1234567890")).thenReturn(Optional.of(testUser));
        when(userRepository.findById(2)).thenReturn(Optional.of(otherUser));
        when(chatMemberRepository.findByUserId(1)).thenReturn(List.of(existingMembership));
        when(chatMemberRepository.findByChat_IdAndUser_Id(10, 2)).thenReturn(existingOtherMembership);

        ConversationDTO result = conversationService.createConversation("1234567890", 2);

        assertEquals(10, result.getId());
        verify(chatRepository, never()).save(any(Chat.class));
    }

    @Test
    void createConversation_createsNewChatIfNotExists() {
        when(userRepository.findByPhone("1234567890")).thenReturn(Optional.of(testUser));
        when(userRepository.findById(2)).thenReturn(Optional.of(otherUser));
        when(chatMemberRepository.findByUserId(1)).thenReturn(List.of());

        Chat savedChat = new Chat();
        savedChat.setId(20);
        savedChat.setChatType(ChatType.PRIVATE);
        savedChat.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        when(chatRepository.save(any(Chat.class))).thenReturn(savedChat);

        ConversationDTO result = conversationService.createConversation("1234567890", 2);

        assertEquals(20, result.getId());
        verify(chatRepository).save(any(Chat.class));
        verify(chatMemberRepository, times(2)).save(any(ChatMember.class));
    }

    @Test
    void sendMessage_detectsDuplicateByMessageUuid() {
        ChatMember member = new ChatMember();
        member.setChat(new Chat());
        member.getChat().setId(1);
        member.getChat().setChatType(ChatType.PRIVATE);
        member.setUser(testUser);

        ChatMessage existingMessage = new ChatMessage();
        existingMessage.setId(99);
        existingMessage.setMessageUuid("test-uuid-123");
        existingMessage.setContent("Hello");
        existingMessage.setChatMember(member);
        existingMessage.setRead(false);
        existingMessage.setCreatedAt(new Timestamp(System.currentTimeMillis()));

        when(userRepository.findByPhone("1234567890")).thenReturn(Optional.of(testUser));
        when(chatMemberRepository.findByChat_IdAndUser_Id(1, 1)).thenReturn(member);
        when(chatMessageRepository.findByMessageUuid("test-uuid-123")).thenReturn(Optional.of(existingMessage));

        MessageRequestDTO request = new MessageRequestDTO();
        request.setConversationId(1);
        request.setText("Hello");
        request.setMessageUuid("test-uuid-123");

        MessageResponseDTO result = conversationService.sendMessage(request, "1234567890");

        assertEquals(99, result.getId());
        verify(chatMessageRepository, never()).save(any(ChatMessage.class));
    }
}
