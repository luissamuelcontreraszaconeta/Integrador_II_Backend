package com.exportrace.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "lots")
public class Lot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String codigo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "producto_id")
    private Product producto;

    private Integer cantidadEmpaques;
    private String tipoEmpaque;
    private Double pesoNetoKg;
    private String plantaProcesamiento;
    private String lineaProcesamiento;
    private String proveedor;
    private String embarcacion;

    private LocalDate fechaProduccion;
    private LocalDate fechaVencimiento;
    private String inspeccionadoPor;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Column(nullable = false)
    private String estado; // DRAFT, PENDING_QA, IN_QA, OBSERVED, VALIDATION_PENDING, READY_FOR_CERTIFICATION, IN_CERTIFICATION, CERTIFIED, READY_FOR_DISPATCH, DISPATCHED

    @Column(nullable = false, unique = true)
    private String qrToken;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion = LocalDateTime.now();

    public Lot() {
        this.fechaCreacion = LocalDateTime.now();
        this.fechaActualizacion = LocalDateTime.now();
        this.estado = "DRAFT";
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public Product getProducto() { return producto; }
    public void setProducto(Product producto) { this.producto = producto; }

    public Integer getCantidadEmpaques() { return cantidadEmpaques; }
    public void setCantidadEmpaques(Integer cantidadEmpaques) { this.cantidadEmpaques = cantidadEmpaques; }

    public String getTipoEmpaque() { return tipoEmpaque; }
    public void setTipoEmpaque(String tipoEmpaque) { this.tipoEmpaque = tipoEmpaque; }

    public Double getPesoNetoKg() { return pesoNetoKg; }
    public void setPesoNetoKg(Double pesoNetoKg) { this.pesoNetoKg = pesoNetoKg; }

    public String getPlantaProcesamiento() { return plantaProcesamiento; }
    public void setPlantaProcesamiento(String plantaProcesamiento) { this.plantaProcesamiento = plantaProcesamiento; }

    public String getLineaProcesamiento() { return lineaProcesamiento; }
    public void setLineaProcesamiento(String lineaProcesamiento) { this.lineaProcesamiento = lineaProcesamiento; }

    public String getProveedor() { return proveedor; }
    public void setProveedor(String proveedor) { this.proveedor = proveedor; }

    public String getEmbarcacion() { return embarcacion; }
    public void setEmbarcacion(String embarcacion) { this.embarcacion = embarcacion; }

    public LocalDate getFechaProduccion() { return fechaProduccion; }
    public void setFechaProduccion(LocalDate fechaProduccion) { this.fechaProduccion = fechaProduccion; }

    public LocalDate getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(LocalDate fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; }

    public String getInspeccionadoPor() { return inspeccionadoPor; }
    public void setInspeccionadoPor(String inspeccionadoPor) { this.inspeccionadoPor = inspeccionadoPor; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getQrToken() { return qrToken; }
    public void setQrToken(String qrToken) { this.qrToken = qrToken; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }
}
