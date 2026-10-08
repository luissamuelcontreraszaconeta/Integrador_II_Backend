package com.exportrace.dto;

import com.exportrace.entity.QaEvidence;
import java.time.LocalDateTime;

public class QaEvidenceDTO {
    private Long id;
    private Long inspectionId;
    private Long lotId;
    private String lotCode;
    private String originalFileName;
    private String fileName;
    private String fileUrl; // Stream route /api/quality/evidence/{id}
    private String mimeType;
    private Long fileSize;
    private String sha256;
    private String description;
    private String uploadedBy;
    private LocalDateTime uploadedAt;
    private Boolean active;

    public QaEvidenceDTO() {}

    public QaEvidenceDTO(QaEvidence e) {
        this.id = e.getId();
        this.inspectionId = e.getInspection() != null ? e.getInspection().getId() : null;
        this.lotId = e.getLot() != null ? e.getLot().getId() : null;
        this.lotCode = e.getLot() != null ? e.getLot().getCodigo() : null;
        this.originalFileName = e.getOriginalFileName();
        this.fileName = e.getOriginalFileName() != null ? e.getOriginalFileName() : e.getStoredFileName();
        this.fileUrl = e.getId() != null ? "/api/quality/evidence/" + e.getId() : "";
        this.mimeType = e.getMimeType();
        this.fileSize = e.getFileSize();
        this.sha256 = e.getSha256();
        this.description = e.getDescription();
        this.uploadedBy = e.getUploadedBy();
        this.uploadedAt = e.getUploadedAt();
        this.active = e.getActive();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getInspectionId() { return inspectionId; }
    public void setInspectionId(Long inspectionId) { this.inspectionId = inspectionId; }

    public Long getLotId() { return lotId; }
    public void setLotId(Long lotId) { this.lotId = lotId; }

    public String getLotCode() { return lotCode; }
    public void setLotCode(String lotCode) { this.lotCode = lotCode; }

    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

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
}
