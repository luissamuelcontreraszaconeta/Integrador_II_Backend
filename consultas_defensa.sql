-- ============================================================================
-- EXPORTRACE - CONSULTAS SQL PARA SUSTENTACIÓN Y EVALUACIÓN DE CASUÍSTICAS
-- Base de Datos: exportrace.db (SQLite / Compatible JPA MySQL)
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. SEGURIDAD Y CRIPTOGRAFÍA: Encriptación de Contraseñas (BCrypt $2a$10$)
-- Explicación: Irreversible, salting aleatorio de 128 bits, protección contra Rainbow Tables.
-- ----------------------------------------------------------------------------
SELECT 
    u.id,
    u.nombre,
    u.email,
    r.nombre AS rol,
    u.activo,
    u.password_hash
FROM users u
JOIN roles r ON u.role_id = r.id;


-- ----------------------------------------------------------------------------
-- 2. INTEGRIDAD DOCUMENTAL: Firmas Criptográficas SHA-256 (Casos 10 y 11)
-- Explicación: Verificación de Magic Bytes (%PDF-) y cálculo SHA-256 para no-repudio.
-- ----------------------------------------------------------------------------
SELECT 
    d.id,
    d.lote_id,
    d.tipo,
    d.version,
    d.nombre AS nombre_original,
    d.mime_type,
    d.sha256,
    d.file_size || ' bytes' AS tamaño,
    datetime(d.fecha_subida/1000, 'unixepoch', 'localtime') AS fecha_subida
FROM documents d;


-- ----------------------------------------------------------------------------
-- 3. GESTIÓN DE LOTES: Unicidad de Código y Máquina de Estados (Casos 1, 2, 3, 4)
-- Explicación: Restricción UNIQUE en BD y control estricto de transiciones de ciclo de vida.
-- ----------------------------------------------------------------------------
SELECT 
    l.id,
    l.codigo,
    l.especie,
    l.variedad,
    l.peso_neto,
    l.unidad_medida,
    l.estado,
    l.version_registro,
    datetime(l.fecha_creacion/1000, 'unixepoch', 'localtime') AS fecha_registro
FROM lots l;


-- ----------------------------------------------------------------------------
-- 4. CONTROL DE CALIDAD QA: Historial Inmutable de Reinspecciones (Caso 5)
-- Explicación: Nunca sobrescribe inspecciones previas (1 inicial + máx 2 reinspecciones).
-- ----------------------------------------------------------------------------
SELECT 
    qi.id,
    qi.lote_id,
    qi.numero_inspeccion,
    qi.inspector_nombre,
    qi.resultado_organoleptico,
    qi.motivo_reinspeccion,
    qi.observaciones,
    datetime(qi.fecha_inspeccion/1000, 'unixepoch', 'localtime') AS fecha_inspeccion
FROM quality_inspections qi
ORDER BY qi.lote_id, qi.numero_inspeccion ASC;


-- ----------------------------------------------------------------------------
-- 5. CADENA DE FRÍO: Telemetría de Sensores y Umbrales Críticos (Casos 6, 7, 8)
-- Explicación: Detección automática de fluctuaciones térmicas e incidentes.
-- ----------------------------------------------------------------------------
SELECT 
    ccr.id,
    ccr.lote_id,
    ccr.temperatura,
    ccr.humedad,
    ccr.alerta_disparada,
    ccr.camara_id,
    datetime(ccr.fecha_registro/1000, 'unixepoch', 'localtime') AS fecha_sensor
FROM cold_chain_records ccr
ORDER BY ccr.fecha_registro DESC
LIMIT 10;


-- ----------------------------------------------------------------------------
-- 6. PISTA DE AUDITORÍA Y TRAZABILIDAD: Registro Inmutable de Eventos (Casos 17, 18, 19)
-- Explicación: Trazabilidad forense con IP, agente de usuario, rol y snapshot de cambios.
-- ----------------------------------------------------------------------------
SELECT 
    a.id,
    datetime(a.created_at/1000, 'unixepoch', 'localtime') AS fecha_evento,
    a.username_snapshot AS usuario,
    a.user_role AS rol,
    a.module AS modulo,
    a.action AS accion,
    a.previous_value AS valor_anterior,
    a.new_value AS valor_nuevo,
    a.result AS resultado,
    a.ip_address AS direccion_ip
FROM audit_logs a
ORDER BY a.id DESC
LIMIT 20;


-- ----------------------------------------------------------------------------
-- 7. SESIONES ACTIVAS Y REVOCACIÓN DE SEGURIDAD (Caso 18)
-- Explicación: Invalidación inmediata de tokens en cascada al desactivar usuarios.
-- ----------------------------------------------------------------------------
SELECT 
    s.id,
    s.user_id,
    s.status,
    s.ip_address,
    s.revocation_reason,
    datetime(s.created_at/1000, 'unixepoch', 'localtime') AS fecha_inicio,
    datetime(s.expires_at/1000, 'unixepoch', 'localtime') AS fecha_expiracion,
    datetime(s.revoked_at/1000, 'unixepoch', 'localtime') AS fecha_revocacion
FROM user_sessions s;
