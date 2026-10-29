package com.exportrace.entity.enums;

public enum IncidentCategory {
    AUTHENTICATION("CAT-01", "Autenticación y sesiones", "AUTENTICACION"),
    ACCESS_CONTROL("CAT-02", "Usuarios, roles y permisos", "ADMINISTRACION"),
    LOTS("CAT-03", "Registro y gestión de lotes", "LOTES"),
    QUALITY("CAT-04", "Calidad / QA", "CALIDAD"),
    COLD_CHAIN("CAT-05", "Cadena de frío", "FRIO"),
    DOCUMENTS("CAT-06", "Documentos y evidencias", "DOCUMENTOS"),
    CERTIFICATION("CAT-07", "Certificación sanitaria", "CERTIFICACION"),
    TRACEABILITY_QR("CAT-08", "QR y trazabilidad", "TRAZABILIDAD"),
    LOGISTICS("CAT-09", "Logística y despacho", "LOGISTICA"),
    DASHBOARD("CAT-10", "Dashboard y reportes", "DASHBOARD"),
    NOTIFICATIONS("CAT-11", "Notificaciones", "NOTIFICATIONS"),
    BACKEND_API("CAT-12", "Backend / API", "SISTEMA"),
    DATABASE("CAT-13", "Base de datos", "SISTEMA"),
    INFRASTRUCTURE("CAT-14", "Infraestructura / Render", "SISTEMA"),
    OTHER("CAT-15", "Otro", "SISTEMA");

    private final String catalogCode;
    private final String label;
    private final String defaultModule;

    IncidentCategory(String catalogCode, String label, String defaultModule) {
        this.catalogCode = catalogCode;
        this.label = label;
        this.defaultModule = defaultModule;
    }

    public String getCatalogCode() {
        return catalogCode;
    }

    public String getLabel() {
        return label;
    }

    public String getDefaultModule() {
        return defaultModule;
    }

    public static IncidentCategory fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase().replace("-", "_");
        for (IncidentCategory cat : values()) {
            if (cat.name().equalsIgnoreCase(normalized)
                    || cat.catalogCode.replace("-", "_").equalsIgnoreCase(normalized)
                    || cat.catalogCode.equalsIgnoreCase(value.trim())) {
                return cat;
            }
        }
        throw new IllegalArgumentException("Categoría de incidencia inválida: " + value);
    }
}
