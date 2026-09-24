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

    public LotDocumentDTO() {}

    public LotDocumentDTO(Document doc) {
        if (doc != null) {
            this.id = doc.getId() != null ? doc.getId().toString() : "";
            this.name = doc.getNombre();
            this.type = doc.getTipo();
            this.uploadedAt = doc.getFechaSubida() != null ? doc.getFechaSubida().toString() : "";
            this.uploadedBy = doc.getSubidoPor();
            this.fileUrl = doc.getUrl();
            this.size = "2.4 MB";
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
}
