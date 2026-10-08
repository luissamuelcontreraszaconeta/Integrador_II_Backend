package com.exportrace.dto;

import com.exportrace.entity.ColdChainIncident;

public class ColdChainIncidentDTO {
    private String id;
    private String lotId;
    private String lotCode;
    private Double temperatureRead;
    private Double temperatureLimit;
    private String conservationType;
    private String status; // ACTIVE, UNDER_REVIEW, RESOLVED
    private String createdAt;
    private String reviewedAt;
    private String resolvedAt;
    private String reviewedBy;
    private String resolvedBy;
    private String technicalJustification;
    private String actionsTaken;
    private String evidenceUrl;
    private String observations;

    public ColdChainIncidentDTO() {}

    public ColdChainIncidentDTO(ColdChainIncident incident) {
        if (incident != null) {
            this.id = incident.getId() != null ? incident.getId().toString() : "";
            this.lotId = incident.getLote() != null && incident.getLote().getId() != null ? incident.getLote().getId().toString() : "";
            this.lotCode = incident.getLote() != null ? incident.getLote().getCodigo() : "";
            this.temperatureRead = incident.getTemperaturaLeida();
            this.temperatureLimit = incident.getTemperaturaLimite();
            this.conservationType = incident.getTipoConservacion();
            this.status = incident.getEstado();
            this.createdAt = incident.getFechaCreacion() != null ? incident.getFechaCreacion().toString() : "";
            this.reviewedAt = incident.getFechaRevision() != null ? incident.getFechaRevision().toString() : null;
            this.resolvedAt = incident.getFechaResolucion() != null ? incident.getFechaResolucion().toString() : null;
            this.reviewedBy = incident.getRevisadoPor();
            this.resolvedBy = incident.getResueltoPor();
            this.technicalJustification = incident.getJustificacionTecnica();
            this.actionsTaken = incident.getAccionesTomadas();
            this.evidenceUrl = incident.getEvidenciaUrl();
            this.observations = incident.getObservaciones();
        }
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getLotId() { return lotId; }
    public void setLotId(String lotId) { this.lotId = lotId; }

    public String getLotCode() { return lotCode; }
    public void setLotCode(String lotCode) { this.lotCode = lotCode; }

    public Double getTemperatureRead() { return temperatureRead; }
    public void setTemperatureRead(Double temperatureRead) { this.temperatureRead = temperatureRead; }

    public Double getTemperatureLimit() { return temperatureLimit; }
    public void setTemperatureLimit(Double temperatureLimit) { this.temperatureLimit = temperatureLimit; }

    public String getConservationType() { return conservationType; }
    public void setConservationType(String conservationType) { this.conservationType = conservationType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(String reviewedAt) { this.reviewedAt = reviewedAt; }

    public String getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(String resolvedAt) { this.resolvedAt = resolvedAt; }

    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }

    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }

    public String getTechnicalJustification() { return technicalJustification; }
    public void setTechnicalJustification(String technicalJustification) { this.technicalJustification = technicalJustification; }

    public String getActionsTaken() { return actionsTaken; }
    public void setActionsTaken(String actionsTaken) { this.actionsTaken = actionsTaken; }

    public String getEvidenceUrl() { return evidenceUrl; }
    public void setEvidenceUrl(String evidenceUrl) { this.evidenceUrl = evidenceUrl; }

    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }
}
