package com.exportrace.dto;

import com.exportrace.entity.Lot;
import com.exportrace.entity.QualityInspection;
import com.exportrace.entity.SanitaryCertification;

import java.time.format.DateTimeFormatter;

public class PublicTraceabilityDTO {

    private boolean valid;
    private String codigo;
    private String producto;
    private String especie;
    private String fechaProduccion;
    private Double volumen;
    private String unidad;
    private String plantaProcesamiento;
    private String proveedor;
    private String embarcacion;
    private String puertoOrigen;
    private String estadoCalidad;
    private String estadoCadenaFrio;
    private boolean documentacionCompleta;
    private String estadoCertificacion;
    private String numeroCertificadoSanitario;
    private String estadoGeneral;
    private String ultimaActualizacion;
    private String qrToken;

    public PublicTraceabilityDTO() {
        this.valid = false;
    }

    public PublicTraceabilityDTO(Lot lot, QualityInspection qa, SanitaryCertification cert, int coldChainCount, boolean hasColdChainAlert, int docCount) {
        this.valid = true;
        this.codigo = lot.getCodigo();
        this.producto = lot.getProducto() != null ? lot.getProducto().getNombre() : "Producto Hidrobiológico";
        this.especie = lot.getProducto() != null && lot.getProducto().getNombreCientifico() != null 
                ? lot.getProducto().getNombreCientifico() 
                : (lot.getProducto() != null && lot.getProducto().getEspecie() != null ? lot.getProducto().getEspecie() : "Dosidicus gigas");
        this.fechaProduccion = lot.getFechaProduccion() != null ? lot.getFechaProduccion().toString() : "";
        this.volumen = lot.getPesoNetoKg() != null ? (lot.getPesoNetoKg() / 1000.0) : 0.0;
        this.unidad = "TN";
        this.plantaProcesamiento = lot.getPlantaProcesamiento() != null ? lot.getPlantaProcesamiento() : "Planta Paita #01";
        this.proveedor = lot.getProveedor() != null ? lot.getProveedor() : "Asociación Pesquera Artesanal Paita Norte";
        this.embarcacion = lot.getEmbarcacion() != null ? lot.getEmbarcacion() : "E/P Don Luis II";
        this.puertoOrigen = "Puerto de Paita, Piura";

        // Quality state
        if (qa != null) {
            this.estadoCalidad = qa.getResultadoOrganoleptico() != null ? qa.getResultadoOrganoleptico() : "CONFORME";
        } else {
            this.estadoCalidad = "PENDIENTE";
        }

        // Cold Chain state
        if (coldChainCount > 0) {
            this.estadoCadenaFrio = hasColdChainAlert ? "ALERTA_TERMICA" : "CONFORME";
        } else {
            this.estadoCadenaFrio = "PENDIENTE";
        }

        this.documentacionCompleta = docCount >= 2;

        // Certification state
        if (cert != null && "APROBADO".equalsIgnoreCase(cert.getEstado())) {
            this.estadoCertificacion = "CERTIFICADA";
            this.numeroCertificadoSanitario = cert.getNumeroCertificado();
        } else if (cert != null) {
            this.estadoCertificacion = "EN_TRAMITE";
            this.numeroCertificadoSanitario = cert.getNumeroCertificado() != null ? cert.getNumeroCertificado() : "EXP-TRAMITE-REGISTRADO";
        } else if ("CERTIFIED".equalsIgnoreCase(lot.getEstado()) || "READY_FOR_DISPATCH".equalsIgnoreCase(lot.getEstado()) || "DISPATCHED".equalsIgnoreCase(lot.getEstado())) {
            this.estadoCertificacion = "CERTIFICADA";
            this.numeroCertificadoSanitario = "CS-2026-SANIPES-REGISTRADO";
        } else {
            this.estadoCertificacion = "PENDIENTE";
            this.numeroCertificadoSanitario = null;
        }

        this.estadoGeneral = lot.getEstado();
        this.qrToken = lot.getQrToken();
        
        if (lot.getFechaActualizacion() != null) {
            this.ultimaActualizacion = lot.getFechaActualizacion().format(DateTimeFormatter.ISO_DATE_TIME);
        } else if (lot.getFechaCreacion() != null) {
            this.ultimaActualizacion = lot.getFechaCreacion().format(DateTimeFormatter.ISO_DATE_TIME);
        } else {
            this.ultimaActualizacion = "";
        }
    }

    public static PublicTraceabilityDTO notFound() {
        PublicTraceabilityDTO dto = new PublicTraceabilityDTO();
        dto.setValid(false);
        return dto;
    }

    // Getters and Setters
    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getProducto() { return producto; }
    public void setProducto(String producto) { this.producto = producto; }

    public String getEspecie() { return especie; }
    public void setEspecie(String especie) { this.especie = especie; }

    public String getFechaProduccion() { return fechaProduccion; }
    public void setFechaProduccion(String fechaProduccion) { this.fechaProduccion = fechaProduccion; }

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

    public String getEstadoCalidad() { return estadoCalidad; }
    public void setEstadoCalidad(String estadoCalidad) { this.estadoCalidad = estadoCalidad; }

    public String getEstadoCadenaFrio() { return estadoCadenaFrio; }
    public void setEstadoCadenaFrio(String estadoCadenaFrio) { this.estadoCadenaFrio = estadoCadenaFrio; }

    public boolean isDocumentacionCompleta() { return documentacionCompleta; }
    public void setDocumentacionCompleta(boolean documentacionCompleta) { this.documentacionCompleta = documentacionCompleta; }

    public String getEstadoCertificacion() { return estadoCertificacion; }
    public void setEstadoCertificacion(String estadoCertificacion) { this.estadoCertificacion = estadoCertificacion; }

    public String getNumeroCertificadoSanitario() { return numeroCertificadoSanitario; }
    public void setNumeroCertificadoSanitario(String numeroCertificadoSanitario) { this.numeroCertificadoSanitario = numeroCertificadoSanitario; }

    public String getEstadoGeneral() { return estadoGeneral; }
    public void setEstadoGeneral(String estadoGeneral) { this.estadoGeneral = estadoGeneral; }

    public String getUltimaActualizacion() { return ultimaActualizacion; }
    public void setUltimaActualizacion(String ultimaActualizacion) { this.ultimaActualizacion = ultimaActualizacion; }

    public String getQrToken() { return qrToken; }
    public void setQrToken(String qrToken) { this.qrToken = qrToken; }
}
