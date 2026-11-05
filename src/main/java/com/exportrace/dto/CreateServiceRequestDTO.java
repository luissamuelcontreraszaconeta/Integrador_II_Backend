package com.exportrace.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Contrato de entrada para crear una Solicitud de Servicio del Catálogo ExporTrace (Piloto SR-INF-01).
 * No permite ni expone setters para: priority, jiraIssueKey, correlationId, requestTypeId ni requesterId
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

    @NotBlank(message = "El código del lote es obligatorio.")
    @Size(max = 60, message = "El código del lote no debe superar los 60 caracteres.")
    private String lotCode;

    @NotBlank(message = "La información requerida es obligatoria.")
    @Size(max = 4000, message = "La información requerida no debe superar los 4000 caracteres.")
    private String requestedInformation;

    @Size(max = 4000, message = "El detalle adicional no debe superar los 4000 caracteres.")
    private String additionalDetail;

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
}
