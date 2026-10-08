package com.exportrace.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "qa_evidences")
public class QaEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspection_id", nullable = false)
    private QualityInspection inspection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id", nullable = false)
    private Lot lot;

    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    @Column(name = "stored_file_name", nullable = false)
    private String storedFileName;

    @Column(name = "storage_path", nullable = false)
    private String storagePath;

    // Legacy column mappings for backward database compatibility
    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "mime_type", nullable = false)
    private String mimeType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "sha256", length = 64)
    private String sha256;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "uploaded_by", nullable = false)
    private String uploadedBy;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    public QaEvidence() {
        this.uploadedAt = LocalDateTime.now();
        this.active = true;
    }

    public QaEvidence(QualityInspection inspection, Lot lot, String originalFileName, String storedFileName,
                      String storagePath, String mimeType, Long fileSize, String sha256,
                      String description, String uploadedBy) {
        this.inspection = inspection;
        this.lot = lot;
        this.originalFileName = originalFileName;
        this.storedFileName = storedFileName;
        this.storagePath = storagePath;
        this.fileName = originalFileName;
        this.fileUrl = storagePath;
        this.mimeType = mimeType;
        this.fileSize = fileSize;
        this.sha256 = sha256;
        this.description = description;
        this.uploadedBy = uploadedBy;
        this.uploadedAt = LocalDateTime.now();
        this.active = true;
    }

    @PrePersist
    @PreUpdate
    public void syncLegacyFields() {
        if (this.fileName == null) {
            this.fileName = this.originalFileName != null ? this.originalFileName : this.storedFileName;
        }
        if (this.fileUrl == null) {
            this.fileUrl = this.storagePath != null ? this.storagePath : (this.id != null ? "/api/quality/evidence/" + this.id : "");
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public QualityInspection getInspection() { return inspection; }
    public void setInspection(QualityInspection inspection) { this.inspection = inspection; }

    public Lot getLot() { return lot; }
    public void setLot(Lot lot) { this.lot = lot; }

    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
        this.fileName = originalFileName;
    }

    public String getStoredFileName() { return storedFileName; }
    public void setStoredFileName(String storedFileName) { this.storedFileName = storedFileName; }

    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
        this.fileUrl = storagePath;
    }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getSha256() { return sha256; }
    public void setSha256(String sha256) { this.sha256 = sha256; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public boolean isActive() { return Boolean.TRUE.equals(this.active); }

    public String getFileName() { return originalFileName != null ? originalFileName : (fileName != null ? fileName : storedFileName); }
    public void setFileName(String fileName) {
        this.fileName = fileName;
        if (this.originalFileName == null) {
            this.originalFileName = fileName;
        }
    }

    public String getFileUrl() {
        return id != null ? "/api/quality/evidence/" + id : (fileUrl != null ? fileUrl : (storagePath != null ? storagePath : ""));
    }
    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }
}
