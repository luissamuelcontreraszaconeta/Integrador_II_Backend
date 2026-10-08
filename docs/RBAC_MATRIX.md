# Matriz RBAC de Roles y Permisos - ExporTrace

Esta matriz describe la asignación de permisos funcionales por cada rol en la plataforma ExporTrace.

---

## 1. Tabla de Privilegios por Rol

| Módulo / Categoría | Código de Permiso | SUPERADMIN | ADMINISTRADOR | PRODUCCION | QA | LOGISTICA | GERENCIA |
| :--- | :--- | :---: | :---: | :---: | :---: | :---: | :---: |
| **SuperAdmin** | `SUPERADMIN_PANEL_VIEW` |  | ❌ | ❌ | ❌ | ❌ | ❌ |
| **SuperAdmin** | `SUPERADMIN_SECURITY_VIEW` |  | ❌ | ❌ | ❌ | ❌ | ❌ |
| **SuperAdmin** | `SUPERADMIN_MODULE_MANAGE` |  | ❌ | ❌ | ❌ | ❌ | ❌ |
| **Administración**| `ADMIN_DASHBOARD_VIEW` |  |  | ❌ | ❌ | ❌ | ❌ |
| **Administración**| `USERS_VIEW` |  |  | ❌ | ❌ | ❌ | ❌ |
| **Administración**| `USERS_MANAGE` |  |  | ❌ | ❌ | ❌ | ❌ |
| **Administración**| `ROLES_VIEW` |  |  | ❌ | ❌ | ❌ | ❌ |
| **Administración**| `AUDIT_VIEW` |  |  | ❌ | ❌ | ❌ | ❌ |
| **Lotes & Trazabilidad** | `LOTS_VIEW` |  |  |  |  |  |  |
| **Lotes & Trazabilidad** | `LOTS_CREATE` |  |  |  | ❌ | ❌ | ❌ |
| **Lotes & Trazabilidad** | `LOTS_EDIT` |  |  |  | ❌ | ❌ | ❌ |
| **Calidad** | `QUALITY_VIEW` |  |  | ❌ |  | ❌ |  |
| **Calidad** | `QUALITY_INSPECT` |  |  | ❌ |  | ❌ | ❌ |
| **Calidad** | `COLD_CHAIN_VIEW` |  |  | ❌ |  | ❌ |  |
| **Logística** | `LOGISTICS_VIEW` |  |  | ❌ | ❌ |  |  |
| **Certificación** | `CERTIFICATION_VIEW` |  |  | ❌ |  |  |  |
| **Despacho** | `DISPATCH_VIEW` |  |  | ❌ | ❌ |  |  |
| **Despacho** | `DISPATCH_MANAGE` |  |  | ❌ | ❌ |  | ❌ |
| **Gerencia** | `EXECUTIVE_DASHBOARD_VIEW` |  | ❌ | ❌ | ❌ | ❌ |  |

---

## 2. Excepciones Dinámicas (`UserModuleAccess`)

Si un usuario perteneciente a un rol operativo (ej. `PRODUCCION`) requiere temporalmente acceder a un módulo ajeno a su perfil base (ej. `QUALITY` o `CERTIFICATION`), el **SUPERADMIN** puede otorgarle una excepción individual desde el panel de SuperAdministración sin alterar su rol:

- Se inserta un registro en la tabla `user_module_access`.
- Al autenticarse, el backend inyecta la autoridad `MODULE_QUALITY` en el `SecurityContext` del usuario.
- El frontend valida automáticamente `hasPerm('MODULE_QUALITY')` en el `roleGuard` habilitando la navegación al módulo asignado.
