package com.exportrace.service;

import com.exportrace.config.JiraProperties;
import com.exportrace.entity.ServiceRequest;
import com.exportrace.entity.enums.JiraSyncStatus;
import com.exportrace.entity.enums.ServiceRequestStatus;
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
import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cliente de integración con Jira Service Management Cloud REST API para los 6 servicios del
 * Catálogo de Servicios de ExporTrace (SR-INF-01..04 y SR-ACC-01..02) y sincronización pull Jira -> ExporTrace (Fase D2 + D3).
 */
@Service
public class JiraServiceRequestService {

    private static final Logger log = LoggerFactory.getLogger(JiraServiceRequestService.class);

    public static final String PILOT_SERVICE_CODE = "SR-INF-01";
    public static final String DEFAULT_SR_INF_01_REQUEST_TYPE_ID = "10116";
    public static final String DEFAULT_SR_INF_02_REQUEST_TYPE_ID = "10119";
    public static final String DEFAULT_SR_INF_03_REQUEST_TYPE_ID = "10120";
    public static final String DEFAULT_SR_INF_04_REQUEST_TYPE_ID = "10121";
    public static final String DEFAULT_SR_ACC_01_REQUEST_TYPE_ID = "10117";
    public static final String DEFAULT_SR_ACC_02_REQUEST_TYPE_ID = "10118";

    private static final Map<String, String> SERVICE_REQUEST_TYPE_MAP = Map.of(
            "SR-INF-01", DEFAULT_SR_INF_01_REQUEST_TYPE_ID,
            "SR-INF-02", DEFAULT_SR_INF_02_REQUEST_TYPE_ID,
            "SR-INF-03", DEFAULT_SR_INF_03_REQUEST_TYPE_ID,
            "SR-INF-04", DEFAULT_SR_INF_04_REQUEST_TYPE_ID,
            "SR-ACC-01", DEFAULT_SR_ACC_01_REQUEST_TYPE_ID,
            "SR-ACC-02", DEFAULT_SR_ACC_02_REQUEST_TYPE_ID
    );

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
     * 1. Validación de configuración para el Catálogo de Servicios.
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
            String msg = "Configuración de Jira Service Management incompleta o inválida. Parámetros faltantes: "
                    + String.join(", ", missing);
            return JiraIncidentService.ConfigValidationResult.invalid(msg);
        }

        return JiraIncidentService.ConfigValidationResult.valid();
    }

    public String resolveRequestTypeId(String serviceCode) {
        if (serviceCode == null) {
            return resolveSrInf01RequestTypeId();
        }
        String normalized = serviceCode.trim().toUpperCase();
        if ("SR-INF-01".equals(normalized)) {
            return resolveSrInf01RequestTypeId();
        }
        return SERVICE_REQUEST_TYPE_MAP.getOrDefault(normalized, resolveSrInf01RequestTypeId());
    }

    public String resolveSrInf01RequestTypeId() {
        return defaultIfBlank(jiraProperties.getSrInf01RequestTypeId(), DEFAULT_SR_INF_01_REQUEST_TYPE_ID);
    }

    /**
     * 2. Consulta la metadata real de campos del Request Type en Jira:
     * GET /rest/servicedeskapi/servicedesk/{serviceDeskId}/requesttype/{requestTypeId}/field
     */
    public RequestTypeFieldsOutcome getRequestTypeFields(String serviceCode) {
        JiraIncidentService.ConfigValidationResult configValidation = validateConfiguration();
        if (!configValidation.isValid()) {
            return RequestTypeFieldsOutcome.error(
                    JiraSyncStatus.ERROR,
                    configValidation.getErrorMessage(),
                    null
            );
        }

        String normalizedCode = serviceCode != null ? serviceCode.trim().toUpperCase() : "SR-INF-01";
        String baseUrl = normalizeBaseUrl(jiraProperties.getBaseUrl());
        String serviceDeskId = jiraProperties.getServiceDeskId().trim();
        String requestTypeId = resolveRequestTypeId(normalizedCode);

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
            Map<String, Map<String, String>> validValuesByFieldId = new LinkedHashMap<>();

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

                        JsonNode vvNode = fieldNode.path("validValues");
                        if (vvNode.isArray() && !vvNode.isEmpty()) {
                            Map<String, String> options = new LinkedHashMap<>();
                            for (JsonNode opt : vvNode) {
                                String val = opt.path("value").asText("").trim();
                                String label = opt.path("label").asText("").trim();
                                if (!val.isEmpty()) {
                                    options.put(val, label);
                                }
                            }
                            validValuesByFieldId.put(fieldId, options);
                        }
                    }
                }
            }

            if (!supportedFieldIds.contains("summary")) {
                return RequestTypeFieldsOutcome.error(
                        JiraSyncStatus.ERROR,
                        "El Request Type de " + normalizedCode + " (" + requestTypeId + ") no expone el campo obligatorio 'summary'.",
                        null
                );
            }

            if (correlationFieldIds.isEmpty()) {
                return RequestTypeFieldsOutcome.error(
                        JiraSyncStatus.ERROR,
                        "El Request Type de " + normalizedCode + " (" + requestTypeId + ") no expone el campo 'ExporTrace Correlation ID'.",
                        null
                );
            }

            RequestTypeFieldMetadata metadata = new RequestTypeFieldMetadata(
                    normalizedCode,
                    requestTypeId,
                    supportedFieldIds,
                    correlationFieldIds,
                    validValuesByFieldId
            );

            return RequestTypeFieldsOutcome.success(metadata);

        } catch (RestClientResponseException ex) {
            JiraClassifiedError classified = classifyHttpError(ex, "consulta de campos del Request Type " + normalizedCode + " en Jira");
            return RequestTypeFieldsOutcome.error(
                    classified.getTargetSyncStatus(),
                    classified.getSanitizedMessage(),
                    classified.getRetryAfterSeconds()
            );
        } catch (RestClientException ex) {
            String sanitized = sanitizeErrorMessage("Error de red/timeout al consultar campos del Request Type " + normalizedCode + " en Jira: " + ex.getMessage());
            return RequestTypeFieldsOutcome.error(JiraSyncStatus.RETRY_PENDING, sanitized, null);
        } catch (Exception ex) {
            String sanitized = sanitizeErrorMessage("Error al procesar metadata del Request Type " + normalizedCode + " de Jira: " + ex.getMessage());
            return RequestTypeFieldsOutcome.error(JiraSyncStatus.ERROR, sanitized, null);
        }
    }

    /**
     * Método compatible con piloto SR-INF-01.
     */
    public SrInf01FieldsOutcome getSrInf01RequestTypeFields() {
        RequestTypeFieldsOutcome outcome = getRequestTypeFields("SR-INF-01");
        if (!outcome.isSuccess()) {
            return SrInf01FieldsOutcome.error(
                    outcome.getErrorSyncStatus(),
                    outcome.getSanitizedError(),
                    outcome.getRetryAfterSeconds()
            );
        }

        RequestTypeFieldMetadata meta = outcome.getMetadata();
        String configuredLotField = defaultIfBlank(jiraProperties.getSrInf01LotCodeFieldId(), "customfield_10263");
        String configuredReqInfoField = defaultIfBlank(jiraProperties.getSrInf01RequestedInfoFieldId(), "customfield_10264");
        String configuredAddDetailField = defaultIfBlank(jiraProperties.getSrInf01AdditionalDetailFieldId(), "customfield_10265");

        if (!meta.getSupportedFieldIds().contains(configuredLotField)) {
            return SrInf01FieldsOutcome.error(
                    JiraSyncStatus.ERROR,
                    "El Request Type de SR-INF-01 (" + meta.getRequestTypeId() + ") no expone el campo obligatorio de Código del lote (" + configuredLotField + ").",
                    null
            );
        }
        if (!meta.getSupportedFieldIds().contains(configuredReqInfoField)) {
            return SrInf01FieldsOutcome.error(
                    JiraSyncStatus.ERROR,
                    "El Request Type de SR-INF-01 (" + meta.getRequestTypeId() + ") no expone el campo obligatorio de Información requerida (" + configuredReqInfoField + ").",
                    null
            );
        }

        boolean supportsAdditionalDetail = meta.getSupportedFieldIds().contains(configuredAddDetailField);
        SrInf01FieldMetadata srMeta = new SrInf01FieldMetadata(
                meta.getSupportedFieldIds(),
                configuredLotField,
                configuredReqInfoField,
                configuredAddDetailField,
                supportsAdditionalDetail,
                meta.getCorrelationFieldIds()
        );
        return SrInf01FieldsOutcome.success(srMeta);
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
            JiraClassifiedError classified = classifyHttpError(ex, "búsqueda JQL por correlationId");
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
     * 4. Creación de Customer Request para cualquiera de los 6 servicios del catálogo:
     * POST /rest/servicedeskapi/request
     */
    public JiraIncidentService.JiraCreateOutcome createCustomerRequest(
            ServiceRequest serviceRequest,
            RequestTypeFieldMetadata metadata) {

        JiraIncidentService.ConfigValidationResult configValidation = validateConfiguration();
        if (!configValidation.isValid()) {
            return JiraIncidentService.JiraCreateOutcome.failed(
                    JiraSyncStatus.ERROR,
                    configValidation.getErrorMessage(),
                    null
            );
        }

        String serviceCode = serviceRequest.getServiceCode() != null
                ? serviceRequest.getServiceCode().trim().toUpperCase()
                : "SR-INF-01";
        String baseUrl = normalizeBaseUrl(jiraProperties.getBaseUrl());
        String serviceDeskId = jiraProperties.getServiceDeskId().trim();
        String requestTypeId = metadata != null ? metadata.getRequestTypeId() : resolveRequestTypeId(serviceCode);

        Map<String, Object> requestFieldValues;
        try {
            requestFieldValues = buildRequestFieldValuesForService(serviceRequest, serviceCode, metadata);
        } catch (IllegalArgumentException iae) {
            return JiraIncidentService.JiraCreateOutcome.failed(
                    JiraSyncStatus.ERROR,
                    sanitizeErrorMessage(iae.getMessage()),
                    null
            );
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
                        "Jira Service Management respondió sin un 'issueKey' válido al crear la solicitud " + serviceCode + ".",
                        null
                );
            }

            return JiraIncidentService.JiraCreateOutcome.created(issueKey, issueId.isEmpty() ? null : issueId);

        } catch (RestClientResponseException ex) {
            JiraClassifiedError classified = classifyHttpError(ex, "creación de solicitud " + serviceCode + " en Jira");
            return JiraIncidentService.JiraCreateOutcome.failed(
                    classified.getTargetSyncStatus(),
                    classified.getSanitizedMessage(),
                    classified.getRetryAfterSeconds()
            );
        } catch (RestClientException ex) {
            String sanitized = sanitizeErrorMessage("Error de conexión/timeout al crear solicitud " + serviceCode + " en Jira: " + ex.getMessage());
            return JiraIncidentService.JiraCreateOutcome.failed(JiraSyncStatus.RETRY_PENDING, sanitized, null);
        } catch (Exception ex) {
            String sanitized = sanitizeErrorMessage("Error inesperado al crear solicitud " + serviceCode + " en Jira: " + ex.getMessage());
            return JiraIncidentService.JiraCreateOutcome.failed(JiraSyncStatus.ERROR, sanitized, null);
        }
    }

    /**
     * Compatibilidad con el piloto SR-INF-01.
     */
    public JiraIncidentService.JiraCreateOutcome createSrInf01CustomerRequest(
            ServiceRequest serviceRequest,
            SrInf01FieldMetadata fieldMetadata) {
        RequestTypeFieldMetadata adapted = null;
        if (fieldMetadata != null) {
            adapted = new RequestTypeFieldMetadata(
                    "SR-INF-01",
                    resolveSrInf01RequestTypeId(),
                    fieldMetadata.getSupportedFieldIds(),
                    fieldMetadata.getCorrelationFieldIds(),
                    Collections.emptyMap()
            );
        }
        return createCustomerRequest(serviceRequest, adapted);
    }

    /**
     *Construye el mapa exacto de requestFieldValues según el servicio y el preflight real.
     */
    public Map<String, Object> buildRequestFieldValuesForService(
            ServiceRequest sr,
            String serviceCode,
            RequestTypeFieldMetadata metadata) {

        Map<String, Object> values = new LinkedHashMap<>();
        values.put("summary", sr.getSummary());

        switch (serviceCode) {
            case "SR-INF-01": {
                String lotFieldId = defaultIfBlank(jiraProperties.getSrInf01LotCodeFieldId(), "customfield_10263");
                String reqInfoFieldId = defaultIfBlank(jiraProperties.getSrInf01RequestedInfoFieldId(), "customfield_10264");
                String addDetailFieldId = defaultIfBlank(jiraProperties.getSrInf01AdditionalDetailFieldId(), "customfield_10265");

                values.put(lotFieldId, sr.getLotCode());
                values.put(reqInfoFieldId, sr.getRequestedInformation());
                if (metadata == null || metadata.getSupportedFieldIds().contains(addDetailFieldId)) {
                    values.put(addDetailFieldId, sr.getAdditionalDetail() != null ? sr.getAdditionalDetail().trim() : "");
                }
                break;
            }
            case "SR-INF-02": {
                // requestTypeId = 10119
                // customfield_10267 = Código del lote (required)
                // customfield_10268 = Tipo de consulta documental (required, select 10069..10072)
                // customfield_10269 = Detalle adicional (optional)
                values.put("customfield_10267", sr.getLotCode());
                String optionId = resolveDocumentQueryTypeOptionId(sr.getDocumentQueryType(), metadata);
                values.put("customfield_10268", Map.of("id", optionId));
                if (metadata == null || metadata.getSupportedFieldIds().contains("customfield_10269")) {
                    values.put("customfield_10269", sr.getAdditionalDetail() != null ? sr.getAdditionalDetail().trim() : "");
                }
                break;
            }
            case "SR-INF-03": {
                // requestTypeId = 10120
                // customfield_10263 = Código del lote (required)
                // customfield_10270 = Dato observado (required)
                // customfield_10271 = Descripción del problema (required)
                values.put("customfield_10263", sr.getLotCode());
                values.put("customfield_10270", sr.getObservedField());
                values.put("customfield_10271", sr.getProblemDescription());
                break;
            }
            case "SR-INF-04": {
                // requestTypeId = 10121
                // customfield_10263 = Código del lote (required)
                // customfield_10272 = Dato a corregir (required)
                // customfield_10273 = Información actual (required)
                // customfield_10274 = Información propuesta (required)
                // customfield_10258 = Justificación (required)
                values.put("customfield_10263", sr.getLotCode());
                values.put("customfield_10272", sr.getFieldToCorrect());
                values.put("customfield_10273", sr.getCurrentValue());
                values.put("customfield_10274", sr.getProposedValue());
                values.put("customfield_10258", sr.getJustification());
                break;
            }
            case "SR-ACC-01": {
                // requestTypeId = 10117
                // customfield_10254 = Nombre del trabajador (required)
                // customfield_10255 = Correo institucional (required)
                // customfield_10256 = Área (required, select 10053,10055..10060)
                // customfield_10257 = Rol requerido (required, select 10054,10061..10065)
                // customfield_10258 = Justificación (required)
                values.put("customfield_10254", sr.getWorkerName());
                values.put("customfield_10255", sr.getInstitutionalEmail());
                values.put("customfield_10256", Map.of("id", resolveAreaOptionId(sr.getArea(), metadata)));
                values.put("customfield_10257", Map.of("id", resolveRole10257OptionId(sr.getRequiredRole(), metadata)));
                values.put("customfield_10258", sr.getJustification());
                break;
            }
            case "SR-ACC-02": {
                // requestTypeId = 10118
                // customfield_10260 = Usuario afectado (required)
                // customfield_10255 = Correo institucional (required)
                // customfield_10256 = Área (required, select 10053,10055..10060)
                // customfield_10261 = Tipo de solicitud (required, select 10066,10067)
                // customfield_10257 = Rol actual (required, select 10054,10061..10065)
                // customfield_10262 = Rol/permisos requeridos (required, select 10068,10073..10078)
                // description = Motivo (required)
                values.put("customfield_10260", sr.getAffectedUser());
                values.put("customfield_10255", sr.getInstitutionalEmail());
                values.put("customfield_10256", Map.of("id", resolveAreaOptionId(sr.getArea(), metadata)));
                values.put("customfield_10261", Map.of("id", resolveAccessRequestTypeOptionId(sr.getAccessRequestType(), metadata)));
                values.put("customfield_10257", Map.of("id", resolveRole10257OptionId(sr.getCurrentRoleField(), metadata)));
                values.put("customfield_10262", Map.of("id", resolvePermissions10262OptionId(sr.getRequiredPermissions(), metadata)));
                values.put("description", sr.getReason());
                break;
            }
            default:
                throw new IllegalArgumentException("Código de servicio no soportado para mapeo Jira: " + serviceCode);
        }

        // Correlation ID: usar exactamente los fieldIds descubiertos en el Request Type
        if (metadata != null && !metadata.getCorrelationFieldIds().isEmpty()) {
            for (String corrFieldId : metadata.getCorrelationFieldIds()) {
                values.put(corrFieldId, sr.getCorrelationId());
            }
        } else {
            String fallbackCorrField = "SR-INF-01".equals(serviceCode)
                    ? defaultIfBlank(jiraProperties.getSrInf01CorrelationFieldId(), "customfield_10266")
                    : defaultIfBlank(jiraProperties.getCorrelationFieldId(), "customfield_10221");
            values.put(fallbackCorrField, sr.getCorrelationId());
        }

        return values;
    }

    /**
     * Traducción de Tipo de consulta documental (customfield_10268 en SR-INF-02):
     * 10069 = Documentos registrados
     * 10070 = Documentos pendientes
     * 10071 = Estado del expediente digital
     * 10072 = Consulta completa
     */
    public String resolveDocumentQueryTypeOptionId(String rawInput, RequestTypeFieldMetadata metadata) {
        String dynamicMatch = matchDynamicValidValue("customfield_10268", rawInput, metadata);
        if (dynamicMatch != null) {
            return dynamicMatch;
        }
        String norm = normalizeOptionKey(rawInput);
        if (norm.equals("10069") || norm.contains("DOCUMENTOS REGISTRADOS")) {
            return "10069";
        }
        if (norm.equals("10070") || norm.contains("DOCUMENTOS PENDIENTES")) {
            return "10070";
        }
        if (norm.equals("10071") || norm.contains("ESTADO DEL EXPEDIENTE") || norm.contains("ESTADO GENERAL DEL EXPEDIENTE")) {
            return "10071";
        }
        if (norm.equals("10072") || norm.contains("CONSULTA COMPLETA") || norm.contains("GUIAS DE DESPACHO") || norm.contains("CERTIFICADO SANITARIO")) {
            return "10072";
        }
        throw new IllegalArgumentException("Valor inválido para Tipo de consulta documental (customfield_10268): " + rawInput);
    }

    /**
     * Traducción de Área (customfield_10256 en SR-ACC-01 y SR-ACC-02):
     * 10053 = Producción
     * 10055 = Calidad
     * 10056 = Logística
     * 10057 = Administración
     * 10058 = Gerencia
     * 10059 = Informática / Soporte
     * 10060 = Otro
     */
    public String resolveAreaOptionId(String rawInput, RequestTypeFieldMetadata metadata) {
        String dynamicMatch = matchDynamicValidValue("customfield_10256", rawInput, metadata);
        if (dynamicMatch != null) {
            return dynamicMatch;
        }
        String norm = normalizeOptionKey(rawInput);
        if (norm.equals("10053") || norm.contains("PRODUCCION")) return "10053";
        if (norm.equals("10055") || norm.contains("CALIDAD") || norm.equals("QA")) return "10055";
        if (norm.equals("10056") || norm.contains("LOGISTICA") || norm.contains("COMERCIO EXTERIOR")) return "10056";
        if (norm.equals("10057") || norm.contains("ADMINISTRACION")) return "10057";
        if (norm.equals("10058") || norm.contains("GERENCIA")) return "10058";
        if (norm.equals("10059") || norm.contains("INFORMATICA") || norm.contains("SOPORTE") || norm.equals("TI")) return "10059";
        if (norm.equals("10060") || norm.equals("OTRO")) return "10060";
        throw new IllegalArgumentException("Valor inválido para Área (customfield_10256): " + rawInput);
    }

    /**
     * Traducción de Rol en customfield_10257 (Rol requerido en SR-ACC-01 y Rol actual en SR-ACC-02):
     * 10054 = PRODUCCION
     * 10061 = QA
     * 10062 = LOGISTICA
     * 10063 = GERENCIA
     * 10064 = ADMINISTRADOR
     * 10065 = SUPERADMIN
     */
    public String resolveRole10257OptionId(String rawInput, RequestTypeFieldMetadata metadata) {
        String dynamicMatch = matchDynamicValidValue("customfield_10257", rawInput, metadata);
        if (dynamicMatch != null) {
            return dynamicMatch;
        }
        String norm = normalizeOptionKey(rawInput);
        if (norm.equals("10054") || norm.startsWith("PRODUCCION")) return "10054";
        if (norm.equals("10061") || norm.startsWith("QA")) return "10061";
        if (norm.equals("10062") || norm.startsWith("LOGISTICA")) return "10062";
        if (norm.equals("10063") || norm.startsWith("GERENCIA")) return "10063";
        if (norm.equals("10064") || norm.startsWith("ADMINISTRADOR")) return "10064";
        if (norm.equals("10065") || norm.startsWith("SUPERADMIN")) return "10065";
        throw new IllegalArgumentException("Valor inválido para Rol (customfield_10257): " + rawInput);
    }

    /**
     * Traducción de Tipo de solicitud (customfield_10261 en SR-ACC-02):
     * 10066 = Cambio de permisos
     * 10067 = Retiro de permisos
     */
    public String resolveAccessRequestTypeOptionId(String rawInput, RequestTypeFieldMetadata metadata) {
        String dynamicMatch = matchDynamicValidValue("customfield_10261", rawInput, metadata);
        if (dynamicMatch != null) {
            return dynamicMatch;
        }
        String norm = normalizeOptionKey(rawInput);
        if (norm.equals("10066") || norm.contains("CAMBIO") || norm.contains("ASIGNACION")) return "10066";
        if (norm.equals("10067") || norm.contains("RETIRO") || norm.contains("DESACTIVACION")) return "10067";
        throw new IllegalArgumentException("Valor inválido para Tipo de solicitud (customfield_10261): " + rawInput);
    }

    /**
     * Traducción de Rol/permisos requeridos (customfield_10262 en SR-ACC-02):
     * 10068 = PRODUCCION
     * 10073 = QA
     * 10074 = LOGISTICA
     * 10075 = GERENCIA
     * 10076 = ADMINISTRADOR
     * 10077 = SUPERADMIN
     * 10078 = Retiro total de accesos
     */
    public String resolvePermissions10262OptionId(String rawInput, RequestTypeFieldMetadata metadata) {
        String dynamicMatch = matchDynamicValidValue("customfield_10262", rawInput, metadata);
        if (dynamicMatch != null) {
            return dynamicMatch;
        }
        String norm = normalizeOptionKey(rawInput);
        if (norm.equals("10068") || norm.startsWith("PRODUCCION")) return "10068";
        if (norm.equals("10073") || norm.startsWith("QA")) return "10073";
        if (norm.equals("10074") || norm.startsWith("LOGISTICA")) return "10074";
        if (norm.equals("10075") || norm.startsWith("GERENCIA")) return "10075";
        if (norm.equals("10076") || norm.startsWith("ADMINISTRADOR")) return "10076";
        if (norm.equals("10077") || norm.startsWith("SUPERADMIN")) return "10077";
        if (norm.equals("10078") || norm.contains("RETIRO TOTAL") || norm.contains("RETIRO")) return "10078";
        throw new IllegalArgumentException("Valor inválido para Rol/permisos requeridos (customfield_10262): " + rawInput);
    }

    private String matchDynamicValidValue(String fieldId, String rawInput, RequestTypeFieldMetadata metadata) {
        if (rawInput == null || rawInput.isBlank() || metadata == null) {
            return null;
        }
        Map<String, String> validValues = metadata.getValidValuesByFieldId().get(fieldId);
        if (validValues == null || validValues.isEmpty()) {
            return null;
        }
        String trimmed = rawInput.trim();
        if (validValues.containsKey(trimmed)) {
            return trimmed;
        }
        String normalizedInput = normalizeOptionKey(trimmed);
        for (Map.Entry<String, String> entry : validValues.entrySet()) {
            if (normalizeOptionKey(entry.getValue()).equals(normalizedInput)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private String normalizeOptionKey(String input) {
        if (input == null) {
            return "";
        }
        String decomposed = Normalizer.normalize(input.trim(), Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{InCombiningDiacriticalMarks}+", "").toUpperCase(Locale.ROOT);
    }

    /**
     * 5. Adjunta archivo al ticket Jira creado usando la API oficial de Jira Cloud:
     * POST /rest/api/3/issue/{issueKey}/attachments (con header X-Atlassian-Token: no-check).
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
                    : "adjunto-exportrace.pdf";

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
            log.warn("[JiraServiceRequestService] No se pudo adjuntar archivo en Jira para {}: {}",
                    issueKey, sanitizeErrorMessage(ex.getMessage()));
            return false;
        }
    }

    /**
     * 6. FASE D3 — Sincronización Pull Jira -> ExporTrace:
     * Consulta estado actual, comentarios públicos (excluyendo notas internas), aprobaciones y SLA.
     */
    public JiraPullSnapshot fetchCustomerRequestSnapshot(String jiraIssueKey, boolean requiresApproval) {
        JiraIncidentService.ConfigValidationResult configValidation = validateConfiguration();
        if (!configValidation.isValid()) {
            return JiraPullSnapshot.failed(configValidation.getErrorMessage());
        }
        if (isBlank(jiraIssueKey)) {
            return JiraPullSnapshot.failed("La solicitud no posee un jiraIssueKey asociado.");
        }

        String baseUrl = normalizeBaseUrl(jiraProperties.getBaseUrl());
        String key = jiraIssueKey.trim();

        try {
            // 6.1 Consultar estado principal: GET /rest/servicedeskapi/request/{jiraIssueKey}
            URI requestUri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .path("/rest/servicedeskapi/request/{issueKey}")
                    .buildAndExpand(key)
                    .toUri();

            String requestBody = getRestClient().get()
                    .uri(requestUri)
                    .header(HttpHeaders.AUTHORIZATION, authHeaderProvider.getAuthorizationHeader())
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .retrieve()
                    .body(String.class);

            JsonNode reqNode = objectMapper.readTree(requestBody != null ? requestBody : "{}");
            String rawJiraStatus = reqNode.path("currentStatus").path("status").asText("").trim();
            String statusCategory = reqNode.path("currentStatus").path("statusCategory").asText("").trim();

            // 6.2 Consultar comentarios: GET /rest/servicedeskapi/request/{jiraIssueKey}/comment
            // IMPORTANTE: Filtrar estrictamente public == true (NUNCA importar notas internas de agentes)
            URI commentUri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .path("/rest/servicedeskapi/request/{issueKey}/comment")
                    .buildAndExpand(key)
                    .toUri();

            List<JiraPulledComment> publicComments = new ArrayList<>();
            try {
                String commentsBody = getRestClient().get()
                        .uri(commentUri)
                        .header(HttpHeaders.AUTHORIZATION, authHeaderProvider.getAuthorizationHeader())
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                        .retrieve()
                        .body(String.class);

                JsonNode commentsRoot = objectMapper.readTree(commentsBody != null ? commentsBody : "{}");
                JsonNode valuesNode = commentsRoot.path("values");
                if (valuesNode.isArray()) {
                    for (JsonNode cNode : valuesNode) {
                        boolean isPublic = cNode.path("public").asBoolean(false);
                        if (!isPublic) {
                            // Regla estricta: ignorar notas internas de agentes
                            continue;
                        }
                        String commentId = cNode.path("id").asText("").trim();
                        String body = cNode.path("body").asText("").trim();
                        if (commentId.isEmpty() || body.isEmpty()) {
                            continue;
                        }
                        String authorName = cNode.path("author").path("displayName").asText("Soporte ExporTrace").trim();
                        if (authorName.isEmpty()) {
                            authorName = "Soporte ExporTrace";
                        }
                        LocalDateTime createdAt = parseJiraIsoDateTime(cNode.path("created").path("iso8601").asText(null));
                        publicComments.add(new JiraPulledComment(commentId, authorName, body, createdAt));
                    }
                }
            } catch (Exception commentEx) {
                log.warn("[JiraServiceRequestService] Error no fatal al consultar comentarios de {}: {}",
                        key, sanitizeErrorMessage(commentEx.getMessage()));
            }

            // 6.3 Consultar aprobaciones cuando corresponda: GET /rest/servicedeskapi/request/{jiraIssueKey}/approval
            String approvalDecision = null;
            if (requiresApproval) {
                URI approvalUri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                        .path("/rest/servicedeskapi/request/{issueKey}/approval")
                        .buildAndExpand(key)
                        .toUri();
                try {
                    String approvalBody = getRestClient().get()
                            .uri(approvalUri)
                            .header(HttpHeaders.AUTHORIZATION, authHeaderProvider.getAuthorizationHeader())
                            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                            .retrieve()
                            .body(String.class);

                    JsonNode appRoot = objectMapper.readTree(approvalBody != null ? approvalBody : "{}");
                    JsonNode appValues = appRoot.path("values");
                    if (appValues.isArray() && !appValues.isEmpty()) {
                        for (JsonNode appNode : appValues) {
                            String decision = appNode.path("finalDecision").asText("").trim();
                            if (!decision.isEmpty()) {
                                approvalDecision = decision;
                            }
                        }
                    }
                } catch (Exception appEx) {
                    log.debug("[JiraServiceRequestService] Consulta de aprobaciones para {} omitida: {}",
                            key, sanitizeErrorMessage(appEx.getMessage()));
                }
            }

            // 6.4 Consultar SLA si está disponible: GET /rest/servicedeskapi/request/{jiraIssueKey}/sla
            String slaSummary = null;
            URI slaUri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .path("/rest/servicedeskapi/request/{issueKey}/sla")
                    .buildAndExpand(key)
                    .toUri();
            try {
                String slaBody = getRestClient().get()
                        .uri(slaUri)
                        .header(HttpHeaders.AUTHORIZATION, authHeaderProvider.getAuthorizationHeader())
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                        .retrieve()
                        .body(String.class);

                JsonNode slaRoot = objectMapper.readTree(slaBody != null ? slaBody : "{}");
                JsonNode slaValues = slaRoot.path("values");
                if (slaValues.isArray() && !slaValues.isEmpty()) {
                    for (JsonNode slaNode : slaValues) {
                        String slaName = slaNode.path("name").asText("").trim();
                        JsonNode ongoing = slaNode.path("ongoingCycle");
                        if (!ongoing.isMissingNode() && !ongoing.isNull() && ongoing.size() > 0) {
                            boolean breached = ongoing.path("breached").asBoolean(false);
                            String remaining = ongoing.path("remainingTime").path("friendly").asText("").trim();
                            slaSummary = slaName + (breached ? " (Vencido)" : (!remaining.isEmpty() ? " (Restante: " + remaining + ")" : " (En curso)"));
                            break;
                        }
                        JsonNode completed = slaNode.path("completedCycles");
                        if (completed.isArray() && !completed.isEmpty()) {
                            JsonNode lastCycle = completed.get(completed.size() - 1);
                            boolean breached = lastCycle.path("breached").asBoolean(false);
                            String elapsed = lastCycle.path("elapsedTime").path("friendly").asText("").trim();
                            slaSummary = slaName + (breached ? " (Incumplido)" : " (Cumplido" + (!elapsed.isEmpty() ? " en " + elapsed : "") + ")");
                            break;
                        }
                    }
                }
            } catch (Exception slaEx) {
                log.debug("[JiraServiceRequestService] Consulta de SLA para {} omitida: {}",
                        key, sanitizeErrorMessage(slaEx.getMessage()));
            }

            // 6.5 Mapear estado Jira y decisión de aprobación hacia estados semánticos de ExporTrace
            String resolvedApprovalStatus = mapApprovalDecision(rawJiraStatus, approvalDecision, requiresApproval);
            ServiceRequestStatus mappedStatus = mapJiraStatusToLocal(rawJiraStatus, statusCategory, resolvedApprovalStatus, requiresApproval);

            return JiraPullSnapshot.success(
                    rawJiraStatus,
                    mappedStatus,
                    resolvedApprovalStatus,
                    slaSummary,
                    publicComments
            );

        } catch (RestClientResponseException ex) {
            JiraClassifiedError classified = classifyHttpError(ex, "actualización desde Jira para " + key);
            return JiraPullSnapshot.failed(classified.getSanitizedMessage());
        } catch (RestClientException ex) {
            return JiraPullSnapshot.failed(sanitizeErrorMessage("Error de conexión/timeout al consultar Jira para " + key + ": " + ex.getMessage()));
        } catch (Exception ex) {
            return JiraPullSnapshot.failed(sanitizeErrorMessage("Error al procesar estado de Jira para " + key + ": " + ex.getMessage()));
        }
    }

    /**
     * Mapeo determinístico de estados reales del proyecto EXT en Jira hacia ServiceRequestStatus local:
     * - Tareas por hacer / To Do -> REGISTERED (o PENDING_APPROVAL si requiere aprobación y está pendiente)
     * - EN REVISIÓN / Pending / Esperando por ayuda -> IN_REVIEW
     * - PENDIENTE DE APROBACIÓN / Waiting for approval -> PENDING_APPROVAL
     * - APROBADA / EN IMPLEMENTACIÓN / En curso / In progress -> IN_PROGRESS
     * - Resuelta / Resolved -> RESOLVED
     * - RECHAZADA / Declined / Cancelado -> REJECTED
     * - COMPLETADA / Finalizada / Done -> COMPLETED
     */
    public ServiceRequestStatus mapJiraStatusToLocal(
            String rawJiraStatus,
            String statusCategory,
            String approvalStatus,
            boolean requiresApproval) {

        if ("REJECTED".equalsIgnoreCase(approvalStatus)) {
            return ServiceRequestStatus.REJECTED;
        }

        String norm = normalizeOptionKey(rawJiraStatus);

        if (norm.contains("RECHAZAD") || norm.contains("DECLINED") || norm.contains("REJECTED") || norm.contains("CANCELAD") || norm.contains("CANCELED")) {
            return ServiceRequestStatus.REJECTED;
        }
        if (norm.contains("COMPLETAD") || norm.contains("FINALIZAD") || norm.equals("DONE") || norm.equals("CLOSED") || norm.equals("CERRADA")) {
            return ServiceRequestStatus.COMPLETED;
        }
        if (norm.contains("RESUELT") || norm.contains("RESOLVED")) {
            return ServiceRequestStatus.RESOLVED;
        }
        if (norm.contains("PENDIENTE DE APROBACION") || norm.contains("WAITING FOR APPROVAL") || norm.contains("ESPERANDO APROBACION")) {
            return ServiceRequestStatus.PENDING_APPROVAL;
        }
        if (norm.contains("EN IMPLEMENTACION") || norm.contains("EN CURSO") || norm.contains("IN PROGRESS")
                || norm.contains("EN PROGRESO") || norm.equals("APROBADA") || norm.equals("APPROVED")) {
            return ServiceRequestStatus.IN_PROGRESS;
        }
        if (norm.contains("EN REVISION") || norm.contains("IN REVIEW") || norm.equals("PENDING")
                || norm.contains("ESPERANDO POR AYUDA") || norm.contains("WAITING FOR SUPPORT")
                || norm.contains("ESPERANDO POR EL CLIENTE")) {
            return ServiceRequestStatus.IN_REVIEW;
        }
        if (norm.contains("TAREAS POR HACER") || norm.equals("TO DO") || norm.equals("OPEN") || norm.equals("ABIERTO")) {
            if (requiresApproval && "APPROVED".equalsIgnoreCase(approvalStatus)) {
                return ServiceRequestStatus.IN_PROGRESS;
            }
            return ServiceRequestStatus.REGISTERED;
        }

        String catNorm = normalizeOptionKey(statusCategory);
        if ("DONE".equals(catNorm)) {
            return ServiceRequestStatus.COMPLETED;
        }
        if ("INDETERMINATE".equals(catNorm)) {
            return ServiceRequestStatus.IN_PROGRESS;
        }
        if (requiresApproval && "APPROVED".equalsIgnoreCase(approvalStatus)) {
            return ServiceRequestStatus.IN_PROGRESS;
        }
        return ServiceRequestStatus.REGISTERED;
    }

    public String mapApprovalDecision(String rawJiraStatus, String approvalDecision, boolean requiresApproval) {
        if (!requiresApproval) {
            return "NOT_REQUIRED";
        }
        String decisionNorm = normalizeOptionKey(approvalDecision);
        if ("APPROVED".equals(decisionNorm) || "APROBADA".equals(decisionNorm)) {
            return "APPROVED";
        }
        if ("DECLINED".equals(decisionNorm) || "REJECTED".equals(decisionNorm) || "RECHAZADA".equals(decisionNorm)) {
            return "REJECTED";
        }

        String statusNorm = normalizeOptionKey(rawJiraStatus);
        if (statusNorm.contains("RECHAZAD") || statusNorm.contains("DECLINED") || statusNorm.contains("REJECTED")) {
            return "REJECTED";
        }
        if (statusNorm.equals("APROBADA") || statusNorm.contains("EN IMPLEMENTACION")
                || statusNorm.contains("COMPLETAD") || statusNorm.contains("FINALIZAD")
                || statusNorm.contains("RESUELT") || statusNorm.equals("DONE")) {
            return "APPROVED";
        }
        return "PENDING_APPROVAL";
    }

    private LocalDateTime parseJiraIsoDateTime(String isoString) {
        if (isBlank(isoString)) {
            return LocalDateTime.now();
        }
        try {
            return OffsetDateTime.parse(isoString.trim()).toLocalDateTime();
        } catch (Exception ignored) {
            try {
                // Handle format like 2026-11-04T21:56:37-0500 (without colon in offset)
                String normalized = isoString.trim();
                if (normalized.matches(".*[+-]\\d{4}$")) {
                    normalized = normalized.substring(0, normalized.length() - 2) + ":" + normalized.substring(normalized.length() - 2);
                    return OffsetDateTime.parse(normalized).toLocalDateTime();
                }
                return LocalDateTime.parse(normalized);
            } catch (Exception e) {
                return LocalDateTime.now();
            }
        }
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
        jqlRefs.add("cf[10221]");
        jqlRefs.add("cf[10266]");

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
            sanitized = sanitized.replace(apiToken.trim(), "[REDACTED]");
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

    public static class RequestTypeFieldMetadata {
        private final String serviceCode;
        private final String requestTypeId;
        private final Set<String> supportedFieldIds;
        private final Set<String> correlationFieldIds;
        private final Map<String, Map<String, String>> validValuesByFieldId;

        public RequestTypeFieldMetadata(
                String serviceCode,
                String requestTypeId,
                Set<String> supportedFieldIds,
                Set<String> correlationFieldIds,
                Map<String, Map<String, String>> validValuesByFieldId) {
            this.serviceCode = serviceCode;
            this.requestTypeId = requestTypeId;
            this.supportedFieldIds = Collections.unmodifiableSet(new LinkedHashSet<>(supportedFieldIds));
            this.correlationFieldIds = Collections.unmodifiableSet(new LinkedHashSet<>(correlationFieldIds));
            this.validValuesByFieldId = validValuesByFieldId != null
                    ? Collections.unmodifiableMap(new LinkedHashMap<>(validValuesByFieldId))
                    : Collections.emptyMap();
        }

        public String getServiceCode() { return serviceCode; }
        public String getRequestTypeId() { return requestTypeId; }
        public Set<String> getSupportedFieldIds() { return supportedFieldIds; }
        public Set<String> getCorrelationFieldIds() { return correlationFieldIds; }
        public Map<String, Map<String, String>> getValidValuesByFieldId() { return validValuesByFieldId; }
    }

    public static class RequestTypeFieldsOutcome {
        private final boolean success;
        private final RequestTypeFieldMetadata metadata;
        private final JiraSyncStatus errorSyncStatus;
        private final String sanitizedError;
        private final Long retryAfterSeconds;

        private RequestTypeFieldsOutcome(
                boolean success,
                RequestTypeFieldMetadata metadata,
                JiraSyncStatus errorSyncStatus,
                String sanitizedError,
                Long retryAfterSeconds) {
            this.success = success;
            this.metadata = metadata;
            this.errorSyncStatus = errorSyncStatus;
            this.sanitizedError = sanitizedError;
            this.retryAfterSeconds = retryAfterSeconds;
        }

        public static RequestTypeFieldsOutcome success(RequestTypeFieldMetadata metadata) {
            return new RequestTypeFieldsOutcome(true, metadata, null, null, null);
        }

        public static RequestTypeFieldsOutcome error(JiraSyncStatus status, String sanitizedError, Long retryAfterSeconds) {
            return new RequestTypeFieldsOutcome(false, null, status, sanitizedError, retryAfterSeconds);
        }

        public boolean isSuccess() { return success; }
        public RequestTypeFieldMetadata getMetadata() { return metadata; }
        public JiraSyncStatus getErrorSyncStatus() { return errorSyncStatus; }
        public String getSanitizedError() { return sanitizedError; }
        public Long getRetryAfterSeconds() { return retryAfterSeconds; }
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

    public static class JiraPulledComment {
        private final String jiraCommentId;
        private final String authorDisplayName;
        private final String body;
        private final LocalDateTime createdAt;

        public JiraPulledComment(String jiraCommentId, String authorDisplayName, String body, LocalDateTime createdAt) {
            this.jiraCommentId = jiraCommentId;
            this.authorDisplayName = authorDisplayName;
            this.body = body;
            this.createdAt = createdAt;
        }

        public String getJiraCommentId() { return jiraCommentId; }
        public String getAuthorDisplayName() { return authorDisplayName; }
        public String getBody() { return body; }
        public LocalDateTime getCreatedAt() { return createdAt; }
    }

    public static class JiraPullSnapshot {
        private final boolean success;
        private final String rawJiraStatus;
        private final ServiceRequestStatus mappedStatus;
        private final String approvalStatus;
        private final String slaSummary;
        private final List<JiraPulledComment> publicComments;
        private final String sanitizedError;

        private JiraPullSnapshot(
                boolean success,
                String rawJiraStatus,
                ServiceRequestStatus mappedStatus,
                String approvalStatus,
                String slaSummary,
                List<JiraPulledComment> publicComments,
                String sanitizedError) {
            this.success = success;
            this.rawJiraStatus = rawJiraStatus;
            this.mappedStatus = mappedStatus;
            this.approvalStatus = approvalStatus;
            this.slaSummary = slaSummary;
            this.publicComments = publicComments != null ? Collections.unmodifiableList(publicComments) : Collections.emptyList();
            this.sanitizedError = sanitizedError;
        }

        public static JiraPullSnapshot success(
                String rawJiraStatus,
                ServiceRequestStatus mappedStatus,
                String approvalStatus,
                String slaSummary,
                List<JiraPulledComment> publicComments) {
            return new JiraPullSnapshot(true, rawJiraStatus, mappedStatus, approvalStatus, slaSummary, publicComments, null);
        }

        public static JiraPullSnapshot failed(String sanitizedError) {
            return new JiraPullSnapshot(false, null, null, null, null, Collections.emptyList(), sanitizedError);
        }

        public boolean isSuccess() { return success; }
        public String getRawJiraStatus() { return rawJiraStatus; }
        public ServiceRequestStatus getMappedStatus() { return mappedStatus; }
        public String getApprovalStatus() { return approvalStatus; }
        public String getSlaSummary() { return slaSummary; }
        public List<JiraPulledComment> getPublicComments() { return publicComments; }
        public String getSanitizedError() { return sanitizedError; }
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
