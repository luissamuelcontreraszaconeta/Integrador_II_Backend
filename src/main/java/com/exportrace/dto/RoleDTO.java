package com.exportrace.dto;

import com.exportrace.entity.Role;
import java.util.List;
import java.util.stream.Collectors;

public class RoleDTO {
    private Long id;
    private String nombre;
    private String descripcion;
    private Boolean activo;
    private Long userCount = 0L;
    private List<String> permissions;

    public RoleDTO() {}

    public RoleDTO(Role r) {
        this.id = r.getId();
        this.nombre = r.getNombre();
        this.descripcion = r.getDescripcion();
        this.activo = r.getActivo();
        if (r.getPermissions() != null) {
            this.permissions = r.getPermissions().stream()
                    .map(p -> p.getCodigo())
                    .collect(Collectors.toList());
        }
    }

    public RoleDTO(Role r, Long userCount) {
        this(r);
        this.userCount = userCount;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public Long getUserCount() { return userCount; }
    public void setUserCount(Long userCount) { this.userCount = userCount; }

    public List<String> getPermissions() { return permissions; }
    public void setPermissions(List<String> permissions) { this.permissions = permissions; }
}
