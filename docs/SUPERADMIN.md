# ExporTrace — Guía de Gobernanza Técnica y Panel de Seguridad SUPERADMIN

Este manual documenta las capacidades de administración técnica exclusivas para usuarios con rol **SUPERADMIN** en **ExporTrace**.

---

## 1. Módulos de Gobernanza Técnica

El rol `SUPERADMIN` cuenta con acceso irrestricto a los siguientes submódulos en `/superadmin`:

1. **Dashboard General (`/superadmin`):** KPIs ejecutivos, volumen total exportado, lotes auditados, tasa de conformidad organoléptica, alertas y actividad reciente en tiempo real.
2. **Usuarios & Accesos (`/superadmin/users`):** Catálogo completo de usuarios, creación de cuentas, cambio de estado (activar/desactivar), reseteo seguro de credenciales y asignación de roles.
3. **Roles & Permisos RBAC (`/superadmin/roles`):** Matriz de permisos por rol, actualización granular de capacidades operativas e invalidación automática de sesiones ante modificaciones.
4. **Catálogo de Módulos (`/superadmin/modules`):** Control del ecosistema de módulos funcionales y excepciones por usuario (`UserModuleAccess`).
5. **Bitácora de Auditoría (`/superadmin/audit`):** Trazabilidad inmutable de cada acción con filtros por usuario, módulo, acción, resultado y rango de fechas.
6. **Seguridad & Gestión de Sesiones (`/superadmin/security`):** Panel en tiempo real de sesiones de usuario, monitoreo de incidentes y **administración dinámica de políticas de sesión por rol**.
7. **Configuración del Sistema (`/superadmin/settings`):** Parámetros globales de retención de auditoría y límites de intentos de acceso.

---

## 2. Administración Dinámica de Políticas de Sesión

Desde `/superadmin/security` (pestaña **"Políticas de Sesión por Rol"**), el SuperAdministrador puede ajustar los parámetros de expiración:

* **Inactividad Máxima (*Idle Timeout*):** Rango permitido: `5` a `480` minutos.
* **Duración Absoluta Máxima (*Absolute Timeout*):** Rango permitido: `30` a `1440` minutos (24 horas).
* **Aviso Previo (*Warning*):** Rango permitido: `1` minuto hasta `< idleTimeoutMinutes`.

### Efecto en Tiempo Real
Al guardar una modificación:
1. El backend persiste la nueva regla en la tabla `session_policies`.
2. Las nuevas sesiones aplicarán los nuevos valores de inmediato.
3. Las sesiones activas existentes consultan la política vigente en su siguiente validación / refresco. Si un usuario ya superó el nuevo límite de inactividad, su sesión expirará de inmediato sin necesidad de esperar el plazo antiguo.
4. Se registra en auditoría el evento inmutable `SESSION_POLICY_UPDATED` documentando los valores previos y los nuevos.

---

## 3. Monitoreo y Revocación de Sesiones Activas

En la pestaña **"Sesiones Activas en Tiempo Real"**, el SuperAdministrador visualiza:
* Nombre y correo del usuario.
* Rol asignado.
* Fecha y hora de inicio de sesión.
* Marca temporal de última actividad.
* Dirección IP del cliente y agente de usuario (*browser / device*).
* Estado (`ACTIVA`, `EXPIRADA`, `REVOCADA`).
* Botón **`[Revocar]`**: Finaliza la sesión de forma instantánea, forzando la expulsión del usuario a la pantalla de inicio de sesión con el motivo registrado.
