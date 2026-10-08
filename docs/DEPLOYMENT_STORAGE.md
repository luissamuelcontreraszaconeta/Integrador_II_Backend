# Guía Normativa de Despliegue: Almacenamiento Persistente en Render (v1.0)
## Módulo de Persistencia de Evidencias Fotográficas (`BR-P0-003` / `OM-05`)

---

### 1. Contexto del Problema y Arquitectura
En la infraestructura de contenedores de **Render** (así como en Docker / Kubernetes estándar), el sistema de archivos raíz (`root filesystem`) es **efímero**. Cualquier archivo binario o imagen subida por los inspectores de calidad que se guarde en una ruta relativa local (ej. `./uploads`) se destruirá irrecuperablemente ante:
- Un nuevo despliegue (*redeploy / git push*).
- Un reinicio automático por mantenimiento o escalado (*restart*).
- Un cambio de variables de entorno.

Para garantizar la custodia forense y trazabilidad sanitaria de las evidencias fotográficas organolépticas (según normativa SANIPES y requerimientos `RF-14`, `RF-15`, `RF-16`), **ExporTrace v1.0** implementa la arquitectura de **Render Persistent Disk**.

---

### 2. Especificación Técnica de Configuración en Render

#### A. Creación del Persistent Disk en el Dashboard de Render
1. Acceder al servicio backend en el dashboard de Render (`exportrace-backend` / Web Service).
2. Navegar a la pestaña **Disks** y pulsar **Add Disk**.
3. Configurar los siguientes parámetros oficiales:
   - **Name:** `exportrace-persistent-disk`
   - **Size:** `1 GB` (ampliable según demanda operacional).
   - **Mount Path:** `/app/data`

#### B. Variables de Entorno en Render (`Environment Variables`)
Configurar la siguiente variable de entorno canónica en el Web Service de Render:

```properties
FILE_UPLOAD_DIR=/app/data/uploads
```

*Nota:* Si no se especifica la variable, el backend adopta por defecto `./data/uploads` para entornos de desarrollo local y pruebas automatizadas, garantizando total portabilidad.

---

### 3. Estructura de Directorios en el Disco Persistente

```text
/app/data/
└── uploads/
    └── evidence/
        ├── lot_1/
        │   ├── 4b67ca3a-04d2-40c7-a0e4-65e379033c89.jpg
        │   └── e1a8b9c2-901f-4d33-91ac-78de45bc0123.png
        ├── lot_2/
        │   └── f893ab45-87d2-4821-bcf3-671290abcdef.webp
        └── ...
```

---

### 4. Medidas de Seguridad Implementadas en v1.0

1. **Nomenclatura UUID Criptográfica:** Los nombres físicos de archivo se generan mediante `UUID.randomUUID().toString() + extension`, imposibilitando la colisión de nombres y ataques de *Path Traversal* (`../../`).
2. **Validación de Magic Bytes:** El backend examina la cabecera binaria (`FF D8 FF` para JPEG, `89 50 4E 47` para PNG, `52 49 46 46` para WEBP). Se rechaza cualquier archivo ejecutable camuflado con extensión `.jpg`.
3. **Integridad Criptográfica SHA-256:** Cada archivo genera un hash SHA-256 durante el streaming de subida, el cual se almacena en la tabla `qa_evidences` y se valida al solicitar la verificación forense.
4. **Streaming Binario Protegido:** Se eliminó la exposición pública de `/uploads/**`. El acceso es exclusivo mediante `GET /api/quality/evidence/{id}` validando token JWT y control de acceso RBAC.
5. **Inmutabilidad Post-Certificación:** Si un lote se encuentra en estado `CERTIFIED`, `READY_FOR_DISPATCH` o `DISPATCHED`, cualquier intento de agregar o eliminar evidencias es bloqueado con código `HTTP 409 Conflict`.
6. **Soft-Delete Auditado:** Las eliminaciones en lotes activos marcan `active = false` conservando el registro histórico y emitiendo evento de auditoría `QA_EVIDENCE_DEACTIVATED`.

---

### 5. Procedimiento de Verificación en Entorno Staging / Producción

Para validar el cierre definitivo de `OM-05` en Render:

1. **Carga Inicial:** Ingresar como usuario `QA` (`qa@exportrace.pe`), seleccionar un lote activo (ej. `LOT-2026-001`) y subir 2 fotografías organolépticas con descripción.
2. **Verificación de Metadatos:** Comprobar que las imágenes se visualizan en el formulario y en el Lightbox protegido.
3. **Reinicio Forzado en Render:** Desde el panel de Render, ejecutar **Manual Deploy > Trigger Deploy** o **Restart Service**.
4. **Comprobación Post-Reinicio:** Tras completarse el despliegue, iniciar sesión nuevamente y acceder al lote `LOT-2026-001`.
5. **Resultado Esperado:** Ambas fotografías deben cargar correctamente, el endpoint `/api/quality/evidence/{id}/verify` debe responder `INTEGRITY_VERIFIED`, confirmando que el volumen persistente conservó íntegramente los archivos.

---

### 6. Evolución Futura (Fuera de Alcance v1.0)
Para fases de alta escalabilidad multi-región, la interfaz `FileStorageService` está diseñada para admitir proveedores en la nube:
- **Amazon S3:** Implementación `S3FileStorageService` con buckets privados y URLs pre-firmadas temporales.
- **Cloudinary:** Implementación `CloudinaryFileStorageService` para optimización y compresión multimedia bajo demanda.
