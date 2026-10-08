# Guía de Visualización y Consultas SQL para la Sustentación
## Base de Datos Relacional ExporTrace (`exportrace.db`)

Esta guía explica **cómo abrir la base de datos visualmente para proyectarla ante el jurado**, qué herramientas utilizar y proporciona **un catálogo de consultas SQL maestras listas para copiar y pegar durante la exposición**.

---

## 1. ¿Dónde está físicamente la Base de Datos?

El archivo de base de datos relacional se encuentra en la raíz del backend:

```text
exportrace-ica-backend/
└── exportrace.db          <-- ARCHIVO PRINCIPAL DE BASE DE DATOS (SQLite)
```

> **Ruta Local:** `C:\Users\mayco\.gemini\antigravity\scratch\exportrace-ica-backend\exportrace.db`  
> **Ruta en Render Cloud:** `/app/data/exportrace.db` (Disco Persistente)

---

## 2. Las Mejores Opciones Visuales para Mostrar la BD al Jurado

### Opción A (La Más Recomendada): DB Browser for SQLite
Es la herramienta gráfica más intuitiva y estándar del mercado para bases de datos SQLite.
1. **Descargar e instalar gratis:** [https://sqlitebrowser.org/dl/](https://sqlitebrowser.org/dl/) (Windows Portable o Instalador).
2. **Abrir la BD:** Abrir el programa $\rightarrow$ Clic en **"Abrir base de datos"** $\rightarrow$ Seleccionar el archivo `exportrace.db`.
3. **Pestañas clave para mostrar:**
   - **Estructura de la base de datos:** Muestra las 13 tablas, claves foráneas, tipos de datos e índices `UNIQUE`.
   - **Navegar datos:** Permite ver las filas de cualquier tabla como si fuera Excel.
   - **Ejecutar SQL:** Pestaña interactiva donde puedes pegar las consultas que se detallan a continuación.

---

### Opción B: Extensión de VS Code (Para proyectar directamente en el Editor)
Si estás proyectando Visual Studio Code:
1. Ir a la pestaña **Extensiones** (`Ctrl + Shift + X`) en VS Code.
2. Buscar e instalar: **"SQLite Viewer"** (de Florian Klampfer).
3. En el explorador de archivos, hacer clic directamente sobre `exportrace.db`.
4. Se abrirá una interfaz visual dentro de VS Code para explorar tablas y registros.

---

### Opción C: DBeaver Community
Para un perfil más corporativo/empresarial:
1. Crear nueva conexión $\rightarrow$ Seleccionar **SQLite**.
2. Seleccionar el archivo `exportrace.db` $\rightarrow$ Clic en **Test Connection** $\rightarrow$ **Finalizar**.
3. Permite generar diagramas Entidad-Relación (ER) automáticos en tiempo real.

---

## 3. Catálogo de Consultas SQL Maestras para Impresionar al Jurado

Estas consultas están optimizadas para demostrar que el sistema cumple con todas las reglas de negocio, relaciones 1:N, auditoría y trazabilidad.

---

### Consulta 1: Trazabilidad Integral de Lotes (End-to-End)
> **Qué demuestra al docente:** Que cada lote está vinculado a su producto, estado en la máquina de estados, fecha de producción, peso y token QR criptográfico.

```sql
SELECT 
    l.id AS lote_id,
    l.codigo AS codigo_lote,
    p.nombre AS producto,
    p.tipo_conservacion AS tipo_frio,
    l.estado AS estado_actual,
    l.peso_neto_kg AS peso_kg,
    l.fecha_produccion,
    l.version AS version_concurrencia,
    l.qr_token
FROM lots l
INNER JOIN products p ON l.producto_id = p.id
ORDER BY l.id DESC;
```

---

### Consulta 2: Demostración del Historial 1:N de Reinspecciones QA (Caso 5 Oficial)
> **Qué demuestra al docente:** Que ante un lote OBSERVADO, el sistema **no sobrescribe** la primera inspección, sino que crea la secuencia `#2` y `#3` preservando todo el historial inmutable.

```sql
SELECT 
    qi.id AS inspection_id,
    l.codigo AS codigo_lote,
    qi.numero_inspeccion AS seq_num,
    qi.inspector_nombre,
    qi.resultado_organoleptico AS dictamen,
    qi.motivo_reinspeccion,
    qi.observaciones,
    qi.fecha_inspeccion,
    qi.fecha_creacion
FROM quality_inspections qi
INNER JOIN lots l ON qi.lote_id = l.id
ORDER BY qi.lote_id ASC, qi.numero_inspeccion ASC;
```

---

### Consulta 3: Evidencias Fotográficas Organolépticas con Hash SHA-256 (Caso 16 / OM-05)
> **Qué demuestra al docente:** Custodia forense de archivos físicos, nombres UUID anti-colisión, tamaño y verificación de integridad criptográfica SHA-256.

```sql
SELECT 
    qe.id AS evidencia_id,
    l.codigo AS codigo_lote,
    qe.original_file_name AS nombre_original,
    qe.stored_file_name AS archivo_fisico_uuid,
    qe.mime_type,
    ROUND(qe.file_size / 1024.0, 2) AS tamano_kb,
    qe.sha256 AS hash_sha256,
    qe.uploaded_by AS inspector_qa,
    qe.uploaded_at AS fecha_subida,
    CASE WHEN qe.active = 1 THEN 'ACTIVA' ELSE 'DESACTIVADA' END AS estado_logico
FROM qa_evidences qe
INNER JOIN lots l ON qe.lot_id = l.id
ORDER BY qe.id DESC;
```

---

### Consulta 4: Expediente Digital y Versionamiento de Documentos (Casos 10 y 11)
> **Qué demuestra al docente:** Que los documentos subidos (Declaración Jurada, Certificado de Origen) gestionan versiones secuenciales (`v1`, `v2`) con hash SHA-256 y archivado histórico.

```sql
SELECT 
    d.id AS doc_id,
    l.codigo AS codigo_lote,
    d.tipo AS tipo_documento,
    d.version AS numero_version,
    d.nombre AS nombre_archivo,
    d.sha256 AS checksum_sha256,
    d.subido_por,
    d.fecha_subida,
    CASE WHEN d.active = 1 THEN 'VIGENTE' ELSE 'HISTORICO_ARCHIVADO' END AS estado_version
FROM documents d
INNER JOIN lots l ON d.lote_id = l.id
ORDER BY d.lote_id ASC, d.tipo ASC, d.version DESC;
```

---

### Consulta 5: Histórico de Telemetría Térmica e Incidencias de Frío (Casos 6, 7 y 8)
> **Qué demuestra al docente:** Clasificación de lecturas (`NORMAL`, `WARNING`, `CRITICAL`), rango dinámico por producto y ciclo de vida de incidencias de frío (`ACTIVE` $\rightarrow$ `UNDER_REVIEW` $\rightarrow$ `RESOLVED`).

```sql
-- A. Historial de Lecturas de Temperatura
SELECT 
    ccr.id AS log_id,
    l.codigo AS codigo_lote,
    p.nombre AS producto,
    ccr.temperatura_celsius AS temp_c,
    ccr.estado_medicion AS clasificacion,
    ccr.ubicacion_camara,
    ccr.responsable_nombre,
    ccr.fecha_hora AS timestamp_medicion
FROM cold_chain_records ccr
INNER JOIN lots l ON ccr.lote_id = l.id
INNER JOIN products p ON l.producto_id = p.id
ORDER BY ccr.fecha_hora DESC
LIMIT 15;

-- B. Incidencias Críticas y Subsanación Técnica de QA
SELECT 
    cci.id AS incidencia_id,
    l.codigo AS codigo_lote,
    cci.temperatura_leida AS temp_desviacion,
    cci.limite_temperatura AS limite_normativo,
    cci.estado AS estado_incidencia,
    cci.justificacion_tecnica,
    cci.resuelto_por AS qa_resolutor,
    cci.fecha_creacion,
    cci.fecha_resolucion
FROM cold_chain_incidents cci
INNER JOIN lots l ON cci.lote_id = l.id
ORDER BY cci.id DESC;
```

---

### Consulta 6: Pista de Auditoría Forense y Seguridad del Sistema (`audit_logs`)
> **Qué demuestra al docente:** Que todas las acciones sensibles (cambios de rol, bloqueos de seguridad `ACCESS_DENIED`, intentos de login fallidos, transiciones de lote) quedan inmutablemente registradas.

```sql
SELECT 
    id,
    user_name AS usuario,
    user_role AS rol,
    action AS accion_ejecutada,
    module AS modulo,
    entity_name AS entidad_afectada,
    entity_id AS id_registro,
    result AS resultado,
    details AS detalle_operacional,
    created_at AS fecha_evento
FROM audit_logs
ORDER BY created_at DESC
LIMIT 20;
```

---

### Consulta 7: Sesiones de Usuario, Control de JWT y Revocaciones (Caso 18)
> **Qué demuestra al docente:** Que las sesiones no son solo tokens en memoria, sino que tienen estado en BD (`ACTIVE`, `REVOKED`, `EXPIRED`) con control de inactividad y duración absoluta.

```sql
SELECT 
    us.id AS session_id,
    u.nombre || ' ' || COALESCE(u.apellido, '') AS usuario,
    r.nombre AS rol,
    us.status AS estado_sesion,
    us.revocation_reason AS motivo_revocacion,
    us.ip_address,
    us.last_activity_at AS ultima_actividad,
    us.expires_at AS expiracion_absoluta
FROM user_sessions us
INNER JOIN users u ON us.user_id = u.id
LEFT JOIN roles r ON u.role_id = r.id
ORDER BY us.last_activity_at DESC;
```

---

## 4. Guion Paso a Paso para la Demostración en Vivo

Si el docente pide: *"A ver, muéstrame la base de datos funcionando en vivo"*:

1. **Paso 1:** Abre **DB Browser for SQLite** y carga `exportrace.db`.
2. **Paso 2 (Estructura):** Muestra la pestaña **Estructura** y señala las tablas `lots`, `quality_inspections`, `cold_chain_records`, `documents`, `qa_evidences` y `audit_logs`. Explica que están modeladas con claves foráneas e índices únicos.
3. **Paso 3 (Datos en Vivo):** Ve a la pestaña **Ejecutar SQL** y corre la **Consulta 1 (Trazabilidad de Lotes)**.
4. **Paso 4 (Demostrar Reinspecciones QA - Caso 5):** Corre la **Consulta 2** y muestra cómo un lote observado tiene `seq_num = 1` y luego `seq_num = 2` con su dictamen conforme sin haber borrado el primer registro.
5. **Paso 5 (Demostrar Evidencias Criptográficas):** Corre la **Consulta 3** y muestra los hashes SHA-256 de 64 caracteres generados para las fotos.
6. **Paso 6 (Demostrar Auditoría):** Corre la **Consulta 6** y muestra cómo el sistema registra cada intento de acceso y transición de lote.
