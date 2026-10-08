package com.exportrace.dto;

import com.exportrace.entity.Product;

public class ThermalProfileDTO {
    private String conservationType; // CONGELADO, REFRIGERADO
    private Double optimalMin;
    private Double optimalMax;
    private Double warningMax;
    private Double criticalMax;
    private String normativeReference;

    public ThermalProfileDTO() {}

    public ThermalProfileDTO(Product product) {
        if (product != null) {
            this.conservationType = product.getTipoConservacion() != null ? product.getTipoConservacion() : "CONGELADO";
            this.optimalMin = product.getTempOptimaMin() != null ? product.getTempOptimaMin() : ("REFRIGERADO".equalsIgnoreCase(this.conservationType) ? 0.0 : -25.0);
            this.optimalMax = product.getTempOptimaMax() != null ? product.getTempOptimaMax() : ("REFRIGERADO".equalsIgnoreCase(this.conservationType) ? 4.0 : -18.0);
            this.warningMax = product.getTempWarningMax() != null ? product.getTempWarningMax() : ("REFRIGERADO".equalsIgnoreCase(this.conservationType) ? 4.0 : -15.0);
            this.criticalMax = product.getTempCriticaMax() != null ? product.getTempCriticaMax() : ("REFRIGERADO".equalsIgnoreCase(this.conservationType) ? 4.0 : -15.0);
            this.normativeReference = "CONGELADO".equalsIgnoreCase(this.conservationType) 
                    ? "Norma SANIPES / Codex Alimentarius (≤ -18.0°C)" 
                    : "Norma Sanitaria Frescos/Refrigerados (≤ 4.0°C, óptimo ~0°C a 2°C)";
        } else {
            this.conservationType = "CONGELADO";
            this.optimalMin = -25.0;
            this.optimalMax = -18.0;
            this.warningMax = -15.0;
            this.criticalMax = -15.0;
            this.normativeReference = "Norma SANIPES (≤ -18.0°C)";
        }
    }

    // Getters and Setters
    public String getConservationType() { return conservationType; }
    public void setConservationType(String conservationType) { this.conservationType = conservationType; }

    public Double getOptimalMin() { return optimalMin; }
    public void setOptimalMin(Double optimalMin) { this.optimalMin = optimalMin; }

    public Double getOptimalMax() { return optimalMax; }
    public void setOptimalMax(Double optimalMax) { this.optimalMax = optimalMax; }

    public Double getWarningMax() { return warningMax; }
    public void setWarningMax(Double warningMax) { this.warningMax = warningMax; }

    public Double getCriticalMax() { return criticalMax; }
    public void setCriticalMax(Double criticalMax) { this.criticalMax = criticalMax; }

    public String getNormativeReference() { return normativeReference; }
    public void setNormativeReference(String normativeReference) { this.normativeReference = normativeReference; }
}
