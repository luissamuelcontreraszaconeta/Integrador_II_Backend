package com.exportrace.dto;

public class UpdateSessionPolicyRequest {
    private Integer idleTimeoutMinutes;
    private Integer absoluteTimeoutMinutes;
    private Integer warningBeforeMinutes;
    private Boolean enabled;

    public UpdateSessionPolicyRequest() {}

    public void validate() {
        if (idleTimeoutMinutes == null || idleTimeoutMinutes < 5 || idleTimeoutMinutes > 480) {
            throw new IllegalArgumentException("El tiempo de inactividad (idleTimeoutMinutes) debe estar entre 5 y 480 minutos.");
        }
        if (absoluteTimeoutMinutes == null || absoluteTimeoutMinutes < 30 || absoluteTimeoutMinutes > 1440) {
            throw new IllegalArgumentException("La duración absoluta (absoluteTimeoutMinutes) debe estar entre 30 y 1440 minutos (24 horas).");
        }
        if (idleTimeoutMinutes > absoluteTimeoutMinutes) {
            throw new IllegalArgumentException("El tiempo de inactividad no puede ser mayor que la duración absoluta de la sesión.");
        }
        if (warningBeforeMinutes == null || warningBeforeMinutes < 1) {
            throw new IllegalArgumentException("El tiempo de aviso previo (warningBeforeMinutes) debe ser de al menos 1 minuto.");
        }
        if (warningBeforeMinutes >= idleTimeoutMinutes) {
            throw new IllegalArgumentException("El tiempo de aviso (" + warningBeforeMinutes + "m) debe ser menor que el tiempo de inactividad (" + idleTimeoutMinutes + "m).");
        }
    }

    public Integer getIdleTimeoutMinutes() { return idleTimeoutMinutes; }
    public void setIdleTimeoutMinutes(Integer idleTimeoutMinutes) { this.idleTimeoutMinutes = idleTimeoutMinutes; }

    public Integer getAbsoluteTimeoutMinutes() { return absoluteTimeoutMinutes; }
    public void setAbsoluteTimeoutMinutes(Integer absoluteTimeoutMinutes) { this.absoluteTimeoutMinutes = absoluteTimeoutMinutes; }

    public Integer getWarningBeforeMinutes() { return warningBeforeMinutes; }
    public void setWarningBeforeMinutes(Integer warningBeforeMinutes) { this.warningBeforeMinutes = warningBeforeMinutes; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}
