package com.exportrace.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "documents", uniqueConstraints = {
        @UniqueConstraint(name = "uk_lot_doc_type_version", columnNames = {"lote_id", "tipo", "version"})
})
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String tipo; // DECLARACION_JURADA, CERTIFICADO_ORIGEN, REGISTRO_SANITARIO, INFORME_ENSAYO, etc.

    @Column(nullable = false)
    private String url;

    @Column(name = "fecha_subida", nullable = false)
    private LocalDateTime fechaSubida;

    @Column(name = "subido_por")
    private String subidoPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_id", nullable = false)
    private Lot lote;

    @Column(name = "version", nullable = false)
    private Integer version = 1;

    @Column(name = "sha256", length = 64)
    private String sha256;

    @Column(name = "mime_type")
    private String mimeType = "application/pdf";

    @Column(name = "file_size")
    private Long fileSize = 0L;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "file_path")
    private String filePath;

    public Document() {
        this.fechaSubida = LocalDateTime.now();
        this.version = 1;
        this.active = true;
        this.mimeType = "application/pdf";
    }

    public Document(String nombre, String tipo, String url, String subidoPor, Lot lote) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.url = url;
        this.subidoPor = subidoPor;
        this.lote = lote;
        this.fechaSubida = LocalDateTime.now();
        this.version = 1;
        this.active = true;
        this.mimeType = "application/pdf";
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

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public String getSha256() { return sha256; }
    public void setSha256(String sha256) { this.sha256 = sha256; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
}
