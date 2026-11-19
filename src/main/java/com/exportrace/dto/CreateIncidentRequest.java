package com.exportrace.dto;

import com.exportrace.entity.enums.IncidentCategory;
import com.exportrace.entity.enums.IncidentImpact;
import com.exportrace.entity.enums.WorkContinuity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Contrato de entrada para reportar una incidencia local en ExporTrace (Manual o Contextual).
 * No permite ni expone setters para: reporterUserId, reporterEmail, reporterRole,
 * priority, status, syncStatus ni jiraIssueKey (estrictamente controlados en backend).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateIncidentRequest {

    @NotNull(message = "La categoría de la incidencia es obligatoria.")
    private IncidentCategory category;

    @NotBlank(message = "El título o resumen de la incidencia es obligatorio.")
    @Size(min = 5, max = 200, message = "El resumen debe tener entre 5 y 200 caracteres.")
    private String summary;

    @NotBlank(message = "La descripción detallada de la incidencia es obligatoria.")
    @Size(min = 10, max = 4000, message = "La descripción debe tener entre 10 y 4000 caracteres.")
    private String description;

    @NotNull(message = "El nivel de impacto es obligatorio.")
    private IncidentImpact impact;

    @NotNull(message = "Debe indicar si puede continuar con su trabajo (workContinuity).")
    private WorkContinuity workContinuity;

    @Size(max = 60, message = "El código de módulo no debe superar los 60 caracteres.")
    private String module;

    @Size(max = 255, message = "La ruta no debe superar los 255 caracteres.")
    private String route;

    private Long relatedLotId;

    @Size(max = 60, message = "El código de lote no debe superar los 60 caracteres.")
    private String lotCode;

    @Size(max = 30, message = "El origen del reporte no debe superar los 30 caracteres.")
    private String source;

    @Size(max = 200, message = "La acción realizada no debe superar los 200 caracteres.")
    private String actionAttempted;

    private Integer httpStatus;

    @Size(max = 80, message = "El código de error no debe superar los 80 caracteres.")
    private String errorCode;

    @Size(max = 100, message = "El identificador técnico no debe superar los 100 caracteres.")
    private String technicalRequestId;

    @Size(max = 1000, message = "El mensaje de error sanitizado no debe superar los 1000 caracteres.")
    private String sanitizedErrorMessage;

    public CreateIncidentRequest() {}

    public CreateIncidentRequest(IncidentCategory category, String summary, String description,
                                 IncidentImpact impact, WorkContinuity workContinuity,
                                 String module, String route, Long relatedLotId) {
        this.category = category;
        this.summary = summary;
        this.description = description;
        this.impact = impact;
        this.workContinuity = workContinuity;
        this.module = module;
        this.route = route;
        this.relatedLotId = relatedLotId;
    }

    public IncidentCategory getCategory() { return category; }
    public void setCategory(IncidentCategory category) { this.category = category; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public IncidentImpact getImpact() { return impact; }
    public void setImpact(IncidentImpact impact) { this.impact = impact; }

    public WorkContinuity getWorkContinuity() { return workContinuity; }
    public void setWorkContinuity(WorkContinuity workContinuity) { this.workContinuity = workContinuity; }

    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }

    public String getRoute() { return route; }
    public void setRoute(String route) { this.route = route; }

    public Long getRelatedLotId() { return relatedLotId; }
    public void setRelatedLotId(Long relatedLotId) { this.relatedLotId = relatedLotId; }

    public String getLotCode() { return lotCode; }
    public void setLotCode(String lotCode) { this.lotCode = lotCode; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getActionAttempted() { return actionAttempted; }
    public void setActionAttempted(String actionAttempted) { this.actionAttempted = actionAttempted; }

    public Integer getHttpStatus() { return httpStatus; }
    public void setHttpStatus(Integer httpStatus) { this.httpStatus = httpStatus; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public String getTechnicalRequestId() { return technicalRequestId; }
    public void setTechnicalRequestId(String technicalRequestId) { this.technicalRequestId = technicalRequestId; }

    public String getSanitizedErrorMessage() { return sanitizedErrorMessage; }
    public void setSanitizedErrorMessage(String sanitizedErrorMessage) { this.sanitizedErrorMessage = sanitizedErrorMessage; }
}
