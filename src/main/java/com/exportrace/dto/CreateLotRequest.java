package com.exportrace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class CreateLotRequest {

    private String codigo;

    private String especie;

    @NotBlank(message = "El producto es obligatorio.")
    private String producto;

    @NotNull(message = "La cantidad de empaques es obligatoria.")
    @Positive(message = "La cantidad de empaques debe ser mayor que cero.")
    private Integer cantidadEmpaques;

    @NotBlank(message = "El tipo de empaque es obligatorio.")
    private String tipoEmpaque;

    @NotNull(message = "El peso neto es obligatorio.")
    @Positive(message = "El peso neto debe ser mayor que cero.")
    private Double pesoNetoKg;

    @NotBlank(message = "La planta de procesamiento es obligatoria.")
    private String plantaProcesamiento;

    private String lineaProcesamiento;

    @NotBlank(message = "La fecha de producción es obligatoria.")
    private String fechaProduccion;

    private String fechaVencimiento;

    private Double temperaturaInicial;

    private String observaciones;

    private String inspeccionadoPor;

    private String proveedor;

    private String embarcacion;

    public CreateLotRequest() {}

    public CreateLotRequest(String producto, Integer cantidadEmpaques, String tipoEmpaque, Double pesoNetoKg, String plantaProcesamiento, String fechaProduccion) {
        this.producto = producto;
        this.cantidadEmpaques = cantidadEmpaques;
        this.tipoEmpaque = tipoEmpaque;
        this.pesoNetoKg = pesoNetoKg;
        this.plantaProcesamiento = plantaProcesamiento;
        this.fechaProduccion = fechaProduccion;
    }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getEspecie() { return especie; }
    public void setEspecie(String especie) { this.especie = especie; }

    public String getProducto() { return producto; }
    public void setProducto(String producto) { this.producto = producto; }

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

    public String getFechaProduccion() { return fechaProduccion; }
    public void setFechaProduccion(String fechaProduccion) { this.fechaProduccion = fechaProduccion; }

    public String getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(String fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; }

    public Double getTemperaturaInicial() { return temperaturaInicial; }
    public void setTemperaturaInicial(Double temperaturaInicial) { this.temperaturaInicial = temperaturaInicial; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public String getInspeccionadoPor() { return inspeccionadoPor; }
    public void setInspeccionadoPor(String inspeccionadoPor) { this.inspeccionadoPor = inspeccionadoPor; }

    public String getProveedor() { return proveedor; }
    public void setProveedor(String proveedor) { this.proveedor = proveedor; }

    public String getEmbarcacion() { return embarcacion; }
    public void setEmbarcacion(String embarcacion) { this.embarcacion = embarcacion; }
}
