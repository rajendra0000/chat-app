package com.chatapp.backend.dto;

import lombok.Data;

@Data
public class ChatMemberDto {

    private Integer id;

    private Integer chatId;

    private Integer userId;
}
