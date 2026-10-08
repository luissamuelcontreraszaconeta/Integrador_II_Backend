package com.exportrace.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cold_chain_incidents")
public class ColdChainIncident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "lote_id", nullable = false)
    private Lot lote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "record_id")
    private ColdChainRecord record;

    @Column(name = "temperatura_leida", nullable = false)
    private Double temperaturaLeida;

    @Column(name = "temperatura_limite", nullable = false)
    private Double temperaturaLimite;

    @Column(name = "tipo_conservacion", nullable = false)
    private String tipoConservacion; // CONGELADO, REFRIGERADO

    @Column(nullable = false)
    private String estado = "ACTIVE"; // ACTIVE, UNDER_REVIEW, RESOLVED

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_revision")
    private LocalDateTime fechaRevision;

    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;

    @Column(name = "revisado_por")
    private String revisadoPor;

    @Column(name = "resuelto_por")
    private String resueltoPor;

    @Column(name = "justificacion_tecnica", columnDefinition = "TEXT")
    private String justificacionTecnica;

    @Column(name = "acciones_tomadas", columnDefinition = "TEXT")
    private String accionesTomadas;

    @Column(name = "evidencia_url")
    private String evidenciaUrl;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    public ColdChainIncident() {
        this.fechaCreacion = LocalDateTime.now();
        this.estado = "ACTIVE";
    }

    public ColdChainIncident(Lot lote, ColdChainRecord record, Double temperaturaLeida, Double temperaturaLimite, String tipoConservacion) {
        this.lote = lote;
        this.record = record;
        this.temperaturaLeida = temperaturaLeida;
        this.temperaturaLimite = temperaturaLimite;
        this.tipoConservacion = tipoConservacion;
        this.estado = "ACTIVE";
        this.fechaCreacion = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Lot getLote() { return lote; }
    public void setLote(Lot lote) { this.lote = lote; }

    public ColdChainRecord getRecord() { return record; }
    public void setRecord(ColdChainRecord record) { this.record = record; }

    public Double getTemperaturaLeida() { return temperaturaLeida; }
    public void setTemperaturaLeida(Double temperaturaLeida) { this.temperaturaLeida = temperaturaLeida; }

    public Double getTemperaturaLimite() { return temperaturaLimite; }
    public void setTemperaturaLimite(Double temperaturaLimite) { this.temperaturaLimite = temperaturaLimite; }

    public String getTipoConservacion() { return tipoConservacion; }
    public void setTipoConservacion(String tipoConservacion) { this.tipoConservacion = tipoConservacion; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public LocalDateTime getFechaRevision() { return fechaRevision; }
    public void setFechaRevision(LocalDateTime fechaRevision) { this.fechaRevision = fechaRevision; }

    public LocalDateTime getFechaResolucion() { return fechaResolucion; }
    public void setFechaResolucion(LocalDateTime fechaResolucion) { this.fechaResolucion = fechaResolucion; }

    public String getRevisadoPor() { return revisadoPor; }
    public void setRevisadoPor(String revisadoPor) { this.revisadoPor = revisadoPor; }

    public String getResueltoPor() { return resueltoPor; }
    public void setResueltoPor(String resueltoPor) { this.resueltoPor = resueltoPor; }

    public String getJustificacionTecnica() { return justificacionTecnica; }
    public void setJustificacionTecnica(String justificacionTecnica) { this.justificacionTecnica = justificacionTecnica; }

    public String getAccionesTomadas() { return accionesTomadas; }
    public void setAccionesTomadas(String accionesTomadas) { this.accionesTomadas = accionesTomadas; }

    public String getEvidenciaUrl() { return evidenciaUrl; }
    public void setEvidenciaUrl(String evidenciaUrl) { this.evidenciaUrl = evidenciaUrl; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
}
