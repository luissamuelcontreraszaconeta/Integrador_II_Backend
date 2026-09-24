package com.exportrace.dto;

import com.exportrace.entity.Lot;

public class ProductionInfoDTO {
    private String productName;
    private String productType;
    private String scientificName;
    private Double quantity;
    private String unit;
    private String registrationDate;
    private String supplier;
    private String vesselName;
    private String receptionDate;
    private String portOfOrigin;
    private String processingType;
    private String productionLine;
    private String shiftSupervisor;
    private String notes;

    public ProductionInfoDTO() {}

    public ProductionInfoDTO(Lot lot) {
        if (lot.getProducto() != null) {
            this.productName = lot.getProducto().getNombreComercial();
            this.productType = lot.getProducto().getCodigo();
            this.scientificName = lot.getProducto().getNombreCientifico();
        }
        this.quantity = lot.getCantidadEmpaques() != null ? lot.getCantidadEmpaques().doubleValue() : (lot.getPesoNetoKg() != null ? lot.getPesoNetoKg() / 1000.0 : 0.0);
        this.unit = lot.getTipoEmpaque() != null ? lot.getTipoEmpaque() : "TN";
        this.registrationDate = lot.getFechaProduccion() != null ? lot.getFechaProduccion().toString() : "";
        this.supplier = lot.getProveedor() != null ? lot.getProveedor() : "Asociación Pesquera Artesanal Paita Norte";
        this.vesselName = lot.getEmbarcacion() != null ? lot.getEmbarcacion() : "E/P Don Luis II (CO-18492-PM)";
        this.receptionDate = lot.getFechaCreacion() != null ? lot.getFechaCreacion().toLocalDate().toString() : "";
        this.portOfOrigin = lot.getPlantaProcesamiento() != null ? lot.getPlantaProcesamiento() : "Puerto de Paita, Piura";
        this.processingType = "Bloque Congelado Rápido (-40°C)";
        this.productionLine = lot.getLineaProcesamiento() != null ? lot.getLineaProcesamiento() : "Línea 01";
        this.shiftSupervisor = lot.getInspeccionadoPor() != null ? lot.getInspeccionadoPor() : "Supervisor de Planta";
        this.notes = lot.getObservaciones();
    }

    // Getters and Setters
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }

    public String getScientificName() { return scientificName; }
    public void setScientificName(String scientificName) { this.scientificName = scientificName; }

    public Double getQuantity() { return quantity; }
    public void setQuantity(Double quantity) { this.quantity = quantity; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getRegistrationDate() { return registrationDate; }
    public void setRegistrationDate(String registrationDate) { this.registrationDate = registrationDate; }

    public String getSupplier() { return supplier; }
    public void setSupplier(String supplier) { this.supplier = supplier; }

    public String getVesselName() { return vesselName; }
    public void setVesselName(String vesselName) { this.vesselName = vesselName; }

    public String getReceptionDate() { return receptionDate; }
    public void setReceptionDate(String receptionDate) { this.receptionDate = receptionDate; }

    public String getPortOfOrigin() { return portOfOrigin; }
    public void setPortOfOrigin(String portOfOrigin) { this.portOfOrigin = portOfOrigin; }

    public String getProcessingType() { return processingType; }
    public void setProcessingType(String processingType) { this.processingType = processingType; }

    public String getProductionLine() { return productionLine; }
    public void setProductionLine(String productionLine) { this.productionLine = productionLine; }

    public String getShiftSupervisor() { return shiftSupervisor; }
    public void setShiftSupervisor(String shiftSupervisor) { this.shiftSupervisor = shiftSupervisor; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
