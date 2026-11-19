package com.exportrace.service;

import com.exportrace.config.JiraProperties;
import com.exportrace.entity.IncidentReport;
import com.exportrace.entity.enums.IncidentCategory;
import com.exportrace.entity.enums.IncidentImpact;
import com.exportrace.entity.enums.IncidentPriority;
import com.exportrace.entity.enums.IncidentUrgency;
import com.exportrace.entity.enums.JiraSyncStatus;
import com.exportrace.entity.enums.WorkContinuity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cliente de integración con Jira Service Management Cloud REST API (Fase C).
 *
 * Responsabilidades:
 * 1. Validar configuración de integración Jira.
 * 2. Consultar metadata real del Request Type (/rest/servicedeskapi/servicedesk/{id}/requesttype/{id}/field).
 * 3. Buscar tickets existentes por correlationId mediante JQL sanitizado (Lookup-Before-Create).
 * 4. Crear Customer Request (/rest/servicedeskapi/request) usando cuenta técnica de integración.
 * 5. Recuperar Customer Request (/rest/servicedeskapi/request/{issueIdOrKey}).
 * 6. Clasificar errores en TRANSITORIOS (RETRY_PENDING) vs PERMANENTES (ERROR) y sanitizar mensajes.
 *
 * SEGURIDAD:
 * - Nunca expone JIRA_API_TOKEN ni Authorization header en logs, excepciones ni DTOs.
 * - Diseñado con estrategia de autenticación desacoplada para permitir evolución futura a OAuth 2.0.
 */
@Service
public class JiraIncidentService {

    private static final Logger log = LoggerFactory.getLogger(JiraIncidentService.class);

    private static final Pattern SAFE_CORRELATION_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9\\-_]{8,64}$");
    private static final Pattern CUSTOM_FIELD_ID_PATTERN = Pattern.compile("^customfield_(\\d+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern AUTH_HEADER_REDACTION_PATTERN = Pattern.compile("(?i)(Basic|Bearer)\\s+[A-Za-z0-9._~+/=-]+");

    private final JiraProperties jiraProperties;
    private final ObjectMapper objectMapper;
    private JiraAuthHeaderProvider authHeaderProvider;
    private RestClient customRestClient;

    /**
     * Abstracción del proveedor de cabecera de autenticación.
     * En V1 utiliza HTTP Basic (email + API token de Atlassian).
     * En una evolución futura puede sustituirse por un proveedor OAuth 2.0 (3LO / Client Credentials)
     * sin alterar la lógica de negocio ni las llamadas REST.
     */
    @FunctionalInterface
    public interface JiraAuthHeaderProvider {
        String getAuthorizationHeader();
    }

    @Autowired
    public JiraIncidentService(JiraProperties jiraProperties, ObjectMapper objectMapper) {
        this.jiraProperties = jiraProperties;
        this.objectMapper = objectMapper;
        this.authHeaderProvider = this::buildHttpBasicAuthHeader;
    }

    /**
     * Permite inyectar un RestClient personalizado (ej. vinculado a MockRestServiceServer en pruebas automatizadas)
     * sin requerir conexión a Internet.
     */
    public void setRestClientForTesting(RestClient restClient) {
        this.customRestClient = restClient;
    }

    /**
     * Permite sustituir la estrategia de autenticación (por ejemplo para evolución OAuth 2.0 o pruebas).
     */
    public void setAuthHeaderProvider(JiraAuthHeaderProvider authHeaderProvider) {
        this.authHeaderProvider = Objects.requireNonNull(authHeaderProvider);
    }

    /**
     * Construye el RestClient de Spring Boot 3.2 con timeouts explícitos configurables.
     */
    protected RestClient getRestClient() {
        if (this.customRestClient != null) {
            return this.customRestClient;
        }
        int connectSec = Math.max(1, jiraProperties.getConnectTimeoutSeconds());
        int readSec = Math.max(1, jiraProperties.getReadTimeoutSeconds());

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(connectSec));
        requestFactory.setReadTimeout(Duration.ofSeconds(readSec));

        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    private String buildHttpBasicAuthHeader() {
        String email = jiraProperties.getAccountEmail() != null ? jiraProperties.getAccountEmail().trim() : "";
        String token = jiraProperties.getApiToken() != null ? jiraProperties.getApiToken().trim() : "";
        String credentials = email + ":" + token;
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encoded;
    }

    /**
     * 1. Validación de configuración obligatoria cuando JIRA_ENABLED=true.
     */
    public ConfigValidationResult validateConfiguration() {
        if (!jiraProperties.isEnabled()) {
            return ConfigValidationResult.disabled();
        }

        List<String> missing = new ArrayList<>();
        String baseUrl = jiraProperties.getBaseUrl() != null ? jiraProperties.getBaseUrl().trim() : "";
        if (baseUrl.isEmpty() || (!baseUrl.startsWith("https://") && !baseUrl.startsWith("http://"))) {
            missing.add("JIRA_BASE_URL");
        }
        if (isBlank(jiraProperties.getServiceDeskId())) {
            missing.add("JIRA_SERVICE_DESK_ID");
        }
        if (isBlank(jiraProperties.getRequestTypeId())) {
            missing.add("JIRA_REQUEST_TYPE_ID");
        }
        if (isBlank(jiraProperties.getAccountEmail())) {
            missing.add("JIRA_ACCOUNT_EMAIL");
        }
        if (isBlank(jiraProperties.getApiToken())) {
            missing.add("JIRA_API_TOKEN");
        }
        if (isBlank(jiraProperties.getCorrelationFieldId())) {
            missing.add("JIRA_CORRELATION_FIELD_ID");
        }

        if (!missing.isEmpty()) {
            String msg = "Configuración de Jira Service Management incompleta o inválida. Parámetros faltantes: "
                    + String.join(", ", missing);
            return ConfigValidationResult.invalid(msg);
        }

        return ConfigValidationResult.valid();
    }

    /**
     * 2. Consulta y valida los campos soportados por el Request Type en Jira Service Management:
     * GET /rest/servicedeskapi/servicedesk/{serviceDeskId}/requesttype/{requestTypeId}/field
     */
    public RequestTypeFieldsOutcome getRequestTypeFields() {
        ConfigValidationResult configValidation = validateConfiguration();
        if (!configValidation.isValid()) {
            return RequestTypeFieldsOutcome.error(
                    JiraSyncStatus.ERROR,
                    configValidation.getErrorMessage(),
                    null
            );
        }

        String baseUrl = normalizeBaseUrl(jiraProperties.getBaseUrl());
        String serviceDeskId = jiraProperties.getServiceDeskId().trim();
        String requestTypeId = jiraProperties.getRequestTypeId().trim();
        String correlationFieldId = jiraProperties.getCorrelationFieldId().trim();
        String codeFieldId = jiraProperties.getExportraceCodeFieldId() != null
                ? jiraProperties.getExportraceCodeFieldId().trim()
                : "";

        URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/rest/servicedeskapi/servicedesk/{serviceDeskId}/requesttype/{requestTypeId}/field")
                .buildAndExpand(serviceDeskId, requestTypeId)
                .toUri();

        try {
            String responseBody = getRestClient().get()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, authHeaderProvider.getAuthorizationHeader())
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .header("X-ExperimentalApi", "opt-in")
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody != null ? responseBody : "{}");
            JsonNode fieldsNode = root.path("requestTypeFields");

            Set<String> supportedFieldIds = new HashSet<>();
            if (fieldsNode.isArray()) {
                for (JsonNode fieldNode : fieldsNode) {
                    String fieldId = fieldNode.path("fieldId").asText("").trim();
                    if (!fieldId.isEmpty()) {
                        supportedFieldIds.add(fieldId);
                    }
                }
            }

            if (!supportedFieldIds.contains("summary")) {
                return RequestTypeFieldsOutcome.error(
                        JiraSyncStatus.ERROR,
                        "El Request Type configurado en Jira (" + requestTypeId + ") no soporta el campo obligatorio 'summary'.",
                        null
                );
            }

            if (!supportedFieldIds.contains(correlationFieldId)) {
                return RequestTypeFieldsOutcome.error(
                        JiraSyncStatus.ERROR,
                        "El Request Type configurado en Jira (" + requestTypeId + ") no expone el custom field de correlación requerido (" + correlationFieldId + ").",
                        null
                );
            }

            boolean supportsDescription = supportedFieldIds.contains("description");
            boolean supportsPriority = supportedFieldIds.contains("priority");
            boolean supportsExportraceCodeField = !codeFieldId.isEmpty() && supportedFieldIds.contains(codeFieldId);

            RequestTypeMetadata metadata = new RequestTypeMetadata(
                    supportedFieldIds,
                    supportsDescription,
                    supportsPriority,
                    supportsExportraceCodeField
            );
            return RequestTypeFieldsOutcome.success(metadata);

        } catch (RestClientResponseException ex) {
            return handleRestClientResponseErrorForFields(ex);
        } catch (RestClientException ex) {
            String sanitized = sanitizeErrorMessage("Error de red/timeout al consultar campos del Request Type en Jira: " + ex.getMessage());
            return RequestTypeFieldsOutcome.error(JiraSyncStatus.RETRY_PENDING, sanitized, null);
        } catch (Exception ex) {
            String sanitized = sanitizeErrorMessage("Error al procesar metadata del Request Type de Jira: " + ex.getMessage());
            return RequestTypeFieldsOutcome.error(JiraSyncStatus.ERROR, sanitized, null);
        }
    }

    /**
     * 3. Búsqueda de incidencia en Jira por correlationId (LOOKUP BEFORE CREATE):
     * GET /rest/api/3/search?jql=...
     * Garantiza idempotencia evitando tickets duplicados tras timeouts de red.
     */
    public JiraLookupOutcome findByCorrelationId(String correlationId) {
        ConfigValidationResult configValidation = validateConfiguration();
        if (!configValidation.isValid()) {
            return JiraLookupOutcome.failed(JiraSyncStatus.ERROR, configValidation.getErrorMessage(), null);
        }

        if (correlationId == null || !SAFE_CORRELATION_ID_PATTERN.matcher(correlationId.trim()).matches()) {
            return JiraLookupOutcome.failed(
                    JiraSyncStatus.ERROR,
                    "El correlationId proporcionado no cumple con el formato UUID seguro requerido para búsqueda JQL.",
                    null
            );
        }

        String safeCorrelationId = escapeJqlLiteral(correlationId.trim());
        String correlationFieldId = jiraProperties.getCorrelationFieldId().trim();
        String jqlFieldRef = toJqlFieldReference(correlationFieldId);
        String jql = jqlFieldRef + " ~ \"" + safeCorrelationId + "\"";

        String baseUrl = normalizeBaseUrl(jiraProperties.getBaseUrl());
        URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/rest/api/3/search/jql")
                .queryParam("jql", jql)
                .queryParam("fields", "key,id,summary")
                .queryParam("maxResults", 5)
                .build()
                .encode()
                .toUri();

        try {
            String responseBody = getRestClient().get()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, authHeaderProvider.getAuthorizationHeader())
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody != null ? responseBody : "{}");
            JsonNode issuesNode = root.path("issues");
            if (!issuesNode.isArray() || issuesNode.isEmpty()) {
                return JiraLookupOutcome.notFound();
            }

            List<JiraTicketSummary> matches = new ArrayList<>();
            for (JsonNode issueNode : issuesNode) {
                String key = issueNode.path("key").asText("").trim();
                String id = issueNode.path("id").asText("").trim();
                if (!key.isEmpty()) {
                    matches.add(new JiraTicketSummary(key, id.isEmpty() ? null : id));
                }
            }

            if (matches.isEmpty()) {
                return JiraLookupOutcome.notFound();
            }
            if (matches.size() == 1) {
                JiraTicketSummary match = matches.get(0);
                return JiraLookupOutcome.foundSingle(match.getIssueKey(), match.getRequestId());
            }

            List<String> duplicateKeys = new ArrayList<>();
            for (JiraTicketSummary m : matches) {
                duplicateKeys.add(m.getIssueKey());
            }
            return JiraLookupOutcome.duplicateConflict(duplicateKeys);

        } catch (RestClientResponseException ex) {
            JiraClassifiedError classified = classifyHttpError(ex, "búsqueda JQL por correlationId");
            return JiraLookupOutcome.failed(classified.getTargetSyncStatus(), classified.getSanitizedMessage(), classified.getRetryAfterSeconds());
        } catch (RestClientException ex) {
            String sanitized = sanitizeErrorMessage("Error de conexión/timeout en búsqueda JQL por correlationId: " + ex.getMessage());
            return JiraLookupOutcome.failed(JiraSyncStatus.RETRY_PENDING, sanitized, null);
        } catch (Exception ex) {
            String sanitized = sanitizeErrorMessage("Error al interpretar respuesta de búsqueda JQL en Jira: " + ex.getMessage());
            return JiraLookupOutcome.failed(JiraSyncStatus.ERROR, sanitized, null);
        }
    }

    /**
     * 4. Creación de Customer Request en Jira Service Management Cloud:
     * POST /rest/servicedeskapi/request
     */
    public JiraCreateOutcome createCustomerRequest(IncidentReport incident, RequestTypeMetadata fieldMetadata) {
        ConfigValidationResult configValidation = validateConfiguration();
        if (!configValidation.isValid()) {
            return JiraCreateOutcome.failed(JiraSyncStatus.ERROR, configValidation.getErrorMessage(), null);
        }

        String baseUrl = normalizeBaseUrl(jiraProperties.getBaseUrl());
        String serviceDeskId = jiraProperties.getServiceDeskId().trim();
        String requestTypeId = jiraProperties.getRequestTypeId().trim();
        String correlationFieldId = jiraProperties.getCorrelationFieldId().trim();
        String codeFieldId = jiraProperties.getExportraceCodeFieldId() != null
                ? jiraProperties.getExportraceCodeFieldId().trim()
                : "";

        Map<String, Object> requestFieldValues = new LinkedHashMap<>();
        requestFieldValues.put("summary", incident.getSummary());

        if (fieldMetadata == null || fieldMetadata.isSupportsDescription()) {
            requestFieldValues.put("description", buildStructuredDescription(incident));
        }

        // Clave externa estable para idempotencia
        requestFieldValues.put(correlationFieldId, incident.getCorrelationId());

        // Si existe custom field para el código local INC-2026-XXXXXX y está soportado en el Request Type
        if (!codeFieldId.isEmpty() && fieldMetadata != null && fieldMetadata.isSupportsExportraceCodeField()) {
            requestFieldValues.put(codeFieldId, incident.getCode());
        }

        // Mapear campos reales del Request Type 10083 (Reportar incidencia ExporTrace) si están presentes en el preflight
        Set<String> supportedIds = fieldMetadata != null ? fieldMetadata.getSupportedFieldIds() : Collections.emptySet();
        if (supportedIds.contains("customfield_10276")) {
            requestFieldValues.put("customfield_10276", Map.of("id", mapCategoryToJiraOptionId(incident.getCategory())));
        }
        if (supportedIds.contains("customfield_10277")) {
            requestFieldValues.put("customfield_10277", Map.of("id", mapImpactToJiraOptionId(incident.getImpact())));
        }
        if (supportedIds.contains("customfield_10278")) {
            requestFieldValues.put("customfield_10278", Map.of("id", mapWorkContinuityToJiraOptionId(incident.getWorkContinuity(), incident.getUrgency())));
        }
        if (supportedIds.contains("customfield_10279")) {
            requestFieldValues.put("customfield_10279", Map.of("id", mapModuleToJiraOptionId(incident.getModule(), incident.getCategory())));
        }
        if (supportedIds.contains("customfield_10280") && incident.getRoute() != null && !incident.getRoute().isBlank()) {
            requestFieldValues.put("customfield_10280", incident.getRoute().trim());
        }
        if (supportedIds.contains("customfield_10281")) {
            String lotCodeVal = (incident.getRelatedLot() != null && incident.getRelatedLot().getCodigo() != null)
                    ? incident.getRelatedLot().getCodigo()
                    : incident.getLotCodeSnapshot();
            if (lotCodeVal != null && !lotCodeVal.isBlank()) {
                requestFieldValues.put("customfield_10281", lotCodeVal.trim());
            }
        }
        if (supportedIds.contains("customfield_10282")) {
            String errHttp = buildErrorHttpSummary(incident.getHttpStatus(), incident.getErrorCode());
            if (!errHttp.isEmpty()) {
                requestFieldValues.put("customfield_10282", errHttp);
            }
        }

        // Solo enviar Priority si el Request Type realmente lo expone como campo escribible
        if (fieldMetadata != null && fieldMetadata.isSupportsPriority() && incident.getPriority() != null) {
            requestFieldValues.put("priority", Map.of("name", mapPriorityToJiraName(incident.getPriority())));
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("serviceDeskId", serviceDeskId);
        payload.put("requestTypeId", requestTypeId);
        payload.put("requestFieldValues", requestFieldValues);

        URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/rest/servicedeskapi/request")
                .build()
                .toUri();

        try {
            String responseBody = getRestClient().post()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, authHeaderProvider.getAuthorizationHeader())
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody != null ? responseBody : "{}");
            String issueKey = root.path("issueKey").asText("").trim();
            if (issueKey.isEmpty()) {
                issueKey = root.path("key").asText("").trim();
            }
            String issueId = root.path("issueId").asText("").trim();
            if (issueId.isEmpty()) {
                issueId = root.path("requestId").asText("").trim();
            }

            if (issueKey.isEmpty()) {
                return JiraCreateOutcome.failed(
                        JiraSyncStatus.ERROR,
                        "Jira Service Management respondió sin un 'issueKey' válido al crear la solicitud.",
                        null
                );
            }

            return JiraCreateOutcome.created(issueKey, issueId.isEmpty() ? null : issueId);

        } catch (RestClientResponseException ex) {
            JiraClassifiedError classified = classifyHttpError(ex, "creación de Customer Request en Jira");
            return JiraCreateOutcome.failed(classified.getTargetSyncStatus(), classified.getSanitizedMessage(), classified.getRetryAfterSeconds());
        } catch (RestClientException ex) {
            String sanitized = sanitizeErrorMessage("Error de conexión/timeout al crear solicitud en Jira Service Management: " + ex.getMessage());
            return JiraCreateOutcome.failed(JiraSyncStatus.RETRY_PENDING, sanitized, null);
        } catch (Exception ex) {
            String sanitized = sanitizeErrorMessage("Error inesperado al crear solicitud en Jira Service Management: " + ex.getMessage());
            return JiraCreateOutcome.failed(JiraSyncStatus.ERROR, sanitized, null);
        }
    }

    /**
     * 5. Recuperación de Customer Request por issueKey o issueId:
     * GET /rest/servicedeskapi/request/{issueIdOrKey}
     */
    public Optional<JiraTicketSummary> getCustomerRequest(String issueIdOrKey) {
        ConfigValidationResult configValidation = validateConfiguration();
        if (!configValidation.isValid() || isBlank(issueIdOrKey)) {
            return Optional.empty();
        }

        String baseUrl = normalizeBaseUrl(jiraProperties.getBaseUrl());
        URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/rest/servicedeskapi/request/{issueIdOrKey}")
                .buildAndExpand(issueIdOrKey.trim())
                .toUri();

        try {
            String responseBody = getRestClient().get()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, authHeaderProvider.getAuthorizationHeader())
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody != null ? responseBody : "{}");
            String issueKey = root.path("issueKey").asText("").trim();
            String issueId = root.path("issueId").asText("").trim();
            if (issueKey.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new JiraTicketSummary(issueKey, issueId.isEmpty() ? null : issueId));
        } catch (Exception ex) {
            log.warn("[JiraIncidentService] No fue posible recuperar el Customer Request '{}': {}",
                    issueIdOrKey, sanitizeErrorMessage(ex.getMessage()));
            return Optional.empty();
        }
    }

    /**
     * Construye la descripción estructurada controlada con el contexto operativo de ExporTrace,
     * sin incluir datos sensibles innecesarios.
     */
    public String buildStructuredDescription(IncidentReport incident) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== REPORTE DE INCIDENCIA EXPORTRACE ===\n");
        sb.append("Código ExporTrace: ").append(nullSafe(incident.getCode())).append("\n");
        sb.append("Correlation ID: ").append(nullSafe(incident.getCorrelationId())).append("\n");
        sb.append("Usuario reportante: ").append(nullSafe(incident.getReporterUsername())).append("\n");
        sb.append("Email: ").append(nullSafe(incident.getReporterEmail())).append("\n");
        sb.append("Rol: ").append(nullSafe(incident.getReporterRole())).append("\n");
        if (incident.getCategory() != null) {
            sb.append("Categoría: ").append(incident.getCategory().getLabel())
              .append(" (").append(incident.getCategory().name())
              .append(" / ").append(incident.getCategory().getCatalogCode()).append(")\n");
        }
        if (incident.getImpact() != null) {
            sb.append("Impacto: ").append(incident.getImpact().name()).append("\n");
        }
        if (incident.getUrgency() != null) {
            sb.append("Urgencia: ").append(incident.getUrgency().name()).append("\n");
        }
        if (incident.getPriority() != null) {
            sb.append("Prioridad ExporTrace: ").append(incident.getPriority().name())
              .append(" (").append(incident.getPriority().getLabel()).append(")\n");
        }
        sb.append("Módulo: ").append(nullSafe(incident.getModule())).append("\n");
        sb.append("Ruta: ").append(nullSafe(incident.getRoute())).append("\n");
        sb.append("Origen del reporte: ").append(nullSafe(incident.getSource())).append("\n");
        if (incident.getRelatedLot() != null && incident.getRelatedLot().getCodigo() != null) {
            sb.append("Lote relacionado: ").append(incident.getRelatedLot().getCodigo())
              .append(" (ID ").append(incident.getRelatedLot().getId()).append(")\n");
        } else if (incident.getLotCodeSnapshot() != null && !incident.getLotCodeSnapshot().isBlank()) {
            sb.append("Lote relacionado: ").append(incident.getLotCodeSnapshot().trim()).append("\n");
        }
        if (incident.getActionAttempted() != null && !incident.getActionAttempted().isBlank()) {
            sb.append("Acción que estaba realizando: ").append(incident.getActionAttempted().trim()).append("\n");
        }
        if (incident.getHttpStatus() != null) {
            sb.append("HTTP Status: ").append(incident.getHttpStatus()).append("\n");
        }
        if (incident.getErrorCode() != null && !incident.getErrorCode().isBlank()) {
            sb.append("Código de error: ").append(incident.getErrorCode().trim()).append("\n");
        }
        if (incident.getTechnicalRequestId() != null && !incident.getTechnicalRequestId().isBlank()) {
            sb.append("Correlation/Request ID técnico: ").append(incident.getTechnicalRequestId().trim()).append("\n");
        }
        if (incident.getSanitizedErrorMessage() != null && !incident.getSanitizedErrorMessage().isBlank()) {
            sb.append("Mensaje de error sanitizado: ").append(incident.getSanitizedErrorMessage().trim()).append("\n");
        }
        if (incident.getAttachmentOriginalName() != null && !incident.getAttachmentOriginalName().isBlank()) {
            sb.append("Evidencia adjunta: ").append(incident.getAttachmentOriginalName().trim()).append("\n");
        }
        sb.append("\n--- DESCRIPCIÓN DEL PROBLEMA ---\n");
        sb.append(nullSafe(incident.getDescription()));
        return sb.toString();
    }

    /**
     * Traducción determinística de Categoría hacia validValues reales de customfield_10276 (Request Type 10083):
     * 10079 = Acceso / autenticación
     * 10087 = Permisos / autorización
     * 10088 = Error funcional
     * 10089 = Información incorrecta
     * 10090 = Integración / API
     * 10091 = Base de datos
     * 10092 = Rendimiento
     * 10093 = Infraestructura
     * 10094 = Notificaciones
     * 10095 = Seguridad
     * 10096 = Otro
     */
    public String mapCategoryToJiraOptionId(IncidentCategory category) {
        if (category == null) {
            return "10096";
        }
        return switch (category) {
            case AUTHENTICATION -> "10079";
            case ACCESS_CONTROL -> "10087";
            case LOTS, QUALITY, COLD_CHAIN, DOCUMENTS, CERTIFICATION, TRACEABILITY_QR, LOGISTICS, DASHBOARD -> "10088";
            case NOTIFICATIONS -> "10094";
            case BACKEND_API -> "10090";
            case DATABASE -> "10091";
            case INFRASTRUCTURE -> "10093";
            case OTHER -> "10096";
        };
    }

    /**
     * Traducción determinística de Impacto hacia validValues reales de customfield_10277 (Request Type 10083):
     * 10080 = Alto
     * 10081 = Medio
     * 10082 = Bajo
     */
    public String mapImpactToJiraOptionId(IncidentImpact impact) {
        if (impact == null) {
            return "10081";
        }
        return switch (impact) {
            case HIGH -> "10080";
            case MEDIUM -> "10081";
            case LOW -> "10082";
        };
    }

    /**
     * Traducción determinística de ¿Puede continuar trabajando? hacia validValues reales de customfield_10278 (Request Type 10083):
     * 10083 = No puedo continuar
     * 10084 = Puedo continuar parcialmente
     * 10085 = Sí, puedo continuar
     */
    public String mapWorkContinuityToJiraOptionId(WorkContinuity workContinuity, IncidentUrgency urgency) {
        if (workContinuity != null) {
            return switch (workContinuity) {
                case NO_PUEDE_CONTINUAR -> "10083";
                case CONTINUA_PARCIALMENTE -> "10084";
                case PUEDE_CONTINUAR -> "10085";
            };
        }
        if (urgency != null) {
            return switch (urgency) {
                case HIGH -> "10083";
                case MEDIUM -> "10084";
                case LOW -> "10085";
            };
        }
        return "10084";
    }

    /**
     * Traducción determinística de Módulo afectado hacia validValues reales de customfield_10279 (Request Type 10083):
     * 10086 = Autenticación / Sesiones
     * 10097 = Producción
     * 10098 = Lotes
     * 10099 = Calidad QA
     * 10100 = Cadena de Frío
     * 10101 = Documentos
     * 10102 = Certificación SANIPES
     * 10103 = QR / Trazabilidad
     * 10104 = Logística / Despacho
     * 10105 = Dashboard / KPIs
     * 10106 = Usuarios y Accesos
     * 10107 = Roles y Permisos
     * 10108 = Notificaciones
     * 10109 = Centro de Soporte
     * 10110 = Backend / API
     * 10111 = Base de Datos
     * 10112 = Infraestructura
     * 10113 = Otro
     */
    public String mapModuleToJiraOptionId(String rawModule, IncidentCategory category) {
        String norm = normalizeStatusKey(rawModule);
        if (norm.contains("AUTENTICACION") || norm.contains("SESION") || norm.contains("LOGIN")) return "10086";
        if (norm.contains("PRODUCCION")) return "10097";
        if (norm.contains("LOTE")) return "10098";
        if (norm.contains("FRIO") || norm.contains("COLD")) return "10100";
        if (norm.contains("CALIDAD") || norm.equals("QA") || norm.contains("QUALITY")) return "10099";
        if (norm.contains("DOCUMENT")) return "10101";
        if (norm.contains("CERTIFICA") || norm.contains("SANIPES")) return "10102";
        if (norm.contains("TRAZABILIDAD") || norm.contains("QR")) return "10103";
        if (norm.contains("LOGISTICA") || norm.contains("DESPACHO") || norm.contains("DISPATCH")) return "10104";
        if (norm.contains("DASHBOARD") || norm.contains("GERENCIA") || norm.contains("KPI")) return "10105";
        if (norm.contains("ROL") || norm.contains("PERMISO") || norm.contains("RBAC") || norm.contains("SEGURIDAD")) return "10107";
        if (norm.contains("USUARIO") || norm.contains("ADMIN") || norm.contains("ACCESO")) return "10106";
        if (norm.contains("NOTIFICA")) return "10108";
        if (norm.contains("SOPORTE") || norm.contains("SUPPORT")) return "10109";
        if (norm.contains("BACKEND") || norm.contains("API")) return "10110";
        if (norm.contains("BASE DE DATOS") || norm.contains("DATABASE") || norm.contains("SQLITE")) return "10111";
        if (norm.contains("INFRAESTRUCTURA") || norm.contains("RENDER")) return "10112";

        if (category != null) {
            return switch (category) {
                case AUTHENTICATION -> "10086";
                case ACCESS_CONTROL -> "10107";
                case LOTS -> "10098";
                case QUALITY -> "10099";
                case COLD_CHAIN -> "10100";
                case DOCUMENTS -> "10101";
                case CERTIFICATION -> "10102";
                case TRACEABILITY_QR -> "10103";
                case LOGISTICS -> "10104";
                case DASHBOARD -> "10105";
                case NOTIFICATIONS -> "10108";
                case BACKEND_API -> "10110";
                case DATABASE -> "10111";
                case INFRASTRUCTURE -> "10112";
                case OTHER -> "10113";
            };
        }
        return "10113";
    }

    private String buildErrorHttpSummary(Integer httpStatus, String errorCode) {
        String codePart = (errorCode != null && !errorCode.isBlank()) ? errorCode.trim() : "";
        String httpPart = httpStatus != null ? "HTTP " + httpStatus : "";
        if (!httpPart.isEmpty() && !codePart.isEmpty()) {
            return codePart.toUpperCase(Locale.ROOT).contains(String.valueOf(httpStatus))
                    ? codePart
                    : httpPart + " (" + codePart + ")";
        }
        return !httpPart.isEmpty() ? httpPart : codePart;
    }

    /**
     * Adjunta evidencia o captura al ticket Jira creado usando POST /rest/api/3/issue/{issueKey}/attachments
     * con cabecera X-Atlassian-Token: no-check (fuera de transacción SQLite).
     */
    public boolean uploadAttachmentToJiraIssue(String issueKey, Resource fileResource, String originalFilename) {
        if (isBlank(issueKey) || fileResource == null) {
            return false;
        }
        String baseUrl = normalizeBaseUrl(jiraProperties.getBaseUrl());
        URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/rest/api/3/issue/{issueKey}/attachments")
                .buildAndExpand(issueKey.trim())
                .toUri();

        try {
            byte[] bytes = fileResource.getInputStream().readAllBytes();
            final String safeFilename = (originalFilename != null && !originalFilename.isBlank())
                    ? originalFilename.trim()
                    : "evidencia-incidencia-exportrace.png";

            ByteArrayResource namedResource = new ByteArrayResource(bytes) {
                @Override
                public String getFilename() {
                    return safeFilename;
                }
            };

            MultiValueMap<String, Object> multipartBody = new LinkedMultiValueMap<>();
            HttpHeaders partHeaders = new HttpHeaders();
            partHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            multipartBody.add("file", new HttpEntity<>(namedResource, partHeaders));

            getRestClient().post()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, authHeaderProvider.getAuthorizationHeader())
                    .header("X-Atlassian-Token", "no-check")
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(multipartBody)
                    .retrieve()
                    .body(String.class);

            return true;
        } catch (Exception ex) {
            log.warn("[JiraIncidentService] No se pudo adjuntar evidencia en Jira para {}: {}",
                    issueKey, sanitizeErrorMessage(ex.getMessage()));
            return false;
        }
    }

    private String mapPriorityToJiraName(IncidentPriority priority) {
        return switch (priority) {
            case P1_CRITICAL -> "Highest";
            case P2_HIGH -> "High";
            case P3_MEDIUM -> "Medium";
            case P4_LOW -> "Low";
        };
    }

    private RequestTypeFieldsOutcome handleRestClientResponseErrorForFields(RestClientResponseException ex) {
        JiraClassifiedError classified = classifyHttpError(ex, "consulta de campos del Request Type en Jira");
        return RequestTypeFieldsOutcome.error(
                classified.getTargetSyncStatus(),
                classified.getSanitizedMessage(),
                classified.getRetryAfterSeconds()
        );
    }

    /**
     * Clasifica errores HTTP devueltos por Jira Service Management:
     * - TRANSITORIOS -> RETRY_PENDING: 429, 502, 503, 504 (y otros 5xx)
     * - PERMANENTES / CONFIGURACIÓN -> ERROR: 400, 401, 403, 404, 422 (y otros 4xx)
     */
    protected JiraClassifiedError classifyHttpError(RestClientResponseException ex, String operationContext) {
        HttpStatusCode statusCode = ex.getStatusCode();
        int rawStatus = statusCode.value();
        Long retryAfterSeconds = extractRetryAfterSeconds(ex.getResponseHeaders());

        if (rawStatus == 429) {
            String retryInfo = retryAfterSeconds != null ? " (Retry-After: " + retryAfterSeconds + "s)" : "";
            String msg = sanitizeErrorMessage("HTTP 429 Too Many Requests durante " + operationContext + retryInfo + ". Reintento pendiente.");
            return new JiraClassifiedError(JiraSyncStatus.RETRY_PENDING, msg, retryAfterSeconds);
        }

        if (rawStatus == 502 || rawStatus == 503 || rawStatus == 504 || statusCode.is5xxServerError()) {
            String msg = sanitizeErrorMessage("HTTP " + rawStatus + " indisponibilidad temporal de Jira durante " + operationContext + ".");
            return new JiraClassifiedError(JiraSyncStatus.RETRY_PENDING, msg, retryAfterSeconds);
        }

        if (rawStatus == 401) {
            String msg = "HTTP 401 No autorizado en Jira Service Management durante " + operationContext + ": verifique JIRA_ACCOUNT_EMAIL y JIRA_API_TOKEN.";
            return new JiraClassifiedError(JiraSyncStatus.ERROR, msg, null);
        }

        if (rawStatus == 403) {
            String msg = "HTTP 403 Permisos insuficientes en Jira Service Management durante " + operationContext + ".";
            return new JiraClassifiedError(JiraSyncStatus.ERROR, msg, null);
        }

        if (rawStatus == 404) {
            String msg = "HTTP 404 Recurso no encontrado en Jira Service Management durante " + operationContext + ": verifique JIRA_SERVICE_DESK_ID y JIRA_REQUEST_TYPE_ID.";
            return new JiraClassifiedError(JiraSyncStatus.ERROR, msg, null);
        }

        if (rawStatus == 400 || rawStatus == 422 || statusCode.is4xxClientError()) {
            String msg = sanitizeErrorMessage("HTTP " + rawStatus + " solicitud rechazada por Jira durante " + operationContext + ".");
            return new JiraClassifiedError(JiraSyncStatus.ERROR, msg, null);
        }

        String msg = sanitizeErrorMessage("HTTP " + rawStatus + " error inesperado durante " + operationContext + ".");
        return new JiraClassifiedError(JiraSyncStatus.ERROR, msg, null);
    }

    private Long extractRetryAfterSeconds(HttpHeaders headers) {
        if (headers == null) {
            return null;
        }
        String retryAfter = headers.getFirst(HttpHeaders.RETRY_AFTER);
        if (retryAfter == null || retryAfter.isBlank()) {
            return null;
        }
        try {
            long seconds = Long.parseLong(retryAfter.trim());
            return Math.max(0L, seconds);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /**
     * Sanitiza cualquier texto de error garantizando que NUNCA se filtre el API Token ni cabeceras Authorization.
     */
    public String sanitizeErrorMessage(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return "Error de comunicación con Jira Service Management.";
        }
        String sanitized = rawMessage;
        String apiToken = jiraProperties.getApiToken();
        if (apiToken != null && !apiToken.isBlank()) {
            sanitized = sanitized.replace(apiToken, "[REDACTED]");
            String email = jiraProperties.getAccountEmail() != null ? jiraProperties.getAccountEmail().trim() : "";
            String encoded = Base64.getEncoder().encodeToString((email + ":" + apiToken.trim()).getBytes(StandardCharsets.UTF_8));
            sanitized = sanitized.replace(encoded, "[REDACTED]");
        }
        sanitized = AUTH_HEADER_REDACTION_PATTERN.matcher(sanitized).replaceAll("$1 [REDACTED]");
        sanitized = sanitized.replaceAll("[\\r\\n\\t]+", " ").trim();
        if (sanitized.length() > 280) {
            sanitized = sanitized.substring(0, 280) + "...";
        }
        return sanitized;
    }

    /**
     * Convierte customfield_10050 -> cf[10050] para consultas JQL estándar en Jira Cloud.
     */
    protected String toJqlFieldReference(String fieldId) {
        Matcher matcher = CUSTOM_FIELD_ID_PATTERN.matcher(fieldId);
        if (matcher.matches()) {
            return "cf[" + matcher.group(1) + "]";
        }
        return "\"" + escapeJqlLiteral(fieldId) + "\"";
    }

    /**
     * Escapa caracteres especiales de JQL (comillas y barras invertidas).
     */
    public String escapeJqlLiteral(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String normalizeBaseUrl(String rawUrl) {
        if (rawUrl == null) {
            return "";
        }
        return rawUrl.trim().replaceAll("/+$", "");
    }

    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    private String nullSafe(String str) {
        return str != null ? str : "—";
    }

    /**
     * Mapeo compatible con el workflow actual y el modelo objetivo de Incidencias (Bloque 3.1):
     * - REPORTADA / Tareas por hacer / To Do -> REPORTED
     * - EN TRIAJE / Triaje -> TRIAGE
     * - EN PROGRESO / En curso / In Progress -> IN_PROGRESS
     * - EN ESPERA DEL USUARIO / Esperando por el cliente / Pending -> WAITING_USER
     * - ESCALADA / Escalated -> ESCALATED
     * - RESUELTA / Resolved -> RESOLVED
     * - CERRADA / Finalizada / Closed / Done -> CLOSED
     * - REABIERTA / Reopened -> REOPENED
     * Con respaldo semántico por statusCategory (NEW/To Do, INDETERMINATE/In Progress, DONE/Done).
     */
    public com.exportrace.entity.enums.IncidentStatus mapJiraIncidentStatusToLocal(String rawJiraStatus, String statusCategory) {
        String norm = normalizeStatusKey(rawJiraStatus);

        if (norm.contains("REABIERT") || norm.contains("REOPEN")) {
            return com.exportrace.entity.enums.IncidentStatus.REOPENED;
        }
        if (norm.contains("CERRAD") || norm.contains("FINALIZAD") || norm.equals("CLOSED") || norm.equals("DONE") || norm.contains("CANCELAD")) {
            return com.exportrace.entity.enums.IncidentStatus.CLOSED;
        }
        if (norm.contains("RESUELT") || norm.contains("RESOLVED")) {
            return com.exportrace.entity.enums.IncidentStatus.RESOLVED;
        }
        if (norm.contains("ESCALAD") || norm.contains("ESCALAT")) {
            return com.exportrace.entity.enums.IncidentStatus.ESCALATED;
        }
        if (norm.contains("ESPERA DEL USUARIO") || norm.contains("ESPERANDO POR EL CLIENTE")
                || norm.contains("WAITING FOR CUSTOMER") || norm.contains("WAITING USER") || norm.equals("PENDING")) {
            return com.exportrace.entity.enums.IncidentStatus.WAITING_USER;
        }
        if (norm.contains("TRIAJE") || norm.contains("TRIAGE") || norm.contains("ESPERANDO POR AYUDA") || norm.contains("WAITING FOR SUPPORT")) {
            return com.exportrace.entity.enums.IncidentStatus.TRIAGE;
        }
        if (norm.contains("EN PROGRESO") || norm.contains("EN CURSO") || norm.contains("IN PROGRESS")) {
            return com.exportrace.entity.enums.IncidentStatus.IN_PROGRESS;
        }
        if (norm.contains("REPORTAD") || norm.contains("TAREAS POR HACER") || norm.equals("TO DO") || norm.equals("OPEN")) {
            return com.exportrace.entity.enums.IncidentStatus.REPORTED;
        }

        String catNorm = normalizeStatusKey(statusCategory);
        if ("DONE".equals(catNorm)) {
            return com.exportrace.entity.enums.IncidentStatus.CLOSED;
        }
        if ("INDETERMINATE".equals(catNorm) || "IN PROGRESS".equals(catNorm)) {
            return com.exportrace.entity.enums.IncidentStatus.IN_PROGRESS;
        }
        return com.exportrace.entity.enums.IncidentStatus.REPORTED;
    }

    private String normalizeStatusKey(String input) {
        if (input == null) {
            return "";
        }
        String decomposed = java.text.Normalizer.normalize(input.trim(), java.text.Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{InCombiningDiacriticalMarks}+", "").toUpperCase(Locale.ROOT);
    }

    // =========================================================================
    // Objetos de resultado normalizados
    // =========================================================================

    public static class ConfigValidationResult {
        private final boolean enabled;
        private final boolean valid;
        private final String errorMessage;

        private ConfigValidationResult(boolean enabled, boolean valid, String errorMessage) {
            this.enabled = enabled;
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ConfigValidationResult disabled() {
            return new ConfigValidationResult(false, false, "La integración con Jira Service Management está deshabilitada (JIRA_ENABLED=false).");
        }

        public static ConfigValidationResult valid() {
            return new ConfigValidationResult(true, true, null);
        }

        public static ConfigValidationResult invalid(String errorMessage) {
            return new ConfigValidationResult(true, false, errorMessage);
        }

        public boolean isEnabled() { return enabled; }
        public boolean isValid() { return valid; }
        public String getErrorMessage() { return errorMessage; }
    }

    public static class RequestTypeMetadata {
        private final Set<String> supportedFieldIds;
        private final boolean supportsDescription;
        private final boolean supportsPriority;
        private final boolean supportsExportraceCodeField;

        public RequestTypeMetadata(
                Set<String> supportedFieldIds,
                boolean supportsDescription,
                boolean supportsPriority,
                boolean supportsExportraceCodeField) {
            this.supportedFieldIds = Collections.unmodifiableSet(new HashSet<>(supportedFieldIds));
            this.supportsDescription = supportsDescription;
            this.supportsPriority = supportsPriority;
            this.supportsExportraceCodeField = supportsExportraceCodeField;
        }

        public Set<String> getSupportedFieldIds() { return supportedFieldIds; }
        public boolean isSupportsDescription() { return supportsDescription; }
        public boolean isSupportsPriority() { return supportsPriority; }
        public boolean isSupportsExportraceCodeField() { return supportsExportraceCodeField; }
    }

    public static class RequestTypeFieldsOutcome {
        private final boolean success;
        private final RequestTypeMetadata metadata;
        private final JiraSyncStatus errorSyncStatus;
        private final String sanitizedError;
        private final Long retryAfterSeconds;

        private RequestTypeFieldsOutcome(
                boolean success,
                RequestTypeMetadata metadata,
                JiraSyncStatus errorSyncStatus,
                String sanitizedError,
                Long retryAfterSeconds) {
            this.success = success;
            this.metadata = metadata;
            this.errorSyncStatus = errorSyncStatus;
            this.sanitizedError = sanitizedError;
            this.retryAfterSeconds = retryAfterSeconds;
        }

        public static RequestTypeFieldsOutcome success(RequestTypeMetadata metadata) {
            return new RequestTypeFieldsOutcome(true, metadata, null, null, null);
        }

        public static RequestTypeFieldsOutcome error(JiraSyncStatus status, String sanitizedError, Long retryAfterSeconds) {
            return new RequestTypeFieldsOutcome(false, null, status, sanitizedError, retryAfterSeconds);
        }

        public boolean isSuccess() { return success; }
        public RequestTypeMetadata getMetadata() { return metadata; }
        public JiraSyncStatus getErrorSyncStatus() { return errorSyncStatus; }
        public String getSanitizedError() { return sanitizedError; }
        public Long getRetryAfterSeconds() { return retryAfterSeconds; }
    }

    public enum LookupStatus {
        NOT_FOUND,
        FOUND_SINGLE,
        DUPLICATE_CONFLICT,
        FAILED
    }

    public static class JiraLookupOutcome {
        private final LookupStatus status;
        private final String issueKey;
        private final String requestId;
        private final List<String> duplicateIssueKeys;
        private final JiraSyncStatus errorSyncStatus;
        private final String sanitizedError;
        private final Long retryAfterSeconds;

        private JiraLookupOutcome(
                LookupStatus status,
                String issueKey,
                String requestId,
                List<String> duplicateIssueKeys,
                JiraSyncStatus errorSyncStatus,
                String sanitizedError,
                Long retryAfterSeconds) {
            this.status = status;
            this.issueKey = issueKey;
            this.requestId = requestId;
            this.duplicateIssueKeys = duplicateIssueKeys != null ? List.copyOf(duplicateIssueKeys) : List.of();
            this.errorSyncStatus = errorSyncStatus;
            this.sanitizedError = sanitizedError;
            this.retryAfterSeconds = retryAfterSeconds;
        }

        public static JiraLookupOutcome notFound() {
            return new JiraLookupOutcome(LookupStatus.NOT_FOUND, null, null, List.of(), null, null, null);
        }

        public static JiraLookupOutcome foundSingle(String issueKey, String requestId) {
            return new JiraLookupOutcome(LookupStatus.FOUND_SINGLE, issueKey, requestId, List.of(issueKey), null, null, null);
        }

        public static JiraLookupOutcome duplicateConflict(List<String> duplicateKeys) {
            String msg = "Conflicto de idempotencia: se encontraron múltiples tickets en Jira ("
                    + String.join(", ", duplicateKeys) + ") para el mismo correlationId.";
            return new JiraLookupOutcome(LookupStatus.DUPLICATE_CONFLICT, null, null, duplicateKeys, JiraSyncStatus.ERROR, msg, null);
        }

        public static JiraLookupOutcome failed(JiraSyncStatus errorSyncStatus, String sanitizedError, Long retryAfterSeconds) {
            return new JiraLookupOutcome(LookupStatus.FAILED, null, null, List.of(), errorSyncStatus, sanitizedError, retryAfterSeconds);
        }

        public LookupStatus getStatus() { return status; }
        public String getIssueKey() { return issueKey; }
        public String getRequestId() { return requestId; }
        public List<String> getDuplicateIssueKeys() { return duplicateIssueKeys; }
        public JiraSyncStatus getErrorSyncStatus() { return errorSyncStatus; }
        public String getSanitizedError() { return sanitizedError; }
        public Long getRetryAfterSeconds() { return retryAfterSeconds; }
    }

    public static class JiraCreateOutcome {
        private final boolean created;
        private final String issueKey;
        private final String requestId;
        private final JiraSyncStatus errorSyncStatus;
        private final String sanitizedError;
        private final Long retryAfterSeconds;

        private JiraCreateOutcome(
                boolean created,
                String issueKey,
                String requestId,
                JiraSyncStatus errorSyncStatus,
                String sanitizedError,
                Long retryAfterSeconds) {
            this.created = created;
            this.issueKey = issueKey;
            this.requestId = requestId;
            this.errorSyncStatus = errorSyncStatus;
            this.sanitizedError = sanitizedError;
            this.retryAfterSeconds = retryAfterSeconds;
        }

        public static JiraCreateOutcome created(String issueKey, String requestId) {
            return new JiraCreateOutcome(true, issueKey, requestId, JiraSyncStatus.SYNCED, null, null);
        }

        public static JiraCreateOutcome failed(JiraSyncStatus errorSyncStatus, String sanitizedError, Long retryAfterSeconds) {
            return new JiraCreateOutcome(false, null, null, errorSyncStatus, sanitizedError, retryAfterSeconds);
        }

        public boolean isCreated() { return created; }
        public String getIssueKey() { return issueKey; }
        public String getRequestId() { return requestId; }
        public JiraSyncStatus getErrorSyncStatus() { return errorSyncStatus; }
        public String getSanitizedError() { return sanitizedError; }
        public Long getRetryAfterSeconds() { return retryAfterSeconds; }
    }

    public static class JiraTicketSummary {
        private final String issueKey;
        private final String requestId;

        public JiraTicketSummary(String issueKey, String requestId) {
            this.issueKey = issueKey;
            this.requestId = requestId;
        }

        public String getIssueKey() { return issueKey; }
        public String getRequestId() { return requestId; }
    }

    protected static class JiraClassifiedError {
        private final JiraSyncStatus targetSyncStatus;
        private final String sanitizedMessage;
        private final Long retryAfterSeconds;

        public JiraClassifiedError(JiraSyncStatus targetSyncStatus, String sanitizedMessage, Long retryAfterSeconds) {
            this.targetSyncStatus = targetSyncStatus;
            this.sanitizedMessage = sanitizedMessage;
            this.retryAfterSeconds = retryAfterSeconds;
        }

        public JiraSyncStatus getTargetSyncStatus() { return targetSyncStatus; }
        public String getSanitizedMessage() { return sanitizedMessage; }
        public Long getRetryAfterSeconds() { return retryAfterSeconds; }
    }
}
