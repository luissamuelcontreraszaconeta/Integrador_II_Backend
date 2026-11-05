package com.exportrace.service;

import com.exportrace.config.JiraProperties;
import com.exportrace.entity.ServiceRequest;
import com.exportrace.entity.enums.JiraSyncStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
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
 * Cliente de integración con Jira Service Management Cloud REST API para Catálogo de Servicios
 * (Piloto controlado SR-INF-01 — requestTypeId = 10116).
 *
 * Reutiliza las garantías de seguridad, idempotencia (Lookup-Before-Create) y clasificación de errores
 * de JiraIncidentService sin modificar ni afectar la integración de IncidentReport.
 */
@Service
public class JiraServiceRequestService {

    private static final Logger log = LoggerFactory.getLogger(JiraServiceRequestService.class);

    public static final String PILOT_SERVICE_CODE = "SR-INF-01";
    public static final String DEFAULT_SR_INF_01_REQUEST_TYPE_ID = "10116";

    private static final Pattern SAFE_CORRELATION_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9\\-_]{8,64}$");
    private static final Pattern CUSTOM_FIELD_ID_PATTERN = Pattern.compile("^customfield_(\\d+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern AUTH_HEADER_REDACTION_PATTERN = Pattern.compile("(?i)(Basic|Bearer)\\s+[A-Za-z0-9._~+/=-]+");

    private final JiraProperties jiraProperties;
    private final ObjectMapper objectMapper;
    private JiraIncidentService.JiraAuthHeaderProvider authHeaderProvider;
    private RestClient customRestClient;

    @Autowired
    public JiraServiceRequestService(JiraProperties jiraProperties, ObjectMapper objectMapper) {
        this.jiraProperties = jiraProperties;
        this.objectMapper = objectMapper;
        this.authHeaderProvider = this::buildHttpBasicAuthHeader;
    }

    public void setRestClientForTesting(RestClient restClient) {
        this.customRestClient = restClient;
    }

    public void setAuthHeaderProvider(JiraIncidentService.JiraAuthHeaderProvider authHeaderProvider) {
        this.authHeaderProvider = Objects.requireNonNull(authHeaderProvider);
    }

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
     * 1. Validación de configuración para el piloto SR-INF-01.
     */
    public JiraIncidentService.ConfigValidationResult validateConfiguration() {
        if (!jiraProperties.isEnabled()) {
            return JiraIncidentService.ConfigValidationResult.disabled();
        }

        List<String> missing = new ArrayList<>();
        String baseUrl = jiraProperties.getBaseUrl() != null ? jiraProperties.getBaseUrl().trim() : "";
        if (baseUrl.isEmpty() || (!baseUrl.startsWith("https://") && !baseUrl.startsWith("http://"))) {
            missing.add("JIRA_BASE_URL");
        }
        if (isBlank(jiraProperties.getServiceDeskId())) {
            missing.add("JIRA_SERVICE_DESK_ID");
        }
        if (isBlank(jiraProperties.getSrInf01RequestTypeId())) {
            missing.add("JIRA_SR_INF_01_REQUEST_TYPE_ID");
        }
        if (isBlank(jiraProperties.getAccountEmail())) {
            missing.add("JIRA_ACCOUNT_EMAIL");
        }
        if (isBlank(jiraProperties.getApiToken())) {
            missing.add("JIRA_API_TOKEN");
        }
        if (isBlank(jiraProperties.getCorrelationFieldId()) && isBlank(jiraProperties.getSrInf01CorrelationFieldId())) {
            missing.add("JIRA_CORRELATION_FIELD_ID");
        }

        if (!missing.isEmpty()) {
            String msg = "Configuración de Jira Service Management incompleta o inválida para SR-INF-01. Parámetros faltantes: "
                    + String.join(", ", missing);
            return JiraIncidentService.ConfigValidationResult.invalid(msg);
        }

        return JiraIncidentService.ConfigValidationResult.valid();
    }

    /**
     * 2. Consulta la metadata real de campos del Request Type 10116 (SR-INF-01):
     * GET /rest/servicedeskapi/servicedesk/{serviceDeskId}/requesttype/{requestTypeId}/field
     */
    public SrInf01FieldsOutcome getSrInf01RequestTypeFields() {
        JiraIncidentService.ConfigValidationResult configValidation = validateConfiguration();
        if (!configValidation.isValid()) {
            return SrInf01FieldsOutcome.error(
                    JiraSyncStatus.ERROR,
                    configValidation.getErrorMessage(),
                    null
            );
        }

        String baseUrl = normalizeBaseUrl(jiraProperties.getBaseUrl());
        String serviceDeskId = jiraProperties.getServiceDeskId().trim();
        String requestTypeId = resolveSrInf01RequestTypeId();

        String configuredLotField = defaultIfBlank(jiraProperties.getSrInf01LotCodeFieldId(), "customfield_10263");
        String configuredReqInfoField = defaultIfBlank(jiraProperties.getSrInf01RequestedInfoFieldId(), "customfield_10264");
        String configuredAddDetailField = defaultIfBlank(jiraProperties.getSrInf01AdditionalDetailFieldId(), "customfield_10265");
        String configuredSrCorrField = defaultIfBlank(jiraProperties.getSrInf01CorrelationFieldId(), "customfield_10266");
        String globalCorrField = defaultIfBlank(jiraProperties.getCorrelationFieldId(), "customfield_10221");

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

            Set<String> supportedFieldIds = new LinkedHashSet<>();
            Set<String> correlationFieldIds = new LinkedHashSet<>();

            if (fieldsNode.isArray()) {
                for (JsonNode fieldNode : fieldsNode) {
                    String fieldId = fieldNode.path("fieldId").asText("").trim();
                    String fieldName = fieldNode.path("name").asText("").trim();
                    if (!fieldId.isEmpty()) {
                        supportedFieldIds.add(fieldId);
                        if (fieldId.equals(configuredSrCorrField)
                                || fieldId.equals(globalCorrField)
                                || "customfield_10221".equals(fieldId)
                                || "customfield_10266".equals(fieldId)
                                || "ExporTrace Correlation ID".equalsIgnoreCase(fieldName)) {
                            correlationFieldIds.add(fieldId);
                        }
                    }
                }
            }

            if (!supportedFieldIds.contains("summary")) {
                return SrInf01FieldsOutcome.error(
                        JiraSyncStatus.ERROR,
                        "El Request Type de SR-INF-01 (" + requestTypeId + ") no expone el campo obligatorio 'summary'.",
                        null
                );
            }

            if (!supportedFieldIds.contains(configuredLotField)) {
                return SrInf01FieldsOutcome.error(
                        JiraSyncStatus.ERROR,
                        "El Request Type de SR-INF-01 (" + requestTypeId + ") no expone el campo obligatorio de Código del lote (" + configuredLotField + ").",
                        null
                );
            }

            if (!supportedFieldIds.contains(configuredReqInfoField)) {
                return SrInf01FieldsOutcome.error(
                        JiraSyncStatus.ERROR,
                        "El Request Type de SR-INF-01 (" + requestTypeId + ") no expone el campo obligatorio de Información requerida (" + configuredReqInfoField + ").",
                        null
                );
            }

            if (correlationFieldIds.isEmpty()) {
                return SrInf01FieldsOutcome.error(
                        JiraSyncStatus.ERROR,
                        "El Request Type de SR-INF-01 (" + requestTypeId + ") no expone el campo 'ExporTrace Correlation ID'.",
                        null
                );
            }

            boolean supportsAdditionalDetail = supportedFieldIds.contains(configuredAddDetailField);

            SrInf01FieldMetadata metadata = new SrInf01FieldMetadata(
                    supportedFieldIds,
                    configuredLotField,
                    configuredReqInfoField,
                    configuredAddDetailField,
                    supportsAdditionalDetail,
                    correlationFieldIds
            );

            return SrInf01FieldsOutcome.success(metadata);

        } catch (RestClientResponseException ex) {
            JiraClassifiedError classified = classifyHttpError(ex, "consulta de campos del Request Type SR-INF-01 en Jira");
            return SrInf01FieldsOutcome.error(
                    classified.getTargetSyncStatus(),
                    classified.getSanitizedMessage(),
                    classified.getRetryAfterSeconds()
            );
        } catch (RestClientException ex) {
            String sanitized = sanitizeErrorMessage("Error de red/timeout al consultar campos del Request Type SR-INF-01 en Jira: " + ex.getMessage());
            return SrInf01FieldsOutcome.error(JiraSyncStatus.RETRY_PENDING, sanitized, null);
        } catch (Exception ex) {
            String sanitized = sanitizeErrorMessage("Error al procesar metadata del Request Type SR-INF-01 de Jira: " + ex.getMessage());
            return SrInf01FieldsOutcome.error(JiraSyncStatus.ERROR, sanitized, null);
        }
    }

    /**
     * 3. Búsqueda de solicitud en Jira por correlationId (LOOKUP BEFORE CREATE):
     * GET /rest/api/3/search/jql
     */
    public JiraIncidentService.JiraLookupOutcome findByCorrelationId(String correlationId) {
        JiraIncidentService.ConfigValidationResult configValidation = validateConfiguration();
        if (!configValidation.isValid()) {
            return JiraIncidentService.JiraLookupOutcome.failed(
                    JiraSyncStatus.ERROR,
                    configValidation.getErrorMessage(),
                    null
            );
        }

        if (correlationId == null || !SAFE_CORRELATION_ID_PATTERN.matcher(correlationId.trim()).matches()) {
            return JiraIncidentService.JiraLookupOutcome.failed(
                    JiraSyncStatus.ERROR,
                    "El correlationId proporcionado no cumple con el formato UUID seguro requerido para búsqueda JQL.",
                    null
            );
        }

        String safeCorrelationId = escapeJqlLiteral(correlationId.trim());
        String jql = buildCorrelationJql(safeCorrelationId);

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
                return JiraIncidentService.JiraLookupOutcome.notFound();
            }

            List<JiraIncidentService.JiraTicketSummary> matches = new ArrayList<>();
            for (JsonNode issueNode : issuesNode) {
                String key = issueNode.path("key").asText("").trim();
                String id = issueNode.path("id").asText("").trim();
                if (!key.isEmpty()) {
                    matches.add(new JiraIncidentService.JiraTicketSummary(key, id.isEmpty() ? null : id));
                }
            }

            if (matches.isEmpty()) {
                return JiraIncidentService.JiraLookupOutcome.notFound();
            }
            if (matches.size() == 1) {
                JiraIncidentService.JiraTicketSummary match = matches.get(0);
                return JiraIncidentService.JiraLookupOutcome.foundSingle(match.getIssueKey(), match.getRequestId());
            }

            List<String> duplicateKeys = new ArrayList<>();
            for (JiraIncidentService.JiraTicketSummary m : matches) {
                duplicateKeys.add(m.getIssueKey());
            }
            return JiraIncidentService.JiraLookupOutcome.duplicateConflict(duplicateKeys);

        } catch (RestClientResponseException ex) {
            JiraClassifiedError classified = classifyHttpError(ex, "búsqueda JQL por correlationId para SR-INF-01");
            return JiraIncidentService.JiraLookupOutcome.failed(
                    classified.getTargetSyncStatus(),
                    classified.getSanitizedMessage(),
                    classified.getRetryAfterSeconds()
            );
        } catch (RestClientException ex) {
            String sanitized = sanitizeErrorMessage("Error de conexión/timeout en búsqueda JQL por correlationId: " + ex.getMessage());
            return JiraIncidentService.JiraLookupOutcome.failed(JiraSyncStatus.RETRY_PENDING, sanitized, null);
        } catch (Exception ex) {
            String sanitized = sanitizeErrorMessage("Error al interpretar respuesta de búsqueda JQL en Jira: " + ex.getMessage());
            return JiraIncidentService.JiraLookupOutcome.failed(JiraSyncStatus.ERROR, sanitized, null);
        }
    }

    /**
     * 4. Creación de Customer Request para SR-INF-01 en Jira Service Management Cloud:
     * POST /rest/servicedeskapi/request
     */
    public JiraIncidentService.JiraCreateOutcome createSrInf01CustomerRequest(
            ServiceRequest serviceRequest,
            SrInf01FieldMetadata fieldMetadata) {

        JiraIncidentService.ConfigValidationResult configValidation = validateConfiguration();
        if (!configValidation.isValid()) {
            return JiraIncidentService.JiraCreateOutcome.failed(
                    JiraSyncStatus.ERROR,
                    configValidation.getErrorMessage(),
                    null
            );
        }

        String baseUrl = normalizeBaseUrl(jiraProperties.getBaseUrl());
        String serviceDeskId = jiraProperties.getServiceDeskId().trim();
        String requestTypeId = resolveSrInf01RequestTypeId();

        String lotFieldId = fieldMetadata != null
                ? fieldMetadata.getLotCodeFieldId()
                : defaultIfBlank(jiraProperties.getSrInf01LotCodeFieldId(), "customfield_10263");
        String reqInfoFieldId = fieldMetadata != null
                ? fieldMetadata.getRequestedInfoFieldId()
                : defaultIfBlank(jiraProperties.getSrInf01RequestedInfoFieldId(), "customfield_10264");
        String addDetailFieldId = fieldMetadata != null
                ? fieldMetadata.getAdditionalDetailFieldId()
                : defaultIfBlank(jiraProperties.getSrInf01AdditionalDetailFieldId(), "customfield_10265");

        Map<String, Object> requestFieldValues = new LinkedHashMap<>();
        requestFieldValues.put("summary", serviceRequest.getSummary());
        requestFieldValues.put(lotFieldId, serviceRequest.getLotCode());
        requestFieldValues.put(reqInfoFieldId, serviceRequest.getRequestedInformation());

        if (fieldMetadata == null || fieldMetadata.isSupportsAdditionalDetail()) {
            String detailVal = serviceRequest.getAdditionalDetail() != null
                    ? serviceRequest.getAdditionalDetail().trim()
                    : "";
            requestFieldValues.put(addDetailFieldId, detailVal);
        }

        if (fieldMetadata != null && !fieldMetadata.getCorrelationFieldIds().isEmpty()) {
            for (String corrFieldId : fieldMetadata.getCorrelationFieldIds()) {
                requestFieldValues.put(corrFieldId, serviceRequest.getCorrelationId());
            }
        } else {
            String fallbackCorrField = defaultIfBlank(
                    jiraProperties.getSrInf01CorrelationFieldId(),
                    defaultIfBlank(jiraProperties.getCorrelationFieldId(), "customfield_10266")
            );
            requestFieldValues.put(fallbackCorrField, serviceRequest.getCorrelationId());
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
                return JiraIncidentService.JiraCreateOutcome.failed(
                        JiraSyncStatus.ERROR,
                        "Jira Service Management respondió sin un 'issueKey' válido al crear la solicitud SR-INF-01.",
                        null
                );
            }

            return JiraIncidentService.JiraCreateOutcome.created(issueKey, issueId.isEmpty() ? null : issueId);

        } catch (RestClientResponseException ex) {
            JiraClassifiedError classified = classifyHttpError(ex, "creación de solicitud SR-INF-01 en Jira");
            return JiraIncidentService.JiraCreateOutcome.failed(
                    classified.getTargetSyncStatus(),
                    classified.getSanitizedMessage(),
                    classified.getRetryAfterSeconds()
            );
        } catch (RestClientException ex) {
            String sanitized = sanitizeErrorMessage("Error de conexión/timeout al crear solicitud SR-INF-01 en Jira: " + ex.getMessage());
            return JiraIncidentService.JiraCreateOutcome.failed(JiraSyncStatus.RETRY_PENDING, sanitized, null);
        } catch (Exception ex) {
            String sanitized = sanitizeErrorMessage("Error inesperado al crear solicitud SR-INF-01 en Jira: " + ex.getMessage());
            return JiraIncidentService.JiraCreateOutcome.failed(JiraSyncStatus.ERROR, sanitized, null);
        }
    }

    public String resolveSrInf01RequestTypeId() {
        return defaultIfBlank(jiraProperties.getSrInf01RequestTypeId(), DEFAULT_SR_INF_01_REQUEST_TYPE_ID);
    }

    private String buildCorrelationJql(String safeCorrelationId) {
        Set<String> jqlRefs = new LinkedHashSet<>();
        String srCorr = jiraProperties.getSrInf01CorrelationFieldId();
        String globalCorr = jiraProperties.getCorrelationFieldId();

        if (!isBlank(srCorr)) {
            jqlRefs.add(toJqlFieldReference(srCorr.trim()));
        }
        if (!isBlank(globalCorr)) {
            jqlRefs.add(toJqlFieldReference(globalCorr.trim()));
        }
        if (jqlRefs.isEmpty()) {
            jqlRefs.add("cf[10266]");
        }

        if (jqlRefs.size() == 1) {
            String ref = jqlRefs.iterator().next();
            return ref + " ~ \"" + safeCorrelationId + "\"";
        }

        List<String> clauses = new ArrayList<>();
        for (String ref : jqlRefs) {
            clauses.add(ref + " ~ \"" + safeCorrelationId + "\"");
        }
        return "(" + String.join(" OR ", clauses) + ")";
    }

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
            String msg = "HTTP 401 No autorizado en Jira Service Management durante " + operationContext + ".";
            return new JiraClassifiedError(JiraSyncStatus.ERROR, msg, null);
        }

        if (rawStatus == 403) {
            String msg = "HTTP 403 Permisos insuficientes en Jira Service Management durante " + operationContext + ".";
            return new JiraClassifiedError(JiraSyncStatus.ERROR, msg, null);
        }

        if (rawStatus == 404) {
            String msg = "HTTP 404 Recurso no encontrado en Jira Service Management durante " + operationContext + ".";
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

    protected String toJqlFieldReference(String fieldId) {
        Matcher matcher = CUSTOM_FIELD_ID_PATTERN.matcher(fieldId);
        if (matcher.matches()) {
            return "cf[" + matcher.group(1) + "]";
        }
        return "\"" + escapeJqlLiteral(fieldId) + "\"";
    }

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

    private String defaultIfBlank(String str, String fallback) {
        return isBlank(str) ? fallback : str.trim();
    }

    public static class SrInf01FieldMetadata {
        private final Set<String> supportedFieldIds;
        private final String lotCodeFieldId;
        private final String requestedInfoFieldId;
        private final String additionalDetailFieldId;
        private final boolean supportsAdditionalDetail;
        private final Set<String> correlationFieldIds;

        public SrInf01FieldMetadata(
                Set<String> supportedFieldIds,
                String lotCodeFieldId,
                String requestedInfoFieldId,
                String additionalDetailFieldId,
                boolean supportsAdditionalDetail,
                Set<String> correlationFieldIds) {
            this.supportedFieldIds = Collections.unmodifiableSet(new LinkedHashSet<>(supportedFieldIds));
            this.lotCodeFieldId = lotCodeFieldId;
            this.requestedInfoFieldId = requestedInfoFieldId;
            this.additionalDetailFieldId = additionalDetailFieldId;
            this.supportsAdditionalDetail = supportsAdditionalDetail;
            this.correlationFieldIds = Collections.unmodifiableSet(new LinkedHashSet<>(correlationFieldIds));
        }

        public Set<String> getSupportedFieldIds() { return supportedFieldIds; }
        public String getLotCodeFieldId() { return lotCodeFieldId; }
        public String getRequestedInfoFieldId() { return requestedInfoFieldId; }
        public String getAdditionalDetailFieldId() { return additionalDetailFieldId; }
        public boolean isSupportsAdditionalDetail() { return supportsAdditionalDetail; }
        public Set<String> getCorrelationFieldIds() { return correlationFieldIds; }
    }

    public static class SrInf01FieldsOutcome {
        private final boolean success;
        private final SrInf01FieldMetadata metadata;
        private final JiraSyncStatus errorSyncStatus;
        private final String sanitizedError;
        private final Long retryAfterSeconds;

        private SrInf01FieldsOutcome(
                boolean success,
                SrInf01FieldMetadata metadata,
                JiraSyncStatus errorSyncStatus,
                String sanitizedError,
                Long retryAfterSeconds) {
            this.success = success;
            this.metadata = metadata;
            this.errorSyncStatus = errorSyncStatus;
            this.sanitizedError = sanitizedError;
            this.retryAfterSeconds = retryAfterSeconds;
        }

        public static SrInf01FieldsOutcome success(SrInf01FieldMetadata metadata) {
            return new SrInf01FieldsOutcome(true, metadata, null, null, null);
        }

        public static SrInf01FieldsOutcome error(JiraSyncStatus status, String sanitizedError, Long retryAfterSeconds) {
            return new SrInf01FieldsOutcome(false, null, status, sanitizedError, retryAfterSeconds);
        }

        public boolean isSuccess() { return success; }
        public SrInf01FieldMetadata getMetadata() { return metadata; }
        public JiraSyncStatus getErrorSyncStatus() { return errorSyncStatus; }
        public String getSanitizedError() { return sanitizedError; }
        public Long getRetryAfterSeconds() { return retryAfterSeconds; }
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
