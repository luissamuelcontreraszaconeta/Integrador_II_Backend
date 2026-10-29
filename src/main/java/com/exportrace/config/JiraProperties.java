package com.exportrace.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Propiedades de configuración para la integración REST ExporTrace -> Jira Service Management Cloud (Fase C).
 * Por defecto jira.enabled=false para garantizar que el desarrollo local y la suite de pruebas
 * funcionen sin credenciales externas ni conexión a Internet.
 *
 * IMPORTANTE: Nunca exponer apiToken en logs, DTOs ni respuestas HTTP.
 */
@Component
public class JiraProperties {

    @Value("${jira.enabled:false}")
    private boolean enabled = false;

    @Value("${jira.base-url:}")
    private String baseUrl = "";

    @Value("${jira.service-desk-id:}")
    private String serviceDeskId = "";

    @Value("${jira.request-type-id:}")
    private String requestTypeId = "";

    @Value("${jira.account-email:}")
    private String accountEmail = "";

    @Value("${jira.api-token:}")
    private String apiToken = "";

    @Value("${jira.correlation-field-id:}")
    private String correlationFieldId = "";

    @Value("${jira.exportrace-code-field-id:}")
    private String exportraceCodeFieldId = "";

    @Value("${jira.connect-timeout-seconds:5}")
    private int connectTimeoutSeconds = 5;

    @Value("${jira.read-timeout-seconds:10}")
    private int readTimeoutSeconds = 10;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getServiceDeskId() {
        return serviceDeskId;
    }

    public void setServiceDeskId(String serviceDeskId) {
        this.serviceDeskId = serviceDeskId;
    }

    public String getRequestTypeId() {
        return requestTypeId;
    }

    public void setRequestTypeId(String requestTypeId) {
        this.requestTypeId = requestTypeId;
    }

    public String getAccountEmail() {
        return accountEmail;
    }

    public void setAccountEmail(String accountEmail) {
        this.accountEmail = accountEmail;
    }

    public String getApiToken() {
        return apiToken;
    }

    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }

    public String getCorrelationFieldId() {
        return correlationFieldId;
    }

    public void setCorrelationFieldId(String correlationFieldId) {
        this.correlationFieldId = correlationFieldId;
    }

    public String getExportraceCodeFieldId() {
        return exportraceCodeFieldId;
    }

    public void setExportraceCodeFieldId(String exportraceCodeFieldId) {
        this.exportraceCodeFieldId = exportraceCodeFieldId;
    }

    public int getConnectTimeoutSeconds() {
        return connectTimeoutSeconds;
    }

    public void setConnectTimeoutSeconds(int connectTimeoutSeconds) {
        this.connectTimeoutSeconds = connectTimeoutSeconds;
    }

    public int getReadTimeoutSeconds() {
        return readTimeoutSeconds;
    }

    public void setReadTimeoutSeconds(int readTimeoutSeconds) {
        this.readTimeoutSeconds = readTimeoutSeconds;
    }

    @Override
    public String toString() {
        return "JiraProperties{" +
                "enabled=" + enabled +
                ", baseUrl='" + baseUrl + '\'' +
                ", serviceDeskId='" + serviceDeskId + '\'' +
                ", requestTypeId='" + requestTypeId + '\'' +
                ", accountEmail='" + accountEmail + '\'' +
                ", apiToken='***'" +
                ", correlationFieldId='" + correlationFieldId + '\'' +
                ", exportraceCodeFieldId='" + exportraceCodeFieldId + '\'' +
                ", connectTimeoutSeconds=" + connectTimeoutSeconds +
                ", readTimeoutSeconds=" + readTimeoutSeconds +
                '}';
    }
}
