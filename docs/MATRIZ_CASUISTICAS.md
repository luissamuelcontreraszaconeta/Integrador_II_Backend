# Matriz Integral de Casuísticas de Prueba y Estado de Cobertura — ExporTrace

**Versión:** 1.0  
**Fecha:** 2026-10-07  
**Estado:** PENDIENTE DE APROBACIÓN DE PLAN

---

## 1. Clasificación General de Casuísticas

| Categoría | Total Casos | Implementada y Probada | Parcialmente Implementada | No Implementada | Decisión Funcional Aprobada |
|---|:---:|:---:|:---:|:---:|:---:|
| **Observaciones Manuales (OM)** | 10 | 4 | 3 | 3 | 0 |
| **Autenticación y Sesiones (AU)** | 20 | 16 | 2 | 1 | 1 (AU-18) |
| **SuperAdmin (SA)** | 10 | 9 | 1 | 0 | 0 |
| **Administrador (AD)** | 15 | 13 | 2 | 0 | 0 |
| **Catálogo de Productos (PR)** | 10 | 5 | 3 | 2 | 0 |
| **Registro de Lotes (LT)** | 30 | 19 | 6 | 5 | 0 |
| **QA y Control de Calidad (QA)** | 20 | 11 | 5 | 3 | 1 (QA-12) |
| **Evidencias QA (EV)** | 15 | 8 | 4 | 2 | 1 (EV-15) |
| **Cadena de Frío (CF)** | 40 | 18 | 12 | 8 | 2 (CF-20, CF-37) |
| **Herramientas de Medición (TM)** | 15 | 3 | 4 | 8 | 0 |
| **Validación Inteligente (VI)** | 20 | 8 | 7 | 5 | 0 |
| **Documentos del Expediente (DOC)** | 20 | 8 | 6 | 6 | 0 |
| **Certificación Sanitaria (CS)** | 20 | 9 | 6 | 4 | 1 (CS-07) |
| **Expediente Digital (EX)** | 15 | 8 | 4 | 3 | 0 |
| **QR de Trazabilidad (QR)** | 20 | 14 | 4 | 2 | 0 |
| **Cliente / Consulta Pública (CL)** | 15 | 11 | 3 | 1 | 0 |
| **Logística de Exportación (LG)** | 20 | 10 | 5 | 5 | 0 |
| **Despacho Portuario (DP)** | 16 | 9 | 3 | 4 | 0 |
| **Notificaciones (NT)** | 15 | 11 | 3 | 1 | 0 |
| **Auditoría Forense (AUD)** | 20 | 14 | 5 | 1 | 0 |
| **Gerencia y KPIs (GE)** | 15 | 10 | 4 | 1 | 0 |
| **Base de Datos & Concurrencia (BD/CC)** | 30 | 15 | 8 | 7 | 0 |
| **Render Cloud & Continuidad (RD/DR)** | 33 | 18 | 8 | 7 | 0 |
| **Seguridad Específica (SEC)** | 20 | 15 | 3 | 2 | 0 |
| **Proceso Integral (Casos A-H)** | 8 | 5 | 2 | 1 | 0 |
| **Total Casuísticas Evaluadas** | **447** | **268 (60.0%)** | **110 (24.6%)** | **63 (14.1%)** | **6 (1.3% Aprobadas)** |

---

## 2. Matriz Detallada por Módulo

### 2.1. Observaciones Manuales de Pruebas en Vivo (OM)

| ID | Origen | Módulo | Escenario | Resultado Esperado | Brecha | Prioridad | Estado Actual | Prueba | Evidencia |
|:---:|:---:|:---:|:---|:---|:---:|:---:|:---:|:---:|:---|
| **OM-01** | Manual | Certificación | Intentar certificar lote con QA en estado OBSERVADO o NO_CONFORME | Bloquear solicitud y aprobación con HTTP 422 | `BR-P0-001` | `P0` | `IMPLEMENTADA Y PROBADA` | `TC-OM-01` | `LotStateMachineServiceTest.testCaso2` / `testCaso3` |
| **OM-02** | Manual | Frío | Registrar lectura para producto refrigerado o congelado | Evaluar según rangos del tipo de producto del catálogo | `BR-P0-002` | `P0` | `IMPLEMENTADA Y PROBADA` | `TC-OM-02` | `ColdChainServiceTest` (Rangos dinámicos Congelado y Refrigerado) |
| **OM-03** | Manual | QA | Realizar múltiples inspecciones sobre el mismo lote | Bloquear tras certificación; registrar historial 1:N (Máx 2 reinspecciones) | `BR-P1-001` | `P1` | `DECISIÓN APROBADA` | `TC-OM-03` | Política aprobada: 1 inicial + máx 2 reinspecciones |
| **OM-04** | Manual | Frío | Registrar temperatura conforme tras alerta térmica previa | Normalizar estado del lote y cerrar alerta con justificación formal QA | `BR-P1-002` / `RF-31` | `P1` | `IMPLEMENTADA Y PROBADA` | `TC-OM-04` | `ColdChainServiceTest` (Alerta ACTIVE -> UNDER_REVIEW -> RESOLVED) |
| **OM-05** | Manual | QA Evidencias | Consultar foto cargada tras reinicio del servicio backend | Foto visible en Lightbox y persistente en almacenamiento cloud/disco | `BR-P0-003` | `P0` | `DECISIÓN APROBADA` | `TC-OM-05` | Política aprobada: Almacenamiento persistente en Storage + BD metadata |
| **OM-06** | Manual | Gerencia | Consultar recomendaciones correctivas en KPIs de observaciones | Sugerir acciones según defecto real (textura, parásitos, etc.) | `BR-P2-001` | `P2` | `NO IMPLEMENTADA` | `TC-OM-06` | Siempre muestra mismo texto estático invariable |
| **OM-07** | Manual | Lotes | Intentar eliminar lote registrado erróneamente | Permitir anulación lógica con motivo y registro de auditoría | `BR-P1-003` | `P1` | `NO IMPLEMENTADA` | `TC-OM-07` | No existe botón ni endpoint de baja/anulación |
| **OM-08** | Manual | Lotes | Editar pesaje o datos de lote antes de pasar a QA | Permitir edición controlada en estado inicial con auditoría | `BR-P1-004` | `P1` | `NO IMPLEMENTADA` | `TC-OM-08` | Formulario bloqueado sin opción de edición en `REGISTERED` |
| **OM-09** | Manual | Despacho | Lote alcanza estado CERTIFIED | Habilitar botón directo "Proceder a Despacho" hacia `/dispatch` | `BR-P0-004` | `P0` | `IMPLEMENTADA Y PROBADA` | `TC-OM-09` | Botón "Proceder a Despacho" y endpoint `/enable-dispatch` activos |
| **OM-10** | Manual | Despacho | Despacho resulta bloqueado por precinto u observación | Ofrecer flujo de subsanación y re-evaluación o cancelación | `BR-P1-005` | `P1` | `NO IMPLEMENTADA` | `TC-OM-10` | Pantalla de bloqueo sin opción de reintento o salida |

---

### 2.2. Autenticación, Sesiones y Seguridad (AU)

| ID | Origen | Módulo | Escenario | Resultado Esperado | Brecha | Prioridad | Estado Actual | Prueba | Evidencia |
|:---:|:---:|:---:|:---|:---|:---:|:---:|:---:|:---:|:---|
| **AU-01** | PDF p.1 | Auth | Email y password correctos | Login exitoso, emite JWT (15 min) + Refresh Token | - | `P0` | `IMPLEMENTADA Y PROBADA` | `TC-AU-01` | HTTP 200 con User, JWT y Policy |
| **AU-02** | PDF p.1 | Auth | Email correcto, password erróneo | Rechazo HTTP 400 sin revelar datos | - | `P0` | `IMPLEMENTADA Y PROBADA` | `TC-AU-02` | HTTP 400 "Credenciales inválidas" |
| **AU-03** | PDF p.1 | Auth | Correo inexistente | Rechazo genérico sin revelar existencia de cuenta | - | `P1` | `IMPLEMENTADA Y PROBADA` | `TC-AU-03` | Auditoría LOGIN_FAILED genérica |
| **AU-04** | PDF p.1 | Auth | Formulario con campos vacíos | Validación en frontend sin disparar petición | - | `P2` | `IMPLEMENTADA Y PROBADA` | `TC-AU-04` | HTML5 `required` en LoginPage |
| **AU-05** | PDF p.1 | Auth | Correo con espacios laterales | Trim / normalización automática | - | `P2` | `IMPLEMENTADA Y PROBADA` | `TC-AU-05` | `.trim().toLowerCase()` aplicado |
| **AU-06** | PDF p.2 | Auth | Usuario desactivado intenta login | Bloquear con mensaje claro y auditar LOGIN_BLOCKED | - | `P0` | `IMPLEMENTADA Y PROBADA` | `TC-AU-06` | HTTP 400 cuenta desactivada |
| **AU-07** | PDF p.2 | Auth | Usuario inactivo con token previo | Bloquear en JwtAuthFilter y revocar sesiones | - | `P0` | `IMPLEMENTADA Y PROBADA` | `TC-AU-07` | JwtAuthFilter verifica `user.activo == true` |
| **AU-08** | PDF p.2 | Auth | Access Token JWT expirado | Renovar vía Refresh Token o redirigir a login | - | `P0` | `IMPLEMENTADA Y PROBADA` | `TC-AU-08` | Interceptor 401 en `apiClient.ts` |
| **AU-09** | PDF p.2 | Auth | JWT manipulado en firma | Rechazo inmediato HTTP 401 | - | `P0` | `IMPLEMENTADA Y PROBADA` | `TC-AU-09` | JJWT SignatureException manejada |
| **AU-10** | PDF p.2 | Auth | Inactividad supera política de rol | Expirar sesión (`SESSION_IDLE_EXPIRED`) y logout | - | `P0` | `IMPLEMENTADA Y PROBADA` | `TC-AU-10` | SessionService calcula inactividad real |
| **AU-11** | PDF p.2 | Auth | Aviso 2 min antes de idle timeout | Mostrar modal interactivo con cuenta regresiva | - | `P1` | `IMPLEMENTADA Y PROBADA` | `TC-AU-11` | `SessionManager.tsx` modal visible |
| **AU-12** | PDF p.2 | Auth | Duración supera absoluteTimeout | Expirar sesión obligando reautenticación | - | `P0` | `IMPLEMENTADA Y PROBADA` | `TC-AU-12` | `SESSION_ABSOLUTE_EXPIRED` lanzado |
| **AU-13** | PDF p.2 | Auth | Logout voluntario | Revocar refresh token en BD y limpiar storage | - | `P0` | `IMPLEMENTADA Y PROBADA` | `TC-AU-13` | UserSession status = 'REVOKED' |
| **AU-14** | PDF p.2 | Auth | Abrir URL protegida sin sesión | Redirección a `/login` | - | `P1` | `IMPLEMENTADA Y PROBADA` | `TC-AU-14` | `auth.guard.ts` intercepta ruta |
| **AU-15** | PDF p.2 | Auth | Producción intenta abrir `/admin` | Denegar acceso con HTTP 403 y registrar auditoría | - | `P0` | `IMPLEMENTADA Y PROBADA` | `TC-AU-15` | `CustomAccessDeniedHandler` activo |
| **AU-16** | PDF p.2 | Auth | Manipulación de UI en cliente | Backend valida permisos con `@PreAuthorize` | - | `P0` | `IMPLEMENTADA Y PROBADA` | `TC-AU-16` | SecurityFilterChain rechaza petición |
| **AU-17** | PDF p.2 | Auth | Múltiples intentos fallidos seguidos | Bloqueo temporal por IP (Rate Limiting) | `BR-P1-007` | `P1` | `NO IMPLEMENTADA` | `TC-AU-17` | No hay limitador de tasa de requests |
| **AU-18** | PDF p.2 | Auth | Dos dispositivos usan misma cuenta | Máximo 1 sesión activa por usuario (nuevo login revoca anterior) | - | `P1` | `DECISIÓN APROBADA` | `TC-AU-18` | Política aprobada: Concurrencia restringida a 1 sesión |
| **AU-19** | PDF p.2 | Auth | Admin desactiva usuario con sesión activa | Revocar de inmediato todas las sesiones activas | - | `P0` | `IMPLEMENTADA Y PROBADA` | `TC-AU-19` | `sessionService.revokeAllUserSessions` |
| **AU-20** | PDF p.2 | Auth | Contraseña restablecida | Revocar sesiones previas y forzar nuevo login | - | `P0` | `IMPLEMENTADA Y PROBADA` | `TC-AU-20` | Invalida sesiones activas en BD |

---

### 2.3. SuperAdmin y Gobernanza (SA)

| ID | Origen | Módulo | Escenario | Resultado Esperado | Brecha | Prioridad | Estado Actual | Prueba |
|:---:|:---:|:---:|:---|:---|:---:|:---:|:---:|:---:|
| **SA-01** | PDF p.3 | SuperAdmin | SuperAdmin crea nuevo usuario Administrador | Guardar usuario y registrar log `USER_CREATED` | - | `P0` | `IMPLEMENTADA Y PROBADA` | `TC-SA-01` |
| **SA-02** | PDF p.3 | SuperAdmin | Intento de duplicar correo de usuario | Rechazo HTTP 400 con mensaje explicativo | - | `P1` | `IMPLEMENTADA Y PROBADA` | `TC-SA-02` |
| **SA-03** | PDF p.3 | SuperAdmin | Desactiva usuario administrador | Bloquear accesos y revocar sesiones activas | - | `P0` | `IMPLEMENTADA Y PROBADA` | `TC-SA-03` |
| **SA-04** | PDF p.3 | SuperAdmin | Modifica política de sesión por rol | Actualizar `session_policies` y auditar `SESSION_POLICY_UPDATED` | - | `P0` | `IMPLEMENTADA Y PROBADA` | `TC-SA-04` |
| **SA-05** | PDF p.3 | SuperAdmin | Consulta catálogo de auditoría | Visualizar logs con filtros por módulo, acción y fecha | - | `P1` | `IMPLEMENTADA Y PROBADA` | `TC-SA-05` |
| **SA-06** | PDF p.3 | SuperAdmin | Intento de borrado de logs de auditoría | Prohibir eliminación física (inmutabilidad) | - | `P0` | `IMPLEMENTADA Y PROBADA` | `TC-SA-06` |
| **SA-07** | PDF p.3 | SuperAdmin | Consulta métricas globales en Dashboard | Cálculo dinámico de lotes, volumen y exportaciones | - | `P1` | `IMPLEMENTADA Y PROBADA` | `TC-SA-07` |
| **SA-08** | PDF p.3 | SuperAdmin | Asignación de permisos por excepción | Actualizar `user_module_access` e invalidar sesiones | - | `P0` | `IMPLEMENTADA Y PROBADA` | `TC-SA-08` |
| **SA-09** | PDF p.3 | SuperAdmin | Intento de desactivar al último SuperAdmin | Bloquear operación para evitar bloqueo del sistema | - | `P0` | `IMPLEMENTADA Y PROBADA` | `TC-SA-09` |
| **SA-10** | PDF p.3 | SuperAdmin | Revoca sesión de usuario activa en tiempo real | Cambiar status a `REVOKED` en `user_sessions` | - | `P0` | `IMPLEMENTADA Y PROBADA` | `TC-SA-10` |

---

### 2.4. Control de Calidad y Evidencias (QA / EV)

| ID | Origen | Módulo | Escenario | Resultado Esperado | Brecha | Prioridad | Estado Actual | Prueba |
|:---:|:---:|:---:|:---|:---|:---:|:---:|:---:|:---:|
| **QA-01** | PDF p.8 | QA | Lote organolépticamente conforme | Marcar `READY_FOR_CERTIFICATION` y notificar | - | `P0` | `IMPLEMENTADA Y PROBADA` | `TC-QA-01` |
| **QA-02** | PDF p.8 | QA | Lote con defecto leve en apariencia o textura | Marcar `OBSERVED` y emitir alerta urgente a Producción | - | `P0` | `IMPLEMENTADA Y PROBADA` | `TC-QA-02` |
| **QA-03** | PDF p.8 | QA | Lote con parásitos o descomposición | Marcar `NO_CONFORME` y bloquear avance | - | `P0` | `IMPLEMENTADA Y PROBADA` | `TC-QA-03` |
| **QA-08** | PDF p.8 | QA | Producción intenta registrar inspección QA | Rechazar HTTP 403 (solo rol QA) | - | `P0` | `IMPLEMENTADA Y PROBADA` | `TC-QA-08` |
| **QA-10** | PDF p.8 | QA | Inspector intenta modificar QA de lote certificado | Bloquear edición con HTTP 400 | `BR-P1-001` | `P0` | `NO IMPLEMENTADA` | `TC-QA-10` |
| **QA-12** | PDF p.9 | QA | Reinspección de lote observado | Guardar nueva inspección conservando historial previo | `BR-P1-001` | `P1` | `PARCIALMENTE IMPLEMENTADA` | `TC-QA-12` |
| **QA-14** | PDF p.9 | QA | Carga de foto de más de 5MB | Rechazar con mensaje de tamaño excedido | `BR-P2-002` | `P2` | `IMPLEMENTADA Y PROBADA` | `TC-QA-14` |
| **QA-15** | PDF p.9 | QA | Archivo ejecutable `.exe` renombrado `.jpg` | Validar Magic Bytes y rechazar | `BR-P1-008` | `P1` | `NO IMPLEMENTADA` | `TC-QA-15` |
| **EV-01** | PDF p.9 | QA | Carga de imagen JPEG válida | Guardar en `/uploads/qa-evidence/` y generar miniatura | `BR-P0-003` | `P0` | `IMPLEMENTADA Y PROBADA` | `TC-EV-01` |
| **EV-10** | PDF p.10 | QA | Archivo de evidencia borrado del servidor | Mostrar fallback controlado sin romper la interfaz | `BR-P0-003` | `P0` | `PARCIALMENTE IMPLEMENTADA` | `TC-EV-10` |
| **EV-15** | PDF p.10 | QA | Reinicio de contenedor Render | Mantener persistencia en volumen `/app/data/uploads` | `BR-P0-003` | `P0` | `NO IMPLEMENTADA` | `TC-EV-15` |

---

### 2.5. Cadena de Frío y Metrología (CF / TM)

| ID | Origen | Módulo | Escenario | Resultado Esperado | Brecha | Prioridad | Estado Actual | Prueba |
|:---:|:---:|:---:|:---|:---|:---:|:---:|:---:|:---:|
| **CF-01** | PDF p.10 | Frío | Pota Congelada a -25°C | Estado `NORMAL / CONFORME` | - | `P0` | `IMPLEMENTADA Y PROBADA` | `TC-CF-01` |
| **CF-03** | PDF p.10 | Frío | Pota Congelada a -18°C | Estado `NORMAL / CONFORME` (límite SANIPES) | - | `P0` | `IMPLEMENTADA Y PROBADA` | `TC-CF-03` |
| **CF-04** | PDF p.10 | Frío | Congelado a -16°C durante transporte | Estado `WARNING` con control de duración | `BR-P0-002` | `P0` | `PARCIALMENTE IMPLEMENTADA` | `TC-CF-04` |
| **CF-07** | PDF p.10 | Frío | Congelado a -14°C | Estado `CRITICAL` y alerta automática | `BR-P0-002` | `P0` | `PARCIALMENTE IMPLEMENTADA` | `TC-CF-07` |
| **CF-12** | PDF p.11 | Frío | Lectura errónea +150°C | Rechazar valor por fuera de límites físicos | `BR-P1-006` | `P1` | `NO IMPLEMENTADA` | `TC-CF-12` |
| **CF-20** | PDF p.11 | Frío | Temperatura vuelve a -20°C tras fluctuación | Normalizar alerta conservando incidencia histórica | `BR-P1-002` | `P1` | `PARCIALMENTE IMPLEMENTADA` | `TC-CF-20` |
| **CF-29** | PDF p.12 | Frío | Pescado Fresco Refrigerado a +2°C | Estado `NORMAL` según perfil de producto | `BR-P0-002` | `P0` | `NO IMPLEMENTADA` | `TC-CF-29` |
| **CF-37** | PDF p.12 | Frío | Desviación térmica crítica tras QA aprobado | Reabrir lote y marcar `OBSERVED` | `BR-P0-002` | `P0` | `NO IMPLEMENTADA` | `TC-CF-37` |
| **TM-01** | PDF p.13 | Frío | Identificación de instrumento sensor | Registrar tipo de sensor y fecha/hora de lectura | `BR-P1-006` | `P1` | `NO IMPLEMENTADA` | `TC-TM-01` |

---

### 2.6. Certificación Sanitaria, Documentos y Despacho (CS / DOC / DP / EX)

| ID | Origen | Módulo | Escenario | Resultado Esperado | Brecha | Prioridad | Estado Actual | Prueba |
|:---:|:---:|:---:|:---|:---|:---:|:---:|:---:|:---:|
| **CS-01** | PDF p.16 | Certificación | Lote sin QA conforme intenta certificar | Bloquear solicitud con HTTP 422/400 | `BR-P0-001` | `P0` | `IMPLEMENTADA Y PROBADA` | `TC-CS-01` (Test Caso 2) |
| **CS-02** | PDF p.16 | Certificación | Lote con QA `CONFORME` y frío estable | Permitir solicitud de Certificado SANIPES | - | `P0` | `IMPLEMENTADA Y PROBADA` | `TC-CS-02` |
| **CS-07** | PDF p.16 | Certificación | Trámite SANIPES aprobado | Transicionar a `CERTIFIED` y habilitar Despacho | `BR-P0-004` | `P0` | `IMPLEMENTADA Y PROBADA` | `TC-CS-07` (Test Caso 7) |
| **CS-13** | PDF p.16 | Certificación | Aprobar certificado sin adjuntar PDF | Exigir documento adjunto o número de trámite oficial | `BR-P1-009` | `P1` | `PARCIALMENTE IMPLEMENTADA` | `TC-CS-13` |
| **DOC-01**| PDF p.15 | Documentos | Carga de Declaración Jurada o Certificado | Almacenar con hash SHA-256 inmutable | `BR-P1-009` | `P1` | `PARCIALMENTE IMPLEMENTADA` | `TC-DOC-01` |
| **DP-01** | PDF p.20 | Despacho | Lote `CERTIFIED` con precinto y contenedor | Autorizar despacho y transicionar a `DISPATCHED` | - | `P0` | `IMPLEMENTADA Y PROBADA` | `TC-DP-01` (Test Caso 8) |
| **DP-04** | PDF p.20 | Despacho | Intentar despachar lote no certificado | Bloquear con HTTP 422/400 | `BR-P0-006` | `P0` | `IMPLEMENTADA Y PROBADA` | `TC-DP-04` (Test Caso 9) |
| **DP-10** | PDF p.21 | Despacho | Contenedor sin precinto de seguridad | Bloquear autorización de despacho | `BR-P1-005` | `P1` | `IMPLEMENTADA Y PROBADA` | `TC-DP-10` |
| **DP-15** | PDF p.21 | Despacho | Despacho bloqueado requiere subsanación | Permitir corrección de datos o cancelación controlada | `BR-P1-005` | `P1` | `NO IMPLEMENTADA` | `TC-DP-15` |
| **EX-01** | PDF p.17 | Expediente | Consulta de expediente digital consolidado | Mostrar QA, frío, certificado y despacho en una sola vista | - | `P0` | `IMPLEMENTADA Y PROBADA` | `TC-EX-01` |

---

### 2.7. Trazabilidad Pública y Códigos QR (QR / CL)

| ID | Origen | Módulo | Escenario | Resultado Esperado | Brecha | Prioridad | Estado Actual | Prueba |
|:---:|:---:|:---:|:---|:---|:---:|:---:|:---:|:---:|
| **QR-01** | PDF p.18 | QR | Generación de código QR para lote | Emisión de token inmutable `QR-<CODIGO>` | - | `P0` | `IMPLEMENTADA Y PROBADA` | `TC-QR-01` |
| **QR-08** | PDF p.18 | QR | Escaneo público por consumidor | Apertura de `/verificar/{token}` sin requerir login | - | `P0` | `IMPLEMENTADA Y PROBADA` | `TC-QR-08` |
| **QR-11** | PDF p.18 | QR | QR generado en producción | URL apunta a `https://exportrace-frontend.onrender.com` | - | `P0` | `IMPLEMENTADA Y PROBADA` | `TC-QR-11` |
| **QR-15** | PDF p.18 | QR | Inspección de respuesta JSON pública | No revelar IDs internos, correos ni datos privados | `BR-P0-007` | `P0` | `PARCIALMENTE IMPLEMENTADA` | `TC-QR-15` |
| **CL-01** | PDF p.19 | Público | Consulta de lote válido | Despliega pasaporte digital con trazabilidad completa | - | `P0` | `IMPLEMENTADA Y PROBADA` | `TC-CL-01` |
| **CL-02** | PDF p.19 | Público | Consulta de token inexistente | Mostrar pantalla amigable "Lote no encontrado" | - | `P1` | `IMPLEMENTADA Y PROBADA` | `TC-CL-02` |

---

### 2.8. Casuísticas del Proceso Integral (Casos A a H)

| ID | Origen | Escenario Integral | Resultado Esperado | Estado Actual | Brecha |
|:---:|:---:|:---|:---|:---:|:---:|
| **Caso A** | PDF p.30 | **Flujo Exitoso Completo:** Registro → QR → QA Conforme → Frío Conforme → Docs OK → Certificado Aprobado → Despacho Autorizado | Lote en estado final `DISPATCHED` con expediente completo | `IMPLEMENTADA Y PROBADA` | `BR-P0-004` |
| **Caso B** | PDF p.30 | **QA Rechaza:** Inspección QA resulta `NO_CONFORME` | Detener flujo; bloquear certificación, expediente y despacho | `IMPLEMENTADA Y PROBADA` | `BR-P0-001` |
| **Caso C** | PDF p.31 | **Falla en Cadena de Frío:** Registro a -10°C en congelado | Generar alerta, observar lote y bloquear certificación | `IMPLEMENTADA Y PROBADA` | `BR-P0-002` |
| **Caso D** | PDF p.31 | **Documento Faltante:** QA y frío conformes pero falta Declaración Jurada | Estado `NO_LISTO_PARA_CERTIFICACION` | `PARCIALMENTE IMPLEMENTADA` | `BR-P0-001` |
| **Caso E** | PDF p.31 | **Certificación Rechazada:** Trámite SANIPES dictamina Rechazado | Bloquear despacho y notificar al responsable | `IMPLEMENTADA Y PROBADA` | - |
| **Caso F** | PDF p.31 | **Intento de Salto:** Operador de producción intenta llamar a `/api/dispatches` | Respuesta HTTP 403 Forbidden y log de auditoría | `IMPLEMENTADA Y PROBADA` | - |
| **Caso G** | PDF p.31 | **Modificación Indebida:** Intentar editar peso de lote ya despachado | Bloquear edición con HTTP 400 | `IMPLEMENTADA Y PROBADA` | - |
| **Caso H** | PDF p.31 | **Desviación Post-Certificación:** Lectura crítica antes de embarcar | Bloquear `READY_FOR_DISPATCH`, generar incidencia técnica de inocuidad sin revocar unilateralmente certificado SANIPES | `IMPLEMENTADA Y PROBADA` | `BR-P1-013` |

---

## 3. Fuente Canónica de Documentación

> [!IMPORTANT]
> El directorio **`/docs`** en la raíz lógica del proyecto es la **FUENTE OFICIAL Y CANÓNICA** de esta matriz de casuísticas. Todas las réplicas en `/exportrace-ica-backend/docs` y `/exportrace-ica-frontend/docs` son espejos sincronizados.
