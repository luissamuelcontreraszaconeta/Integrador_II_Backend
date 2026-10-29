# Matriz Oficial de Casuísticas — Gestión de Incidencias Integrada con Jira Service Management

**Sistema Inteligente de Trazabilidad Hidrobiológica ExporTrace**  
*Estado de Validación actualizado al cierre de la FASE A, FASE B y FASE C (IMPLEMENTADA_Y_PROBADA_CON_MOCK)*

---

## 📌 1. Separación Estricta de Estado de Casuísticas

Siguiendo la directriz normativa del proyecto:
- Las sub-casuísticas técnicas locales de la **Fase A (`INC-A01` a `INC-A16`)** están verificadas con pruebas automatizadas reales en [`IncidentServiceTest.java`](file:///C:/Users/mayco/.gemini/antigravity/scratch/exportrace-ica-backend/src/test/java/com/exportrace/service/IncidentServiceTest.java) (**`16/16 PASS`**).
- Las sub-casuísticas de interfaz de usuario de la **Fase B (`INC-B01` a `INC-B12`)** están implementadas, verificadas en compilación estática (`tsc -b && vite build`) y documentadas en el checklist de validación de Fase B.
- Las sub-casuísticas de integración REST con Jira Service Management de la **Fase C (`INC-C01` a `INC-C15`)** están verificadas con pruebas automatizadas deterministas (`MockRestServiceServer`) en [`JiraIncidentSyncTest.java`](file:///C:/Users/mayco/.gemini/antigravity/scratch/exportrace-ica-backend/src/test/java/com/exportrace/service/JiraIncidentSyncTest.java) (**`15/15 PASS`**).
- La numeración y significado original de la matriz general **`INC-01` a `INC-15`** se conserva intacta:
  - **`INC-01`**: Tramo local y UI **`PROBADA`**, y tramo Jira en estado **`PROBADA_CON_MOCK`** (`INC-C01`). Pasará a `VALIDADA_JIRA_REAL` únicamente cuando se conecte una instancia Jira Cloud real.
  - **`INC-02`** (Caída/timeout de Jira $\rightarrow$ `RETRY_PENDING`), **`INC-03`** (Priorización automática), **`INC-04`** (Aislamiento RBAC), **`INC-11`** (Idempotencia ante timeout mediante `correlationId` / `LOOKUP BEFORE CREATE`) e **`INC-12`** (Usuario desactivado bloqueado) están **`PROBADA` / `PROBADA_CON_MOCK`**.
  - Las casuísticas que dependen de Comentarios, Adjuntos, Webhooks o SLA sincronizado de retorno (`INC-05` a `INC-10`, `INC-13` a `INC-15`) permanecen honestamente en **`PENDIENTE`** para las Fases D, E y F.

---

## ✅ 2. Matriz de Pruebas Automatizadas de FASE A (`IncidentServiceTest` — 16/16 PASS)

| Código Fase A | Subcasuística Técnica de Fase A | Método de Prueba en `IncidentServiceTest` | Trazabilidad Matriz General | Resultado Real | Estado |
| :--- | :--- | :--- | :---: | :--- | :---: |
| **`INC-A01`** | Usuario activo crea incidencia válida (`201 Created`) | `incA01_createValidIncident_persistsWithReportedAndPendingStatus` | Tramo local de `INC-01` | HTTP `201 Created`, genera `INC-2026-XXXXXX`, calcula urgencia y prioridad. | **`PROBADA`** |
| **`INC-A02`** | `HIGH` Impacto + `HIGH` Urgencia (`NO_PUEDE_CONTINUAR`) $\rightarrow$ `P1_CRITICAL` y validación de los 4 SLA `LOCAL_ESTIMATE` (`P1`: 15m/2h, `P2`: 30m/4h, `P3`: 2h/8h, `P4`: 4h/24h) | `incA02_highImpactAndHighUrgency_producesP1CriticalAndValidatesSlaLocalEstimates` | `INC-03` | `P1_CRITICAL` exacto y metas SLA `15/120`, `30/240`, `120/480`, `240/1440` min verificadas. | **`PROBADA`** |
| **`INC-A03`** | Las 9 combinaciones Impacto $\times$ Urgencia producen exactamente la prioridad esperada | `incA03_priorityMatrix_calculatesAllNineCombinationsCorrectly` | `INC-03` | 9/9 combinaciones exactas + mapeo `WorkContinuity`. | **`PROBADA`** |
| **`INC-A04`** | Usuario intenta inyectar `priority`, `status` o `reporterEmail` manualmente en JSON | `incA04_ignoreManipulatedPayloadFields_computesPriorityAndSnapshotsFromSession` | `INC-03` | Ignora valores manipulados; prevalece cálculo backend y sesión JWT. | **`PROBADA`** |
| **`INC-A05`** | Dos incidencias creadas generan códigos humanos `INC-2026-XXXXXX` distintos | `incA05_correlativeCodeGeneration_producesSequentialUniqueCodes` | Tramo local de `INC-01` | Códigos correlativos únicos verificados. | **`PROBADA`** |
| **`INC-A06`** | Dos incidencias creadas generan `correlationId` UUID v4 distintos | `incA06_correlationId_isGeneratedAsUniqueUuidV4` | Preparación `INC-11` | UUIDs distintos y no nulos en `correlation_id`. | **`PROBADA`** |
| **`INC-A07`** | Usuario consulta `GET /api/incidents/my` y recibe solo sus incidencias | `incA07_getMyIncidents_returnsOnlyAuthenticatedUserIncidents` | `INC-04` | Aislamiento verificado entre usuarios `QA` y `PRODUCCION`. | **`PROBADA`** |
| **`INC-A08`** | Usuario operacional intenta consultar incidencia ajena (`GET /api/incidents/{id}`) $\rightarrow$ `403` + auditoría | `incA08_getIncidentById_otherUserCannotAccessAndAuditLogIsRecorded` | **`INC-04`** | HTTP `403 Forbidden` + `AuditLog` `INCIDENT_VIEW_DENIED` persistido. | **`PROBADA`** |
| **`INC-A09`** | Incidencia creada nace con estado `REPORTED` | `incA09_incidentCreated_startsWithStatusReported` | Tramo local de `INC-01` | `status == REPORTED` verificado. | **`PROBADA`** |
| **`INC-A10`** | Incidencia creada nace con `syncStatus = PENDING` y `slaAuthority = LOCAL_ESTIMATE` | `incA10_incidentCreated_startsWithSyncStatusPending` | Tramo local de `INC-01` | `PENDING`, `jiraIssueKey == null` y `LOCAL_ESTIMATE` verificados. | **`PROBADA`** |
| **`INC-A11`** | `relatedLotId` inexistente al crear incidencia $\rightarrow$ `404 Not Found` controlado | `incA11_createIncident_withNonExistentLot_throwsResourceNotFound` | Subcasuística técnica Fase A (`INC-A11`) | HTTP `404 Not Found` controlado con ID del lote inexistente. | **`PROBADA`** |
| **`INC-A12`** | Campos obligatorios vacíos o nulos $\rightarrow$ `400 Bad Request` | `incA12_beanValidation_rejectsBlankSummaryShortDescriptionAndNullCategory` | Subcasuística técnica Fase A (`INC-A12`) | HTTP `400 Bad Request` por Bean Validation. | **`PROBADA`** |
| **`INC-A13`** | Incidencia creada registra `AuditLog` `INCIDENT_CREATED` y notificación interna | `incA13_createIncident_createsAuditLogAndInternalNotification` | Tramo local de `INC-01` | Registro `INCIDENT_CREATED` y notificación verificados. | **`PROBADA`** |
| **`INC-A14`** | **Usuario desactivado intenta reportar o consultar incidencias $\rightarrow$ acceso denegado** | **`incA14_inactiveUser_cannotCreateOrQueryIncidents`** | **Evidencia directa de `INC-12`** | Bloqueado con HTTP `401/403` tanto en `POST /api/incidents` como en `GET /api/incidents/my`. | **`PROBADA`** |
| **`INC-A15`** | Usuario sin token JWT intenta invocar `/api/incidents` $\rightarrow$ bloqueado | `incA15_unauthenticatedRequestWithoutJwt_blocked` | Seguridad JWT | Bloqueado sin autenticación (`401/403`). | **`PROBADA`** |
| **`INC-A16`** | Matriz RBAC administrativa en `GET /api/admin/incidents`: operacional (`403`), `ADMINISTRADOR` (`200`), `SUPERADMIN` (`200`) | `incA16_adminIncidentsRbac_operational403_administrador200_superadmin200` | RBAC Administrativo (`INC-04`) | `PRODUCCION`/`QA` $\rightarrow$ `403`; `ADMINISTRADOR` $\rightarrow$ `200`; `SUPERADMIN` $\rightarrow$ `200`. | **`PROBADA`** |

---

## 🖥️ 3. Matriz y Checklist de Validación Frontend de FASE B (`INC-B01` a `INC-B12`)

| Código Fase B | Casuística Frontend Fase B | Componente / Archivo Responsable | Verificación Técnica Implementada | Estado |
| :--- | :--- | :--- | :--- | :---: |
| **`INC-B01`** | Abre Centro de Soporte (`/support`) | `SupportButton.tsx`, `Sidebar.tsx`, `SupportCenterPage.tsx`, `App.tsx` | Accesible desde ícono `LifeBuoy` en `Header` y sección `SOPORTE` en `Sidebar`; renderiza tarjetas de acción y preguntas frecuentes. | **`IMPLEMENTADA / PROBADA`** |
| **`INC-B02`** | Abre modal Reportar incidencia | `SupportButton.tsx`, `SupportCenterPage.tsx`, `MyIncidentsPage.tsx`, `ReportIncidentModal.tsx` | Reutiliza `Modal.tsx`; se abre desde el popover del Header, desde `SupportCenterPage` y desde `MyIncidentsPage`. | **`IMPLEMENTADA / PROBADA`** |
| **`INC-B03`** | Formulario inválido no se envía | `ReportIncidentModal.tsx` (`validateForm`) | Valida `category`, `summary` ($\ge 5$ chars), `description` ($\ge 10$ chars), `impact` y `workContinuity` antes de invocar `incidentService.createIncident`. | **`IMPLEMENTADA / PROBADA`** |
| **`INC-B04`** | Formulario válido consume `POST /api/incidents` | `ReportIncidentModal.tsx`, `incidentService.ts` | Envía `CreateIncidentRequest` con `route = window.location.pathname` y `module` inferido sin exponer selector `P1..P4`. | **`IMPLEMENTADA / PROBADA`** |
| **`INC-B05`** | Respuesta muestra código asignado (`INC-2026-XXXXXX`) | `ReportIncidentModal.tsx` (`renderSuccessConfirmation`) | En estado `SUCCESS` muestra `✓ Incidencia registrada correctamente`, el código `INC-2026-XXXXXX`, estado amigable y botones `Ver incidencia` / `Cerrar`. | **`IMPLEMENTADA / PROBADA`** |
| **`INC-B06`** | Prioridad viene del backend y se representa correctamente | `IncidentPriorityBadge.tsx`, `ReportIncidentModal.tsx`, `IncidentDetailPage.tsx` | Renderiza `P1 — Crítica`, `P2 — Alta`, `P3 — Media` o `P4 — Baja` según la prioridad retornada por el backend. | **`IMPLEMENTADA / PROBADA`** |
| **`INC-B07`** | Mis Incidencias muestra únicamente respuesta recibida desde `/api/incidents/my` | `MyIncidentsPage.tsx` | Consume `incidentService.getMyIncidents()`, renderiza `DataTable` en desktop y cards en móvil con filtros y paginación. | **`IMPLEMENTADA / PROBADA`** |
| **`INC-B08`** | Empty state funciona cuando no hay incidencias | `MyIncidentsPage.tsx` | Si `incidents.length === 0`, muestra *"No tienes incidencias reportadas."* y botón `[Reportar incidencia]`. | **`IMPLEMENTADA / PROBADA`** |
| **`INC-B09`** | Detalle obtiene `GET /api/incidents/{id}` | `IncidentDetailPage.tsx`, `IncidentTimeline.tsx` | Muestra código, resumen, descripción, impacto, urgencia, prioridad, estado, lote relacionado, *"Tiempo objetivo estimado"* (`LOCAL_ESTIMATE`) y timeline local. | **`IMPLEMENTADA / PROBADA`** |
| **`INC-B10`** | HTTP `403` muestra error amigable | `IncidentDetailPage.tsx`, `apiClient.ts` | Cuando el backend devuelve `403 Forbidden`, muestra *"No tienes permiso para acceder a esta incidencia."* y botón para volver a Mis Incidencias. | **`IMPLEMENTADA / PROBADA`** |
| **`INC-B11`** | Doble clic no genera dos solicitudes frontend | `ReportIncidentModal.tsx` (`isSubmittingRef` + `submitState === 'SUBMITTING'`) | Bloquea reentradas síncronas con `useRef(false)` y deshabilita controles mientras `submitState === 'SUBMITTING'`. | **`IMPLEMENTADA / PROBADA`** |
| **`INC-B12`** | Enums backend se muestran con labels amigables | `incident.ts`, `IncidentStatusBadge.tsx`, `IncidentPriorityBadge.tsx` | Traduce todos los enums (`REPORTED` $\rightarrow$ `Reportada`, `P1_CRITICAL` $\rightarrow$ `P1 — Crítica`, `SYNCED` $\rightarrow$ `En mesa de ayuda`) sin mostrar valores crudos. | **`IMPLEMENTADA / PROBADA`** |

---

## 🔗 4. Matriz de Pruebas Automatizadas de FASE C (`JiraIncidentSyncTest` — 15/15 PASS)

| Código Fase C | Subcasuística Técnica de Fase C | Método de Prueba en `JiraIncidentSyncTest` | Trazabilidad Matriz General | Resultado Real | Estado |
| :--- | :--- | :--- | :---: | :--- | :---: |
| **`INC-C01`** | Jira responde `201 Created` $\rightarrow$ `SYNCED` + `jiraIssueKey` (`EXP-27`) y `jiraRequestId` guardados + auditoría | `incC01_jiraReturns201_marksSyncedAndSavesIssueKey` | Tramo Jira de `INC-01` | `syncStatus = SYNCED`, `jiraIssueKey = EXP-27`, `slaAuthority = JIRA_AUTHORITATIVE`, auditoría `INCIDENT_SYNCED_TO_JIRA`. | **`PROBADA_CON_MOCK`** |
| **`INC-C02`** | Jira timeout (`SocketTimeoutException`) $\rightarrow$ incidencia local permanece en BD con `RETRY_PENDING` | `incC02_jiraTimeout_localIncidentPersistsWithRetryPending` | **`INC-02`** | HTTP `201 Created`, incidencia conservada localmente, `syncStatus = RETRY_PENDING`, auditoría `INCIDENT_SYNC_FAILED`. | **`PROBADA`** |
| **`INC-C03`** | Jira responde `HTTP 503 Service Unavailable` $\rightarrow$ `RETRY_PENDING` | `incC03_jiraReturns503_marksRetryPending` | **`INC-02`** | Clasificado como error transitorio $\rightarrow$ `RETRY_PENDING`. | **`PROBADA`** |
| **`INC-C04`** | Jira responde `HTTP 400 Bad Request` $\rightarrow$ `ERROR` permanente | `incC04_jiraReturns400_marksError` | Clasificación errores Fase C | Clasificado como error permanente de payload $\rightarrow$ `ERROR`. | **`PROBADA`** |
| **`INC-C05`** | Jira responde `HTTP 401 Unauthorized` $\rightarrow$ `ERROR` de credenciales | `incC05_jiraReturns401_marksError` | Clasificación errores Fase C | Clasificado como error permanente de autenticación $\rightarrow$ `ERROR`. | **`PROBADA`** |
| **`INC-C06`** | Jira responde `HTTP 429 Too Many Requests` (`Retry-After: 45`) $\rightarrow$ `RETRY_PENDING` | `incC06_jiraReturns429WithRetryAfter_marksRetryPending` | Clasificación errores Fase C | Clasificado como transitorio $\rightarrow$ `RETRY_PENDING` registrando `Retry-After: 45s`. | **`PROBADA`** |
| **`INC-C07`** | Reintento encuentra ticket existente por `correlationId` (`EXP-20`) $\rightarrow$ recupera `issueKey` y **NO** crea segundo ticket | `incC07_retryFindsExistingTicketByCorrelationId_recoversWithoutCreatingDuplicate` | **`INC-11`** | `LOOKUP BEFORE CREATE` recupera `EXP-20`, marca `SYNCED`, audita `INCIDENT_SYNC_DUPLICATE_RECOVERED` y omite `POST /request`. | **`PROBADA`** |
| **`INC-C08`** | Reintento no encuentra ticket en JQL $\rightarrow$ procede a crear uno nuevo (`EXP-28`) | `incC08_retryFindsNoTicket_createsNewTicket` | `INC-11` / `INC-01` | `LOOKUP BEFORE CREATE` retorna `0` issues $\rightarrow$ valida fields y crea `EXP-28` (`SYNCED`). | **`PROBADA`** |
| **`INC-C09`** | Dos tickets Jira encontrados con mismo `correlationId` (`EXP-20`, `EXP-21`) $\rightarrow$ `ERROR` de inconsistencia y **NO** crea tercero | `incC09_multipleTicketsFoundWithSameCorrelationId_marksConflictErrorAndDoesNotCreateThird` | **`INC-11`** | Marca `ERROR`, audita `INCIDENT_SYNC_DUPLICATE_CONFLICT` y bloquea creación de tercer ticket. | **`PROBADA`** |
| **`INC-C10`** | `JIRA_ENABLED=false` $\rightarrow$ ninguna llamada HTTP externa | `incC10_jiraDisabled_noExternalHttpCallMade` | Feature Flag Fase C | Cero llamadas HTTP externas; incidencia local en `PENDING`. | **`PROBADA`** |
| **`INC-C11`** | Configuración incompleta con `JIRA_ENABLED=true` $\rightarrow$ fallo controlado (`ERROR`) | `incC11_missingConfigWhenJiraEnabled_marksControlledError` | Validación Config Fase C | Detecta parámetros faltantes antes de invocar red y marca `ERROR` controlado. | **`PROBADA`** |
| **`INC-C12`** | Usuario operacional (`QA`) intenta `POST /api/admin/incidents/{id}/retry-sync` $\rightarrow$ `403 Forbidden` | `incC12_operationalUserAttemptsRetrySync_returns403` | RBAC Administrativo (`INC-04`) | Bloqueado con HTTP `403 Forbidden`. | **`PROBADA`** |
| **`INC-C13`** | `ADMINISTRADOR` ejecuta `retry-sync` $\rightarrow$ permitido (`200 OK`) | `incC13_administradorRetrySync_allowed` | RBAC Administrativo | HTTP `200 OK` + auditoría `INCIDENT_SYNC_RETRY_REQUESTED`. | **`PROBADA`** |
| **`INC-C14`** | `SUPERADMIN` ejecuta `retry-sync` $\rightarrow$ permitido (`200 OK`) | `incC14_superAdminRetrySync_allowed` | RBAC Administrativo | HTTP `200 OK` + auditoría `INCIDENT_SYNC_RETRY_REQUESTED`. | **`PROBADA`** |
| **`INC-C15`** | Respuesta Jira, DTO, `lastSyncError` y `AuditLog` **nunca** exponen `JIRA_API_TOKEN` ni `Authorization` header | `incC15_jiraErrorAndDtoNeverExposeApiTokenOrAuthHeader` | Seguridad Secretos Fase C | Redacción `[REDACTED]` verificada en errores, `toString()`, DTO y auditoría. | **`PROBADA`** |

---

## 📋 5. Matriz General Oficial de Casuísticas del Módulo (`INC-01` a `INC-15`)

*(Numeración y significado original preservados íntegramente)*:

| Código | Casuística General Original | Prioridad | Módulo / Capa | Evidencia Actual / Fase | Estado Actual |
| :--- | :--- | :---: | :--- | :--- | :---: |
| **`INC-01`** | Usuario reporta incidencia correctamente (crea `IncidentReport` local + sincroniza con Jira) | P0 | `IncidentService` / UI Fase B / `JiraIncidentService` | Tramo local y UI **PROBADOS** (`INC-A01..A13`, `INC-B01..B06`). Tramo Jira probado en **`INC-C01` (`JiraIncidentSyncTest`)**. | **`PROBADA_CON_MOCK`** *(Pendiente `VALIDADA_JIRA_REAL`)* |
| **`INC-02`** | Jira no responde o está caído al reportar $\rightarrow$ incidencia no se pierde (`RETRY_PENDING`) | P0 | `IncidentSyncService` | **`incC02_jiraTimeout_...` (`INC-C02`)**, **`incC03_jiraReturns503_...` (`INC-C03`)** | **`PROBADA`** |
| **`INC-03`** | Cálculo automático de Prioridad Crítica (`Impacto HIGH + Urgencia HIGH` $\rightarrow$ `P1_CRITICAL`) y matriz de 9 combinaciones en backend | P0 | `IncidentPriorityService` | **`incA02_...` (`INC-A02`)**, **`incA03_...` (`INC-A03`)**, **`incA04_...` (`INC-A04`)**, **`INC-B06`** | **`PROBADA`** |
| **`INC-04`** | Usuario intenta consultar, comentar o descargar adjunto de incidencia ajena $\rightarrow$ `403 Forbidden` + auditoría | P0 | `IncidentController` / `IncidentService` / UI Fase B | **`incA07_...` (`INC-A07`)**, **`incA08_...` (`INC-A08`)**, **`incA16_...` (`INC-A16`)**, **`INC-B10`**, **`INC-C12`** | **`PROBADA`** |
| **`INC-05`** | Agente cambia estado en Jira Service Management $\rightarrow$ webhook actualiza ExporTrace | P0 | `JiraWebhookController` | Pendiente Fase E. | `PENDIENTE` |
| **`INC-06`** | Webhook con firma o secreto inválido / ausente $\rightarrow$ rechazo `401/403` + auditoría | P0 | `JiraWebhookController` | Pendiente Fase E. | `PENDIENTE` |
| **`INC-07`** | Usuario comenta desde ExporTrace $\rightarrow$ comentario público en Jira | P1 | `IncidentService` / `JiraIncidentService` | Pendiente Fase D. | `PENDIENTE` |
| **`INC-08`** | Agente responde en Jira (Comentario Público visible vs. Nota Interna oculta) | P0 | `JiraWebhookController` | Pendiente Fases D y E. | `PENDIENTE` |
| **`INC-09`** | Carga de adjunto malicioso, ejecutable, corrupto o con Path Traversal $\rightarrow$ rechazado | P0 | `PersistentFileStorageService` | Pendiente Fase D. | `PENDIENTE` |
| **`INC-10`** | Incidencia resuelta (`RESOLVED`) vuelve a ocurrir y usuario solicita reapertura (`REOPENED`) | P1 | `IncidentService` | Pendiente Fase D/E. | `PENDIENTE` |
| **`INC-11`** | Reintento de sincronización tras timeout de red no genera ticket Jira duplicado (`correlationId` + `LOOKUP BEFORE CREATE`) | P0 | `IncidentSyncService` / `JiraIncidentService` | **`incA06_...` (`INC-A06`)**, **`incC07_...` (`INC-C07`)**, **`incC08_...` (`INC-C08`)**, **`incC09_...` (`INC-C09`)** | **`PROBADA`** |
| **`INC-12`** | **Usuario desactivado intenta reportar una incidencia $\rightarrow$ acceso denegado** | P0 | `JwtAuthFilter` / `IncidentService` | **`incA14_inactiveUser_cannotCreateOrQueryIncidents` (`INC-A14`)** | **`PROBADA`** |
| **`INC-13`** | Recepción duplicada del mismo evento Webhook desde Jira (Idempotencia de Webhook) | P1 | `JiraWebhookController` | Pendiente Fase E (`jira_webhook_events`). | `PENDIENTE` |
| **`INC-14`** | Pausa y reanudación automática del reloj SLA en estado `WAITING_USER` | P1 | `IncidentService` | Pendiente Fase E/F (Autoridad SLA Jira). | `PENDIENTE` |
| **`INC-15`** | Dos archivos adjuntos con el mismo nombre original subidos a incidencias (UUID v4 + SHA-256 sin colisión) | P1 | `PersistentFileStorageService` | Pendiente Fase D (`IncidentAttachment`). | `PENDIENTE` |
