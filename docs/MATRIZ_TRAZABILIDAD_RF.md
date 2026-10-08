# Matriz de Trazabilidad de Requerimientos Funcionales y No Funcionales — ExporTrace

**Versión:** 1.0  
**Fecha:** 2026-10-07  
**Estado:** PENDIENTE DE APROBACIÓN

---

## 1. Matriz de Requerimientos Funcionales (RF)

| Requerimiento | Descripción | Casuísticas | Componente Frontend | Endpoint REST | Servicio Backend | Entidad / BD | Caso de Prueba | Estado |
|---|---|---|---|---|---|---|---|:---:|
| **RF-01** | Autenticación y Control de Sesiones JWT | `AU-01`..`20` | `LoginPage.tsx`, `SessionManager.tsx` | `POST /api/auth/login`, `POST /api/auth/refresh` | `AuthService`, `SessionService` | `User`, `UserSession` | `TC-AU-01` | **IMPLEMENTADO** |
| **RF-02** | Gobernanza y Roles RBAC | `SA-01`..`10`, `AD-01`..`15` | `SuperAdminRolesPage.tsx`, `UserManagementPage.tsx` | `GET/PUT /api/superadmin/roles`, `/users` | `UserService`, `RoleService` | `Role`, `Permission` | `TC-SA-04` | **IMPLEMENTADO** |
| **RF-03** | Registro Digital de Lote de Producción | `LT-01`..`30`, `OM-07`, `OM-08` | `RegisterLotPage.tsx`, `LotsListPage.tsx` | `POST /api/lots`, `GET /api/lots` | `LotService` | `Lot`, `Product` | `TC-LT-01` | **PARCIAL** *(Falta edición y anulación)* |
| **RF-04** | Generación de Código QR Inmutable | `QR-01`..`20` | `QRCodeModal.tsx`, `TraceabilityQRCode.tsx` | `GET /api/lots/qr/{token}` | `LotService` | `Lot.qr_token` | `TC-QR-01` | **IMPLEMENTADO** |
| **RF-05** | Inspección de Calidad Sanitaria y Reinspecciones (QualityTrac) | `QA-01`..`20`, `OM-03` | `InspectionFormPage.tsx`, `QualityDashboardPage.tsx` | `POST /api/quality/lot/{lotId}`, `POST /api/quality/lot/{lotId}/reinspect` | `QualityService` | `QualityInspection` (1:N) | `TC-QA-01`..`11` | **IMPLEMENTADO (Bloque P0 / P0-C / Caso 5)** |
| **RF-06** | Carga y Custodia Segura de Evidencias Fotográficas | `EV-01`..`15`, `OM-05` | `InspectionFormPage.tsx` (Lightbox) | `POST /api/quality/inspections/{id}/evidence`, `GET /api/quality/evidence/{id}` | `QualityService`, `PersistentFileStorageService` | `QaEvidence` | `TC-EV-01`..`13` | **IMPLEMENTADO (Bloque P0-C)** |
| **RF-07** | Monitoreo y Alertas de Cadena de Frío | `CF-01`..`40`, `OM-02`, `OM-04` | `ColdChainPage.tsx` | `POST /api/cold-chain/lot/{lotId}` | `ColdChainService` | `ColdChainRecord`, `ColdChainIncident` | `TC-CF-01` | **IMPLEMENTADO (Bloque P0-B)** |
| **RF-08** | Metrología e Identificación de Sensores Frigoríficos | `TM-01`..`15` | `ColdChainPage.tsx` | `POST /api/cold-chain/lot/{lotId}` | `ColdChainService` | `ColdChainRecord` | `TC-TM-01` | **NO IMPLEMENTADO** |
| **RF-09** | Gestión de Documentos y Declaraciones Juradas | `DOC-01`..`20` | `CertificationTrackerPage.tsx` | `POST /api/documents/lot/{lotId}` | `DocumentService` | `Document` | `TC-DOC-01` | **PARCIAL** *(Falta hash SHA-256)* |
| **RF-10** | Trámite y Validación de Certificado Sanitario SANIPES | `CS-01`..`20`, `OM-01` | `CertificationTrackerPage.tsx` | `POST /api/certifications/lot/{lotId}/request` | `CertificationService` | `SanitaryCertification` | `TC-CS-01` | **IMPLEMENTADO (Bloque P0)** |
| **RF-11A** | *(Corregido)* Consolidación de Expediente Digital | `EX-01`..`15` | `PublicTraceabilityPage.tsx`, `CertificationTrackerPage.tsx` | `GET /api/lots/expediente/{code}` | `LotService`, `CertificationService` | `Lot`, `Document`, `QualityInspection` | `TC-EX-01` | **IMPLEMENTADO** |
| **RF-11B** | *(Corregido - antes duplicado)* Gestión Logística de Despacho | `LG-01`..`20`, `DP-01`..`16`, `OM-09`, `OM-10` | `DispatchPage.tsx`, `LogisTracDashboardPage.tsx` | `POST /api/dispatches/lot/{lotId}` | `DispatchService` | `Dispatch` | `TC-DP-01` | **IMPLEMENTADO (Bloque P0)** |
| **RF-12** | Consulta Pública de Trazabilidad por Consumidor | `CL-01`..`15` | `PublicTraceabilityPage.tsx` | `GET /api/public/traceability/{token}` | `PublicTraceabilityController` | `Lot`, `QualityInspection` | `TC-CL-01` | **IMPLEMENTADO** |
| **RF-13** | Centro Real de Notificaciones por Rol | `NT-01`..`15` | `NotificationBell.tsx`, `NotificationsPage.tsx` | `GET/PATCH /api/notifications` | `NotificationService` | `Notification` | `TC-NT-01` | **IMPLEMENTADO** |
| **RF-14** | Bitácora Inmutable de Auditoría Forense | `AUD-01`..`20` | `AuditLogPage.tsx`, `SuperAdminAuditPage.tsx` | `GET /api/superadmin/audit` | `AuditService` | `AuditLog` | `TC-AUD-01` | **IMPLEMENTADO** |
| **RF-15** | Dashboard Ejecutivo y Analítica de Exportación (Gerencia) | `GE-01`..`15`, `OM-06` | `ManagementDashboardPage.tsx`, `SuperAdminDashboardPage.tsx` | `GET /api/superadmin/dashboard` | `SuperAdminService` | `Lot`, `QualityInspection` | `TC-GE-01` | **PARCIAL** *(Acciones correctivas estáticas)* |
| **RF-24** | Gestión Persistente del Ciclo de Vida de Sesiones | `AU-10`..`13` | `SessionManager.tsx`, `SuperAdminSecurityPage.tsx` | `GET /api/superadmin/security/sessions` | `SessionService` | `UserSession` | `TC-RF-24` | **IMPLEMENTADO** |
| **RF-25** | Revocación Instantánea de Sesiones Activas | `AU-19`, `AU-20`, `SA-10` | `SuperAdminSecurityPage.tsx` | `POST /api/superadmin/security/sessions/{id}/revoke` | `SessionService` | `UserSession` | `TC-RF-25` | **IMPLEMENTADO** |
| **RF-26** | *(Nuevo)* Administración Dinámica de Políticas de Sesión por Rol | `SA-04` | `SuperAdminSecurityPage.tsx` | `PUT /api/superadmin/security/session-policies/{role}` | `SessionService` | `SessionPolicy` | `TC-RF-26` | **IMPLEMENTADO** |
| **RF-27** | *(Nuevo)* Máquina Canónica de Estados del Lote | Sección 7 de casuísticas | Todas las vistas de Lotes, QA, Certificación y Despacho | Servicios de transición de estados | `LotStateMachineService`, `LotService` | `Lot.estado` (`LotStatus`) | `TC-RF-27` (12 Casos) | **IMPLEMENTADO (Bloque P0)** |
| **RF-28** | *(Nuevo)* Perfil Paramétrico de Rangos Térmicos por Especie | `PR-07`, `CF-29`, `CF-30` | `ProductManagementPage.tsx`, `ColdChainPage.tsx` | `GET/POST /api/products` | `ProductService`, `ColdChainService` | `Product` | `TC-RF-28` | **IMPLEMENTADO (Bloque P0-B)** |
| **RF-29** | *(Oficial)* Flujo de Transición Explícita de Certificación a Despacho | `DP-01`, `DP-04`, `LG-01`, `LG-04`, `OM-09`, `Caso A` | `CertificationTrackerPage.tsx`, `DispatchPage.tsx` | `POST /api/certifications/lot/{lotId}/enable-dispatch` | `CertificationService`, `LotStateMachineService` | `Lot.estado` (`READY_FOR_DISPATCH`) | `TC-RF-29` | **IMPLEMENTADO (Bloque P0)** |
| **RF-30** | *(Nuevo)* Anulación y Edición Inicial Controlada de Lotes | `OM-07`, `OM-08`, `LT-29` | `LotDetailPage.tsx` | `PATCH /api/lots/{id}/cancel`, `PUT /api/lots/{id}` | `LotService` | `Lot`, `LotHistory` | `TC-RF-30` | **PENDIENTE (Decisión Aprobada)** |
| **RF-31** | *(Nuevo)* Gestión y Subsanación Técnica de Alertas de Cadena de Frío | `CF-20`, `CF-31`, `OM-04` | `ColdChainPage.tsx` | `POST /api/cold-chain/lot/{id}/resolve-alert` | `ColdChainService` | `ColdChainRecord`, `ColdChainIncident` | `TC-RF-31` | **IMPLEMENTADO (Bloque P0-B)** |

---

## 2. Requerimientos No Funcionales (RNF)

| RNF | Nombre | Criterio de Aceptación | Componente / Capa | Estado |
|---|---|---|---|:---:|
| **RNF-01** | **Rendimiento de Endpoints** | Tiempo de respuesta transaccional < 300ms en condiciones estándar. | Spring Boot Controller / JPA | **CUMPLIDO** |
| **RNF-02** | **Disponibilidad Cloud** | Disponibilidad 24/7 en Render con endpoint de liveness `/api/health`. | Render Web Service / Docker | **CUMPLIDO** |
| **RNF-03** | **Encriptación de Contraseñas** | Algoritmo BCrypt con salt de 10 rondas para todas las credenciales. | Spring Security `PasswordEncoder` | **CUMPLIDO** |
| **RNF-04** | **Cifrado en Tránsito (TLS)** | Comunicación exclusiva HTTPS / TLS 1.3 con certificados gestionados. | Render Reverse Proxy | **CUMPLIDO** |
| **RNF-05** | **Diseño Responsive** | Interfaz utilizable en monitores 1080p, tablets y smartphones de planta (375px+). | Tailwind CSS / React 18 | **CUMPLIDO** |
| **RNF-06** | **Persistencia de Base de Datos** | Almacenamiento continuo en volumen persistente de Render `/app/data/exportrace.db`. | SQLite / Docker Volume | **CUMPLIDO / PROBADO_LOCALMENTE** |
| **RNF-07** | **Persistencia de Archivos** | Almacenamiento de fotos en volumen persistente `/app/data/uploads` (`FILE_UPLOAD_DIR`). Metadatos en BD y acceso controlado vía API. | `PersistentFileStorageService` / Render Disk | **CUMPLIDO / PROBADO_LOCALMENTE** |
| **RNF-08** | **Inmutabilidad de Auditoría** | Registros de `audit_logs` no borrables ni editables desde la interfaz de usuario. | JPA / SQLite | **CUMPLIDO** |
| **RNF-09** | **Expiración Segura de Sesión** | Backend valida inactividad y duración absoluta de sesiones de forma independiente al frontend. | `SessionService` | **CUMPLIDO** |
| **RNF-10** | **Protección de Tokens** | Access tokens de 15 min; refresh tokens almacenados únicamente en hash SHA-256. | `JwtUtil`, `UserSession` | **CUMPLIDO** |
| **RNF-11** | **Rate Limiting & Anti-Brute Force** | Limitación de peticiones por IP en login (máx 5 intentos fallidos / 15 min). | `SecurityConfig` / RateLimiter | **PENDIENTE** |
| **RNF-12** | **Integridad Criptográfica de Evidencias** | Checksum SHA-256 calculado y validado para cada foto/archivo cargado y verificación en streaming. | `PersistentFileStorageService`, `QaEvidence` | **CUMPLIDO / PROBADO_LOCALMENTE** |

---

## 3. Resolución de Discrepancias y Duplicidades

1. **Corrección de RF-11 Duplicado:**
   * En versiones anteriores del documento existían dos requisitos rotulados como `RF-11` ("Expediente Digital" y "Gestión de Despacho").
   * **Resolución Oficial:** Se oficializa `RF-11A: Consolidación de Expediente Digital` y `RF-11B: Gestión Logística de Despacho y Pre-Embarque`.
2. **Requisitos Nuevos Oficializados:**
   * `RF-24`: Gestión Persistente del Ciclo de Vida de Sesiones.
   * `RF-25`: Revocación Instantánea de Sesiones Activas.
   * `RF-26`: Administración Dinámica de Políticas de Sesión por Rol.
   * `RF-27`: Máquina Canónica de Estados del Lote (10 Estados Canónicos).
   * `RF-28`: Perfil Paramétrico de Rangos Térmicos por Especie (`CONGELADO` vs `REFRIGERADO`).
   * `RF-29`: Flujo de Transición Explícita de Certificación Sanitaria a Despacho (`READY_FOR_DISPATCH`).
   * `RF-30`: Anulación y Edición Inicial Controlada de Lotes.
   * `RF-31`: Gestión y Subsanación Técnica de Alertas de Cadena de Frío (`ACTIVE` $\rightarrow$ `UNDER_REVIEW` $\rightarrow$ `RESOLVED`).

---

## 4. Fuente Documental Única y Canónica

> [!IMPORTANT]
> La carpeta **`/docs`** (en la raíz lógica del proyecto) constituye la **FUENTE CANÓNICA OFICIAL Y EXCLUSIVA** de la documentación de arquitectura, requisitos y casuísticas de ExporTrace. Las copias ubicadas en subdirectorios de módulos (`exportrace-ica-backend/docs` y `exportrace-ica-frontend/docs`) son réplicas subordinadas sincronizadas de forma automatizada.
