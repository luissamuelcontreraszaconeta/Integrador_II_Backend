package com.exportrace.dto;

import com.exportrace.entity.ColdChainRecord;
import com.exportrace.entity.Dispatch;
import com.exportrace.entity.Lot;
import com.exportrace.entity.QualityInspection;
import com.exportrace.entity.SanitaryCertification;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * DTO público y sanitizado de verificación de trazabilidad por QR.
 * Excluye estrictamente credenciales, correos, roles, usuarios internos, IDs internos,
 * rutas de archivos internos y bitácoras de auditoría.
 */
public class PublicTraceabilityDTO {

    private boolean valid;
    private boolean authentic;
    private String resultadoAutenticidad;
    private String mensaje;
    private String lotCode;
    private String codigo;
    private String producto;
    private String especie;
    private String fechaProduccion;
    private String fechaRelevante;
    private Double volumen;
    private String unidad;
    private String plantaProcesamiento;
    private String proveedor;
    private String embarcacion;
    private String puertoOrigen;
    private String estadoSanitario;
    private String estadoGeneral;
    private String estadoCalidad;
    private String estadoCadenaFrio;
    private String resumenCadenaFrio;
    private boolean documentacionCompleta;
    private String estadoCertificacion;
    private String numeroCertificadoSanitario;
    private String estadoDespacho;
    private String puertoDestino;
    private String ultimaActualizacion;
    private String qrToken;

    public PublicTraceabilityDTO() {
        this.valid = false;
        this.authentic = false;
        this.resultadoAutenticidad = "NO_REGISTRADO";
        this.mensaje = "No se encontró un registro válido para este código.";
    }

    public PublicTraceabilityDTO(
            Lot lot,
            QualityInspection qa,
            SanitaryCertification cert,
            Dispatch dispatch,
            List<ColdChainRecord> coldRecords,
            boolean hasColdChainAlert,
            int docCount) {

        this.valid = true;
        this.authentic = true;
        this.resultadoAutenticidad = "REGISTRO_AUTENTICO_EXPORTRACE";
        this.mensaje = "✓ Registro auténtico en ExporTrace";

        this.lotCode = lot.getCodigo();
        this.codigo = lot.getCodigo();
        this.producto = lot.getProducto() != null && lot.getProducto().getNombre() != null
                ? lot.getProducto().getNombre()
                : "Producto Hidrobiológico";
        this.especie = lot.getProducto() != null && lot.getProducto().getNombreCientifico() != null
                ? lot.getProducto().getNombreCientifico()
                : (lot.getProducto() != null && lot.getProducto().getEspecie() != null
                        ? lot.getProducto().getEspecie()
                        : "Dosidicus gigas");

        this.fechaProduccion = lot.getFechaProduccion() != null ? lot.getFechaProduccion().toString() : "";
        this.volumen = lot.getPesoNetoKg() != null ? (lot.getPesoNetoKg() / 1000.0) : 0.0;
        this.unidad = "TN";
        this.plantaProcesamiento = lot.getPlantaProcesamiento() != null ? lot.getPlantaProcesamiento() : "Planta Paita #01";
        this.proveedor = lot.getProveedor() != null ? lot.getProveedor() : "Asociación Pesquera Artesanal Paita Norte";
        this.embarcacion = lot.getEmbarcacion() != null ? lot.getEmbarcacion() : "E/P Don Luis II";
        this.puertoOrigen = "Puerto de Paita, Piura";

        // Estado QA
        if (qa != null && qa.getResultadoOrganoleptico() != null && !qa.getResultadoOrganoleptico().isBlank()) {
            this.estadoCalidad = qa.getResultadoOrganoleptico().trim().toUpperCase();
        } else {
            this.estadoCalidad = "PENDIENTE";
        }

        // Estado Cadena de Frío resumido
        int coldChainCount = coldRecords != null ? coldRecords.size() : 0;
        if (coldChainCount > 0) {
            this.estadoCadenaFrio = hasColdChainAlert ? "ALERTA_TERMICA" : "CONFORME";
            Double minTemp = coldRecords.stream()
                    .map(ColdChainRecord::getTemperaturaCelsius)
                    .filter(t -> t != null)
                    .min(Double::compareTo)
                    .orElse(-18.0);
            this.resumenCadenaFrio = hasColdChainAlert
                    ? "OBSERVADA (" + coldChainCount + " lecturas registradas)"
                    : String.format(java.util.Locale.US, "CONFORME (%d lecturas, mín. %.1f°C)", coldChainCount, minTemp);
        } else {
            this.estadoCadenaFrio = "PENDIENTE";
            this.resumenCadenaFrio = "PENDIENTE DE REGISTRO";
        }

        this.documentacionCompleta = docCount >= 2;

        // Estado Certificación Sanitaria
        if (cert != null && "APROBADO".equalsIgnoreCase(cert.getEstado())) {
            this.estadoCertificacion = "CERTIFICADA";
            this.numeroCertificadoSanitario = cert.getNumeroCertificado();
        } else if (cert != null) {
            this.estadoCertificacion = "EN_TRAMITE";
            this.numeroCertificadoSanitario = cert.getNumeroCertificado() != null ? cert.getNumeroCertificado() : "EXP-TRAMITE-REGISTRADO";
        } else if ("CERTIFIED".equalsIgnoreCase(lot.getEstado())
                || "READY_FOR_DISPATCH".equalsIgnoreCase(lot.getEstado())
                || "DISPATCHED".equalsIgnoreCase(lot.getEstado())) {
            this.estadoCertificacion = "CERTIFICADA";
            this.numeroCertificadoSanitario = "CS-2026-SANIPES-REGISTRADO";
        } else {
            this.estadoCertificacion = "PENDIENTE";
            this.numeroCertificadoSanitario = null;
        }

        // Estado Despacho
        String rawLotState = lot.getEstado() != null ? lot.getEstado().trim().toUpperCase() : "REGISTERED";
        if (dispatch != null && "DESPACHADO".equalsIgnoreCase(dispatch.getEstado())) {
            this.estadoDespacho = "DESPACHADO";
            this.puertoDestino = dispatch.getPuertoDestino();
        } else if ("DISPATCHED".equals(rawLotState)) {
            this.estadoDespacho = "DESPACHADO";
            this.puertoDestino = dispatch != null ? dispatch.getPuertoDestino() : "Puerto Internacional";
        } else if ("READY_FOR_DISPATCH".equals(rawLotState) || "CERTIFIED".equals(rawLotState)) {
            this.estadoDespacho = "AUTORIZADO PARA DESPACHO";
            this.puertoDestino = dispatch != null ? dispatch.getPuertoDestino() : null;
        } else {
            this.estadoDespacho = "PENDIENTE DE DESPACHO";
            this.puertoDestino = null;
        }

        // Estado General / Sanitario legible
        this.estadoGeneral = rawLotState;
        this.estadoSanitario = resolveEstadoSanitarioLabel(rawLotState, this.estadoCalidad, this.estadoCertificacion);

        this.qrToken = lot.getQrToken();

        if (lot.getFechaActualizacion() != null) {
            this.ultimaActualizacion = lot.getFechaActualizacion().format(DateTimeFormatter.ISO_DATE_TIME);
        } else if (lot.getFechaCreacion() != null) {
            this.ultimaActualizacion = lot.getFechaCreacion().format(DateTimeFormatter.ISO_DATE_TIME);
        } else {
            this.ultimaActualizacion = "";
        }

        this.fechaRelevante = !this.fechaProduccion.isEmpty()
                ? this.fechaProduccion
                : (this.ultimaActualizacion.length() >= 10 ? this.ultimaActualizacion.substring(0, 10) : "");
    }

    private String resolveEstadoSanitarioLabel(String lotStatus, String qaState, String certState) {
        if ("REJECTED".equalsIgnoreCase(lotStatus) || "NO_CONFORME".equalsIgnoreCase(qaState)) {
            return "OBSERVADO / NO APTO";
        }
        if ("CERTIFICADA".equalsIgnoreCase(certState) || "READY_FOR_DISPATCH".equalsIgnoreCase(lotStatus) || "DISPATCHED".equalsIgnoreCase(lotStatus)) {
            return "APTO PARA EXPORTACIÓN (CERTIFICADO SANIPES)";
        }
        if ("CONFORME".equalsIgnoreCase(qaState)) {
            return "CONFORME EN CONTROL DE CALIDAD";
        }
        return "EN EVALUACIÓN SANITARIA";
    }

    public static PublicTraceabilityDTO notFound() {
        return new PublicTraceabilityDTO();
    }

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }

    public boolean isAuthentic() { return authentic; }
    public void setAuthentic(boolean authentic) { this.authentic = authentic; }

    public String getResultadoAutenticidad() { return resultadoAutenticidad; }
    public void setResultadoAutenticidad(String resultadoAutenticidad) { this.resultadoAutenticidad = resultadoAutenticidad; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public String getLotCode() { return lotCode; }
    public void setLotCode(String lotCode) { this.lotCode = lotCode; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getProducto() { return producto; }
    public void setProducto(String producto) { this.producto = producto; }

    public String getEspecie() { return especie; }
    public void setEspecie(String especie) { this.especie = especie; }

    public String getFechaProduccion() { return fechaProduccion; }
    public void setFechaProduccion(String fechaProduccion) { this.fechaProduccion = fechaProduccion; }

    public String getFechaRelevante() { return fechaRelevante; }
    public void setFechaRelevante(String fechaRelevante) { this.fechaRelevante = fechaRelevante; }

    public Double getVolumen() { return volumen; }
    public void setVolumen(Double volumen) { this.volumen = volumen; }

    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }

    public String getPlantaProcesamiento() { return plantaProcesamiento; }
    public void setPlantaProcesamiento(String plantaProcesamiento) { this.plantaProcesamiento = plantaProcesamiento; }

    public String getProveedor() { return proveedor; }
    public void setProveedor(String proveedor) { this.proveedor = proveedor; }

    public String getEmbarcacion() { return embarcacion; }
    public void setEmbarcacion(String embarcacion) { this.embarcacion = embarcacion; }

    public String getPuertoOrigen() { return puertoOrigen; }
    public void setPuertoOrigen(String puertoOrigen) { this.puertoOrigen = puertoOrigen; }

    public String getEstadoSanitario() { return estadoSanitario; }
    public void setEstadoSanitario(String estadoSanitario) { this.estadoSanitario = estadoSanitario; }

    public String getEstadoGeneral() { return estadoGeneral; }
    public void setEstadoGeneral(String estadoGeneral) { this.estadoGeneral = estadoGeneral; }

    public String getEstadoCalidad() { return estadoCalidad; }
    public void setEstadoCalidad(String estadoCalidad) { this.estadoCalidad = estadoCalidad; }

    public String getEstadoCadenaFrio() { return estadoCadenaFrio; }
    public void setEstadoCadenaFrio(String estadoCadenaFrio) { this.estadoCadenaFrio = estadoCadenaFrio; }

    public String getResumenCadenaFrio() { return resumenCadenaFrio; }
    public void setResumenCadenaFrio(String resumenCadenaFrio) { this.resumenCadenaFrio = resumenCadenaFrio; }

    public boolean isDocumentacionCompleta() { return documentacionCompleta; }
    public void setDocumentacionCompleta(boolean documentacionCompleta) { this.documentacionCompleta = documentacionCompleta; }

    public String getEstadoCertificacion() { return estadoCertificacion; }
    public void setEstadoCertificacion(String estadoCertificacion) { this.estadoCertificacion = estadoCertificacion; }

    public String getNumeroCertificadoSanitario() { return numeroCertificadoSanitario; }
    public void setNumeroCertificadoSanitario(String numeroCertificadoSanitario) { this.numeroCertificadoSanitario = numeroCertificadoSanitario; }

    public String getEstadoDespacho() { return estadoDespacho; }
    public void setEstadoDespacho(String estadoDespacho) { this.estadoDespacho = estadoDespacho; }

    public String getPuertoDestino() { return puertoDestino; }
    public void setPuertoDestino(String puertoDestino) { this.puertoDestino = puertoDestino; }

    public String getUltimaActualizacion() { return ultimaActualizacion; }
    public void setUltimaActualizacion(String ultimaActualizacion) { this.ultimaActualizacion = ultimaActualizacion; }

    public String getQrToken() { return qrToken; }
    public void setQrToken(String qrToken) { this.qrToken = qrToken; }
}
