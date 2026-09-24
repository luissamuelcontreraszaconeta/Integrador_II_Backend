package com.exportrace.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sanitary_certifications")
public class SanitaryCertification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_id", nullable = false, unique = true)
    private Lot lote;

    @Column(name = "numero_certificado")
    private String numeroCertificado;

    @Column(nullable = false)
    private String estado; // SOLICITADO, EN_EVALUACION, APROBADO, RECHAZADO

    @Column(name = "entidad_emisora")
    private String entidadEmisora = "SANIPES";

    @Column(name = "fecha_solicitud")
    private LocalDateTime fechaSolicitud;

    @Column(name = "fecha_emision")
    private LocalDateTime fechaEmision;

    @Column(name = "fecha_vencimiento")
    private LocalDateTime fechaVencimiento;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "pdf_url")
    private String pdfUrl;

    public SanitaryCertification() {
        this.estado = "SOLICITADO";
        this.fechaSolicitud = LocalDateTime.now();
        this.entidadEmisora = "SANIPES";
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Lot getLote() { return lote; }
    public void setLote(Lot lote) { this.lote = lote; }

    public String getNumeroCertificado() { return numeroCertificado; }
    public void setNumeroCertificado(String numeroCertificado) { this.numeroCertificado = numeroCertificado; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getEntidadEmisora() { return entidadEmisora; }
    public void setEntidadEmisora(String entidadEmisora) { this.entidadEmisora = entidadEmisora; }

    public LocalDateTime getFechaSolicitud() { return fechaSolicitud; }
    public void setFechaSolicitud(LocalDateTime fechaSolicitud) { this.fechaSolicitud = fechaSolicitud; }

    public LocalDateTime getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(LocalDateTime fechaEmision) { this.fechaEmision = fechaEmision; }

    public LocalDateTime getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(LocalDateTime fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public String getPdfUrl() { return pdfUrl; }
    public void setPdfUrl(String pdfUrl) { this.pdfUrl = pdfUrl; }
}
