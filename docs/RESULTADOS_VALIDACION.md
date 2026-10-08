# Matriz Oficial de Resultados de Validación y Estabilización — ExporTrace
## Consolidado Integral de Suites Automatizadas (87 Tests PASS)

Este documento sirve como registro oficial de la ejecución de pruebas de estabilización, validación de compuertas y cierre formal de brechas de la plataforma **ExporTrace**.

---

## 1. Control de Ejecuciones y Consolidado de Suites

| ID Suite / Ejecución | Módulo / Brechas | Archivo de Prueba | Total Tests | Aprobados | Fallidos | Estado General |
| :--- | :--- | :--- | :---: | :---: | :---: | :---: |
| `SUITE-01-LOTS` | Gestión de Lotes (`BR-P1-004`, `BR-P1-010`) | `LotManagementServiceTest.java` | 11 | 11 | 0 | **100% PASS** |
| `SUITE-02-FSM` | Máquina de Estados P0 (`BR-P0-001`, `BR-P0-006`) | `LotStateMachineServiceTest.java` | 12 | 12 | 0 | **100% PASS** |
| `SUITE-03-QA-REINSP` | Reinspecciones QA 1:N (`BR-P1-001`) | `QualityReinspectionServiceTest.java` | 11 | 11 | 0 | **100% PASS** |
| `SUITE-04-COLDCHAIN` | Cadena de Frío P0-B (`BR-P0-002`) | `ColdChainServiceTest.java` | 8 | 8 | 0 | **100% PASS** |
| `SUITE-05-COLDFRESH` | Frescura Térmica / Data Gap (`BR-P1-006`, Caso 8) | `ColdChainFreshnessTest.java` | 10 | 10 | 0 | **100% PASS** |
| `SUITE-06-EVIDENCE` | Evidencias Fotográficas P0-C (`BR-P0-003`, `OM-05`) | `QaEvidenceServiceTest.java` | 13 | 13 | 0 | **100% PASS** |
| `SUITE-07-DOCUMENTS` | Versionamiento Docs & Magic Bytes (`BR-P1-008/009`) | `DocumentVersioningServiceTest.java` | 11 | 11 | 0 | **100% PASS** |
| `SUITE-08-RBAC` | Control de Acceso RBAC Backend (Casos 17, 19) | `SecurityRbacIntegrationTest.java` | 6 | 6 | 0 | **100% PASS** |
| `SUITE-09-JWT-DEACT` | Desactivación Inmediata de Usuario (Caso 18) | `UserDeactivationSecurityTest.java` | 5 | 5 | 0 | **100% PASS** |
| **TOTAL CONSOLIDADO** | **9 Suites Automatizadas** | **9 Archivos de Prueba** | **87** | **87** | **0** | **100% PASS (0 Errores)** |

---

## 2. Batería de Pruebas de Transición y Compuertas P0 (Casos 1 al 12)

| Caso | Descripción y Precondición | Intento de Operación | Resultado Esperado | Resultado Obtenido (Backend API & Motor) | HTTP Status | PASS / FAIL | Evidencia Automatizada | Estado |
| :---: | :--- | :--- | :--- | :--- | :---: | :---: | :--- | :---: |
| **CASO 1** | Lote en estado `REGISTERED` (creación inicial). | Intentar `POST /api/dispatches/lot/{id}` para despachar directamente. | **BLOQUEADO** | Bloqueo por compuerta de estado. Mensaje: *"BLOQUEO P0: No se puede autorizar el despacho directo de un lote en estado: Registrado."* | `422` | **PASS** | `testCaso1_RegistradoDirectoADespachado_Bloqueado` | **VALIDADO** |
| **CASO 2** | Lote en `REGISTERED` sin registro de inspección QA. | Intentar `POST /api/certifications/lot/{id}/request`. | **BLOQUEADO** | Rechazado con mensaje: *"BLOQUEO P0: El lote no cuenta con ninguna inspección de calidad registrada."* | `422` | **PASS** | `testCaso2_LoteSinQA_IntentaCertificarse_Bloqueado` | **VALIDADO** |
| **CASO 3** | Lote con inspección QA y dictamen `OBSERVADO`. | Intentar `POST /api/certifications/lot/{id}/request`. | **BLOQUEADO** | Rechazado con mensaje: *"BLOQUEO P0: No se puede iniciar certificación porque el lote tiene una inspección QA con resultado 'OBSERVADO'. Se requiere dictamen CONFORME."* | `422` | **PASS** | `testCaso3_QAObservado_IntentaCertificarse_Bloqueado` | **VALIDADO** |
| **CASO 4** | Lote con inspección QA y dictamen `NO_CONFORME`. | Intentar avanzar a `READY_FOR_CERTIFICATION`. | **BLOQUEADO** | Rechazado con mensaje: *"BLOQUEO P0: No se puede iniciar certificación porque el lote tiene una inspección QA con resultado 'NO_CONFORME'. Se requiere dictamen CONFORME."* | `422` | **PASS** | `testCaso4_QANoConforme_IntentaAvanzar_Bloqueado` | **VALIDADO** |
| **CASO 5** | QA `CONFORME` pero sensor registra temperatura crítica (> -15°C congelado). | Intentar validar para certificación. | **BLOQUEADO** | Rechazado con mensaje: *"BLOQUEO P0: No se puede iniciar certificación porque existe una desviación crítica de temperatura en cadena de frío."* | `422` | **PASS** | `testCaso5_QAConforme_FrioCritico_Bloqueado` | **VALIDADO** |
| **CASO 6** | QA `CONFORME` y frío estable pero sin documentos adjuntos. | Intentar validar para certificación. | **NO LISTO PARA CERTIFICACIÓN** | Rechazado con mensaje: *"BLOQUEO P0: No se puede iniciar certificación porque la documentación obligatoria del expediente digital está incompleta."* | `422` | **PASS** | `testCaso6_QAConforme_FrioConforme_SinDocumentos_Bloqueado` | **VALIDADO** |
| **CASO 7** | QA `CONFORME` + frío estable + expediente documental completo. | Ejecutar solicitud de Certificación SANIPES. | **PERMITIDO** | Trámite iniciado con éxito. Estado del lote transiciona a `IN_CERTIFICATION`. Expediente asignado. | `200` | **PASS** | `testCaso7_QAConforme_FrioConforme_DocsCompletos_Permitido` | **VALIDADO** |
| **CASO 8** | Trámite SANIPES en evaluación (`IN_CERTIFICATION`). | Intentar habilitar hacia despacho mediante `/enable-dispatch`. | **BLOQUEADO** | Rechazado con mensaje: *"BLOQUEO P0: El lote debe estar CERTIFICADO antes de pasar a preparación de despacho. Trámite en estado: SOLICITADO."* | `422` | **PASS** | `testCaso8_CertificacionEnEvaluacion_IntentaDespachoListo_Bloqueado` | **VALIDADO** |
| **CASO 9** | Certificación sanitaria rechazada por autoridad (`RECHAZADO`). | Intentar autorizar despacho de contenedor. | **BLOQUEADO** | Rechazado con mensaje: *"BLOQUEO P0: Despacho denegado. Se requiere Certificado Sanitario SANIPES en estado APROBADO."* | `422` | **PASS** | `testCaso9_CertificacionRechazada_IntentaDespacharse_Bloqueado` | **VALIDADO** |
| **CASO 10** | Certificado emitido y aprobado + QA conforme + frío estable + docs. | Ejecutar `POST /api/certifications/lot/{id}/enable-dispatch`. | **PERMITIDO** | Transición exitosa a `READY_FOR_DISPATCH`. Lote habilitado en la bandeja de Despacho. | `200` | **PASS** | `testCaso10_CertificadoValido_PermitirListoParaDespacho` | **VALIDADO** |
| **CASO 11** | Lote en estado `DISPATCHED` (contenedor precintado y DUA). | Intentar revertir lote a estado `REGISTERED` o Producción. | **BLOQUEADO** | Rechazado con mensaje: *"BLOQUEO P0: El lote ya ha sido DESPACHADO (estado terminal inmutable). No puede volver a etapas previas."* | `409` | **PASS** | `testCaso11_LoteDespachado_IntentaVolverAProduccion_Bloqueado` | **VALIDADO** |
| **CASO 12** | Lote en estado `CERTIFIED` o `DISPATCHED`. | Intentar modificar la inspección QA histórica (`POST /api/quality/lot/{id}`). | **BLOQUEADO** | Rechazado con mensaje: *"BLOQUEO P0: No se puede modificar la inspección QA de un lote que ya ha sido certificado o despachado (Certificado)."* | `409` | **PASS** | `testCaso12_LoteCertificado_IntentaModificarQAHistorica_Bloqueado` | **VALIDADO** |

---

## 3. Batería de Pruebas de Cadena de Frío y Perfiles Térmicos P0-B (Casos CF-01 a CF-08)

| Caso | Escenario y Precondición | Intento de Operación | Resultado Esperado | Resultado Obtenido | HTTP Status | PASS / FAIL | Evidencia Automatizada | Estado |
| :---: | :--- | :--- | :--- | :--- | :---: | :---: | :--- | :---: |
| **TC-CF-01** | Producto Congelado: Evaluación a -25°C, -20°C, -18°C, -17°C, -15°C, -14°C, -5°C. | `evaluateTemperatureStatus` | Rangos conformes: ≤-18 NORMAL, -17 a -15 WARNING, >-15 CRITICAL | Coincidencia exacta con matriz SANIPES | `200` | **PASS** | `testFrozenProductTemperatureRanges` | **VALIDADO** |
| **TC-CF-02** | Producto Refrigerado: Evaluación a 2°C, 4°C, 5°C, -3°C. | `evaluateTemperatureStatus` | Rangos conformes: 0°C a 4°C NORMAL, >4°C CRITICAL, <-1°C CRITICAL | Coincidencia exacta con norma de frescos | `200` | **PASS** | `testRefrigeratedProductTemperatureRanges` | **VALIDADO** |
| **TC-CF-03** | Lecturas de temperatura extremas (+150°C y -150°C). | Intentar registrar lectura fuera de rango físico. | **BLOQUEADO (400)** | Rechazado con mensaje: *"Lectura de temperatura físicamente imposible (-80.0°C a +60.0°C)."* | `400` | **PASS** | `testPhysicallyImprobableTemperaturesRejected` | **VALIDADO** |
| **TC-CF-04** | Registro de lectura CRITICAL (-10°C en congelado). | Registro de lectura crítica + posterior retorno a -20°C (NORMAL). | Incidencia pasa a `ACTIVE` y permanece `ACTIVE` tras normalizarse temperatura | Incidencia creada y persistente; retorno a normalidad no borra la incidencia | `200` | **PASS** | `testCriticalCreatesActiveIncidentAndNormalDoesNotDeleteIt` | **VALIDADO** |
| **TC-CF-05** | Incidencia en estado `ACTIVE`. | QA ejecuta revisión (`UNDER_REVIEW`) y resolución técnica (`RESOLVED`) con justificación. | Transición `ACTIVE` $\rightarrow$ `UNDER_REVIEW` $\rightarrow$ `RESOLVED` con registro de justificación | Transiciones completadas y auditadas (`COLD_CHAIN_INCIDENT_RESOLVED`) | `200` | **PASS** | `testIncidentLifecycleTransitions` | **VALIDADO** |
| **TC-CF-06** | Lote con QA conforme y documentos pero con incidencia térmica `ACTIVE`. | Intentar solicitar Certificación SANIPES. | **BLOQUEADO (422)** | Rechazado: *"BLOQUEO P0: No se puede iniciar certificación porque existe una desviación crítica de temperatura en cadena de frío sin subsanar."* | `422` | **PASS** | `testLotWithActiveIncidentCannotBeCertified` | **VALIDADO** |
| **TC-CF-07** | Lote `CERTIFIED` sufre desviación crítica (-8°C) antes de embarque (`BR-P1-013`). | Registro de lectura crítica en lote certificado. | Despacho bloqueado, lote a `OBSERVED`, Certificado SANIPES conservado intacto | Despacho bloqueado (422), lote observado, certificado conservado | `200` / `422` | **PASS** | `testCertifiedLotReceivesCriticalTemperature_BlocksDispatchAndPreservesCert` | **VALIDADO** |
| **TC-CF-08** | Lote bloqueado por frío completa estabilización (-21°C) y resolución QA formal. | QA aprueba subsanación técnica (`RESOLVED`). | Lote rehabilitado automáticamente a `READY_FOR_DISPATCH` | Lote retorna a `READY_FOR_DISPATCH` y permite autorización de despacho | `200` | **PASS** | `testResolveIncidentRehabilitatesLotForDispatch` | **VALIDADO** |

---

## 4. Batería de Pruebas de Frescura Térmica y Gap de Datos (Casos FT-01 a FT-10, Caso 8)

| Caso | Escenario y Precondición | Intento de Operación | Resultado Esperado | HTTP Status | PASS / FAIL | Evidencia Automatizada |
| :---: | :--- | :--- | :--- | :---: | :---: | :--- |
| **TC-FT-01** | Lectura reciente (1h atrás). | `isColdChainFresh` | `true` | `200` | **PASS** | `testColdChainFresh_WithRecentReading_ReturnsTrue` |
| **TC-FT-02** | Lectura en límite permitido (11h 59m). | `isColdChainFresh` | `true` | `200` | **PASS** | `testColdChainFresh_WithLimitReading_ReturnsTrue` |
| **TC-FT-03** | Lectura vencida (13h atrás). | `isColdChainFresh` | `false` | `200` | **PASS** | `testColdChainFresh_WithExpiredReading_ReturnsFalse` |
| **TC-FT-04** | Sin lecturas registradas. | `isColdChainFresh` | `false` | `200` | **PASS** | `testColdChainFresh_WithNoReading_ReturnsFalse` |
| **TC-FT-05** | Múltiples lecturas antiguas y recientes. | `isColdChainFresh` | Evalúa la más reciente cronológicamente (`true`) | `200` | **PASS** | `testColdChainFresh_MultipleReadings_PicksLatest` |
| **TC-FT-06** | Lectura con timestamp futuro (> 5 min). | `addTemperatureLog` con fecha futura | **BLOQUEADO (400 Bad Request)** | `400` | **PASS** | `testAddReading_FutureTimestamp_Rejected400` |
| **TC-FT-07** | Intento de certificación con lectura > 12h. | `requestCertification` | **BLOQUEADO (422)** + Audit `COLD_CHAIN_DATA_GAP` | `422` | **PASS** | `testCertificationBlocked_WhenColdChainReadingExpired_Throws422` |
| **TC-FT-08** | Intento de certificación sin lecturas. | `requestCertification` | **BLOQUEADO (422)** | `422` | **PASS** | `testCertificationBlocked_WhenNoColdChainReadings_Throws422` |
| **TC-FT-09** | Intento de despacho con lectura > 12h. | `registerDispatch` | **BLOQUEADO (422)** | `422` | **PASS** | `testDispatchBlocked_WhenColdChainReadingExpired_Throws422` |
| **TC-FT-10** | Certificación con lectura fresca (2h). | `requestCertification` | **PERMITIDO** | `200` | **PASS** | `testCertificationAllowed_WhenColdChainReadingIsFresh` |

---

## 5. Batería de Pruebas de Documentos y Magic Bytes PDF (Casos DOC-01 a DOC-11, Casos 10 y 11)

| Caso | Escenario y Precondición | Intento de Operación | Resultado Esperado | HTTP Status | PASS / FAIL | Evidencia Automatizada |
| :---: | :--- | :--- | :--- | :---: | :---: | :--- |
| **TC-DOC-01** | Primer documento cargado. | `uploadDocumentFile` | `version = 1`, `active = true`, SHA-256 calculado | `200` | **PASS** | `testDocumentUpload_FirstVersion_SetsVersion1` |
| **TC-DOC-02** | Segunda versión del mismo tipo. | `uploadDocumentFile` | `version = 2`, `active = true`, conserva v1 (`active = false`) | `200` | **PASS** | `testDocumentUpload_SecondVersion_SetsVersion2_AndPreservesHistory` |
| **TC-DOC-03** | Distintos tipos de documento. | `uploadDocumentFile` | Secuencias independientes de versionamiento por tipo | `200` | **PASS** | `testDocumentUpload_DifferentTypes_IndependentSequences` |
| **TC-DOC-04** | Lote en estado `CERTIFIED`. | `uploadDocumentFile` | **BLOQUEADO (409 Conflict)** por inmutabilidad | `409` | **PASS** | `testDocumentUpload_CertifiedLot_Blocked409` |
| **TC-DOC-05** | Lote en estado `DISPATCHED`. | `uploadDocumentFile` | **BLOQUEADO (409 Conflict)** por inmutabilidad | `409` | **PASS** | `testDocumentUpload_DispatchedLot_Blocked409` |
| **TC-DOC-06** | Archivo con Magic Bytes PDF válidos (`%PDF-`). | `uploadDocumentFile` | Aceptado y persistido con checksum SHA-256 (64 hex) | `200` | **PASS** | `testDocumentUpload_ValidPdfMagicBytes_Accepted` |
| **TC-DOC-07** | Archivo `.exe` camuflado con extensión `.pdf` (MZ header). | `uploadDocumentFile` | **BLOQUEADO (400 Bad Request)** por cabecera mágica inválida | `400` | **PASS** | `testDocumentUpload_SpoofedExecutableAsPdf_Rejected400` |
| **TC-DOC-08** | Archivo de texto plano camuflado como `.pdf`. | `uploadDocumentFile` | **BLOQUEADO (400 Bad Request)** | `400` | **PASS** | `testDocumentUpload_PlainTextAsPdf_Rejected400` |
| **TC-DOC-09** | Archivo vacío (0 bytes). | `uploadDocumentFile` | **BLOQUEADO (400 Bad Request)** | `400` | **PASS** | `testDocumentUpload_EmptyFile_Rejected400` |
| **TC-DOC-10** | Extensión no permitida (`.docx`). | `uploadDocumentFile` | **BLOQUEADO (400 Bad Request)** | `400` | **PASS** | `testDocumentUpload_InvalidExtension_Rejected400` |
| **TC-DOC-11** | Registro de auditoría forense. | `uploadDocumentFile` | Auditoría inmutable `DOCUMENT_UPLOADED` registrada | `200` | **PASS** | `testDocumentUpload_EmitsAuditLog` |

---

## 6. Batería de Pruebas de Seguridad RBAC y Sesiones JWT (Casos SEC-01 a SEC-11, Casos 17, 18 y 19)

| Caso | Escenario y Precondición | Intento de Operación | Resultado Esperado | HTTP Status | PASS / FAIL | Evidencia Automatizada |
| :---: | :--- | :--- | :--- | :---: | :---: | :--- |
| **TC-SEC-01** | Usuario PRODUCCION intenta registrar inspección QA. | `POST /api/quality/lot/{id}` | **BLOQUEADO (403 Forbidden)** por `@PreAuthorize` | `403` | **PASS** | `SecurityRbacIntegrationTest.testCaso17_ProduccionTriesQaInspection_Forbidden403` |
| **TC-SEC-02** | Usuario PRODUCCION intenta reinspección QA. | `POST /api/quality/lot/{id}/reinspect` | **BLOQUEADO (403 Forbidden)** | `403` | **PASS** | `SecurityRbacIntegrationTest.testCaso17_ProduccionTriesQaReinspection_Forbidden403` |
| **TC-SEC-03** | Usuario QA registra inspección de calidad. | `POST /api/quality/lot/{id}` | **AUTORIZADO (200 OK)** | `200` | **PASS** | `SecurityRbacIntegrationTest.testCaso17_QaRegistersInspection_Ok200` |
| **TC-SEC-04** | Usuario PRODUCCION intenta despachar contenedor. | `POST /api/dispatches/lot/{id}` | **BLOQUEADO (403 Forbidden)** | `403` | **PASS** | `SecurityRbacIntegrationTest.testCaso19_ProduccionTriesDispatch_Forbidden403` |
| **TC-SEC-05** | Usuario LOGISTICA accede a endpoints de despacho. | `POST /api/dispatches/lot/{id}` | **AUTORIZADO** por Spring Security (no 403) | `200/422` | **PASS** | `SecurityRbacIntegrationTest.testCaso19_LogisticaAccessesDispatch_AuthorizedBySecurity` |
| **TC-SEC-06** | Petición anónima (sin token) a endpoint protegido. | `POST /api/quality/lot/{id}` | **BLOQUEADO (401/403)** | `401/403` | **PASS** | `SecurityRbacIntegrationTest.testAnonymousRequest_Blocked` |
| **TC-SEC-07** | Usuario activo inicia sesión. | `login` y petición con JWT | **AUTORIZADO (200 OK)** | `200` | **PASS** | `UserDeactivationSecurityTest.testUserActive_LoginAndRequest_Success` |
| **TC-SEC-08** | Usuario desactivado con JWT activo previo. | Petición subsiguiente con JWT | **RECHAZADO INMEDIATAMENTE (401/403)** | `401/403` | **PASS** | `UserDeactivationSecurityTest.testUserDeactivated_JwtRejectedImmediately` |
| **TC-SEC-09** | Usuario desactivado intenta refrescar token. | `refreshSession` | **RECHAZADO (`SESSION_REVOKED`)** | RuntimeException | **PASS** | `UserDeactivationSecurityTest.testUserDeactivated_RefreshTokenRejected` |
| **TC-SEC-10** | Usuario desactivado intenta iniciar sesión. | `login` | **BLOQUEADO** (cuenta desactivada) | RuntimeException | **PASS** | `UserDeactivationSecurityTest.testUserDeactivated_ReLoginBlocked` |
| **TC-SEC-11** | Reactivación de usuario no restaura tokens revocados. | `refreshSession` con token antiguo | **RECHAZADO (`SESSION_REVOKED`)** | RuntimeException | **PASS** | `UserDeactivationSecurityTest.testUserReactivated_DoesNotRestoreRevokedTokens` |
