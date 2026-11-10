package com.exportrace.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Contrato de entrada para crear una Solicitud de Servicio del Catálogo ExporTrace (6 servicios: SR-INF-01..04, SR-ACC-01..02).
 * No permite ni expone setters para: serviceDeskId, requestTypeId, fieldIds, jiraIssueKey, jiraRequestId,
 * correlationId, requesterId, estado Jira, aprobación ni SLA authority
 * (todos estrictamente determinados y controlados en el backend a partir del JWT y catálogo).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateServiceRequestDTO {

    @NotBlank(message = "El código de servicio del catálogo (serviceCode) es obligatorio.")
    @Size(max = 30, message = "El código de servicio no debe superar los 30 caracteres.")
    private String serviceCode;

    @NotBlank(message = "El resumen de la solicitud es obligatorio.")
    @Size(min = 5, max = 200, message = "El resumen debe tener entre 5 y 200 caracteres.")
    private String summary;

    @Size(max = 60, message = "El código del lote no debe superar los 60 caracteres.")
    private String lotCode;

    @Size(max = 4000, message = "La información requerida no debe superar los 4000 caracteres.")
    private String requestedInformation;

    @Size(max = 4000, message = "El detalle adicional no debe superar los 4000 caracteres.")
    private String additionalDetail;

    // SR-INF-02
    @Size(max = 120)
    private String documentQueryType;

    // SR-INF-03
    @Size(max = 200)
    private String observedField;

    @Size(max = 4000)
    private String problemDescription;

    // SR-INF-04
    @Size(max = 200)
    private String fieldToCorrect;

    @Size(max = 4000)
    private String currentValue;

    @Size(max = 4000)
    private String proposedValue;

    // SR-INF-04 & SR-ACC-01
    @Size(max = 4000)
    private String justification;

    // SR-ACC-01
    @Size(max = 150)
    private String workerName;

    // SR-ACC-02
    @Size(max = 150)
    private String affectedUser;

    // SR-ACC-01 & SR-ACC-02
    @Size(max = 150)
    private String institutionalEmail;

    @Size(max = 100)
    private String area;

    // SR-ACC-01
    @Size(max = 100)
    private String requiredRole;

    // SR-ACC-02
    @Size(max = 100)
    private String accessRequestType;

    @Size(max = 100)
    private String currentRoleField;

    @Size(max = 150)
    private String requiredPermissions;

    @Size(max = 4000)
    private String reason;

    public CreateServiceRequestDTO() {}

    public CreateServiceRequestDTO(String serviceCode, String summary, String lotCode,
                                   String requestedInformation, String additionalDetail) {
        this.serviceCode = serviceCode;
        this.summary = summary;
        this.lotCode = lotCode;
        this.requestedInformation = requestedInformation;
        this.additionalDetail = additionalDetail;
    }

    public String getServiceCode() { return serviceCode; }
    public void setServiceCode(String serviceCode) { this.serviceCode = serviceCode; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getLotCode() { return lotCode; }
    public void setLotCode(String lotCode) { this.lotCode = lotCode; }

    public String getRequestedInformation() { return requestedInformation; }
    public void setRequestedInformation(String requestedInformation) { this.requestedInformation = requestedInformation; }

    public String getAdditionalDetail() { return additionalDetail; }
    public void setAdditionalDetail(String additionalDetail) { this.additionalDetail = additionalDetail; }

    public String getDocumentQueryType() { return documentQueryType; }
    public void setDocumentQueryType(String documentQueryType) { this.documentQueryType = documentQueryType; }

    public String getObservedField() { return observedField; }
    public void setObservedField(String observedField) { this.observedField = observedField; }

    public String getProblemDescription() { return problemDescription; }
    public void setProblemDescription(String problemDescription) { this.problemDescription = problemDescription; }

    public String getFieldToCorrect() { return fieldToCorrect; }
    public void setFieldToCorrect(String fieldToCorrect) { this.fieldToCorrect = fieldToCorrect; }

    public String getCurrentValue() { return currentValue; }
    public void setCurrentValue(String currentValue) { this.currentValue = currentValue; }

    public String getProposedValue() { return proposedValue; }
    public void setProposedValue(String proposedValue) { this.proposedValue = proposedValue; }

    public String getJustification() { return justification; }
    public void setJustification(String justification) { this.justification = justification; }

    public String getWorkerName() { return workerName; }
    public void setWorkerName(String workerName) { this.workerName = workerName; }

    public String getAffectedUser() { return affectedUser; }
    public void setAffectedUser(String affectedUser) { this.affectedUser = affectedUser; }

    public String getInstitutionalEmail() { return institutionalEmail; }
    public void setInstitutionalEmail(String institutionalEmail) { this.institutionalEmail = institutionalEmail; }

    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }

    public String getRequiredRole() { return requiredRole; }
    public void setRequiredRole(String requiredRole) { this.requiredRole = requiredRole; }

    public String getAccessRequestType() { return accessRequestType; }
    public void setAccessRequestType(String accessRequestType) { this.accessRequestType = accessRequestType; }

    public String getCurrentRoleField() { return currentRoleField; }
    public void setCurrentRoleField(String currentRoleField) { this.currentRoleField = currentRoleField; }

    public String getRequiredPermissions() { return requiredPermissions; }
    public void setRequiredPermissions(String requiredPermissions) { this.requiredPermissions = requiredPermissions; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
