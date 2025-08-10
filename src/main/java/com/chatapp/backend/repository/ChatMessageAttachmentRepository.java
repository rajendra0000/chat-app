package com.chatapp.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;


import com.chatapp.backend.model.ChatMessageAttachment;

public interface ChatMessageAttachmentRepository extends JpaRepository<ChatMessageAttachment, Integer> {

}
