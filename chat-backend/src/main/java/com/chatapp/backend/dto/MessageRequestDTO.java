package com.chatapp.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MessageRequestDTO {

    @NotNull(message = "Conversation ID is required")
    private Integer conversationId;

    @NotBlank(message = "Message text is required")
    @Size(max = 4000, message = "Message must not exceed 4000 characters")
    private String text;

    private Integer documentId;

    @Size(max = 36, message = "Message UUID must not exceed 36 characters")
    private String messageUuid; // Optional: for idempotency
}