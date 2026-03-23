package com.chatapp.backend.dto;

import lombok.Data;

@Data
public class ChatMemberDTO {

    private Integer id;

    private Integer chatId;

    private Integer userId;

    private String name;

    private String role;

    private boolean blocked;

    private String phone;
}
