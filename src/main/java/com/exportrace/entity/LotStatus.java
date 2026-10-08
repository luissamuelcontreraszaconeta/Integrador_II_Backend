package com.exportrace.entity;

public enum LotStatus {
    REGISTERED("Registrado", "Lote creado, pendiente de inspección técnica QA"),
    UNDER_QA_INSPECTION("En Inspección QA", "En evaluación física y organoléptica"),
    OBSERVED("Observado", "Observado por calidad o desviación térmica"),
    REJECTED("Rechazado", "No conforme definitivo, descarte técnico"),
    READY_FOR_CERTIFICATION("Listo para Certificación", "QA conforme y frío estable, apto para trámite SANIPES"),
    IN_CERTIFICATION("En Certificación", "Expediente en trámite ante autoridad sanitaria"),
    CERTIFIED("Certificado", "Certificado Sanitario Oficial emitido y vigente"),
    READY_FOR_DISPATCH("Listo para Despacho", "Habilitado para asignación de contenedor, precinto y DUA"),
    DISPATCHED("Despachado", "Contenedor precintado y despacho aduanero completado"),
    CANCELLED("Anulado", "Lote anulado administrativamente");

    private final String etiqueta;
    private final String descripcion;

    LotStatus(String etiqueta, String descripcion) {
        this.etiqueta = etiqueta;
        this.descripcion = descripcion;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public static LotStatus fromString(String statusStr) {
        if (statusStr == null || statusStr.trim().isEmpty()) {
            return REGISTERED;
        }
        String clean = statusStr.trim().toUpperCase().replace(" ", "_");
        // Handle legacy statuses
        switch (clean) {
            case "DRAFT":
            case "REGISTRADO":
            case "PENDING_QA":
                return REGISTERED;
            case "IN_QA":
            case "EN_PROCESO":
            case "UNDER_QA":
                return UNDER_QA_INSPECTION;
            case "OBSERVADO":
                return OBSERVED;
            case "RECHAZADO":
            case "NO_CONFORME":
                return REJECTED;
            case "APROBADO":
            case "VALIDATION_PENDING":
            case "QA_APROBADO":
                return READY_FOR_CERTIFICATION;
            case "EN_CERTIFICACION":
            case "CERTIFICACION_PENDIENTE":
                return IN_CERTIFICATION;
            case "CERTIFICADO":
                return CERTIFIED;
            case "LISTO_DESPACHO":
            case "APTO_DESPACHO":
                return READY_FOR_DISPATCH;
            case "DESPACHADO":
            case "EN_TRANSITO":
                return DISPATCHED;
            case "ANULADO":
            case "CANCELADO":
                return CANCELLED;
            default:
                try {
                    return LotStatus.valueOf(clean);
                } catch (IllegalArgumentException e) {
                    return REGISTERED;
                }
        }
    }
}
