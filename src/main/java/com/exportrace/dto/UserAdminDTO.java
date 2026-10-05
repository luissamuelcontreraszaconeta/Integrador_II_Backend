package com.exportrace.dto;

import com.exportrace.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class UserAdminDTO {
    private Long id;
    private String nombre;
    private String apellido;
    private String email;
    private String area;
    private String rol;
    private Boolean activo;
    private String estado;
    private LocalDateTime fechaCreacion;
    private LocalDateTime ultimoAcceso;
    private List<String> permissions;

    public UserAdminDTO() {}

    public UserAdminDTO(User user) {
        this.id = user.getId();
        this.nombre = user.getNombre();
        this.apellido = user.getApellido();
        this.email = user.getEmail();
        this.area = user.getArea();
        this.activo = user.getActivo();
        this.estado = user.getEstado();
        this.fechaCreacion = user.getFechaCreacion();
        this.ultimoAcceso = user.getUltimoAcceso();
        if (user.getRole() != null) {
            this.rol = user.getRole().getNombre();
            if (user.getRole().getPermissions() != null) {
                this.permissions = user.getRole().getPermissions().stream()
                        .map(p -> p.getCodigo())
                        .collect(Collectors.toList());
            }
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public LocalDateTime getUltimoAcceso() { return ultimoAcceso; }
    public void setUltimoAcceso(LocalDateTime ultimoAcceso) { this.ultimoAcceso = ultimoAcceso; }

    public List<String> getPermissions() { return permissions; }
    public void setPermissions(List<String> permissions) { this.permissions = permissions; }
}
