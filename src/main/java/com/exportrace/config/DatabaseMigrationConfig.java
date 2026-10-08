package com.exportrace.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import jakarta.annotation.PostConstruct;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class DatabaseMigrationConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMigrationConfig.class);
    private final DataSource dataSource;

    public DatabaseMigrationConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void migrateSchemaIfNeeded() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            migrateNotificationsTable(stmt);
            migrateQaEvidencesTable(stmt);
            migrateLotsTable(stmt);
            migrateQualityInspectionsTable(stmt);
            migrateDocumentsTable(stmt);

        } catch (Exception e) {
            log.error("[DatabaseMigration] Error during schema migration check: {}", e.getMessage(), e);
        }
    }

    private void migrateQualityInspectionsTable(Statement stmt) throws Exception {
        ResultSet rsTable = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='quality_inspections'");
        if (!rsTable.next()) {
            return;
        }
        rsTable.close();

        ResultSet rsCols = stmt.executeQuery("PRAGMA table_info(quality_inspections)");
        List<String> columns = new ArrayList<>();
        while (rsCols.next()) {
            columns.add(rsCols.getString("name").toLowerCase());
        }
        rsCols.close();

        boolean needsMigration = !columns.contains("numero_inspeccion") ||
                                 !columns.contains("motivo_reinspeccion") ||
                                 !columns.contains("fecha_creacion") ||
                                 !columns.contains("creado_por");

        if (needsMigration) {
            log.info("[DatabaseMigration] Migrating 'quality_inspections' table to 1:N sequence schema...");

            stmt.execute("BEGIN TRANSACTION;");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS quality_inspections_canonical (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    lote_id BIGINT NOT NULL,
                    numero_inspeccion INTEGER NOT NULL DEFAULT 1,
                    inspector_nombre VARCHAR(255),
                    fecha_inspeccion TIMESTAMP,
                    apariencia VARCHAR(255),
                    evaluacion_color VARCHAR(255),
                    textura VARCHAR(255),
                    olor VARCHAR(255),
                    examen_parasitologico VARCHAR(255),
                    resultado_organoleptico VARCHAR(255),
                    observaciones TEXT,
                    motivo_reinspeccion TEXT,
                    evidencia_fotos_url TEXT,
                    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    creado_por VARCHAR(150),
                    CONSTRAINT uk_lote_numero_inspeccion UNIQUE (lote_id, numero_inspeccion)
                );
            """);

            ResultSet rsCount = stmt.executeQuery("SELECT COUNT(*) FROM quality_inspections");
            int count = 0;
            if (rsCount.next()) {
                count = rsCount.getInt(1);
            }
            rsCount.close();

            if (count > 0) {
                String numCol = columns.contains("numero_inspeccion") ? "numero_inspeccion" : "1";
                String motCol = columns.contains("motivo_reinspeccion") ? "motivo_reinspeccion" : "NULL";
                String fcreaCol = columns.contains("fecha_creacion") ? "fecha_creacion" : "COALESCE(fecha_inspeccion, datetime('now'))";
                String userCol = columns.contains("creado_por") ? "creado_por" : "COALESCE(inspector_nombre, 'qa@exportrace.pe')";

                stmt.execute(String.format("""
                    INSERT INTO quality_inspections_canonical (
                        id, lote_id, numero_inspeccion, inspector_nombre, fecha_inspeccion,
                        apariencia, evaluacion_color, textura, olor, examen_parasitologico,
                        resultado_organoleptico, observaciones, motivo_reinspeccion, evidencia_fotos_url,
                        fecha_creacion, creado_por
                    )
                    SELECT
                        id,
                        lote_id,
                        COALESCE(%s, 1),
                        inspector_nombre,
                        fecha_inspeccion,
                        apariencia,
                        evaluacion_color,
                        textura,
                        olor,
                        examen_parasitologico,
                        resultado_organoleptico,
                        observaciones,
                        %s,
                        evidencia_fotos_url,
                        %s,
                        %s
                    FROM quality_inspections;
                """, numCol, motCol, fcreaCol, userCol));
            }

            stmt.execute("DROP TABLE quality_inspections;");
            stmt.execute("ALTER TABLE quality_inspections_canonical RENAME TO quality_inspections;");
            stmt.execute("COMMIT;");

            log.info("[DatabaseMigration] Successfully migrated 'quality_inspections' to 1:N schema.");
        }
    }

    private void migrateLotsTable(Statement stmt) throws Exception {
        ResultSet rsTable = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='lots'");
        if (!rsTable.next()) {
            return;
        }
        rsTable.close();

        ResultSet rsCols = stmt.executeQuery("PRAGMA table_info(lots)");
        List<String> columns = new ArrayList<>();
        while (rsCols.next()) {
            columns.add(rsCols.getString("name").toLowerCase());
        }
        rsCols.close();

        if (!columns.contains("version")) {
            log.info("[DatabaseMigration] Adding 'version' column for optimistic locking in 'lots' table...");
            stmt.execute("ALTER TABLE lots ADD COLUMN version BIGINT DEFAULT 0;");
            stmt.execute("UPDATE lots SET version = 0 WHERE version IS NULL;");
        }
    }

    private void migrateNotificationsTable(Statement stmt) throws Exception {
        // Check if table notifications exists
        ResultSet rsTable = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='notifications'");
        if (!rsTable.next()) {
            return; // Table will be created by Hibernate
        }
        rsTable.close();

        // Check columns in notifications table
        ResultSet rsCols = stmt.executeQuery("PRAGMA table_info(notifications)");
        List<String> columns = new ArrayList<>();
        while (rsCols.next()) {
            columns.add(rsCols.getString("name").toLowerCase());
        }
        rsCols.close();

        // If legacy column 'fecha_creacion' exists alongside or instead of 'created_at'
        if (columns.contains("fecha_creacion")) {
            log.info("[DatabaseMigration] Legacy columns detected in 'notifications' table. Migrating to canonical schema...");

            stmt.execute("BEGIN TRANSACTION;");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS notifications_canonical (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    created_at TIMESTAMP NOT NULL,
                    entity_id VARCHAR(255),
                    entity_type VARCHAR(255),
                    is_read BOOLEAN NOT NULL DEFAULT 0,
                    message TEXT NOT NULL,
                    module VARCHAR(255) NOT NULL,
                    priority VARCHAR(255) NOT NULL,
                    read_at TIMESTAMP,
                    route VARCHAR(255),
                    target_role VARCHAR(255),
                    title VARCHAR(255) NOT NULL,
                    type VARCHAR(255) NOT NULL,
                    user_id BIGINT
                );
            """);

            ResultSet rsCount = stmt.executeQuery("SELECT COUNT(*) FROM notifications");
            int count = 0;
            if (rsCount.next()) {
                count = rsCount.getInt(1);
            }
            rsCount.close();

            if (count > 0) {
                if (columns.contains("created_at")) {
                    stmt.execute("""
                        INSERT INTO notifications_canonical (
                            id, created_at, entity_id, entity_type, is_read, message, module, priority, read_at, route, target_role, title, type, user_id
                        )
                        SELECT 
                            id,
                            COALESCE(created_at, fecha_creacion, datetime('now')),
                            entity_id,
                            entity_type,
                            COALESCE(is_read, CASE WHEN leido = 1 THEN 1 ELSE 0 END, 0),
                            COALESCE(message, mensaje, ''),
                            COALESCE(module, 'SYSTEM'),
                            COALESCE(priority, 'NORMAL'),
                            read_at,
                            COALESCE(route, link),
                            COALESCE(target_role, rol_destino),
                            COALESCE(title, titulo, ''),
                            COALESCE(type, tipo, 'INFO'),
                            user_id
                        FROM notifications;
                    """);
                } else {
                    stmt.execute("""
                        INSERT INTO notifications_canonical (
                            id, created_at, entity_id, entity_type, is_read, message, module, priority, read_at, route, target_role, title, type, user_id
                        )
                        SELECT 
                            id,
                            COALESCE(fecha_creacion, datetime('now')),
                            NULL,
                            NULL,
                            CASE WHEN leido = 1 THEN 1 ELSE 0 END,
                            COALESCE(mensaje, ''),
                            'SYSTEM',
                            'NORMAL',
                            NULL,
                            link,
                            rol_destino,
                            COALESCE(titulo, ''),
                            COALESCE(tipo, 'INFO'),
                            NULL
                        FROM notifications;
                    """);
                }
            }

            stmt.execute("DROP TABLE notifications;");
            stmt.execute("ALTER TABLE notifications_canonical RENAME TO notifications;");
            stmt.execute("COMMIT;");

            log.info("[DatabaseMigration] Successfully migrated 'notifications' table to canonical schema.");
        }
    }

    private void migrateQaEvidencesTable(Statement stmt) throws Exception {
        ResultSet rsTable = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='qa_evidences'");
        if (!rsTable.next()) {
            return;
        }
        rsTable.close();

        ResultSet rsCols = stmt.executeQuery("PRAGMA table_info(qa_evidences)");
        List<String> columns = new ArrayList<>();
        while (rsCols.next()) {
            columns.add(rsCols.getString("name").toLowerCase());
        }
        rsCols.close();

        boolean needsMigration = !columns.contains("original_file_name") ||
                                 !columns.contains("stored_file_name") ||
                                 !columns.contains("storage_path") ||
                                 !columns.contains("sha256") ||
                                 !columns.contains("active");

        if (needsMigration) {
            log.info("[DatabaseMigration] Migrating 'qa_evidences' table to include P0-C canonical fields...");

            stmt.execute("BEGIN TRANSACTION;");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS qa_evidences_canonical (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    inspection_id BIGINT NOT NULL,
                    lot_id BIGINT NOT NULL,
                    original_file_name VARCHAR(255) NOT NULL,
                    stored_file_name VARCHAR(255) NOT NULL,
                    storage_path VARCHAR(500) NOT NULL,
                    mime_type VARCHAR(100) NOT NULL,
                    file_size BIGINT NOT NULL,
                    sha256 VARCHAR(64),
                    description TEXT,
                    uploaded_by VARCHAR(150) NOT NULL,
                    uploaded_at TIMESTAMP NOT NULL,
                    active BOOLEAN NOT NULL DEFAULT 1
                );
            """);

            ResultSet rsCount = stmt.executeQuery("SELECT COUNT(*) FROM qa_evidences");
            int count = 0;
            if (rsCount.next()) {
                count = rsCount.getInt(1);
            }
            rsCount.close();

            if (count > 0) {
                String origCol = columns.contains("original_file_name") ? "original_file_name" : (columns.contains("file_name") ? "file_name" : "'evidence.jpg'");
                String storedCol = columns.contains("stored_file_name") ? "stored_file_name" : (columns.contains("file_name") ? "file_name" : "'evidence.jpg'");
                String pathCol = columns.contains("storage_path") ? "storage_path" : (columns.contains("file_url") ? "file_url" : "'uploads/evidence.jpg'");
                String shaCol = columns.contains("sha256") ? "sha256" : "NULL";
                String activeCol = columns.contains("active") ? "active" : "1";

                stmt.execute(String.format("""
                    INSERT INTO qa_evidences_canonical (
                        id, inspection_id, lot_id, original_file_name, stored_file_name, storage_path, mime_type, file_size, sha256, description, uploaded_by, uploaded_at, active
                    )
                    SELECT 
                        id,
                        inspection_id,
                        lot_id,
                        COALESCE(%s, 'evidence.jpg'),
                        COALESCE(%s, 'evidence.jpg'),
                        COALESCE(%s, 'uploads/evidence.jpg'),
                        COALESCE(mime_type, 'image/jpeg'),
                        COALESCE(file_size, 0),
                        %s,
                        description,
                        COALESCE(uploaded_by, 'qa@exportrace.pe'),
                        COALESCE(uploaded_at, datetime('now')),
                        COALESCE(%s, 1)
                    FROM qa_evidences;
                """, origCol, storedCol, pathCol, shaCol, activeCol));
            }

            stmt.execute("DROP TABLE qa_evidences;");
            stmt.execute("ALTER TABLE qa_evidences_canonical RENAME TO qa_evidences;");
            stmt.execute("COMMIT;");

            log.info("[DatabaseMigration] Successfully migrated 'qa_evidences' to canonical P0-C schema.");
        }
    }

    private void migrateDocumentsTable(Statement stmt) throws Exception {
        ResultSet rsTable = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='documents'");
        if (!rsTable.next()) {
            return;
        }
        rsTable.close();

        ResultSet rsCols = stmt.executeQuery("PRAGMA table_info(documents)");
        List<String> columns = new ArrayList<>();
        while (rsCols.next()) {
            columns.add(rsCols.getString("name").toLowerCase());
        }
        rsCols.close();

        boolean needsMigration = !columns.contains("version") ||
                                 !columns.contains("sha256") ||
                                 !columns.contains("mime_type") ||
                                 !columns.contains("file_size") ||
                                 !columns.contains("active") ||
                                 !columns.contains("file_path");

        if (needsMigration) {
            log.info("[DatabaseMigration] Migrating 'documents' table to canonical versioned schema...");

            stmt.execute("BEGIN TRANSACTION;");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS documents_canonical (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    lote_id BIGINT NOT NULL,
                    nombre VARCHAR(255) NOT NULL,
                    tipo VARCHAR(100) NOT NULL,
                    url VARCHAR(500) NOT NULL,
                    fecha_subida TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    subido_por VARCHAR(150),
                    version INTEGER NOT NULL DEFAULT 1,
                    sha256 VARCHAR(64),
                    mime_type VARCHAR(100) DEFAULT 'application/pdf',
                    file_size BIGINT DEFAULT 0,
                    active BOOLEAN NOT NULL DEFAULT 1,
                    file_path VARCHAR(500),
                    CONSTRAINT uk_lot_doc_type_version UNIQUE (lote_id, tipo, version)
                );
            """);

            ResultSet rsCount = stmt.executeQuery("SELECT COUNT(*) FROM documents");
            int count = 0;
            if (rsCount.next()) {
                count = rsCount.getInt(1);
            }
            rsCount.close();

            if (count > 0) {
                String verCol = columns.contains("version") ? "version" : "1";
                String shaCol = columns.contains("sha256") ? "sha256" : "NULL";
                String mimeCol = columns.contains("mime_type") ? "mime_type" : "'application/pdf'";
                String sizeCol = columns.contains("file_size") ? "file_size" : "2500000";
                String activeCol = columns.contains("active") ? "active" : "1";
                String pathCol = columns.contains("file_path") ? "file_path" : "url";

                stmt.execute(String.format("""
                    INSERT INTO documents_canonical (
                        id, lote_id, nombre, tipo, url, fecha_subida, subido_por, version, sha256, mime_type, file_size, active, file_path
                    )
                    SELECT 
                        id,
                        lote_id,
                        COALESCE(nombre, 'documento.pdf'),
                        COALESCE(tipo, 'DECLARACION_JURADA'),
                        COALESCE(url, '/documents/doc.pdf'),
                        COALESCE(fecha_subida, datetime('now')),
                        COALESCE(subido_por, 'sistema@exportrace.pe'),
                        COALESCE(%s, 1),
                        %s,
                        COALESCE(%s, 'application/pdf'),
                        COALESCE(%s, 2500000),
                        COALESCE(%s, 1),
                        COALESCE(%s, url)
                    FROM documents;
                """, verCol, shaCol, mimeCol, sizeCol, activeCol, pathCol));
            }

            stmt.execute("DROP TABLE documents;");
            stmt.execute("ALTER TABLE documents_canonical RENAME TO documents;");
            stmt.execute("COMMIT;");

            log.info("[DatabaseMigration] Successfully migrated 'documents' to canonical versioned schema.");
        }
    }
}
