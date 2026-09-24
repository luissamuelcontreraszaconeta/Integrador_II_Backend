package com.exportrace.dto;

import com.exportrace.entity.Lot;
import java.util.ArrayList;
import java.util.List;

public class LotDTO {
    private String id;
    private String code;
    private String status;
    private String createdAt;
    private String updatedAt;
    private String createdBy;

    private ProductionInfoDTO production;
    private QAInspectionDTO qa;
    private List<ColdChainRecordDTO> coldChainLogs = new ArrayList<>();
    private List<LotDocumentDTO> documents = new ArrayList<>();
    private String qrToken;

    public LotDTO() {}

    public LotDTO(Lot lot) {
        if (lot != null) {
            this.id = lot.getId() != null ? lot.getId().toString() : "";
            this.code = lot.getCodigo();
            this.status = lot.getEstado();
            this.createdAt = lot.getFechaCreacion() != null ? lot.getFechaCreacion().toString() : "";
            this.updatedAt = lot.getFechaActualizacion() != null ? lot.getFechaActualizacion().toString() : "";
            this.createdBy = lot.getInspeccionadoPor() != null ? lot.getInspeccionadoPor() : "Operaciones";
            this.qrToken = lot.getQrToken();
            this.production = new ProductionInfoDTO(lot);
        }
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public ProductionInfoDTO getProduction() { return production; }
    public void setProduction(ProductionInfoDTO production) { this.production = production; }

    public QAInspectionDTO getQa() { return qa; }
    public void setQa(QAInspectionDTO qa) { this.qa = qa; }

    public List<ColdChainRecordDTO> getColdChainLogs() { return coldChainLogs; }
    public void setColdChainLogs(List<ColdChainRecordDTO> coldChainLogs) { this.coldChainLogs = coldChainLogs; }

    public List<LotDocumentDTO> getDocuments() { return documents; }
    public void setDocuments(List<LotDocumentDTO> documents) { this.documents = documents; }

    public String getQrToken() { return qrToken; }
    public void setQrToken(String qrToken) { this.qrToken = qrToken; }
}
