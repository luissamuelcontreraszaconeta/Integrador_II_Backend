package com.exportrace.dto;

import com.exportrace.entity.enums.IncidentCategory;
import com.exportrace.entity.enums.IncidentImpact;
import com.exportrace.entity.enums.WorkContinuity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Contrato de entrada para reportar una incidencia local en ExporTrace.
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
}
