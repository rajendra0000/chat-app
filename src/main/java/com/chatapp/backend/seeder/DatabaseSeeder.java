package com.chatapp.backend.seeder;

import com.chatapp.backend.enums.ChatType;
import com.chatapp.backend.model.*;
import com.chatapp.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ChatRepository chatRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final DocumentRepository documentRepository;
    private final ChatMessageAttachmentRepository chatMessageAttachmentRepository;
    private final EntityManager entityManager;

    @Autowired
    public DatabaseSeeder(
            UserRepository userRepository,
            RoleRepository roleRepository,
            ChatRepository chatRepository,
            ChatMemberRepository chatMemberRepository,
            ChatMessageRepository chatMessageRepository,
            DocumentRepository documentRepository,
            ChatMessageAttachmentRepository chatMessageAttachmentRepository,
            EntityManager entityManager) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.chatRepository = chatRepository;
        this.chatMemberRepository = chatMemberRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.documentRepository = documentRepository;
        this.chatMessageAttachmentRepository = chatMessageAttachmentRepository;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Skip seeding if chats already exist
        if (chatRepository.count() > 0) {
            System.out.println("Database already seeded with chats, skipping...");
            return;
        }

        // Step 1: Create or retrieve roles
        Role userRole = roleRepository.findByName("USER").orElseGet(() -> {
            Role newRole = new Role();
            newRole.setName("USER");
            return roleRepository.save(newRole);
        });
        // Merge userRole to ensure it's managed
        userRole = entityManager.merge(userRole);

        Role adminRole = roleRepository.findByName("ADMIN").orElseGet(() -> {
            Role newRole = new Role();
            newRole.setName("ADMIN");
            return roleRepository.save(newRole);
        });
        // Merge adminRole to ensure it's managed
        adminRole = entityManager.merge(adminRole);

        // Step 2: Create users (10 USERs and 1 ADMIN)
        List<String> phones = List.of("9116914178", "7014123863", "1234567890", "0987654321", "5555555555",
                "1112223333", "4445556666", "7778889999", "1011121314", "1516171819",
                "9998887777");
        List<String> names = List.of("Alice Smith", "Bob Johnson", "Charlie Brown", "David Wilson", "Eve Davis",
                "Frank Miller", "Grace Lee", "Hannah Kim", "Ian Clark", "Julia Adams",
                "Admin User");
        List<String> emails = List.of("alice@example.com", "bob@example.com", "charlie@example.com", "david@example.com",
                "eve@example.com", "frank@example.com", "grace@example.com", "hannah@example.com",
                "ian@example.com", "julia@example.com", "admin@example.com");

        List<User> users = new ArrayList<>();
        for (int i = 0; i < phones.size(); i++) {
            String phone = phones.get(i);
            Optional<User> existingUser = userRepository.findByPhone(phone);
            if (existingUser.isEmpty()) {
                User newUser = new User(phone, names.get(i));
                newUser.setEmail(emails.get(i));
                newUser.setStatus("active");
                if (i == phones.size() - 1) { // Last user is ADMIN
                    newUser.setRoles(List.of(adminRole));
                } else { // All others are USER
                    newUser.setRoles(List.of(userRole));
                }
                users.add(userRepository.save(newUser));
            } else {
                users.add(existingUser.get());
            }
        }

        // Step 3: Create private chats (6 pairs)
        List<int[]> privatePairs = List.of(
                new int[]{0, 1},  // Alice & Bob
                new int[]{2, 3},  // Charlie & David
                new int[]{4, 5},  // Eve & Frank
                new int[]{6, 7},  // Grace & Hannah
                new int[]{8, 9},  // Ian & Julia
                new int[]{0, 4}   // Alice & Eve
        );
        for (int[] pair : privatePairs) {
            User userA = users.get(pair[0]);
            User userB = users.get(pair[1]);
            Chat privateChat = new Chat();
            privateChat.setTitle(userA.getName() + " & " + userB.getName());
            privateChat.setChatType(ChatType.PRIVATE);
            privateChat.setInitiatedBy(userA.getId());
            privateChat = chatRepository.save(privateChat);

            // Add chat members using setters
            ChatMember memberA = new ChatMember();
            memberA.setChat(privateChat);
            memberA.setUser(userA);
            memberA.setMessages(new ArrayList<>());

            ChatMember memberB = new ChatMember();
            memberB.setChat(privateChat);
            memberB.setUser(userB);
            memberB.setMessages(new ArrayList<>());

            chatMemberRepository.saveAll(List.of(memberA, memberB));

            // Add 12 messages per private chat
            List<ChatMessage> messages = new ArrayList<>();
            for (int i = 0; i < 12; i++) {
                ChatMember sender = (i % 2 == 0) ? memberA : memberB;
                boolean isEdited = (i == 3 || i == 8);
                boolean isRead = (i < 7);
                ChatMessage message = new ChatMessage();
                message.setContent("Private chat message " + (i + 1));
                message.setIsEdited(isEdited);
                message.setRead(isRead);
                message.setChatMember(sender);
                messages.add(message);
            }
            chatMessageRepository.saveAll(messages);
        }

        // Step 4: Create group chats (4 groups)
        List<int[]> groupMembers = List.of(
                new int[]{0, 1, 2, 3},        // Alice, Bob, Charlie, David
                new int[]{4, 5, 6, 7},        // Eve, Frank, Grace, Hannah
                new int[]{2, 3, 8, 9},        // Charlie, David, Ian, Julia
                new int[]{0, 1, 4, 5, 6, 10}  // Alice, Bob, Eve, Frank, Grace, Admin
        );
        for (int i = 0; i < groupMembers.size(); i++) {
            int[] memberIndices = groupMembers.get(i);
            Chat groupChat = new Chat();
            groupChat.setTitle("Group Chat " + (i + 1));
            groupChat.setChatType(ChatType.GROUP);
            groupChat.setInitiatedBy(users.get(memberIndices[0]).getId());
            groupChat = chatRepository.save(groupChat);

            // Add chat members using setters
            List<ChatMember> chatMembers = new ArrayList<>();
            for (int index : memberIndices) {
                User user = users.get(index);
                ChatMember member = new ChatMember();
                member.setChat(groupChat);
                member.setUser(user);
                member.setMessages(new ArrayList<>());
                chatMembers.add(member);
            }
            chatMemberRepository.saveAll(chatMembers);

            // Add 20 messages per group chat
            List<ChatMessage> messages = new ArrayList<>();
            for (int j = 0; j < 20; j++) {
                ChatMember sender = chatMembers.get(j % chatMembers.size());
                boolean isEdited = (j == 5 || j == 15);
                boolean isRead = (j < 12);
                ChatMessage message = new ChatMessage();
                message.setContent("Group chat " + (i + 1) + " message " + (j + 1));
                message.setIsEdited(isEdited);
                message.setRead(isRead);
                message.setChatMember(sender);
                messages.add(message);
            }
            chatMessageRepository.saveAll(messages);
        }

        // Step 5: Create documents (5 sample files)
        List<Document> documents = new ArrayList<>();
        documents.add(new Document(null, "doc-001", "local", "/files/report.pdf", "http://localhost/files/report.pdf", null, null));
        documents.add(new Document(null, "doc-002", "local", "/files/photo.jpg", "http://localhost/files/photo.jpg", null, null));
        documents.add(new Document(null, "doc-003", "local", "/files/notes.txt", "http://localhost/files/notes.txt", null, null));
        documents.add(new Document(null, "doc-004", "local", "/files/presentation.pptx", "http://localhost/files/presentation.pptx", null, null));
        documents.add(new Document(null, "doc-005", "local", "/files/data.csv", "http://localhost/files/data.csv", null, null));
        documentRepository.saveAll(documents);

        // Step 6: Attach documents to messages
        List<ChatMessage> allMessages = chatMessageRepository.findAll();
        List<ChatMessageAttachment> attachments = new ArrayList<>();
        if (allMessages.size() >= 12) { // First private chat, 4th message
            attachments.add(new ChatMessageAttachment(null, null, null, allMessages.get(3), documents.get(0)));
        }
        if (allMessages.size() >= 24) { // Second private chat, 6th message
            attachments.add(new ChatMessageAttachment(null, null, null, allMessages.get(17), documents.get(1)));
        }
        if (allMessages.size() >= 92) { // First group chat, 8th message
            attachments.add(new ChatMessageAttachment(null, null, null, allMessages.get(79), documents.get(2)));
        }
        if (allMessages.size() >= 132) { // Second group chat, 10th message
            attachments.add(new ChatMessageAttachment(null, null, null, allMessages.get(121), documents.get(3)));
        }
        if (allMessages.size() >= 192) { // Fourth group chat, 12th message
            attachments.add(new ChatMessageAttachment(null, null, null, allMessages.get(183), documents.get(4)));
        }
        chatMessageAttachmentRepository.saveAll(attachments);

        System.out.println("Database seeding completed successfully!");
    }
}