# Matriz de Permisos RBAC por Rol

**Sistema ExporTrace — Configuración de Roles, Permisos y Módulos**

---

## 📊 Matriz Completa: Rol × Permiso × Módulo

| Módulo | Código de Permiso | ADMINISTRADOR | PRODUCCION | QA | LOGISTICA | GERENCIA |
| :--- | :--- | :---: | :---: | :---: | :---: | :---: |
| **Administración** | `ADMIN_DASHBOARD_VIEW` | ✅ | ❌ | ❌ | ❌ | ❌ |
| **Administración** | `USERS_VIEW` | ✅ | ❌ | ❌ | ❌ | ❌ |
| **Administración** | `USERS_CREATE` | ✅ | ❌ | ❌ | ❌ | ❌ |
| **Administración** | `USERS_UPDATE` | ✅ | ❌ | ❌ | ❌ | ❌ |
| **Administración** | `USERS_DISABLE` | ✅ | ❌ | ❌ | ❌ | ❌ |
| **Administración** | `USERS_RESET_PASSWORD` | ✅ | ❌ | ❌ | ❌ | ❌ |
| **Administración** | `ROLES_VIEW` | ✅ | ❌ | ❌ | ❌ | ❌ |
| **Administración** | `ROLES_MANAGE` | ✅ | ❌ | ❌ | ❌ | ❌ |
| **Lotes** | `LOTS_VIEW` | ✅ | ✅ | ✅ | ✅ | ✅ |
| **Lotes** | `LOTS_CREATE` | ✅ | ✅ | ❌ | ❌ | ❌ |
| **Lotes** | `LOTS_UPDATE` | ✅ | ✅ | ❌ | ❌ | ❌ |
| **Calidad QA** | `QUALITY_VIEW` | ✅ | ✅ | ✅ | ❌ | ✅ |
| **Calidad QA** | `QUALITY_MANAGE` | ✅ | ❌ | ✅ | ❌ | ❌ |
| **Cadena de Frío**| `COLD_CHAIN_VIEW` | ✅ | ❌ | ✅ | ✅ | ✅ |
| **Cadena de Frío**| `COLD_CHAIN_MANAGE` | ✅ | ❌ | ✅ | ❌ | ❌ |
| **Logística Comex**| `LOGISTICS_VIEW` | ✅ | ❌ | ❌ | ✅ | ✅ |
| **Logística Comex**| `LOGISTICS_MANAGE` | ✅ | ❌ | ❌ | ✅ | ❌ |
| **Certificación** | `CERTIFICATION_VIEW` | ✅ | ❌ | ✅ | ✅ | ✅ |
| **Certificación** | `CERTIFICATION_MANAGE`| ✅ | ❌ | ❌ | ✅ | ❌ |
| **Despacho** | `DISPATCH_VIEW` | ✅ | ❌ | ❌ | ✅ | ✅ |
| **Despacho** | `DISPATCH_MANAGE` | ✅ | ❌ | ❌ | ✅ | ❌ |
| **Gerencia** | `EXECUTIVE_DASHBOARD_VIEW`| ✅ | ❌ | ❌ | ❌ | ✅ |
| **Auditoría** | `AUDIT_VIEW` | ✅ | ❌ | ❌ | ❌ | ✅ |

---

## 🎯 Resumen de Responsabilidades por Rol

1. **ADMINISTRADOR**:
   - Gobernanza total del sistema, control de usuarios, gestión de permisos granulares, configuración de roles, catálogo de módulos y auditoría general.
2. **PRODUCCION**:
   - Registro de lotes de materia prima, pesado neto, embarcaciones, proveedores y actualización de datos de planta. Consulta de dictámenes de calidad.
3. **QA (Control de Calidad)**:
   - Inspección organoléptica de lotes, dictamen de conformidad, registro de temperaturas en cámaras frigoríficas y consulta de expedientes de certificación.
4. **LOGISTICA**:
   - Tramitación de certificados sanitarios oficiales ante SANIPES, gestión de DUA de exportación, control de precintos y autorización de despacho de contenedores.
5. **GERENCIA**:
   - Visualización ejecutiva de indicadores consolidados de trazabilidad, rendimiento de lotes, estado de certificaciones y consulta de bitácora de auditoría.
