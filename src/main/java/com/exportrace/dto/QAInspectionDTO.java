package com.exportrace.dto;

import com.exportrace.entity.QualityInspection;
import java.util.Collections;
import java.util.List;

public class QAInspectionDTO {
    private Long id;
    private Long lotId;
    private Integer inspectionNumber;
    private String inspectedAt;
    private String inspectorName;
    private String appearance;
    private String color;
    private String texture;
    private String smell;
    private String parasiteCheck;
    private String organolepticResult;
    private String observations;
    private String reinspectionReason;
    private String createdAt;
    private String createdBy;
    private List<String> evidenceUrls;

    public QAInspectionDTO() {}

    public QAInspectionDTO(QualityInspection qi) {
        if (qi != null) {
            this.id = qi.getId();
            this.lotId = qi.getLote() != null ? qi.getLote().getId() : null;
            this.inspectionNumber = qi.getNumeroInspeccion() != null ? qi.getNumeroInspeccion() : 1;
            this.inspectedAt = qi.getFechaInspeccion() != null ? qi.getFechaInspeccion().toString() : null;
            this.inspectorName = qi.getInspectorNombre();
            this.appearance = qi.getApariencia();
            this.color = qi.getEvaluacionColor();
            this.texture = qi.getTextura();
            this.smell = qi.getOlor();
            this.parasiteCheck = qi.getExamenParasitologico();
            this.organolepticResult = qi.getResultadoOrganoleptico();
            this.observations = qi.getObservaciones();
            this.reinspectionReason = qi.getMotivoReinspeccion();
            this.createdAt = qi.getFechaCreacion() != null ? qi.getFechaCreacion().toString() : null;
            this.createdBy = qi.getCreadoPor();
            this.evidenceUrls = qi.getEvidenciaFotosUrl() != null && !qi.getEvidenciaFotosUrl().isEmpty() ? 
                    List.of(qi.getEvidenciaFotosUrl().split(",")) : Collections.emptyList();
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getLotId() { return lotId; }
    public void setLotId(Long lotId) { this.lotId = lotId; }

    public Integer getInspectionNumber() { return inspectionNumber; }
    public void setInspectionNumber(Integer inspectionNumber) { this.inspectionNumber = inspectionNumber; }

    public String getInspectedAt() { return inspectedAt; }
    public void setInspectedAt(String inspectedAt) { this.inspectedAt = inspectedAt; }

    public String getInspectorName() { return inspectorName; }
    public void setInspectorName(String inspectorName) { this.inspectorName = inspectorName; }

    public String getAppearance() { return appearance; }
    public void setAppearance(String appearance) { this.appearance = appearance; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getTexture() { return texture; }
    public void setTexture(String texture) { this.texture = texture; }

    public String getSmell() { return smell; }
    public void setSmell(String smell) { this.smell = smell; }

    public String getParasiteCheck() { return parasiteCheck; }
    public void setParasiteCheck(String parasiteCheck) { this.parasiteCheck = parasiteCheck; }

    public String getOrganolepticResult() { return organolepticResult; }
    public void setOrganolepticResult(String organolepticResult) { this.organolepticResult = organolepticResult; }

    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }

    public String getReinspectionReason() { return reinspectionReason; }
    public void setReinspectionReason(String reinspectionReason) { this.reinspectionReason = reinspectionReason; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public List<String> getEvidenceUrls() { return evidenceUrls; }
    public void setEvidenceUrls(List<String> evidenceUrls) { this.evidenceUrls = evidenceUrls; }
}
