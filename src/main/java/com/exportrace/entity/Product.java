package com.exportrace.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String codigo;

    @Column(nullable = false)
    private String nombre;

    private String especie;

    private String nombreCientifico;

    private String tipoProcesamiento;

    private String unidadMedida; // TN, KG, CAJAS, PALLETS

    private String descripcion;

    private Boolean activo = true;

    // Thermal Profile Parameters (RF-28, BR-P0-002)
    @Column(name = "tipo_conservacion")
    private String tipoConservacion = "CONGELADO"; // CONGELADO, REFRIGERADO

    @Column(name = "temp_optima_min")
    private Double tempOptimaMin = -25.0;

    @Column(name = "temp_optima_max")
    private Double tempOptimaMax = -18.0;

    @Column(name = "temp_warning_max")
    private Double tempWarningMax = -15.0;

    @Column(name = "temp_critica_max")
    private Double tempCriticaMax = -15.0;

    public Product() {}

    public Product(String codigo, String nombre, String nombreCientifico, String descripcion) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.especie = nombre;
        this.nombreCientifico = nombreCientifico;
        this.descripcion = descripcion;
        this.tipoProcesamiento = "Bloque Congelado Rápido (-40°C)";
        this.unidadMedida = "TN";
        this.activo = true;
        this.tipoConservacion = "CONGELADO";
        this.tempOptimaMin = -25.0;
        this.tempOptimaMax = -18.0;
        this.tempWarningMax = -15.0;
        this.tempCriticaMax = -15.0;
    }

    public Product(String nombre, String especie, String nombreCientifico, String tipoProcesamiento, String unidadMedida) {
        this.codigo = nombre;
        this.nombre = nombre;
        this.especie = especie;
        this.nombreCientifico = nombreCientifico;
        this.tipoProcesamiento = tipoProcesamiento;
        this.unidadMedida = unidadMedida;
        this.activo = true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getNombreComercial() { return nombre; }
    public void setNombreComercial(String nombreComercial) { this.nombre = nombreComercial; }

    public String getEspecie() { return especie; }
    public void setEspecie(String especie) { this.especie = especie; }

    public String getNombreCientifico() { return nombreCientifico; }
    public void setNombreCientifico(String nombreCientifico) { this.nombreCientifico = nombreCientifico; }

    public String getTipoProcesamiento() { return tipoProcesamiento; }
    public void setTipoProcesamiento(String tipoProcesamiento) { this.tipoProcesamiento = tipoProcesamiento; }

    public String getUnidadMedida() { return unidadMedida; }
    public void setUnidadMedida(String unidadMedida) { this.unidadMedida = unidadMedida; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public String getTipoConservacion() { return tipoConservacion; }
    public void setTipoConservacion(String tipoConservacion) { this.tipoConservacion = tipoConservacion; }

    public Double getTempOptimaMin() { return tempOptimaMin; }
    public void setTempOptimaMin(Double tempOptimaMin) { this.tempOptimaMin = tempOptimaMin; }

    public Double getTempOptimaMax() { return tempOptimaMax; }
    public void setTempOptimaMax(Double tempOptimaMax) { this.tempOptimaMax = tempOptimaMax; }

    public Double getTempWarningMax() { return tempWarningMax; }
    public void setTempWarningMax(Double tempWarningMax) { this.tempWarningMax = tempWarningMax; }

    public Double getTempCriticaMax() { return tempCriticaMax; }
    public void setTempCriticaMax(Double tempCriticaMax) { this.tempCriticaMax = tempCriticaMax; }

    public boolean isCongelado() {
        return "CONGELADO".equalsIgnoreCase(this.tipoConservacion);
    }

    public boolean isRefrigerado() {
        return "REFRIGERADO".equalsIgnoreCase(this.tipoConservacion);
    }
}
