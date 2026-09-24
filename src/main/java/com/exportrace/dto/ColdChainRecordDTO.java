package com.exportrace.dto;

import com.exportrace.entity.ColdChainRecord;

public class ColdChainRecordDTO {
    private String id;
    private String recordedAt;
    private String time;
    private Double temperature;
    private String location;
    private String responsible;
    private String status;
    private String observations;

    public ColdChainRecordDTO() {}

    public ColdChainRecordDTO(ColdChainRecord ccr) {
        if (ccr != null) {
            this.id = ccr.getId() != null ? ccr.getId().toString() : "";
            this.recordedAt = ccr.getFechaHora() != null ? ccr.getFechaHora().toString() : "";
            this.time = ccr.getFechaHora() != null ? ccr.getFechaHora().toLocalTime().toString() : "";
            this.temperature = ccr.getTemperaturaCelsius();
            this.location = ccr.getUbicacionCamara();
            this.responsible = ccr.getResponsableNombre();
            this.status = ccr.getEstadoMedicion();
            this.observations = ccr.getObservaciones();
        }
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRecordedAt() { return recordedAt; }
    public void setRecordedAt(String recordedAt) { this.recordedAt = recordedAt; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getResponsible() { return responsible; }
    public void setResponsible(String responsible) { this.responsible = responsible; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }
}
