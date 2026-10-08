package com.exportrace.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class UpdateLotRequest {

    @NotNull(message = "La versión de control de concurrencia es obligatoria.")
    private Long version;

    @Positive(message = "La cantidad de empaques debe ser mayor que cero.")
    private Integer cantidadEmpaques;

    private String tipoEmpaque;

    @Positive(message = "El peso neto debe ser mayor que cero.")
    private Double pesoNetoKg;

    private String plantaProcesamiento;
    private String lineaProcesamiento;
    private String proveedor;
    private String embarcacion;
    private String fechaProduccion;
    private String fechaVencimiento;
    private String observaciones;
    private String inspeccionadoPor;

    public UpdateLotRequest() {}

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

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

    public String getFechaProduccion() { return fechaProduccion; }
    public void setFechaProduccion(String fechaProduccion) { this.fechaProduccion = fechaProduccion; }

    public String getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(String fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public String getInspeccionadoPor() { return inspeccionadoPor; }
    public void setInspeccionadoPor(String inspeccionadoPor) { this.inspeccionadoPor = inspeccionadoPor; }
}
