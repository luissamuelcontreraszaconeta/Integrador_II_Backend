package com.exportrace.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "dispatches")
public class Dispatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_id", nullable = false, unique = true)
    private Lot lote;

    @Column(name = "guia_remision")
    private String guiaRemision;

    @Column(name = "duas_exportacion")
    private String duasExportacion;

    @Column(name = "puerto_origen")
    private String puertoOrigen = "Puerto del Callao, Perú";

    @Column(name = "puerto_destino")
    private String puertoDestino;

    private String transportista;

    @Column(name = "placa_vehiculo")
    private String placaVehiculo;

    @Column(name = "numero_contenedor")
    private String numeroContenedor;

    @Column(name = "precinto_seguridad")
    private String precintoSeguridad;

    @Column(name = "fecha_despacho")
    private LocalDateTime fechaDespacho;

    @Column(name = "fecha_estimada_llegada")
    private LocalDateTime fechaEstimadaLlegada;

    @Column(nullable = false)
    private String estado; // EN_PREPARACION, DESPACHADO, EN_TRANSITO, ENTREGADO

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    public Dispatch() {
        this.estado = "EN_PREPARACION";
        this.puertoOrigen = "Puerto del Callao, Perú";
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Lot getLote() { return lote; }
    public void setLote(Lot lote) { this.lote = lote; }

    public String getGuiaRemision() { return guiaRemision; }
    public void setGuiaRemision(String guiaRemision) { this.guiaRemision = guiaRemision; }

    public String getDuasExportacion() { return duasExportacion; }
    public void setDuasExportacion(String duasExportacion) { this.duasExportacion = duasExportacion; }

    public String getPuertoOrigen() { return puertoOrigen; }
    public void setPuertoOrigen(String puertoOrigen) { this.puertoOrigen = puertoOrigen; }

    public String getPuertoDestino() { return puertoDestino; }
    public void setPuertoDestino(String puertoDestino) { this.puertoDestino = puertoDestino; }

    public String getTransportista() { return transportista; }
    public void setTransportista(String transportista) { this.transportista = transportista; }

    public String getPlacaVehiculo() { return placaVehiculo; }
    public void setPlacaVehiculo(String placaVehiculo) { this.placaVehiculo = placaVehiculo; }

    public String getNumeroContenedor() { return numeroContenedor; }
    public void setNumeroContenedor(String numeroContenedor) { this.numeroContenedor = numeroContenedor; }

    public String getPrecintoSeguridad() { return precintoSeguridad; }
    public void setPrecintoSeguridad(String precintoSeguridad) { this.precintoSeguridad = precintoSeguridad; }

    public LocalDateTime getFechaDespacho() { return fechaDespacho; }
    public void setFechaDespacho(LocalDateTime fechaDespacho) { this.fechaDespacho = fechaDespacho; }

    public LocalDateTime getFechaEstimadaLlegada() { return fechaEstimadaLlegada; }
    public void setFechaEstimadaLlegada(LocalDateTime fechaEstimadaLlegada) { this.fechaEstimadaLlegada = fechaEstimadaLlegada; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
}
