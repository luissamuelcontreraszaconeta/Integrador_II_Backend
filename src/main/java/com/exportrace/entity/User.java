package com.exportrace.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    private String apellido;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    private String area;

    private Boolean activo = true;

    private LocalDateTime fechaCreacion = LocalDateTime.now();

    private LocalDateTime ultimoAcceso;

    public User() {}

    public User(String nombre, String apellido, String email, String passwordHash, Role role, String area) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.area = area;
        this.activo = true;
        this.fechaCreacion = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getContrasena() { return passwordHash; }
    public void setContrasena(String contrasena) { this.passwordHash = contrasena; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public String getEstado() { return Boolean.TRUE.equals(activo) ? "ACTIVO" : "INACTIVO"; }
    public void setEstado(String estado) { this.activo = "ACTIVO".equalsIgnoreCase(estado); }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public LocalDateTime getUltimoAcceso() { return ultimoAcceso; }
    public void setUltimoAcceso(LocalDateTime ultimoAcceso) { this.ultimoAcceso = ultimoAcceso; }
}
