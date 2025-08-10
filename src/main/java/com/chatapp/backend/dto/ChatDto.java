package com.chatapp.backend.dto;

import lombok.Data;
import java.sql.Timestamp;
import java.util.List;

import com.chatapp.backend.enums.ChatType;

@Data
public class ChatDto {

    private Integer id;

    private String title;

    private ChatType chatType;

    private Integer initiatedBy;

    private Timestamp createdAt;

    private Timestamp updatedAt;

    private List<ChatMemberDto> members; 
}
