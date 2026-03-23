package com.chatapp.backend.service;

import com.chatapp.backend.dto.FileUploadResponseDTO;
import com.chatapp.backend.model.Document;
import com.chatapp.backend.repository.DocumentRepository;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import jakarta.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Set;

@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/svg+xml",
            "video/mp4", "video/webm", "audio/webm", "audio/ogg", "audio/mpeg",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/plain", "text/csv",
            "application/zip"
    );

    @Autowired
    private Cloudinary cloudinary;

    @Autowired
    private DocumentRepository documentRepository;

    /**
     * Maps MIME type to Cloudinary resource_type.
     * Cloudinary uses "image", "video" (also covers audio), or "raw" (for documents).
     */
    private String resolveResourceType(String mimeType) {
        if (mimeType.startsWith("image/")) return "image";
        if (mimeType.startsWith("video/") || mimeType.startsWith("audio/")) return "video";
        return "raw"; // PDF, docs, zip, etc.
    }

    @Transactional
    public FileUploadResponseDTO uploadFile(MultipartFile file) throws Exception {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "File size exceeds maximum allowed size of 10MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "File type not allowed. Allowed types: images, videos, audio, PDF, documents, text, CSV, ZIP");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.length() > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Filename too long or invalid (max 200 characters)");
        }

        String resourceType = resolveResourceType(contentType);

        // Upload to Cloudinary — use "auto" for resource_type detection
        @SuppressWarnings("unchecked")
        Map<String, Object> uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "resource_type", resourceType,
                        "folder", "chat-app",
                        "use_filename", false,
                        "unique_filename", true
                )
        );

        String publicId  = (String) uploadResult.get("public_id");   // stable identifier
        String secureUrl = (String) uploadResult.get("secure_url");   // permanent CDN URL (https)

        // Persist document record
        Document document = new Document();
        document.setExternalId(publicId);
        document.setStorageEnv("cloudinary");
        document.setPath(secureUrl);        // store the permanent URL directly
        document.setMimeType(contentType);
        document.setFileName(originalFilename);
        document = documentRepository.save(document);

        log.info("File uploaded to Cloudinary: {} ({} bytes, type: {}, publicId: {})",
                originalFilename, file.getSize(), contentType, publicId);

        FileUploadResponseDTO response = new FileUploadResponseDTO();
        response.setUrl(secureUrl);
        response.setFileName(originalFilename);
        response.setDocumentId(document.getId());
        return response;
    }

    /**
     * Returns the stored Cloudinary URL for a document.
     * Cloudinary URLs are permanent (no expiry) — no caching or regeneration needed.
     */
    public String getPresignedUrl(Integer documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
        // path column stores the permanent secure_url from Cloudinary
        return document.getPath();
    }
}