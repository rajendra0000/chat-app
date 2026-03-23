package com.chatapp.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String externalId;

    private String storageEnv;

    @Column(name = "path", length = 500)
    private String path;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "file_name", length = 500)
    private String fileName;

    @CreationTimestamp
    private Timestamp createdAt;

    @UpdateTimestamp
    private Timestamp updatedAt;

    // @OneToMany(mappedBy = "document", cascade = CascadeType.ALL)
    // private List<ChatMessageAttachments> attachments;

}
