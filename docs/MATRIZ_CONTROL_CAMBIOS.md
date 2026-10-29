# Matriz Maestra de Control de Cambios — ExporTrace

**Curso:** Taller Integrador II — Semana 11: Control de Cambios e Integración Continua  
**Sistema:** Sistema Inteligente de Trazabilidad Hidrobiológica ExporTrace  

---

## 1. Tabla Resumen de Solicitudes de Cambio (RFCs), Riesgo, Trazabilidad Git/CI e Incidencias

| ID RFC | Título del Cambio | Tipo | Riesgo | Capas Afectadas | Rama Git / Pull Request | Compuerta GitHub Actions CI | Relación `RFC ↔ INC` | PIR Requerido | Estado Actual |
| :---: | :--- | :---: | :---: | :--- | :--- | :---: | :---: | :---: | :---: |
| **`RFC-EXP-2026-001`** | Migración de esquema `quality_inspections` (`1:1` $\rightarrow$ `1:N`) e historial de reinspección QA con evidencias SHA-256 | `NORMAL` | **`ALTO`** | Backend, BD (`DatabaseMigrationConfig`), Frontend QA | `main` (Integrador 2 Consolidado) | `118/118 PASS` | Previene incidencias en módulo `QUALITY` (`CAT-04`) | `SÍ` *(Cerrado sin incidentes)* | **`COMPLETADO`** |
| **`RFC-EXP-2026-002`** | Corrección de validación de ID numérico de lote en Cadena de Frío (`lot-001` $\rightarrow$ `Long`) | `EMERGENCIA` | **`CRÍTICO`** | Backend (`ColdChainController`), Frontend (`ColdChain`) | Hotfix consolidado en `main` | `118/118 PASS` | Resuelve error `HTTP 400` en módulo `COLD_CHAIN` (`CAT-05`) | `SÍ` *(Servicio restablecido)* | **`COMPLETADO`** |
| **`RFC-EXP-2026-003`** | Implementación del Centro de Soporte y Modelo Local de Incidencias (Fases A y B) | `NORMAL` | `MEDIO` | Backend (`IncidentReport`, `IncidentService`), BD, Frontend (`Support`) | Pendiente PR de funcionalidad a `main` | Validado localmente (`103/103 PASS` + Vite Build) | Habilita registro de `INC-2026-XXXXXX` (`INC-01`, `INC-03`, `INC-04`, `INC-12`) | `NO` *(Riesgo Medio sin incidentes)* | **`COMPLETADO`** *(En entorno local)* |
| **`RFC-EXP-2026-004`** | Integración REST ExporTrace $\rightarrow$ Jira Service Management con `LOOKUP BEFORE CREATE` (Fase C) | `NORMAL` | `MEDIO` | Backend (`JiraIncidentService`, `IncidentSyncService`), Frontend (`incident.ts`) | Pendiente PR de funcionalidad a `main` | Validado localmente con Mock (`118/118 PASS`) | Cubre `INC-01` (Mock), `INC-02` (`RETRY_PENDING`) e `INC-11` (Idempotencia) | `NO` *(Feature flag `JIRA_ENABLED=false`)* | **`COMPLETADO`** *(Probado con Mock)* |
| **`RFC-EXP-2026-005`** | Configuración del pipeline de Integración Continua en GitHub Actions (Java 21 + Node 24) | `NORMAL` | `BAJO` | CI (`.github/workflows/ci.yml`), Documentación (`/docs`) | Rama: `ci/github-actions-pipeline` en `Integrador_II_Backend` e `Integrador_II_Frontend` | **`VALIDADO_GITHUB_ACTIONS`** (`Backend CI PASS` + `Frontend CI PASS`) | Compuerta preventiva contra regresiones antes de merge | `SÍ` (`PIR-EXP-2026-005` registrado) | **`COMPLETADO`** |
| **`RFC-EXP-2026-006`** | Ajuste de etiquetas amigables de sincronización y visualización de ticket en UI de Soporte | `ESTÁNDAR` | `BAJO` | Frontend (`incident.ts`, `ReportIncidentModal.tsx`, `IncidentDetailPage.tsx`) | Pendiente PR junto con Fase C | Validado en `npm run build` (`0` errores) | Mejora claridad visual de `INC-01` e `INC-02` para el usuario | `NO` *(Cambio Estándar)* | **`COMPLETADO`** *(En entorno local)* |
| **`RFC-EXP-2026-007`** | Conexión y validación con instancia real de Jira Service Management Cloud (`VALIDADA_JIRA_REAL`) | `NORMAL` | `MEDIO` | Variables de entorno (`JIRA_*`), Proyecto JSM (`EXP`) | Ejecución de Preflight Local (`2026-10-21`) | Preflight ejecutado: **DETENIDO** por ausencia de variables de entorno `JIRA_*` en el host | Cierre en vivo de `INC-01` pendiente de variables `JIRA_*` | `SÍ` *(Pendiente cierre real)* | **`EN_IMPLEMENTACION`** *(Bloqueado en Preflight)* |
| **`RFC-EXP-2026-008`** | Comentarios Públicos, Adjuntos Seguros (`Magic Bytes + SHA-256`) y Reapertura (Fase D) | `NORMAL` | `ALTO` | Backend, BD (`incident_comments`, `incident_attachments`), Almacenamiento, Frontend | `feature/RFC-EXP-2026-008-incident-comments-attachments` *(Futura)* | Requerirá nuevos tests + CI `PASS` | Cubrirá `INC-07`, `INC-09`, `INC-10`, `INC-15` | `SÍ` | **`PLANIFICADO`** |
| **`RFC-EXP-2026-009`** | Webhook entrante Jira $\rightarrow$ ExporTrace con validación HMAC y deduplicación (Fase E) | `NORMAL` | **`ALTO`** | Backend (`JiraWebhookController`), BD (`jira_webhook_events`), Seguridad | `feature/RFC-EXP-2026-009-jira-webhook-hmac` *(Futura)* | Requerirá tests HMAC + CI `PASS` | Cubrirá `INC-05`, `INC-06`, `INC-08`, `INC-13` | `SÍ` | **`PLANIFICADO`** |

---

## 2. Resumen Estadístico de Control de Cambios

- **Total de RFCs Registradas en Matriz:** `9`
  - **Cambios Estándar (`STANDARD`):** `1` (`RFC-EXP-2026-006`)
  - **Cambios Normales (`NORMAL`):** `7` (`RFC-EXP-2026-001`, `003`, `004`, `005`, `007`, `008`, `009`)
  - **Cambios de Emergencia (`EMERGENCY`):** `1` (`RFC-EXP-2026-002`)
- **Estado de Ejecución:**
  - **`COMPLETADO`:** `6` (`RFC-EXP-2026-001` a `RFC-EXP-2026-006`)
  - **`EN_IMPLEMENTACION`:** `1` (`RFC-EXP-2026-007` — Detenido en Preflight por variables `JIRA_*` faltantes)
  - **`PLANIFICADO`:** `2` (`RFC-EXP-2026-008` a `RFC-EXP-2026-009`)
  - **`RECHAZADO` / `REVERTIDO`:** `0`
