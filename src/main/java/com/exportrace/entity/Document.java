package com.exportrace.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String tipo; // qa_lab_report, sanitary_cert, customs_doc, invoice, etc.

    @Column(nullable = false)
    private String url;

    @Column(name = "fecha_subida", nullable = false)
    private LocalDateTime fechaSubida;

    @Column(name = "subido_por")
    private String subidoPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_id", nullable = false)
    private Lot lote;

    public Document() {
        this.fechaSubida = LocalDateTime.now();
    }

    public Document(String nombre, String tipo, String url, String subidoPor, Lot lote) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.url = url;
        this.subidoPor = subidoPor;
        this.lote = lote;
        this.fechaSubida = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public LocalDateTime getFechaSubida() { return fechaSubida; }
    public void setFechaSubida(LocalDateTime fechaSubida) { this.fechaSubida = fechaSubida; }

    public String getSubidoPor() { return subidoPor; }
    public void setSubidoPor(String subidoPor) { this.subidoPor = subidoPor; }

    public Lot getLote() { return lote; }
    public void setLote(Lot lote) { this.lote = lote; }
}
