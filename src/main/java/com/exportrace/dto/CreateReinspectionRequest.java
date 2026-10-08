package com.exportrace.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateReinspectionRequest {

    @NotBlank(message = "El motivo de la reinspección es obligatorio.")
    private String motivoReinspeccion;

    @NotBlank(message = "El resultado organoléptico es obligatorio.")
    private String resultadoOrganoleptico; // CONFORME, OBSERVADO, NO_CONFORME

    private String inspectorName;
    private String appearance;
    private String color;
    private String texture;
    private String smell;
    private String parasiteCheck;
    private String observations;

    public CreateReinspectionRequest() {}

    public CreateReinspectionRequest(String motivoReinspeccion, String resultadoOrganoleptico, String inspectorName) {
        this.motivoReinspeccion = motivoReinspeccion;
        this.resultadoOrganoleptico = resultadoOrganoleptico;
        this.inspectorName = inspectorName;
    }

    public String getMotivoReinspeccion() { return motivoReinspeccion; }
    public void setMotivoReinspeccion(String motivoReinspeccion) { this.motivoReinspeccion = motivoReinspeccion; }

    public String getResultadoOrganoleptico() { return resultadoOrganoleptico; }
    public void setResultadoOrganoleptico(String resultadoOrganoleptico) { this.resultadoOrganoleptico = resultadoOrganoleptico; }

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

    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }
}
