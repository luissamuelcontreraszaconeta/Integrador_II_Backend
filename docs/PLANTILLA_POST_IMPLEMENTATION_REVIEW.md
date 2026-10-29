# Plantilla de Revisión Post-Implementación (PIR — Post Implementation Review)

**Sistema Inteligente de Trazabilidad Hidrobiológica ExporTrace**  
*Esta plantilla debe completarse obligatoriamente después de todo Cambio de Emergencia, Cambio Normal de riesgo Alto/Crítico o Cambio Revertido.*

---

## 1. Datos de Identificación

| Campo | Detalle |
| :--- | :--- |
| **ID PIR** | `PIR-EXP-2026-XXX` |
| **ID RFC Evaluada** | `RFC-EXP-2026-XXX` |
| **Título del Cambio** | *[Título de la RFC]* |
| **Tipo de Cambio** | `[ ] NORMAL` &nbsp;&nbsp; `[ ] EMERGENCIA` &nbsp;&nbsp; `[ ] ESTÁNDAR (Con incidente)` |
| **Fecha de Implementación** | `AAAA-MM-DD` |
| **Fecha de Revisión PIR** | `AAAA-MM-DD` |
| **Participantes de la Revisión** | Product Owner, Líder Técnico, Responsable de Operaciones, Desarrollador |

---

## 2. Cuestionario de Evaluación Post-Implementación

| # | Pregunta de Control | Respuesta (`SÍ` / `NO` / `PARCIAL`) | Evidencia / Comentarios Técnicos |
| :---: | :--- | :---: | :--- |
| **1** | **¿El cambio logró su objetivo técnico y funcional esperado?** | `[ ] SÍ` `[ ] NO` `[ ] PARCIAL` | *[Detallar cómo se verificó el cumplimiento del objetivo]* |
| **2** | **¿Hubo incidentes o anomalías derivados del cambio?** | `[ ] SÍ` `[ ] NO` | *[Si hubo incidente, indicar código `INC-2026-XXXXXX`]* |
| **3** | **¿Se cumplió la ventana de implementación planificada?** | `[ ] SÍ` `[ ] NO` | *[Indicar duración real frente a la estimada]* |
| **4** | **¿Fue necesario ejecutar el Plan de Rollback?** | `[ ] SÍ` `[ ] NO` | *[Si se ejecutó rollback, detallar motivo y tiempo de recuperación]* |
| **5** | **¿Fallaron pruebas automatizadas o post-deploy durante el proceso?** | `[ ] SÍ` `[ ] NO` | *[Indicar resultados de GitHub Actions CI y Smoke Test]* |

---

## 3. Trazabilidad con Gestión de Incidencias (`RFC ↔ INC`)

- **¿El cambio se originó para resolver una incidencia previa?**
  - Código de incidencia origen: `INC-2026-XXXXXX` / `N/A`
- **¿El cambio provocó una nueva incidencia tras su despliegue?**
  - Código de incidencia generada: `INC-2026-XXXXXX` / `N/A`
  - Ticket Jira Service Management asociado (si aplica): `EXP-XX` / `N/A`

---

## 4. Lecciones Aprendidas y Acciones de Mejora

- **¿Qué funcionó bien durante este cambio?**
  > *[Ej. El pipeline de GitHub Actions detectó a tiempo una diferencia de tipos antes de hacer merge a `main`.]*

- **¿Qué dificultades o desviaciones se presentaron?**
  > *[Ej. Diferencia de saltos de línea LF/CRLF o necesidad de aislar la base de datos SQLite en el runner de CI.]*

- **¿Qué acciones concretas deben aplicarse para mejorar futuros cambios?**
  1. *[Acción de mejora 1 — Responsable]*
  2. *[Acción de mejora 2 — Responsable]*

---

## 5. Dictamen de Cierre del PIR

- **Estado definitivo del cambio:** `[ ] CERRADO EXITOSAMENTE` &nbsp;&nbsp; `[ ] CERRADO TRAS ROLLBACK (Requiere nueva RFC)`
- **Conformidad del Líder Técnico / Product Owner:** ___________________________

---

## Ejemplo Real Registrado: `PIR-EXP-2026-005` (Sobre `RFC-EXP-2026-005`)

| Campo | Registro Real |
| :--- | :--- |
| **ID PIR / ID RFC** | `PIR-EXP-2026-005` $\leftrightarrow$ `RFC-EXP-2026-005` |
| **Título del Cambio** | Implementación y Validación del Pipeline de Integración Continua con GitHub Actions |
| **¿El cambio logró su objetivo?** | **SÍ.** Se crearon los workflows `.github/workflows/ci.yml` en `Integrador_II_Backend` e `Integrador_II_Frontend` y ambos pasaron exitosamente en sus Pull Requests reales (`All checks passed`). |
| **¿Hubo incidentes?** | **NO.** Ninguna funcionalidad existente fue afectada. |
| **¿Se cumplió la ventana?** | **SÍ.** Implementado y verificado dentro de la sesión de la Semana 11. |
| **¿Fue necesario rollback?** | **NO.** |
| **¿Fallaron pruebas?** | **NO.** `118/118` pruebas Backend `PASS` y `npm ci && npm run build` Frontend `PASS`. |
| **¿Qué aprendimos?** | En proyectos donde Backend y Frontend residen en dos repositorios Git separados (`Integrador_II_Backend` e `Integrador_II_Frontend`), cada repositorio debe tener su propio `.github/workflows/ci.yml` apuntando a la raíz (`.`) sin usar `working-directory` de subcarpeta. |
| **¿Qué debe mejorar?** | Activar la regla de *Branch Protection* en la interfaz de GitHub para exigir ambos checks antes de fusionar futuros Pull Requests a `main`. |
