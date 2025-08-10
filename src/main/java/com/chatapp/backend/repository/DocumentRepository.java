package com.chatapp.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.chatapp.backend.model.Document;

public interface DocumentRepository extends JpaRepository<Document, Integer> {
}
