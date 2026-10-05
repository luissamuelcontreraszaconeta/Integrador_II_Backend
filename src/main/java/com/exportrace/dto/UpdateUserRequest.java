package com.exportrace.dto;

public class UpdateUserRequest {
    private String nombre;
    private String apellido;
    private String area;
    private String rol;
    private Boolean activo;

    public UpdateUserRequest() {}

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}
