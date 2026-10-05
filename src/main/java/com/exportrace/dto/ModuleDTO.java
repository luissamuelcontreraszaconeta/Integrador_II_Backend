package com.exportrace.dto;

import com.exportrace.entity.Module;
import java.util.List;

public class ModuleDTO {
    private Long id;
    private String codigo;
    private String nombre;
    private String descripcion;
    private String ruta;
    private String icono;
    private Boolean activo;
    private Integer orden;
    private List<String> permissions;

    public ModuleDTO() {}

    public ModuleDTO(Module m) {
        this.id = m.getId();
        this.codigo = m.getCodigo();
        this.nombre = m.getNombre();
        this.descripcion = m.getDescripcion();
        this.ruta = m.getRuta();
        this.icono = m.getIcono();
        this.activo = m.getActivo();
        this.orden = m.getOrden();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getRuta() { return ruta; }
    public void setRuta(String ruta) { this.ruta = ruta; }

    public String getIcono() { return icono; }
    public void setIcono(String icono) { this.icono = icono; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public Integer getOrden() { return orden; }
    public void setOrden(Integer orden) { this.orden = orden; }

    public List<String> getPermissions() { return permissions; }
    public void setPermissions(List<String> permissions) { this.permissions = permissions; }
}
