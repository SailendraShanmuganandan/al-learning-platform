package com.allearning.backend.catalog.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.Locale;
import java.util.UUID;

@Service
public class VideoAttachmentStorageService {

    private static final Logger log = LoggerFactory.getLogger(VideoAttachmentStorageService.class);
    private static final long MAX_FILE_SIZE_BYTES = 25 * 1024 * 1024; // 25 MB

    private final Path rootDir;

    public record StoredPdfInfo(
            String relativePath,
            String originalFilename,
            long fileSize
    ) {}

    public VideoAttachmentStorageService(
            @Value("${app.storage.video-notes-dir:uploads/video-notes}") String storageDir
    ) {
        this.rootDir = Paths.get(storageDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootDir);
        } catch (IOException e) {
            log.error("Failed to create video notes upload directory at {}", this.rootDir, e);
            throw new IllegalStateException("Could not initialize video notes storage directory", e);
        }
    }

    public StoredPdfInfo storePdf(Long productId, MultipartFile file, String oldRelativePath) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PDF file cannot be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PDF file exceeds maximum allowed size of 25MB");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            originalFilename = "lecture_notes.pdf";
        } else {
            originalFilename = Paths.get(originalFilename).getFileName().toString();
        }

        if (!originalFilename.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PDF files (.pdf) are allowed");
        }

        // Generate safe internal filename: video_{productId}_{UUID}.pdf
        String secureFilename = "video_" + productId + "_" + UUID.randomUUID() + ".pdf";
        Path destination = this.rootDir.resolve(secureFilename).normalize();

        if (!destination.startsWith(this.rootDir)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file path detected");
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Failed to store PDF file for productId={}", productId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save PDF file");
        }

        // Delete old file if present
        deleteFile(oldRelativePath);

        return new StoredPdfInfo(secureFilename, originalFilename, file.getSize());
    }

    public Resource loadAsResource(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No PDF attachment associated with this video");
        }

        try {
            Path file = this.rootDir.resolve(relativePath).normalize();
            if (!file.startsWith(this.rootDir)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file path");
            }

            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "PDF attachment file not found on disk");
            }
        } catch (MalformedURLException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "PDF file not found");
        }
    }

    public void deleteFile(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }
        try {
            Path file = this.rootDir.resolve(relativePath).normalize();
            if (file.startsWith(this.rootDir) && Files.exists(file)) {
                Files.delete(file);
                log.info("Deleted old video attachment PDF: {}", relativePath);
            }
        } catch (IOException e) {
            log.warn("Could not delete video attachment file: {}", relativePath, e);
        }
    }
}
