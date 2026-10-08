package com.exportrace.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface FileStorageService {
    
    /**
     * Stored file metadata result
     */
    record StoredFile(
            String originalFileName,
            String storedFileName,
            String relativeStoragePath,
            String mimeType,
            long fileSize,
            String sha256
    ) {}

    /**
     * Stores an evidence image with cryptographic UUID naming, SHA-256 calculation, and magic-bytes validation.
     */
    StoredFile storeEvidence(MultipartFile file, String subDirectory);

    /**
     * Stores a PDF document with %PDF- magic-bytes validation, SHA-256 calculation, and cryptographic UUID naming.
     */
    StoredFile storeDocument(MultipartFile file, String subDirectory);

    /**
     * Legacy helper for backward compatibility
     */
    String storeFile(MultipartFile file, String subDirectory);

    /**
     * Loads a file as a readable Spring Resource
     */
    Resource loadFileAsResource(String relativePath);

    /**
     * Loads a file resource by filename and subfolder
     */
    Resource loadFileAsResource(String fileName, String subDirectory);

    /**
     * Verifies if a stored file matches the expected SHA-256 checksum on disk
     */
    boolean verifyIntegrity(String relativePath, String expectedSha256);

    /**
     * Calculates SHA-256 from an InputStream
     */
    String calculateSha256(InputStream inputStream);

    /**
     * Soft/Physical deletion of file
     */
    void deleteFile(String fileName, String subDirectory);

    /**
     * Deletes file given its relative path
     */
    void deleteFile(String relativePath);
}
