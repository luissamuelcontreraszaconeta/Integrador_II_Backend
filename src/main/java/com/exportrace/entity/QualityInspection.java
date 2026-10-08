package com.exportrace.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "quality_inspections", uniqueConstraints = {
    @UniqueConstraint(name = "uk_lote_numero_inspeccion", columnNames = {"lote_id", "numero_inspeccion"})
})
public class QualityInspection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_id", nullable = false)
    private Lot lote;

    @Column(name = "numero_inspeccion", nullable = false)
    private Integer numeroInspeccion = 1;

    @Column(name = "inspector_nombre")
    private String inspectorNombre;

    @Column(name = "fecha_inspeccion")
    private LocalDateTime fechaInspeccion;

    private String apariencia;
    
    @Column(name = "evaluacion_color")
    private String evaluacionColor;
    
    private String textura;
    private String olor;
    
    @Column(name = "examen_parasitologico")
    private String examenParasitologico;
    
    @Column(name = "resultado_organoleptico")
    private String resultadoOrganoleptico; // CONFORME, OBSERVADO, NO_CONFORME

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "motivo_reinspeccion", columnDefinition = "TEXT")
    private String motivoReinspeccion;

    @Column(name = "evidencia_fotos_url", columnDefinition = "TEXT")
    private String evidenciaFotosUrl;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "creado_por")
    private String creadoPor;

    public QualityInspection() {
        this.numeroInspeccion = 1;
        this.fechaInspeccion = LocalDateTime.now();
        this.fechaCreacion = LocalDateTime.now();
        this.resultadoOrganoleptico = "CONFORME";
        this.apariencia = "EXCELENTE";
        this.evaluacionColor = "CONFORME";
        this.textura = "FIRM";
        this.olor = "CARACTERISTICO";
        this.examenParasitologico = "AUSENCIA";
    }

    public QualityInspection(Lot lote, Integer numeroInspeccion, String resultadoOrganoleptico, String inspectorNombre, String motivoReinspeccion) {
        this();
        this.lote = lote;
        this.numeroInspeccion = numeroInspeccion;
        this.resultadoOrganoleptico = resultadoOrganoleptico;
        this.inspectorNombre = inspectorNombre;
        this.motivoReinspeccion = motivoReinspeccion;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Lot getLote() { return lote; }
    public void setLote(Lot lote) { this.lote = lote; }

    public Integer getNumeroInspeccion() { return numeroInspeccion; }
    public void setNumeroInspeccion(Integer numeroInspeccion) { this.numeroInspeccion = numeroInspeccion; }

    public String getInspectorNombre() { return inspectorNombre; }
    public void setInspectorNombre(String inspectorNombre) { this.inspectorNombre = inspectorNombre; }

    public LocalDateTime getFechaInspeccion() { return fechaInspeccion; }
    public void setFechaInspeccion(LocalDateTime fechaInspeccion) { this.fechaInspeccion = fechaInspeccion; }

    public String getApariencia() { return apariencia; }
    public void setApariencia(String apariencia) { this.apariencia = apariencia; }

    public String getEvaluacionColor() { return evaluacionColor; }
    public void setEvaluacionColor(String evaluacionColor) { this.evaluacionColor = evaluacionColor; }

    public String getTextura() { return textura; }
    public void setTextura(String textura) { this.textura = textura; }

    public String getOlor() { return olor; }
    public void setOlor(String olor) { this.olor = olor; }

    public String getExamenParasitologico() { return examenParasitologico; }
    public void setExamenParasitologico(String examenParasitologico) { this.examenParasitologico = examenParasitologico; }

    public String getResultadoOrganoleptico() { return resultadoOrganoleptico; }
    public void setResultadoOrganoleptico(String resultadoOrganoleptico) { this.resultadoOrganoleptico = resultadoOrganoleptico; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public String getMotivoReinspeccion() { return motivoReinspeccion; }
    public void setMotivoReinspeccion(String motivoReinspeccion) { this.motivoReinspeccion = motivoReinspeccion; }

    public String getEvidenciaFotosUrl() { return evidenciaFotosUrl; }
    public void setEvidenciaFotosUrl(String evidenciaFotosUrl) { this.evidenciaFotosUrl = evidenciaFotosUrl; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public String getCreadoPor() { return creadoPor; }
    public void setCreadoPor(String creadoPor) { this.creadoPor = creadoPor; }
}
