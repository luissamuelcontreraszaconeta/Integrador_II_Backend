package com.exportrace.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cold_chain_records")
public class ColdChainRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_id", nullable = false)
    private Lot lote;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "temperatura_celsius", nullable = false)
    private Double temperaturaCelsius;

    @Column(name = "ubicacion_camara")
    private String ubicacionCamara;

    @Column(name = "responsable_nombre")
    private String responsableNombre;

    @Column(name = "estado_medicion", nullable = false)
    private String estadoMedicion; // NORMAL, WARNING, CRITICAL

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    public ColdChainRecord() {
        this.fechaHora = LocalDateTime.now();
        this.estadoMedicion = "NORMAL";
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Lot getLote() { return lote; }
    public void setLote(Lot lote) { this.lote = lote; }

    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }

    public Double getTemperaturaCelsius() { return temperaturaCelsius; }
    public void setTemperaturaCelsius(Double temperaturaCelsius) { this.temperaturaCelsius = temperaturaCelsius; }

    public String getUbicacionCamara() { return ubicacionCamara; }
    public void setUbicacionCamara(String ubicacionCamara) { this.ubicacionCamara = ubicacionCamara; }

    public String getResponsableNombre() { return responsableNombre; }
    public void setResponsableNombre(String responsableNombre) { this.responsableNombre = responsableNombre; }

    public String getEstadoMedicion() { return estadoMedicion; }
    public void setEstadoMedicion(String estadoMedicion) { this.estadoMedicion = estadoMedicion; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
}
