package com.exportrace.dto;

public class CreateLotRequest {
    private String especie;
    private String producto;
    private Integer cantidadEmpaques;
    private String tipoEmpaque;
    private Double pesoNetoKg;
    private String plantaProcesamiento;
    private String lineaProcesamiento;
    private String fechaProduccion;
    private String fechaVencimiento;
    private Double temperaturaInicial;
    private String observaciones;
    private String inspeccionadoPor;

    public CreateLotRequest() {}

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
}
