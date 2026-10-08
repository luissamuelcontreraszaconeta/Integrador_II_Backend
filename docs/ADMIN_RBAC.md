# Módulo de Administración del Sistema & Control de Acceso RBAC

**Sistema Inteligente de Trazabilidad Hidrobiológica ExporTrace**  
*Documento Técnico de Arquitectura de Seguridad, Control de Accesos y Auditoría Inmutable*

---

## 📌 1. Objetivo del Módulo de Administración

El **Módulo de Administración de ExporTrace** tiene como propósito centralizar la gestión de identidades corporativas, aplicar el modelo de control de acceso basado en roles (**RBAC** - *Role-Based Access Control*) con privilegios granulares, supervisar el catálogo de módulos funcionales y registrar una **bitácora inmutable de auditoría (System Audit Trail)** que rastree las operaciones críticas de todos los usuarios del sistema.

### Diferenciación de Dashboards:
- **Dashboard de Gerencia (`/management`)**: Enfocado en indicadores de negocio, volumen de lotes procesados, cumplimiento de cadena de frío, porcentaje de aprobación de certificados sanitarios SANIPES y despachos aduaneros.
- **Dashboard de Administración (`/admin`)**: Enfocado en la gobernanza tecnológica, usuarios activos/inactivos, roles asignados, salud del sistema, intentos de acceso denegados y actividad administrativa reciente.

---

## 🏗️ 2. Modelo y Arquitectura RBAC

La arquitectura de seguridad sigue un esquema jerárquico y desacoplado donde los usuarios pertenecen a un rol, y cada rol agrupa un conjunto dinámico de permisos granulares asociados a módulos funcionales:

```text
                  ┌──────────────┐
                  │     User     │
                  └──────┬───────┘
                         │ 1:1
                         ▼
                  ┌──────────────┐
                  │     Role     │
                  └──────┬───────┘
                         │ N:M (@JoinTable role_permissions)
                         ▼
                  ┌──────────────┐
                  │  Permission  │
                  └──────┬───────┘
                         │ N:1
                         ▼
                  ┌──────────────┐
                  │    Module    │
                  └──────────────┘
```

### Entidades Principales:
1. **`User`**: Almacena credenciales encriptadas con **BCrypt**, área departamental, estado de activación (`activo: true/false`), fecha de alta y última marca de acceso.
2. **`Role`**: Catálogo de perfiles funcionales (`ADMINISTRADOR`, `PRODUCCION`, `QA`, `LOGISTICA`, `GERENCIA`).
3. **`Permission`**: Privilegios atómicos con códigos normalizados (ej. `USERS_VIEW`, `LOTS_CREATE`).
4. **`Module`**: Metadatos de navegación funcional (`DASHBOARD`, `LOTES`, `CALIDAD`, `FRIO`, `LOGISTICA`, `CERTIFICACION`, `DESPACHO`, `GERENCIA`, `ADMINISTRACION`, `AUDITORIA`).
5. **`AuditLog`**: Bitácora inmutable de eventos con snapshot de usuario, IP, User-Agent, valores anteriores/nuevos y dictamen.

---

## 🔒 3. Reglas de Seguridad y Salvaguardas Administrativas

1. **Autorización Real en Backend (Principio de No Confianza en el Cliente)**:  
   La seguridad no depende de si el frontend oculta un botón o enlace. Cada endpoint REST de `/api/admin/**` y de los módulos operativos está protegido con anotaciones `@PreAuthorize("hasAuthority('...')")`. Si un usuario manipula una petición HTTP sin poseer el permiso, Spring Security rechaza la solicitud con código **HTTP 403 Forbidden** y activa el manejador `CustomAccessDeniedHandler`.
2. **Carga en Tiempo Real de Permisos en `JwtAuthFilter`**:  
   En cada solicitud HTTP autenticada, `JwtAuthFilter` recupera el estado de activación y la matriz de permisos actualizada desde la base de datos. Si una cuenta es desactivada o un permiso es revocado por el Administrador, el cambio surte efecto **inmediatamente sin necesidad de revocar o esperar la expiración del JWT de 24 horas**.
3. **Protección contra Orfandad del Sistema**:  
   El sistema valida en la capa `UserService` que nunca se desactive ni se degrade el rol del **último Administrador activo** de la plataforma.
4. **Almacenamiento Seguro de Credenciales**:  
   Bajo ninguna circunstancia se almacenan contraseñas en texto plano ni se exponen `passwordHash` en los DTOs de respuesta.
5. **Inmutabilidad de la Auditoría**:  
   No existen endpoints de actualización ni eliminación (`DELETE`/`PUT`) sobre la entidad `AuditLog`. La bitácora es de **solo lectura**.

---

## 📋 4. Catálogo de Permisos Granulares

| Código de Permiso | Módulo Asociado | Descripción Funcional |
| :--- | :--- | :--- |
| `ADMIN_DASHBOARD_VIEW` | ADMINISTRACION | Acceso al panel administrativo y métricas de seguridad |
| `USERS_VIEW` | ADMINISTRACION | Visualizar catálogo y expediente de usuarios |
| `USERS_CREATE` | ADMINISTRACION | Registrar nuevos usuarios en la plataforma |
| `USERS_UPDATE` | ADMINISTRACION | Editar datos y asignar roles a usuarios |
| `USERS_DISABLE` | ADMINISTRACION | Activar y desactivar cuentas de usuarios |
| `USERS_RESET_PASSWORD` | ADMINISTRACION | Restablecer contraseñas de usuarios |
| `ROLES_VIEW` | ADMINISTRACION | Consultar roles y permisos del sistema |
| `ROLES_MANAGE` | ADMINISTRACION | Modificar la matriz de permisos de roles |
| `LOTS_VIEW` | LOTES | Visualizar catálogo y trazabilidad de lotes |
| `LOTS_CREATE` | LOTES | Registrar nuevos lotes de materia prima |
| `LOTS_UPDATE` | LOTES | Modificar información de lotes existentes |
| `QUALITY_VIEW` | CALIDAD | Consultar evaluaciones organolépticas de QA |
| `QUALITY_MANAGE` | CALIDAD | Dictaminar conformidad organoléptica de lotes |
| `COLD_CHAIN_VIEW` | FRIO | Consultar lecturas de cámaras frigoríficas |
| `COLD_CHAIN_MANAGE` | FRIO | Registrar mediciones y alertas de temperatura |
| `LOGISTICS_VIEW` | LOGISTICA | Consultar estado de expedientes y DUA |
| `LOGISTICS_MANAGE` | LOGISTICA | Gestionar documentación de comercio exterior |
| `CERTIFICATION_VIEW` | CERTIFICACION | Consultar certificados sanitarios SANIPES |
| `CERTIFICATION_MANAGE` | CERTIFICACION | Tramitar y registrar certificados oficiales |
| `DISPATCH_VIEW` | DESPACHO | Consultar programaciones de embarque |
| `DISPATCH_MANAGE` | DESPACHO | Autorizar despachos y precintos de contenedores |
| `EXECUTIVE_DASHBOARD_VIEW`| GERENCIA | Acceso al dashboard ejecutivo y KPIs |
| `AUDIT_VIEW` | AUDITORIA | Consultar bitácora general e historial de usuario |

---

## 🌐 5. Catálogo de Endpoints de la API Administrativa

Todos los endpoints se ubican bajo `/api/admin` y requieren token JWT Bearer válido con el permiso correspondiente:

| Método | Endpoint | Permiso Requerido | Descripción |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/admin/dashboard` | `ADMIN_DASHBOARD_VIEW` | Retorna KPIs, usuarios activos/inactivos y actividad reciente |
| `GET` | `/api/admin/users` | `USERS_VIEW` | Lista usuarios con filtros por búsqueda, rol y estado |
| `GET` | `/api/admin/users/{id}` | `USERS_VIEW` | Retorna el detalle completo de un usuario |
| `POST` | `/api/admin/users` | `USERS_CREATE` | Crea un nuevo usuario con contraseña encriptada BCrypt |
| `PUT` | `/api/admin/users/{id}` | `USERS_UPDATE` | Actualiza datos generales y rol del usuario |
| `PATCH`| `/api/admin/users/{id}/status` | `USERS_DISABLE` | Activa o desactiva la cuenta del usuario |
| `POST` | `/api/admin/users/{id}/reset-password` | `USERS_RESET_PASSWORD` | Restablece la contraseña asignando una temporal |
| `GET` | `/api/admin/users/{id}/history` | `AUDIT_VIEW` | Retorna el historial de actividad paginado del usuario (RF-21) |
| `GET` | `/api/admin/roles` | `ROLES_VIEW` | Lista roles con conteo de usuarios y permisos |
| `GET` | `/api/admin/roles/{id}` | `ROLES_VIEW` | Retorna detalle de un rol específico |
| `PUT` | `/api/admin/roles/{id}/permissions` | `ROLES_MANAGE` | Actualiza la lista de permisos asignados al rol |
| `PATCH`| `/api/admin/roles/{id}/status` | `ROLES_MANAGE` | Activa o desactiva un rol sin usuarios |
| `GET` | `/api/admin/permissions` | `ROLES_VIEW` | Retorna el catálogo completo de permisos |
| `GET` | `/api/admin/modules` | `ADMIN_DASHBOARD_VIEW` | Retorna el catálogo de módulos del sistema |
| `GET` | `/api/admin/audit` | `AUDIT_VIEW` | Consulta general paginada de la bitácora de auditoría |

---

## 📜 6. Auditoría y Trazabilidad de Actividad (RF-17 & RF-21)

### ¿Qué se registra?
- **Identidad del Actor**: ID de usuario, nombre completo snapshot y rol al momento de la acción.
- **Contexto de Red**: Dirección IP del cliente (`X-Forwarded-For` o remota) y User-Agent del navegador.
- **Acción & Módulo**: Código de operación (ej. `USER_CREATED`, `LOGIN_SUCCESS`, `ROLE_PERMISSIONS_UPDATED`).
- **Recurso Afectado**: Tipo de entidad (`User`, `Role`, `Lot`) e identificador.
- **Valores Comparativos**: `previousValue` y `newValue` para auditoría de cambios.
- **Resultado**: `EXITOSO`, `DENEGADO` (acceso no autorizado) o `FALLIDO`.

### ¿Qué NO se registra?
- ❌ Contraseñas en texto plano ni hashes.
- ❌ Tokens JWT completos ni secretos de firma.
- ❌ Datos sensibles innecesarios.

---

## 📊 7. Matriz de Trazabilidad de Requerimientos

| Requerimiento | Descripción | Ruta Frontend | Endpoint Backend | Componentes Backend | Estado |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **RF-13** | Gestión de Usuarios | `/admin/users` | `GET/POST /api/admin/users` | `UserController`, `UserService` | **Implementado** |
| **RF-14** | Activación/Desactivación de Cuentas | `/admin/users` | `PATCH /api/admin/users/{id}/status` | `UserService`, `UserRepository` | **Implementado** |
| **RF-15** | Gestión de Permisos Granulares | `/admin/roles` | `GET /api/admin/permissions` | `PermissionRepository`, `RoleService`| **Implementado** |
| **RF-16** | Control de Acceso a Módulos | Sidebar / Router | `GET /api/admin/modules` | `ModuleService`, `roleGuard.ts` | **Implementado** |
| **RF-17** | Auditoría General del Sistema | `/admin/audit` | `GET /api/admin/audit` | `AuditLogRepository`, `AuditService` | **Implementado** |
| **RF-18** | Dashboard Administrativo | `/admin` | `GET /api/admin/dashboard` | `AdminController`, `AdminDashboardDTO`| **Implementado** |
| **RF-19** | Restablecimiento de Credenciales | `/admin/users` | `POST /api/admin/users/{id}/reset-password` | `UserService`, `PasswordEncoder` | **Implementado** |
| **RF-20** | Control de Acceso Administrativo | Route Guards | `@PreAuthorize` en Controllers | `SecurityConfig`, `JwtAuthFilter` | **Implementado** |
| **RF-21** | Historial de Actividad de Usuarios | `/admin/users/:id` | `GET /api/admin/users/{id}/history` | `AuditService.getUserHistory` | **Implementado** |
