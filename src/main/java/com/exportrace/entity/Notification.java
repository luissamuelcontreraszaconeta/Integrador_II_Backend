package com.exportrace.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String mensaje;

    @Column(nullable = false)
    private String tipo; // INFO, WARNING, ALERT, SUCCESS

    @Column(name = "rol_destino", nullable = false)
    private String rolDestino; // QA, LOGISTICA, GERENCIA, PRODUCCION, ALL

    @Column(nullable = false)
    private Boolean leido;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    private String link;

    public Notification() {
        this.leido = false;
        this.fechaCreacion = LocalDateTime.now();
        this.tipo = "INFO";
        this.rolDestino = "ALL";
    }

    public Notification(String titulo, String mensaje, String tipo, String rolDestino, String link) {
        this.titulo = titulo;
        this.mensaje = mensaje;
        this.tipo = tipo;
        this.rolDestino = rolDestino;
        this.link = link;
        this.leido = false;
        this.fechaCreacion = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getRolDestino() { return rolDestino; }
    public void setRolDestino(String rolDestino) { this.rolDestino = rolDestino; }

    public Boolean getLeido() { return leido; }
    public void setLeido(Boolean leido) { this.leido = leido; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }
}
