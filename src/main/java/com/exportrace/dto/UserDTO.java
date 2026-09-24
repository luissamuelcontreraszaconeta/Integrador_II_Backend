package com.exportrace.dto;

import com.exportrace.entity.User;

public class UserDTO {
    private Long id;
    private String nombre;
    private String email;
    private String area;
    private String rol;
    private String estado;

    public UserDTO() {}

    public UserDTO(User user) {
        this.id = user.getId();
        this.nombre = user.getNombre();
        this.email = user.getEmail();
        this.area = user.getArea();
        this.rol = user.getRole() != null ? user.getRole().getNombre() : "";
        this.estado = user.getEstado();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
