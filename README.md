# ExporTrace — Documentación Técnica del Backend (REST API)

Este directorio contiene el código fuente de la API REST del backend de **ExporTrace**, construida sobre **Java 21** y **Spring Boot 3**, utilizando **SQLite** como motor de persistencia relacional autocontenido.

---

## 🛠️ 1. Stack Tecnológico

Las dependencias y tecnologías utilizadas en este proyecto se encuentran definidas en `pom.xml`:

| Tecnología | Versión | Propósito en el Proyecto |
| :--- | :--- | :--- |
| **Java JDK** | `21` | Lenguaje de programación base con características modernas. |
| **Spring Boot** | `3.2.5` | Framework principal para desarrollo de aplicaciones web y REST APIs. |
| **Spring Web** | `3.2.5` | Creación de controladores REST y serialización JSON (Jackson). |
| **Spring Data JPA** | `3.2.5` | Abstracción de acceso a datos y mapeo objeto-relacional (ORM). |
| **Spring Security** | `3.2.5` | Seguridad web, autenticación sin estado y autorización por roles (RBAC). |
| **JJWT** | `0.11.5` | Creación, firma y verificación de tokens de seguridad JWT. |
| **SQLite JDBC** | `3.45.1.0` | Driver JDBC oficial para la conexión con el archivo de base de datos SQLite. |
| **Hibernate Dialects**| `6.4.4.Final`| Dialecto comunitario Hibernate para dialectos relacionales SQLite. |
| **Bean Validation** | `3.2.5` | Validación de restricciones en DTOs y entidades de entrada. |

---

## 🏗️ 2. Arquitectura por Capas

El backend sigue una arquitectura multicapa estrictamente desacoplada:

```text
Controller (HTTP REST)
    ↓
Service (Lógica de Negocio y Trazabilidad)
    ↓
Repository (Interfaces Spring Data JPA)
    ↓
Entity (Mapeo Relacional Hibernate)
    ↓
Database (SQLite / exportrace.db)
```

### Organización del Código (`src/main/java/com/exportrace/`):
- **`config/`**: Configuración de Spring Security, reglas CORS y seudónimo de datos iniciales (`DataInitializer`).
- **`controller/`**: Endpoints REST que reciben peticiones HTTP y retornan DTOs JSON.
- **`dto/`**: Objetos de transferencia de datos de entrada/salida para desacoplar las entidades de la API.
- **`entity/`**: 11 entidades relacionales JPA anotadas con `@Entity` y `@Table`.
- **`exception/`**: Manejo centralizado de excepciones con respuestas estructuradas (`GlobalExceptionHandler`).
- **`repository/`**: Repositorios de datos que extienden de `JpaRepository<T, ID>`.
- **`security/`**: Filtro de inspección JWT (`JwtAuthFilter`) y utilidades criptográficas (`JwtUtil`).
- **`service/`**: Capa de negocio donde se ejecutan las validaciones y los registros inmutables de auditoría en `LotHistory`.

---

## 💾 3. Base de Datos SQLite (`exportrace.db`)

El sistema utiliza la base de datos autocontenida `exportrace.db` ubicada en la raíz del proyecto. No requiere la instalación previa de motores externos como MySQL o PostgreSQL.

### Entidades Principales y Relaciones:
1. **`Role`**: Roles del sistema (`ADMINISTRADOR`, `PRODUCCION`, `QA`, `LOGISTICA`, `GERENCIA`).
2. **`User`**: Cuentas de usuario con contraseñas encriptadas en BCrypt.
3. **`Product`**: Catálogo de especies procesadas (`POTA_CONGELADA_BLOCK`, `LANGOSTINO_ENTERO`, etc.).
4. **`Lot`**: Registro del Lote Digital con código auto-incremental (`EXP-2026-001`) y token QR Hash SHA-256.
5. **`QualityInspection`**: Inspección organoléptica y dictamen de calidad (Relación 1:1 con `Lot`).
6. **`ColdChainRecord`**: Medición de temperatura frigorífica (°C) (Relación N:1 con `Lot`).
7. **`Document`**: Archivos del expediente digital (Relación N:1 con `Lot`).
8. **`SanitaryCertification`**: Trámite de Certificación Sanitaria SANIPES (Relación 1:1 con `Lot`).
9. **`Dispatch`**: Despacho de exportación, contenedor y precinto de seguridad (Relación 1:1 con `Lot`).
10. **`LotHistory`**: Registro inmutable de auditoría para trazabilidad ante cada cambio de estado (Relación N:1 con `Lot`).
11. **`Notification`**: Alertas internas del sistema dirigidas por rol.

---

## 🔌 4. Catálogo de Endpoints REST API

### 4.1 Autenticación y Salud
| Método | Endpoint | Descripción | Autenticación | Rol Autorizado |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/health` | Estado del servidor backend y la BD SQLite. | Pública | Todos |
| `POST` | `/api/auth/login` | Autenticación de usuario y emisión de JWT Token. | Pública | Todos |

**Ejemplo Request (`POST /api/auth/login`):**
```json
{
  "email": "admin@exportrace.pe",
  "password": "Admin123"
}
```

**Ejemplo Response (`200 OK`):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbms...",
  "user": {
    "id": 1,
    "nombre": "Ing. Carlos Mendoza",
    "email": "admin@exportrace.pe",
    "area": "Administración & TI",
    "rol": "ADMINISTRADOR",
    "estado": "ACTIVO"
  }
}
```

---

### 4.2 Lotes y Trazabilidad (`/api/lots`)
| Método | Endpoint | Descripción | Autenticación | Rol Autorizado |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/lots` | Obtiene el listado completo de lotes. | Requiere JWT | Todos |
| `GET` | `/api/lots/{id}` | Busca un lote por su ID primario. | Requiere JWT | Todos |
| `GET` | `/api/lots/code/{code}` | Busca un lote por su código (`EXP-2026-001`). | Requiere JWT | Todos |
| `GET` | `/api/lots/qr/{token}` | Busca un lote por su Token QR inmutable. | Pública | Todos |
| `POST` | `/api/lots` | Registra un nuevo lote digital de producción. | Requiere JWT | `PRODUCCION`, `ADMIN` |
| `PATCH` | `/api/lots/{id}/status` | Actualiza el estado del lote y registra auditoría. | Requiere JWT | `PRODUCCION`, `QA`, `LOGISTICA`, `ADMIN` |
| `GET` | `/api/lots/{id}/history` | Obtiene el historial inmutable de trazabilidad. | Requiere JWT | Todos |

---

### 4.3 Control de Calidad y Cadena de Frío
| Método | Endpoint | Descripción | Autenticación | Rol Autorizado |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/quality/lot/{lotId}` | Obtiene el reporte de inspección QA del lote. | Requiere JWT | `QA`, `GERENCIA`, `ADMIN` |
| `POST` | `/api/quality/lot/{lotId}` | Registra/actualiza la inspección organoléptica. | Requiere JWT | `QA`, `ADMIN` |
| `GET` | `/api/cold-chain/lot/{lotId}`| Obtiene la serie histórica de lecturas de frío. | Requiere JWT | `QA`, `GERENCIA`, `ADMIN` |
| `POST` | `/api/cold-chain/lot/{lotId}`| Registra una nueva lectura de temperatura. | Requiere JWT | `QA`, `ADMIN` |

---

### 4.4 Certificación SANIPES y Logística
| Método | Endpoint | Descripción | Autenticación | Rol Autorizado |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/certifications` | Lista todos los trámites sanitarios. | Requiere JWT | `LOGISTICA`, `GERENCIA`, `ADMIN` |
| `POST` | `/api/certifications/lot/{lotId}/request` | Solicita inicio de trámite SANIPES. | Requiere JWT | `LOGISTICA`, `ADMIN` |
| `POST` | `/api/certifications/lot/{lotId}/approve` | Registra la aprobación del Certificado Sanitario. | Requiere JWT | `LOGISTICA`, `ADMIN` |
| `GET` | `/api/dispatches/lot/{lotId}` | Obtiene los datos del despacho de exportación. | Requiere JWT | `LOGISTICA`, `GERENCIA`, `ADMIN` |
| `POST` | `/api/dispatches/lot/{lotId}` | Registra el despacho de contenedor y DUA. | Requiere JWT | `LOGISTICA`, `ADMIN` |

---

### 4.5 Usuarios, Roles y Productos
| Método | Endpoint | Descripción | Autenticación | Rol Autorizado |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/users` | Lista todos los usuarios registrados. | Requiere JWT | `ADMINISTRADOR` |
| `POST` | `/api/users` | Crea un nuevo usuario con rol asignado. | Requiere JWT | `ADMINISTRADOR` |
| `GET` | `/api/roles` | Lista los roles del sistema. | Requiere JWT | `ADMINISTRADOR` |
| `GET` | `/api/products` | Lista el catálogo de especies hidrobiológicas. | Requiere JWT | Todos |

---

## 🔐 5. Seguridad y Autenticación

```text
React Client               JwtAuthFilter             SecurityConfig             Controller
     │                           │                         │                        │
     │─── Authorization Bearer ─►│                         │                        │
     │    <JWT Token>            │─── Valida Firma JWT ───►│                        │
     │                           │    y extrae el Rol      │─── Verifica Permisos ─►│
     │                           │                         │    por Rol (RBAC)      │
```

1. **Hashing Criptográfico**: Las contraseñas se almacenan mediante `BCryptPasswordEncoder`.
2. **Sin Estado (Stateless)**: La sesión no se guarda en memoria del servidor. Cada petición incluye el token JWT.
3. **CORS Habilitado**: Configurado explícitamente en `CorsConfig.java` para permitir solicitudes únicamente desde `http://localhost:5173`.

---

## 🔑 6. Credenciales de Prueba (Sembradas en DataInitializer)

> [!IMPORTANT]
> **Aviso de Entorno Académico / Demostración**:  
> Estas cuentas son exclusivamente para desarrollo, demostración y pruebas académicas. No utilizar estas credenciales en ambientes productivos.

| Rol Empresarial | Correo Electrónico | Contraseña | Área de Acceso |
| :--- | :--- | :--- | :--- |
| **Administrador** | `admin@exportrace.pe` | `Admin123` | Administración General & TI |
| **Producción** | `produccion@exportrace.pe` | `Prod123` | Operaciones / Planta de Procesamiento |
| **QA / Calidad** | `qa@exportrace.pe` | `QA123` | Control de Calidad & Registro Frigorífico |
| **Logística & Comex** | `logistica@exportrace.pe` | `Log123` | Tramitación SANIPES & Despachos |
| **Gerencia** | `gerencia@exportrace.pe` | `Ger123` | Dashboard Ejecutivo & Reportes |

---

## ⚙️ 7. Variables de Entorno (.env.example)

Plantilla para la configuración del servidor backend:

```properties
SERVER_PORT=8080
JWT_SECRET=ExportRaceSuperSecretKey2026WithAtLeast256BitsForSecurityPass
JWT_EXPIRATION=86400000
SPRING_DATASOURCE_URL=jdbc:sqlite:exportrace.db
```

---

## 🚀 8. Compilación y Ejecución

```bash
# 1. Navegar al directorio del backend
cd exportrace-ica-backend

# 2. Compilar el proyecto con Maven
mvn clean package -DskipTests

# 3. Ejecutar la aplicación Spring Boot (Puerto 8080)
mvn spring-boot:run
```

**Ejecución alternativa mediante JAR:**
```bash
java -jar target/exportrace-ica-backend-1.0.0.jar
```
