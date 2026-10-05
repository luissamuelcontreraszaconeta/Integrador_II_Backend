package com.exportrace.dto;

import com.exportrace.entity.Permission;

public class PermissionDTO {
    private Long id;
    private String codigo;
    private String nombre;
    private String descripcion;
    private String moduleCodigo;
    private String moduleNombre;
    private Boolean activo;

    public PermissionDTO() {}

    public PermissionDTO(Permission p) {
        this.id = p.getId();
        this.codigo = p.getCodigo();
        this.nombre = p.getNombre();
        this.descripcion = p.getDescripcion();
        this.activo = p.getActivo();
        if (p.getModule() != null) {
            this.moduleCodigo = p.getModule().getCodigo();
            this.moduleNombre = p.getModule().getNombre();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getModuleCodigo() { return moduleCodigo; }
    public void setModuleCodigo(String moduleCodigo) { this.moduleCodigo = moduleCodigo; }

    public String getModuleNombre() { return moduleNombre; }
    public void setModuleNombre(String moduleNombre) { this.moduleNombre = moduleNombre; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}
