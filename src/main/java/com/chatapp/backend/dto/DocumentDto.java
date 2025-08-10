package com.chatapp.backend.dto;

import lombok.Data;
import java.sql.Timestamp;

@Data
public class DocumentDto {

    private Integer id;

    private String externalId;

    private String storageEnv;

    private String path;

    private String url;

    private Timestamp createdAt;

    private Timestamp updatedAt;
}
