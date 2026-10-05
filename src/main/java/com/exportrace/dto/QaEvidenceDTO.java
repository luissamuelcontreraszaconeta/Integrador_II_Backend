package com.exportrace.dto;

import com.exportrace.entity.QaEvidence;
import java.time.LocalDateTime;

public class QaEvidenceDTO {
    private Long id;
    private Long inspectionId;
    private Long lotId;
    private String lotCode;
    private String fileName;
    private String fileUrl;
    private String mimeType;
    private Long fileSize;
    private String description;
    private String uploadedBy;
    private LocalDateTime uploadedAt;

    public QaEvidenceDTO() {}

    public QaEvidenceDTO(QaEvidence e) {
        this.id = e.getId();
        this.inspectionId = e.getInspection() != null ? e.getInspection().getId() : null;
        this.lotId = e.getLot() != null ? e.getLot().getId() : null;
        this.lotCode = e.getLot() != null ? e.getLot().getCodigo() : null;
        this.fileName = e.getFileName();
        this.fileUrl = e.getFileUrl();
        this.mimeType = e.getMimeType();
        this.fileSize = e.getFileSize();
        this.description = e.getDescription();
        this.uploadedBy = e.getUploadedBy();
        this.uploadedAt = e.getUploadedAt();
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

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
}
