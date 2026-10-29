# Guía de Configuración: Jira Service Management Cloud para ExporTrace (Fase C)

Este documento describe paso a paso cómo configurar **Jira Service Management (JSM) Cloud** para habilitar la integración REST desde el backend Spring Boot de **ExporTrace** (`FASE C`).

> **Regla de Seguridad:**
> Nunca almacenes `JIRA_API_TOKEN`, credenciales reales ni cabeceras `Authorization` dentro de `application.properties`, `.env` versionados, frontend (`VITE_*`), logs ni documentación. Todos los ejemplos de esta guía utilizan exclusivamente placeholders.

---

## 1. Crear o Seleccionar el Proyecto en Jira Service Management

1. Ingresa a tu instancia de Atlassian Cloud (`https://TU-DOMINIO.atlassian.net`).
2. Ve a **Proyectos** → **Crear proyecto** → **Jira Service Management**.
3. Selecciona la plantilla **Gestión de servicios de TI (ITSM)** o **Asistencia al cliente**.
4. Define:
   - **Nombre:** `Mesa de Ayuda ExporTrace`
   - **Clave de proyecto sugerida:** `EXP` (los tickets generados tendrán formato `EXP-1`, `EXP-2`, etc.).

---

## 2. Identificar el `serviceDeskId`

El `serviceDeskId` es el identificador numérico del portal de servicio en la API de Jira Service Management (distinto del `projectId` de Jira Core).

Puedes consultarlo ejecutando:

```bash
curl -s -u "cuenta-integracion@example.com:TU_API_TOKEN" \
  -H "Accept: application/json" \
  "https://TU-DOMINIO.atlassian.net/rest/servicedeskapi/servicedesk"
```

En el JSON de respuesta ubica tu proyecto `EXP`:

```json
{
  "values": [
    {
      "id": "XX",
      "projectId": "10001",
      "projectName": "Mesa de Ayuda ExporTrace",
      "projectKey": "EXP"
    }
  ]
}
```

El campo `"id"` corresponde a `JIRA_SERVICE_DESK_ID`.

---

## 3. Crear el Request Type: `"Reportar incidencia ExporTrace"`

1. Dentro del proyecto en Jira Service Management, ingresa a **Configuración del proyecto** → **Tipos de solicitud (Request types)**.
2. Haz clic en **Crear tipo de solicitud**:
   - **Nombre:** `Reportar incidencia ExporTrace`
   - **Tipo de incidencia subyacente (Issue Type):** `[System] Incident` o `Support`.
3. Guarda el Request Type.

---

## 4. Identificar el `requestTypeId`

Consulta los tipos de solicitud disponibles en tu `serviceDeskId`:

```bash
curl -s -u "cuenta-integracion@example.com:TU_API_TOKEN" \
  -H "Accept: application/json" \
  "https://TU-DOMINIO.atlassian.net/rest/servicedeskapi/servicedesk/XX/requesttype"
```

En la respuesta ubica `"Reportar incidencia ExporTrace"`:

```json
{
  "values": [
    {
      "id": "XX",
      "name": "Reportar incidencia ExporTrace",
      "serviceDeskId": "XX"
    }
  ]
}
```

El campo `"id"` corresponde a `JIRA_REQUEST_TYPE_ID`.

---

## 5. Crear el Custom Field `"ExporTrace Correlation ID"`

Para garantizar **idempotencia estricta (`LOOKUP BEFORE CREATE`)** y evitar tickets duplicados ante timeouts de red, ExporTrace almacena su `correlationId` (UUID v4) en un campo dedicado de Jira:

1. Ve a **Configuración de Jira (⚙️)** → **Incidencias (Issues)** → **Campos personalizados (Custom fields)**.
2. Haz clic en **Crear campo personalizado**:
   - **Tipo:** `Campo de texto (una sola línea)` (`Short text / plain text`).
   - **Nombre:** `ExporTrace Correlation ID`
   - **Descripción:** `Identificador UUID único de correlación generado por ExporTrace para idempotencia.`
3. Asócialo a las pantallas del proyecto `Mesa de Ayuda ExporTrace` (`EXP`).
4. Opcionalmente, puedes crear un segundo campo de texto llamado `ExporTrace Incident Code` para almacenar en un campo separado el código legible `INC-2026-XXXXXX` (`JIRA_EXPORTRACE_CODE_FIELD_ID`).

---

## 6. Obtener el ID del Custom Field (`customfield_XXXXX`)

Consulta el catálogo de campos de Jira mediante:

```bash
curl -s -u "cuenta-integracion@example.com:TU_API_TOKEN" \
  -H "Accept: application/json" \
  "https://TU-DOMINIO.atlassian.net/rest/api/3/field"
```

Busca el objeto cuyo `"name"` sea `"ExporTrace Correlation ID"`:

```json
{
  "id": "customfield_XXXXX",
  "key": "customfield_XXXXX",
  "name": "ExporTrace Correlation ID",
  "custom": true
}
```

El valor `"customfield_XXXXX"` corresponde a `JIRA_CORRELATION_FIELD_ID`.

---

## 7. Incluir el Custom Field en el Request Type y Consultar sus Campos

1. Ve a **Configuración del proyecto** → **Tipos de solicitud** → **Reportar incidencia ExporTrace** → **Formulario de solicitud (Request form)**.
2. Haz clic en **Agregar campo** y selecciona `ExporTrace Correlation ID` (`customfield_XXXXX`).
   - Puedes marcarlo como visible u oculto con valor predefinido si tu esquema de JSM lo permite, siempre que aparezca expuesto en el endpoint de campos del Request Type.
3. Verifica que el Request Type exponga correctamente `summary`, `description` y `customfield_XXXXX` consultando:

```bash
curl -s -u "cuenta-integracion@example.com:TU_API_TOKEN" \
  -H "Accept: application/json" \
  "https://TU-DOMINIO.atlassian.net/rest/servicedeskapi/servicedesk/XX/requesttype/XX/field"
```

Respuesta esperada:

```json
{
  "requestTypeFields": [
    { "fieldId": "summary", "name": "Resumen", "required": true },
    { "fieldId": "description", "name": "Descripción", "required": false },
    { "fieldId": "customfield_XXXXX", "name": "ExporTrace Correlation ID", "required": true }
  ]
}
```

> **Nota sobre el campo `priority`:**
> `JiraIncidentService` inspecciona dinámicamente `requestTypeFields`.
> - Si el Request Type incluye `"fieldId": "priority"`, ExporTrace envía automáticamente la prioridad mapeada (`P1_CRITICAL` → `Highest`, `P2_HIGH` → `High`, `P3_MEDIUM` → `Medium`, `P4_LOW` → `Low`).
> - Si el Request Type **no** incluye `priority` en el formulario del portal, ExporTrace **no falla**: envía la prioridad dentro de la descripción estructurada y puedes configurar una regla en **Jira Automation** (*When Issue Created → If description contains `Prioridad ExporTrace: P1_CRITICAL` → Edit issue fields: Priority = Highest*) para reflejarla automáticamente.

---

## 8. Generar el API Token de la Cuenta de Integración

1. Inicia sesión en Atlassian con la cuenta técnica de integración (con permisos de Agente / Service Desk Team o cliente autorizado en el proyecto `EXP`).
2. Ingresa a: `https://id.atlassian.com/manage-profile/security/api-tokens`
3. Haz clic en **Crear token de API (Create API token)**.
4. Asígnale la etiqueta `exportrace-backend-integration`.
5. Copia el token generado y configúralo únicamente como variable de entorno en el servidor backend.

---

## 9. Configurar Variables de Entorno en el Backend

Configura las variables de entorno en tu entorno de ejecución (Render / contenedor / variables de sistema local no versionadas):

```dotenv
JIRA_ENABLED=false
JIRA_BASE_URL=https://TU-DOMINIO.atlassian.net
JIRA_SERVICE_DESK_ID=XX
JIRA_REQUEST_TYPE_ID=XX
JIRA_ACCOUNT_EMAIL=cuenta-integracion@example.com
JIRA_API_TOKEN=***
JIRA_CORRELATION_FIELD_ID=customfield_XXXXX
JIRA_EXPORTRACE_CODE_FIELD_ID=
JIRA_CONNECT_TIMEOUT_SECONDS=5
JIRA_READ_TIMEOUT_SECONDS=10
```

- Mientras `JIRA_ENABLED=false`, ExporTrace opera en modo 100% local sin intentar conexiones externas.
- Cuando cambies a `JIRA_ENABLED=true`, `IncidentSyncService` y `JiraIncidentService` sincronizarán cada nueva incidencia hacia Jira Service Management después de confirmar la transacción local en base de datos.

---

## 10. Probar Autenticación y Creación

1. **Preflight de autenticación y metadata:**
   Verifica que las credenciales y el Request Type respondan `HTTP 200` ejecutando el comando `curl` del **Paso 7**.
2. **Prueba desde ExporTrace:**
   - Inicia sesión en ExporTrace y reporta una incidencia desde el Centro de Soporte (`POST /api/incidents`).
   - Verifica que la respuesta `201 Created` retorne:
     - `syncStatus: "SYNCED"`
     - `jiraIssueKey: "EXP-XX"`
     - `slaAuthority: "JIRA_AUTHORITATIVE"`
3. **Prueba de Reintento Administrativo (`LOOKUP BEFORE CREATE`):**
   - Si una incidencia quedó en `RETRY_PENDING` o `ERROR`, un usuario con rol `ADMINISTRADOR` o `SUPERADMIN` puede invocar:
     `POST /api/admin/incidents/{id}/retry-sync`
   - El backend buscará primero por `correlationId` en Jira (`/rest/api/3/search`) antes de crear un nuevo ticket, garantizando cero duplicados.

---

## 11. Revocación y Rotación del Token en Caso de Compromiso

Si sospechas que el `JIRA_API_TOKEN` ha sido expuesto:

1. Ingresa inmediatamente a `https://id.atlassian.com/manage-profile/security/api-tokens`.
2. Localiza el token `exportrace-backend-integration` y haz clic en **Revocar (Revoke)**.
3. Genera un nuevo token de API.
4. Actualiza la variable de entorno `JIRA_API_TOKEN` en el servidor backend y reinicia el servicio.
5. Ejecuta `POST /api/admin/incidents/{id}/retry-sync` sobre las incidencias que hayan quedado en `RETRY_PENDING` o `ERROR` durante la ventana de rotación.
