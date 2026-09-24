package com.exportrace.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "lot_histories")
public class LotHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_id", nullable = false)
    private Lot lote;

    @Column(name = "estado_anterior")
    private String estadoAnterior;

    @Column(name = "estado_nuevo", nullable = false)
    private String estadoNuevo;

    @Column(name = "usuario_nombre")
    private String usuarioNombre;

    @Column(name = "usuario_rol")
    private String usuarioRol;

    @Column(columnDefinition = "TEXT")
    private String comentario;

    @Column(name = "fecha_cambio", nullable = false)
    private LocalDateTime fechaCambio;

    public LotHistory() {
        this.fechaCambio = LocalDateTime.now();
    }

    public LotHistory(Lot lote, String estadoAnterior, String estadoNuevo, String usuarioNombre, String usuarioRol, String comentario) {
        this.lote = lote;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.usuarioNombre = usuarioNombre;
        this.usuarioRol = usuarioRol;
        this.comentario = comentario;
        this.fechaCambio = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Lot getLote() { return lote; }
    public void setLote(Lot lote) { this.lote = lote; }

    public String getEstadoAnterior() { return estadoAnterior; }
    public void setEstadoAnterior(String estadoAnterior) { this.estadoAnterior = estadoAnterior; }

    public String getEstadoNuevo() { return estadoNuevo; }
    public void setEstadoNuevo(String estadoNuevo) { this.estadoNuevo = estadoNuevo; }

    public String getUsuarioNombre() { return usuarioNombre; }
    public void setUsuarioNombre(String usuarioNombre) { this.usuarioNombre = usuarioNombre; }

    public String getUsuarioRol() { return usuarioRol; }
    public void setUsuarioRol(String usuarioRol) { this.usuarioRol = usuarioRol; }

    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }

    public LocalDateTime getFechaCambio() { return fechaCambio; }
    public void setFechaCambio(LocalDateTime fechaCambio) { this.fechaCambio = fechaCambio; }
}
