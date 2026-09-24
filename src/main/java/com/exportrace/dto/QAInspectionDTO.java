package com.exportrace.dto;

import com.exportrace.entity.QualityInspection;
import java.util.Collections;
import java.util.List;

public class QAInspectionDTO {
    private String inspectedAt;
    private String inspectorName;
    private String appearance;
    private String color;
    private String texture;
    private String smell;
    private String parasiteCheck;
    private String organolepticResult;
    private String observations;
    private List<String> evidenceUrls;

    public QAInspectionDTO() {}

    public QAInspectionDTO(QualityInspection qi) {
        if (qi != null) {
            this.inspectedAt = qi.getFechaInspeccion() != null ? qi.getFechaInspeccion().toString() : null;
            this.inspectorName = qi.getInspectorNombre();
            this.appearance = qi.getApariencia();
            this.color = qi.getEvaluacionColor();
            this.texture = qi.getTextura();
            this.smell = qi.getOlor();
            this.parasiteCheck = qi.getExamenParasitologico();
            this.organolepticResult = qi.getResultadoOrganoleptico();
            this.observations = qi.getObservaciones();
            this.evidenceUrls = qi.getEvidenciaFotosUrl() != null && !qi.getEvidenciaFotosUrl().isEmpty() ? 
                    List.of(qi.getEvidenciaFotosUrl().split(",")) : Collections.emptyList();
        }
    }

    // Getters and Setters
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

    public List<String> getEvidenceUrls() { return evidenceUrls; }
    public void setEvidenceUrls(List<String> evidenceUrls) { this.evidenceUrls = evidenceUrls; }
}
