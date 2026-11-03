# Plan de Implementación por Fases (A – G) — Gestión de Incidencias con Jira Service Management

**Sistema Inteligente de Trazabilidad Hidrobiológica ExporTrace**  
*Hoja de Ruta Técnica — Actualizada al Cierre de FASE A, FASE B y FASE C*

---

## 📊 1. Estado de Avance por Fase

| Fase | Nombre de la Fase | Estado | Validación / Pruebas |
| :---: | :--- | :---: | :---: |
| **FASE A** | Modelo local de incidencias + BD + Priorización (`WorkContinuity`) + API local | **COMPLETADA Y APROBADA** | **16 / 16 PASS** (`IncidentServiceTest`) |
| **FASE B** | Frontend Centro de Soporte (`LifeBuoy`) + Modal de Reporte + Mis Incidencias + Detalle | **COMPLETADA Y APROBADA** | **12 / 12 Casuísticas UI (`INC-B01..B12`)**<br/>`npm run build` (`0` errores TS) |
| **FASE C** | Integración REST ExporTrace $\rightarrow$ Jira Service Management + Idempotencia (`correlationId`) | **`VALIDADA_JIRA_REAL`** (`RFC-EXP-2026-007`) | **15 / 15 PASS** (`JiraIncidentSyncTest`)<br/>**118 / 118 PASS** (Suite Backend Total)<br/>Ticket Real Jira Cloud: **`EXT-1`** (`requestId=10172`, `INC-2026-000001`) |
| **FASE D** | Comentarios públicos, Adjuntos seguros (`Magic Bytes + SHA-256`) y Reapertura | `PENDIENTE` | — |
| **FASE E** | Webhook Jira $\rightarrow$ ExporTrace con validación HMAC (`JIRA_WEBHOOK_SECRET`) | `PENDIENTE` | — |
| **FASE F** | Notificaciones avanzadas, SLA sincronizado desde Jira y Scheduler de reintentos | `PENDIENTE` | — |
| **FASE G** | Cierre integral de pruebas (`INC-01`..`INC-15`), despliegue en Render y evidencias | `PENDIENTE` | — |

---

## ✅ 2. Resumen de Fases Completadas (FASE A, FASE B y FASE C)

### 2.1 FASE A — Backend Local, Base de Datos y Priorización (Aprobada)
- **Enums (`com.exportrace.entity.enums`)**: `IncidentCategory` (`CAT-01`..`CAT-15`), `IncidentImpact`, `IncidentUrgency`, `WorkContinuity`, `IncidentPriority` (`P1`: 15m/2h, `P2`: 30m/4h, `P3`: 2h/8h, `P4`: 4h/24h como `LOCAL_ESTIMATE`), `IncidentStatus`, `JiraSyncStatus`.
- **Entidad y Migración**: `IncidentReport` (`incident_reports` con `@Version` y 7 índices/constraints creados idempotentemente en `DatabaseMigrationConfig`).
- **Servicios y Controlador**: `IncidentPriorityService`, `IncidentService`, `IncidentController` (`POST /api/incidents`, `GET /api/incidents/my`, `GET /api/incidents/{id}`, `GET /api/admin/incidents`).

### 2.2 FASE B — Frontend Centro de Soporte y Mis Incidencias (Aprobada)
- **Acceso Global y Navegación**: `SupportButton.tsx` (`LifeBuoy` en `Header.tsx` y `SuperAdminLayout.tsx`), sección `SOPORTE` en `Sidebar.tsx`, rutas `/support`, `/support/incidents`, `/support/incidents/:id`.
- **Componentes y Vistas**: `ReportIncidentModal.tsx`, `IncidentPriorityBadge.tsx`, `IncidentStatusBadge.tsx`, `IncidentTimeline.tsx`, `SupportCenterPage.tsx`, `MyIncidentsPage.tsx`, `IncidentDetailPage.tsx`.

### 2.3 FASE C — Integración REST ExporTrace $\rightarrow$ Jira Service Management (`VALIDADA_JIRA_REAL`)
- **Configuración Segura (`JiraProperties.java` + `application.properties` + `.env.example`)**:
  - `JIRA_ENABLED=false` como valor seguro por defecto.
  - Timeouts explícitos configurables (`JIRA_CONNECT_TIMEOUT_SECONDS=5`, `JIRA_READ_TIMEOUT_SECONDS=10`).
  - Redacción estricta de `JIRA_API_TOKEN` y cabeceras `Authorization`.
- **Cliente REST (`JiraIncidentService.java`)**:
  - Basado en `RestClient` de Spring Boot 3.2 con `SimpleClientHttpRequestFactory`.
  - Validación de campos reales del Request Type (`GET /rest/servicedeskapi/servicedesk/{id}/requesttype/{id}/field`).
  - Búsqueda de idempotencia JQL sanitizada (`GET /rest/api/3/search/jql` por `JIRA_CORRELATION_FIELD_ID`).
  - Creación de solicitud (`POST /rest/servicedeskapi/request`) con descripción estructurada controlada y mapeo condicional de `priority` y `JIRA_EXPORTRACE_CODE_FIELD_ID`.
- **Validación Real en Jira Service Management Cloud (`RFC-EXP-2026-007`)**:
  - Proyecto: **Soporte ExporTrace** (`serviceDeskId = 2`, Key: `EXT`).
  - Request Type: **Reportar incidencia ExporTrace** (`requestTypeId = 10083`).
  - Campo de correlación: `customfield_10221` (`ExporTrace Correlation ID`).
  - Evidencia real controlada: `incidentCode = INC-2026-000001`, `jiraIssueKey = EXT-1`, `jiraRequestId = 10172`, `syncStatus = SYNCED`, `correlationId = b84deac3-3849-4a00-9ac5-b508565e6368` (`1` único ticket encontrado en Jira).
- **Orquestador de Sincronización (`IncidentSyncService.java`)**:
  - Separación de transacción de BD local (`TransactionTemplate`) y llamada HTTP externa.
  - Estrategia **LOOKUP BEFORE CREATE** para impedir tickets duplicados ante timeouts.
  - Clasificación de errores transitorios (`RETRY_PENDING`: timeout, 429, 502, 503, 504) vs permanentes (`ERROR`: 400, 401, 403, 404, 422, configuración inválida, conflicto de duplicidad).
  - Endpoint administrativo de reintento manual `POST /api/admin/incidents/{id}/retry-sync` para `ADMINISTRADOR` y `SUPERADMIN`.
  - Auditoría de los 6 eventos (`INCIDENT_SYNC_ATTEMPT`, `INCIDENT_SYNCED_TO_JIRA`, `INCIDENT_SYNC_FAILED`, `INCIDENT_SYNC_RETRY_REQUESTED`, `INCIDENT_SYNC_DUPLICATE_RECOVERED`, `INCIDENT_SYNC_DUPLICATE_CONFLICT`).
- **Guía de Configuración**: `/docs/JIRA_SERVICE_MANAGEMENT_SETUP.md`.

---

## 🔜 3. Fases Siguientes (Pendientes de Autorización)

### FASE D — Comentarios, Adjuntos y Reapertura
- Implementar `IncidentComment`, `IncidentAttachment` (reutilizando `PersistentFileStorageService` para imágenes `.jpg/.png/.webp` y documentos `.pdf` con SHA-256) y endpoint de reapertura.

### FASE E — Webhook Jira $\rightarrow$ ExporTrace
- Implementar `JiraWebhookController` validado con firma HMAC (`JIRA_WEBHOOK_SECRET`), tabla de deduplicación `jira_webhook_events` y filtrado estricto de notas internas de agentes (`INC-05`, `INC-06`, `INC-08`, `INC-13`).

### FASE F & FASE G — Notificaciones, SLA Sincronizado y Validación Integral
- Sincronización del SLA autoritativo desde Jira (`INC-14`), scheduler de reintentos y cierre del 100% de la matriz de pruebas (`INC-01`..`INC-15`).
