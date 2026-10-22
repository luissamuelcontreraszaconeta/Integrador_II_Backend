# Integración Continua (CI) y Control de Cambios con GitHub Actions — ExporTrace

**Curso:** Taller Integrador II — Semana 11: Control de Cambios e Integración Continua  
**Proyecto:** Sistema Inteligente de Trazabilidad Hidrobiológica ExporTrace  
**Estado de Implementación:** `IMPLEMENTADO_LOCALMENTE` *(Pendiente `VALIDADO_GITHUB_ACTIONS` tras apertura de Pull Request en GitHub)*

---

## 1. Objetivo

Establecer un pipeline automatizado de **Integración Continua (CI)** mediante **GitHub Actions** (`ExporTrace CI`) que actúe como control técnico de calidad en cada **Pull Request** y actualización hacia la rama `main`, verificando de forma reproducible:

- **Backend (`exportrace-ica-backend`):**
  - Compilación con **Java 21 (Eclipse Temurin)** y **Maven**.
  - Ejecución íntegra de la suite automatizada de pruebas (**118 pruebas**: 87 de Integrador 2 + 16 de Fase A + 15 de Fase C).
  - Empaquetado del artefacto ejecutable (`exportrace-ica-backend-1.0.0.jar`) y publicación como artefacto de GitHub Actions.
- **Frontend (`exportrace-ica-frontend`):**
  - Instalación limpia y reproducible de dependencias con **Node.js 24** mediante `npm ci` (`package-lock.json`).
  - Verificación estricta de tipos con **TypeScript** (`tsc -b`).
  - Construcción del bundle de producción con **Vite** (`vite build`).

Cualquier fallo en compilación, pruebas unitarias/integración, tipado estático o empaquetado detiene el workflow y marca el estado del Pull Request como **`FAIL`**.

---

## 2. Topología de Repositorios Detectada y Ubicación de los Workflows

En el entorno del proyecto se identificaron dos repositorios Git vinculados a GitHub bajo un directorio de trabajo común (`scratch/`):

| Componente | Directorio Local | Repositorio Remoto GitHub (`origin`) | Rama Principal | Archivo Workflow Activo en GitHub |
| :--- | :--- | :--- | :---: | :--- |
| **Backend Spring Boot** | `scratch/exportrace-ica-backend/` | `https://github.com/luissamuelcontreraszaconeta/Integrador_II_Backend.git` | `main` | `exportrace-ica-backend/.github/workflows/ci.yml` |
| **Frontend React + Vite** | `scratch/exportrace-ica-frontend/` | `https://github.com/luissamuelcontreraszaconeta/Integrador_II_Frontend.git` | `main` | `exportrace-ica-frontend/.github/workflows/ci.yml` |
| **Carpeta Raíz Local** *(No es repo Git)* | `scratch/` | *(Sin `.git` — No conectado a GitHub)* | — | `scratch/.github/workflows/ci.yml` *(Solo referencia local; NO se ejecuta en GitHub)* |

> **Nota Importante de Evidencia:**
> Dado que `exportrace-ica-backend` (`Integrador_II_Backend`) y `exportrace-ica-frontend` (`Integrador_II_Frontend`) son dos repositorios Git independientes y la carpeta padre `scratch/` **no** es un repositorio Git, los workflows que realmente ejecuta GitHub Actions en cada Pull Request son:
> 1. **`exportrace-ica-backend/.github/workflows/ci.yml`** (donde `pom.xml` y `target/exportrace-ica-backend-1.0.0.jar` están en la raíz del repositorio Backend, sin `working-directory`).
> 2. **`exportrace-ica-frontend/.github/workflows/ci.yml`** (donde `package.json` y `package-lock.json` están en la raíz del repositorio Frontend, sin `working-directory`).
> El archivo combinado en `scratch/.github/workflows/ci.yml` se conserva únicamente como referencia conceptual local y no se ejecuta en GitHub.

---

## 3. Diagrama Conceptual del Flujo de Control de Cambios e Integración Continua

### Flujo Exitoso (`PASS`) y Flujo Correctivo (`FAIL`)

```
Developer
  │
  ▼
1. Crea Rama de Trabajo (feature/*, fix/*, chore/*)
  │
  ▼
2. Implementa Cambio + Valida Localmente + Commit & Push
  │
  ▼
3. Abre Pull Request hacia rama 'main'
  │
  ▼
4. GitHub Actions dispara automáticamente "ExporTrace CI"
  ├──► Job: Backend CI (Java 21 + mvn -B clean test + package)
  └──► Job: Frontend CI (Node 24 + npm ci + npm run build)
         │
         ├──► [SI AMBOS JOBS = PASS ✅]
         │       │
         │       ▼
         │    5. Code Review / Aprobación Técnica
         │       │
         │       ▼
         │    6. Merge seguro a 'main'
         │
         └──► [SI CUALQUIER JOB = FAIL ❌]
                 │
                 ▼
              Merge Bloqueado → Analizar log en Actions
                 │
                 ▼
              Corregir código en la misma rama → Nuevo commit & push
                 │
                 ▼
              GitHub Actions re-ejecuta automáticamente el CI
```

---

## 4. Especificación Técnica del Workflow (`ci.yml`)

### 4.1 Triggers (Disparadores)
```yaml
on:
  pull_request:
    branches:
      - main
  push:
    branches:
      - main
```
- Se ejecuta automáticamente al abrir, sincronizar (`synchronize`) o reabrir cualquier **Pull Request** dirigido a `main`.
- Se ejecuta también al integrar cambios en `main` (`push`) para certificar el estado saludable de la línea base.
- Aplica principio de mínimo privilegio con `permissions: contents: read`.

### 4.2 Paralelismo de Jobs
Los jobs `backend` y `frontend` no tienen dependencia entre sí (`needs:` no requerido), por lo que se ejecutan **en paralelo** sobre runners `ubuntu-latest`, reduciendo el tiempo total de retroalimentación en el Pull Request.

### 4.3 Job `backend` — Backend CI (Java 21 & Maven)
- **Sistema Operativo:** `ubuntu-latest`
- **JDK:** `actions/setup-java@v4` con `distribution: temurin` y `java-version: "21"` (alineado con `<java.version>21</java.version>` en `pom.xml`).
- **Caché:** `cache: maven` vinculada a `pom.xml` para acelerar la descarga de dependencias de `~/.m2/repository`.
- **Comandos ejecutados:**
  1. `mvn -B clean test` — Compila código fuente y ejecuta las **118 pruebas automatizadas** en modo batch (`-B`).
  2. `mvn -B package -DskipTests` — Empaqueta el JAR ejecutable de Spring Boot sin duplicar la ejecución de los 118 tests recién validados en el paso anterior.
- **Artefacto generado (`CI ≠ CD`):**
  - Mediante `actions/upload-artifact@v4`, si los pasos previos son exitosos (`if: success()`), publica `target/exportrace-ica-backend-1.0.0.jar` bajo el nombre `exportrace-ica-backend-1.0.0` (retención de 14 días).
  - En esta etapa el pipeline realiza únicamente **Integración Continua (CI)** y **no** despliega automáticamente a producción.

### 4.4 Job `frontend` — Frontend CI (Node 24, TypeScript & Vite)
- **Sistema Operativo:** `ubuntu-latest`
- **Entorno Node.js:** `actions/setup-node@v4` con `node-version: "24"` (alineado con `@types/node: ^24.13.3` en `package.json` y el entorno Node 24 del proyecto).
- **Caché:** `cache: npm` vinculada a `package-lock.json`.
- **Comandos ejecutados:**
  1. `npm ci` — Instalación limpia, determinista y reproducible a partir de `package-lock.json` (falla si `package.json` y `package-lock.json` están desincronizados).
  2. `npm run build` — Ejecuta secuencialmente `tsc -b && vite build`, validando el tipado estático de TypeScript y generando el bundle optimizado en `dist/`.

---

## 5. Criterio de Aprobación (`PASS` / `FAIL`)

| Verificación | Condición de `PASS` ✅ | Condición de `FAIL` ❌ |
| :--- | :--- | :--- |
| **Compilación Backend** | `javac` compila las clases de negocio y test con código `0`. | Error de sintaxis, tipos o dependencias Maven faltantes. |
| **Suite de Pruebas Backend** | `118` tests ejecutados con `Failures: 0` y `Errors: 0`. | `Failures > 0` o `Errors > 0` en cualquier prueba JUnit 5 / MockMvc. |
| **Empaquetado y Artefacto** | Se genera y sube `exportrace-ica-backend-1.0.0.jar`. | Fallo al empaquetar o archivo JAR no encontrado (`if-no-files-found: error`). |
| **Instalación Frontend** | `npm ci` instala exactamente el árbol de `package-lock.json`. | Inconsistencia entre `package.json` y `package-lock.json`. |
| **TypeScript & Vite Build** | `tsc -b` reporta `0` errores y `vite build` genera `dist/`. | Cualquier error de tipos TypeScript, importación rota o fallo de empaquetado Vite. |

---

## 6. Seguridad, Manejo de Secretos y Base de Datos en CI

1. **Cero Secretos Productivos en CI:**
   - El pipeline **no requiere ni utiliza** `JIRA_API_TOKEN`, `JIRA_ACCOUNT_EMAIL`, credenciales de Render, ni contraseñas de producción.
   - Se define explícitamente `JIRA_ENABLED: "false"` en las variables de entorno del job `backend`.
   - Las pruebas de integración con Jira (`JiraIncidentSyncTest`, `INC-C01`..`INC-C15`) utilizan `MockRestServiceServer` en memoria, garantizando validación determinista sin llamadas a Internet.
2. **Aislamiento de Base de Datos SQLite:**
   - El archivo `exportrace.db` local está excluido mediante `.gitignore` (`*.db`) y no se versiona en Git.
   - Durante la ejecución en GitHub Actions, el job `backend` inyecta `SPRING_DATASOURCE_URL: jdbc:sqlite:target/exportrace-ci-test.db`, creando una base de datos SQLite efímera y aislada dentro de `target/` que se destruye al finalizar el runner.

---

## 7. Procedimiento Formal de Control de Cambios

Todo cambio evolutivo o correctivo en ExporTrace debe seguir este flujo de 8 pasos:

1. **Crear rama de trabajo** a partir de `main` actualizado:
   ```bash
   git checkout main
   git pull origin main
   git checkout -b feature/nombre-del-cambio
   ```
2. **Implementar el cambio** respetando la arquitectura y pruebas existentes.
3. **Validar localmente** antes de subir cambios (`mvn -B clean test`, `mvn -B package -DskipTests`, `npm ci`, `npm run build`).
4. **Registrar commit y publicar la rama:**
   ```bash
   git add .
   git commit -m "feat(modulo): descripción clara del cambio"
   git push -u origin feature/nombre-del-cambio
   ```
5. **Abrir un Pull Request (PR)** en GitHub hacia la rama `main`.
6. **Ejecución automática de GitHub Actions (`ExporTrace CI`):** Esperar a que las verificaciones `Backend CI` y `Frontend CI` finalicen en estado verde (`PASS`).
7. **Revisión de código (Code Review):** Revisar el diff del Pull Request y los resultados del pipeline.
8. **Fusión (Merge) a `main`:** Integrar el Pull Request únicamente cuando el CI esté en `PASS` y la revisión haya sido aprobada. No realizar commits directos sobre `main` como práctica habitual.

---

## 8. Procedimiento ante un Pipeline Fallido (`FAIL`)

Si una verificación de GitHub Actions falla en un Pull Request:

1. Hacer clic en **Details** junto al check en rojo (`Backend CI` o `Frontend CI`) dentro del Pull Request en GitHub.
2. Identificar el paso exacto que falló (por ejemplo, `Execute Backend Test Suite` o `Typecheck and Build Production Bundle`).
3. Reproducir el fallo localmente ejecutando el mismo comando:
   - Backend: `mvn -B clean test`
   - Frontend: `npm ci && npm run build`
4. Corregir la causa raíz en la misma rama del Pull Request.
5. Hacer un nuevo `git commit` y `git push` a la rama: GitHub Actions disparará automáticamente una nueva ejecución de `ExporTrace CI`.

---

## 9. Configuración Recomendada de Protección de Rama (`Branch Protection`) en GitHub

Para garantizar que ningún cambio se fusione en `main` sin superar el pipeline de Integración Continua, se recomienda configurar manualmente la regla de protección en GitHub siguiendo estos pasos:

1. Ingresar al repositorio en GitHub (`Integrador_II_Backend` y `Integrador_II_Frontend`).
2. Ir a **Settings** → **Branches** (en el menú lateral izquierdo, sección *Code and automation*).
3. Hacer clic en **Add branch protection rule** (o **Add branch ruleset**).
4. En **Branch name pattern**, escribir: `main`.
5. Marcar las siguientes opciones:
   - ✅ **Require a pull request before merging**
     - Opcionalmente activar *Require approvals* (`1` aprobación mínima si trabaja más de un integrante).
   - ✅ **Require status checks to pass before merging**
     - Marcar *Require branches to be up to date before merging*.
     - En el buscador de *Status checks*, seleccionar:
       - En el repositorio Backend: **`Backend CI (Java 21 & Maven)`**
       - En el repositorio Frontend: **`Frontend CI (Node 24, TypeScript & Vite)`**
       - *(O ambos checks si se utiliza un repositorio unificado)*.
   - ✅ **Do not allow bypassing the above settings**.
6. Hacer clic en **Create** / **Save changes**.
