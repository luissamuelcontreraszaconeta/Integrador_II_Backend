package com.exportrace.dto;

import com.exportrace.entity.Document;

public class LotDocumentDTO {
    private String id;
    private String name;
    private String type;
    private String uploadedAt;
    private String uploadedBy;
    private String fileUrl;
    private String size;
    private boolean required;
    private Integer version = 1;
    private String sha256;
    private Boolean active = true;
    private String mimeType;

    public LotDocumentDTO() {}

    public LotDocumentDTO(Document doc) {
        if (doc != null) {
            this.id = doc.getId() != null ? doc.getId().toString() : "";
            this.name = doc.getNombre();
            this.type = doc.getTipo();
            this.uploadedAt = doc.getFechaSubida() != null ? doc.getFechaSubida().toString() : "";
            this.uploadedBy = doc.getSubidoPor();
            this.fileUrl = doc.getUrl();
            this.version = doc.getVersion() != null ? doc.getVersion() : 1;
            this.sha256 = doc.getSha256();
            this.active = doc.getActive() != null ? doc.getActive() : true;
            this.mimeType = doc.getMimeType();
            if (doc.getFileSize() != null && doc.getFileSize() > 0) {
                this.size = String.format("%.2f MB", doc.getFileSize() / (1024.0 * 1024.0));
            } else {
                this.size = "2.4 MB";
            }
            this.required = true;
        }
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(String uploadedAt) { this.uploadedAt = uploadedAt; }

    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }

    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public String getSha256() { return sha256; }
    public void setSha256(String sha256) { this.sha256 = sha256; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
}
