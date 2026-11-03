# Plantilla Oficial de Solicitud de Cambio (RFC — Request for Change)

**Sistema Inteligente de Trazabilidad Hidrobiológica ExporTrace**  
*Utilice esta plantilla para documentar formalmente toda solicitud de cambio antes de su implementación.*

---

## 1. Identificación General de la RFC

| Campo | Detalle |
| :--- | :--- |
| **ID RFC** | `RFC-EXP-2026-XXX` |
| **Título del Cambio** | *[Título breve, claro y específico de la modificación]* |
| **Solicitante** | *[Nombre y rol dentro del equipo ExporTrace]* |
| **Fecha de Solicitud** | `AAAA-MM-DD` |
| **Tipo de Cambio** | `[ ] ESTÁNDAR` &nbsp;&nbsp; `[ ] NORMAL` &nbsp;&nbsp; `[ ] EMERGENCIA` |
| **Prioridad** | `[ ] BAJA` &nbsp;&nbsp; `[ ] MEDIA` &nbsp;&nbsp; `[ ] ALTA` &nbsp;&nbsp; `[ ] URGENTE` |
| **Estado Actual de la RFC** | `[ ] PLANIFICADO` &nbsp;&nbsp; `[ ] APROBADO` &nbsp;&nbsp; `[ ] EN_IMPLEMENTACION` &nbsp;&nbsp; `[ ] COMPLETADO` &nbsp;&nbsp; `[ ] RECHAZADO` &nbsp;&nbsp; `[ ] REVERTIDO` |
| **Incidencia Relacionada (si aplica)** | `INC-2026-XXXXXX` / `N/A` |

---

## 2. Descripción y Justificación

- **Descripción detallada del cambio:**
  > *[Explicar qué se va a agregar, modificar o eliminar en ExporTrace y cómo funcionará después del cambio.]*

- **Justificación técnica / de negocio:**
  > *[Explicar por qué es necesario el cambio, qué requerimiento académico u operativo resuelve y cuál es el riesgo de NO realizarlo.]*

---

## 3. Alcance Técnico y Componentes Afectados

- **Ambientes involucrados:**
  - `[ ] Desarrollo Local (localhost:8080 / localhost:5173)`
  - `[ ] Integración Continua (GitHub Actions Ubuntu Runner)`
  - `[ ] Producción / Cloud (Render Backend + Frontend)`
- **Capas y componentes afectados:**
  - `[ ] Backend Spring Boot (exportrace-ica-backend)`: *[Listar clases/paquetes]*
  - `[ ] Frontend React + TypeScript (exportrace-ica-frontend)`: *[Listar componentes/páginas/servicios]*
  - `[ ] Base de Datos (SQLite / Migraciones)`: *[Listar tablas, columnas o índices]*
  - `[ ] Infraestructura / Variables de Entorno`: *[Listar variables, ej. JIRA_ENABLED]*
  - `[ ] Integración Externa (Jira Service Management)`: *[Detallar endpoints o payloads]*
  - `[ ] Documentación (/docs)` y/o `Workflow CI (.github/workflows/ci.yml)`
- **Dependencias previas:**
  > *[Indicar si requiere otra RFC completada, credenciales configuradas, migración previa, etc.]*

---

## 4. Evaluación de Impacto y Riesgo

| Criterio | Valoración | Sustento |
| :--- | :---: | :--- |
| **Probabilidad de Falla** | `BAJA / MEDIA / ALTA` | *[Justificar según complejidad técnica]* |
| **Impacto en la Operación / SLA / Seguridad** | `BAJO / MEDIO / ALTO` | *[Indicar módulos o roles afectados]* |
| **Nivel de Riesgo Resultante (`Probabilidad × Impacto`)** | **`BAJO / MEDIO / ALTO / CRÍTICO`** | *[Según matriz de PLAN_CONTROL_CAMBIOS.md]* |
| **¿Requiere Backup Previo de Base de Datos?** | `SÍ / NO` | *[Obligatorio si modifica esquema o datos]* |

---

## 5. Trazabilidad en GitHub y Plan de Pruebas

- **Repositorio(s):** `Integrador_II_Backend` / `Integrador_II_Frontend`
- **Rama Git asignada:** `feature/RFC-EXP-2026-XXX-nombre-descriptivo`
- **Título del Pull Request:** `[RFC-EXP-2026-XXX] Título del cambio`
- **Casos de prueba a validar:**
  1. **Pruebas previas locales:** *[Describir pruebas manuales/unitarias en entorno local]*
  2. **Pruebas automatizadas nuevas o actualizadas:** *[Listar clases/métodos JUnit, ej. `INC-C01..INC-C15`]*
  3. **Pruebas de regresión en GitHub Actions CI:**
     - `[ ] Backend CI (Java 21 & Maven)`: `mvn -B clean test` (**118/118 PASS**) + `mvn -B package -DskipTests`.
     - `[ ] Frontend CI (Node 24, TypeScript & Vite)`: `npm ci` + `npm run build` (`0` errores).
  4. **Pruebas Post-Deploy (Smoke Test):** *[Describir verificación posterior al despliegue]*

---

## 6. Plan de Implementación y Ventana

- **Fecha y ventana propuesta:** `AAAA-MM-DD` — `[HH:MM a HH:MM]`
- **Responsable de implementación:** *[Nombre / Rol]*
- **Secuencia paso a paso de implementación:**
  1. Verificar respaldo previo (código Git / archivo `.db` si aplica).
  2. Desarrollar y validar en la rama dedicada `feature/RFC-EXP-2026-XXX-...`.
  3. Abrir Pull Request hacia `main` y confirmar ejecución exitosa (`PASS`) de **GitHub Actions**.
  4. Realizar Code Review y aprobar el Pull Request.
  5. Ejecutar Merge a `main` y verificar despliegue.
  6. Ejecutar Smoke Test post-implementación.

---

## 7. Plan de Rollback (Reversión)

| Pregunta de Control | Respuesta Operativa para esta RFC |
| :--- | :--- |
| **1. ¿Qué versión/commit estaba estable antes del cambio?** | *[Indicar hash de commit o tag estable en `main`]* |
| **2. ¿Cómo regresar el código y el servicio a la versión estable?** | *[Ej. `git revert <commit>` + Redeploy del último deploy estable en Render]* |
| **3. ¿Qué base de datos debe respaldarse y cómo se restaura?** | *[Ej. Restaurar archivo `exportrace_pre_RFC-EXP-2026-XXX.db` o N/A]* |
| **4. ¿Qué variables de entorno o feature flags deben revertirse?** | *[Ej. Asegurar `JIRA_ENABLED=false` o restaurar variable previa]* |
| **5. ¿Cuánto tarda la reversión estimada?** | *[Ej. 5 – 10 minutos]* |
| **6. ¿Quién ejecuta el rollback si se detecta un fallo?** | **Responsable de Operaciones / Líder Técnico** |

---

## 8. Autorización, Resultado y Lecciones Aprendidas

| Rol Aprobador | Nombre / Cargo en el Proyecto | Decisión (`APROBADO` / `RECHAZADO`) | Fecha |
| :--- | :--- | :---: | :---: |
| **Product Owner** | Equipo ExporTrace — PO | `[ ] APROBADO` &nbsp; `[ ] RECHAZADO` | `AAAA-MM-DD` |
| **Líder Técnico** | Equipo ExporTrace — Tech Lead | `[ ] APROBADO` &nbsp; `[ ] RECHAZADO` | `AAAA-MM-DD` |
| **Operaciones / Despliegue** *(Si Riesgo Alto/Crítico)* | Equipo ExporTrace — Operaciones | `[ ] APROBADO` &nbsp; `[ ] RECHAZADO` | `AAAA-MM-DD` |

- **Resultado final de la implementación:** `[ ] EXITOSO` &nbsp;&nbsp; `[ ] EXITOSO CON OBSERVACIONES` &nbsp;&nbsp; `[ ] FALLIDO / REVERTIDO`
- **Lecciones aprendidas / Observaciones de cierre:**
  > *[Registrar hallazgos relevantes tras el cierre o enlazar al documento PIR correspondiente.]*

---

## Ejemplo Completo de Referencia: `RFC-EXP-2026-004`

| Campo | Valor Registrado |
| :--- | :--- |
| **ID RFC** | `RFC-EXP-2026-004` |
| **Título** | Integración REST ExporTrace $\rightarrow$ Jira Service Management Cloud (Fase C) |
| **Solicitante** | Desarrollador Backend / Líder Técnico ExporTrace |
| **Fecha** | `2026-10-21` |
| **Tipo de Cambio** | `NORMAL` |
| **Prioridad / Impacto / Riesgo** | Prioridad: `ALTA` \| Impacto: `MEDIO` \| Riesgo: **`MEDIO`** |
| **Descripción** | Implementar `JiraIncidentService` e `IncidentSyncService` para sincronizar incidencias locales (`IncidentReport`) hacia Jira Service Management Cloud con estrategia idempotente `LOOKUP BEFORE CREATE` (`correlationId`), separación de transacción local SQLite y reintento manual administrativo (`POST /api/admin/incidents/{id}/retry-sync`). |
| **Justificación** | Permitir que la mesa de ayuda gestione tickets en Jira Service Management sin que el usuario final salga de ExporTrace y sin perder incidencias si Jira presenta caídas o timeouts. |
| **Componentes Afectados** | Backend: `JiraProperties`, `JiraIncidentService`, `IncidentSyncService`, `IncidentService`, `IncidentController`, `application.properties`. Frontend: `incident.ts`, `incidentService.ts`, `ReportIncidentModal.tsx`, `IncidentDetailPage.tsx`. |
| **Dependencias** | Fases A y B aprobadas (`103/103 PASS`). |
| **Casos de Prueba** | Suite `JiraIncidentSyncTest` (`INC-C01` a `INC-C15`, `15/15 PASS`) + regresión total (`118/118 PASS`) + `npm run build` (`0` errores). |
| **Plan de Rollback** | 1) Mantener o conmutar `JIRA_ENABLED=false` en variables de entorno (desactiva llamadas externas en < 2 min conservando 100% la operación local). 2) Si se requiere reversión de código: `git revert` del merge commit en `main`. |
| **Aprobadores** | Product Owner + Líder Técnico |
| **Resultado** | `COMPLETADO` localmente (`118/118 PASS`, listo para activación con credenciales reales de Jira). |
