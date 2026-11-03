# Calendario Oficial de Cambios — ExporTrace

**Curso:** Taller Integrador II — Semana 11: Control de Cambios e Integración Continua  
**Sistema:** Sistema Inteligente de Trazabilidad Hidrobiológica ExporTrace  

---

## 1. Estados Oficiales del Calendario de Cambios

Todo cambio registrado en este calendario debe encontrarse en uno de los siguientes estados canónicos:

- **`PLANIFICADO`**: RFC redactada y programada para evaluación o futura ventana de implementación.
- **`APROBADO`**: RFC evaluada y autorizada formalmente por el Líder Técnico / CAB, lista para iniciar o concluir su despliegue.
- **`EN_IMPLEMENTACION`**: Cambio actualmente en desarrollo en rama dedicada, en revisión de Pull Request o en ejecución dentro de la ventana autorizada.
- **`COMPLETADO`**: Cambio integrado a `main` con GitHub Actions `PASS`, validado y cerrado exitosamente.
- **`RECHAZADO`**: RFC desestimada durante la evaluación o Pull Request cancelado por no cumplir criterios de calidad/alcance.
- **`REVERTIDO`**: Cambio que presentó anomalías post-implementación y fue revertido aplicando su Plan de Rollback.

---

## 2. Registro Cronológico y Programación de Cambios (`RFC-EXP-2026-*`)

| ID RFC | Cambio | Tipo | Responsable | Fecha | Ventana | Ambiente | Estado |
| :---: | :--- | :---: | :--- | :---: | :--- | :--- | :---: |
| **`RFC-EXP-2026-001`** | Evolución del esquema `quality_inspections` de `1:1` a `1:N` y soporte de reinspecciones QA con evidencias SHA-256 | `NORMAL` *(Alto Riesgo)* | Líder Técnico / Desarrollador Backend | `2026-10-07` | Ventana Planificada Académica (No-Demo) | Local + Render (`main`) | **`COMPLETADO`** |
| **`RFC-EXP-2026-002`** | Corrección de conversión de ID de lote (`lot-001` String vs `Long`) en endpoints y vista de Cadena de Frío | `EMERGENCIA` | Desarrollador Fullstack / Operaciones | `2026-10-14` | Intervención Inmediata Correctiva | Local + Render (`main`) | **`COMPLETADO`** |
| **`RFC-EXP-2026-003`** | Incorporación del módulo local de Gestión de Incidencias (Fase A: Modelo + API + Priorización) y Centro de Soporte (Fase B: React UI) | `NORMAL` | Equipo Desarrollo ExporTrace | `2026-10-20` | Ventana Planificada Académica | Desarrollo Local | **`COMPLETADO`** |
| **`RFC-EXP-2026-004`** | Integración REST ExporTrace $\rightarrow$ Jira Service Management Cloud con idempotencia `LOOKUP BEFORE CREATE` (Fase C) | `NORMAL` | Líder Técnico / Desarrollador Backend | `2026-10-21` | Ventana Planificada Académica | Desarrollo Local (Mock CI) | **`COMPLETADO`** |
| **`RFC-EXP-2026-005`** | Implementación del pipeline de Integración Continua en GitHub Actions (`Backend CI` Java 21 + `Frontend CI` Node 24) | `NORMAL` | Responsable de Operaciones / Líder Técnico | `2026-10-21` | Ventana Planificada Semana 11 | GitHub Actions (`Integrador_II_Backend` / `Integrador_II_Frontend`) | **`COMPLETADO`** |
| **`RFC-EXP-2026-006`** | Actualización de textos amigables de estado de sincronización y ticket en UI del Centro de Soporte | `ESTÁNDAR` | Desarrollador Frontend | `2026-10-21` | Ventana Estándar Preautorizada | Desarrollo Local | **`COMPLETADO`** |
| **`RFC-EXP-2026-007`** | Activación de credenciales reales y validación de preflight contra Jira Service Management Cloud (`VALIDADA_JIRA_REAL`) | `NORMAL` | Operaciones / Líder Técnico | `2026-10-21` | Ventana Planificada Académica (Bloqueado en Preflight hasta definir variables `JIRA_*` en entorno local) | Desarrollo Local / Jira Cloud | **`EN_IMPLEMENTACION`** *(Detenido en Preflight: faltan variables `JIRA_*`)* |
| **`RFC-EXP-2026-008`** | Implementación de Comentarios Públicos, Adjuntos Seguros (`Magic Bytes + SHA-256`) y Reapertura de Incidencias (Fase D) | `NORMAL` | Equipo Desarrollo ExporTrace | *Por definir según autorización* | Ventana Planificada Académica | Desarrollo Local + CI | **`PLANIFICADO`** |
| **`RFC-EXP-2026-009`** | Implementación de Webhook entrante Jira $\rightarrow$ ExporTrace con firma HMAC y deduplicación (Fase E) | `NORMAL` *(Alto Riesgo)* | Líder Técnico / Desarrollador Backend | *Por definir según autorización* | Ventana Planificada Académica | Desarrollo Local + Render | **`PLANIFICADO`** |

---

## 3. Reglas Operativas de Calendarización (Política Académica)

1. **Restricción Pre-Sustentación (*Change Freeze*):** Ninguna RFC en estado `PLANIFICADO` o `APROBADO` de tipo `NORMAL` o `ESTÁNDAR` debe ejecutarse inmediatamente antes ni durante una presentación o revisión calificada con el docente.
2. **Verificación Pre-Ventana:** Antes de cambiar el estado de una RFC a `EN_IMPLEMENTACION`, el responsable debe confirmar:
   - Que la rama Git siga la convención `feature/RFC-EXP-2026-XXX-...`.
   - Que exista respaldo previo si el cambio afecta base de datos o variables de entorno.
   - Que los checks **`Backend CI (Java 21 & Maven)`** y **`Frontend CI (Node 24, TypeScript & Vite)`** se encuentren operativos en GitHub Actions.
