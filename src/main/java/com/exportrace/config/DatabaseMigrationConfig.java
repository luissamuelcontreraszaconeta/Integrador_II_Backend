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
            migrateIncidentReportsTable(stmt);
            migrateServiceRequestsTable(stmt);

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

    private void migrateIncidentReportsTable(Statement stmt) throws Exception {
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS incident_reports (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                code VARCHAR(30) NOT NULL,
                correlation_id VARCHAR(64) NOT NULL,
                reporter_user_id BIGINT NOT NULL,
                reporter_username VARCHAR(150) NOT NULL,
                reporter_email VARCHAR(150) NOT NULL,
                reporter_role VARCHAR(50) NOT NULL,
                category VARCHAR(40) NOT NULL,
                summary VARCHAR(200) NOT NULL,
                description TEXT NOT NULL,
                impact VARCHAR(20) NOT NULL,
                urgency VARCHAR(20) NOT NULL,
                priority VARCHAR(20) NOT NULL,
                status VARCHAR(30) NOT NULL DEFAULT 'REPORTED',
                module VARCHAR(60),
                route VARCHAR(255),
                related_lot_id BIGINT,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                resolved_at TIMESTAMP,
                closed_at TIMESTAMP,
                jira_issue_key VARCHAR(50),
                jira_request_id VARCHAR(50),
                jira_last_sync_at TIMESTAMP,
                sync_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
                sync_attempts INTEGER NOT NULL DEFAULT 0,
                last_sync_error TEXT,
                assigned_agent_name VARCHAR(150),
                resolution_summary TEXT,
                sla_first_response_target_at TIMESTAMP,
                first_response_at TIMESTAMP,
                sla_resolution_target_at TIMESTAMP,
                sla_breached BOOLEAN NOT NULL DEFAULT 0,
                version BIGINT DEFAULT 0,
                CONSTRAINT uk_incident_code UNIQUE (code),
                CONSTRAINT uk_incident_correlation_id UNIQUE (correlation_id)
            );
        """);

        stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_uk_incident_code ON incident_reports(code);");
        stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_uk_incident_correlation_id ON incident_reports(correlation_id);");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_incident_reporter_user ON incident_reports(reporter_user_id);");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_incident_status ON incident_reports(status);");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_incident_priority ON incident_reports(priority);");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_incident_sync_status ON incident_reports(sync_status);");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_incident_created_at ON incident_reports(created_at);");
    }

    private void migrateServiceRequestsTable(Statement stmt) throws Exception {
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS service_requests (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                code VARCHAR(30) NOT NULL,
                service_code VARCHAR(30) NOT NULL,
                service_title VARCHAR(200) NOT NULL,
                category_key VARCHAR(40) NOT NULL,
                category_label VARCHAR(100) NOT NULL,
                correlation_id VARCHAR(64) NOT NULL,
                requester_user_id BIGINT NOT NULL,
                requester_username VARCHAR(150) NOT NULL,
                requester_email VARCHAR(150) NOT NULL,
                requester_role VARCHAR(50) NOT NULL,
                summary VARCHAR(200) NOT NULL,
                lot_code VARCHAR(60) NOT NULL DEFAULT '',
                requested_information TEXT NOT NULL DEFAULT '',
                additional_detail TEXT,
                document_query_type VARCHAR(120),
                observed_field VARCHAR(200),
                problem_description TEXT,
                field_to_correct VARCHAR(200),
                current_value TEXT,
                proposed_value TEXT,
                justification TEXT,
                worker_name VARCHAR(150),
                affected_user VARCHAR(150),
                institutional_email VARCHAR(150),
                area VARCHAR(100),
                required_role VARCHAR(100),
                access_request_type VARCHAR(100),
                current_role_field VARCHAR(100),
                required_permissions VARCHAR(150),
                reason TEXT,
                requires_approval BOOLEAN DEFAULT 0,
                approval_status VARCHAR(40) DEFAULT 'NOT_REQUIRED',
                jira_status VARCHAR(100),
                jira_sla_summary VARCHAR(255),
                attachment_original_name VARCHAR(255),
                attachment_stored_path VARCHAR(500),
                attachment_mime_type VARCHAR(100),
                attachment_size BIGINT,
                attachment_sha256 VARCHAR(64),
                jira_attachment_synced BOOLEAN DEFAULT 0,
                status VARCHAR(30) NOT NULL DEFAULT 'REGISTERED',
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                sla_target VARCHAR(100) NOT NULL DEFAULT '4 horas hábiles',
                sla_authority VARCHAR(40) NOT NULL DEFAULT 'LOCAL_ESTIMATE',
                jira_issue_key VARCHAR(50),
                jira_request_id VARCHAR(50),
                jira_request_type_id VARCHAR(30),
                jira_last_sync_at TIMESTAMP,
                sync_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
                sync_attempts INTEGER NOT NULL DEFAULT 0,
                last_sync_error TEXT,
                version BIGINT DEFAULT 0,
                CONSTRAINT uk_service_request_code UNIQUE (code),
                CONSTRAINT uk_service_request_correlation_id UNIQUE (correlation_id)
            );
        """);

        List<String> existingCols = new ArrayList<>();
        try (ResultSet rs = stmt.executeQuery("PRAGMA table_info(service_requests)")) {
            while (rs.next()) {
                existingCols.add(rs.getString("name").toLowerCase());
            }
        }

        String[][] newCols = {
            {"document_query_type", "VARCHAR(120)"},
            {"observed_field", "VARCHAR(200)"},
            {"problem_description", "TEXT"},
            {"field_to_correct", "VARCHAR(200)"},
            {"current_value", "TEXT"},
            {"proposed_value", "TEXT"},
            {"justification", "TEXT"},
            {"worker_name", "VARCHAR(150)"},
            {"affected_user", "VARCHAR(150)"},
            {"institutional_email", "VARCHAR(150)"},
            {"area", "VARCHAR(100)"},
            {"required_role", "VARCHAR(100)"},
            {"access_request_type", "VARCHAR(100)"},
            {"current_role_field", "VARCHAR(100)"},
            {"required_permissions", "VARCHAR(150)"},
            {"reason", "TEXT"},
            {"requires_approval", "BOOLEAN DEFAULT 0"},
            {"approval_status", "VARCHAR(40) DEFAULT 'NOT_REQUIRED'"},
            {"jira_status", "VARCHAR(100)"},
            {"jira_sla_summary", "VARCHAR(255)"},
            {"attachment_original_name", "VARCHAR(255)"},
            {"attachment_stored_path", "VARCHAR(500)"},
            {"attachment_mime_type", "VARCHAR(100)"},
            {"attachment_size", "BIGINT"},
            {"attachment_sha256", "VARCHAR(64)"},
            {"jira_attachment_synced", "BOOLEAN DEFAULT 0"}
        };

        for (String[] col : newCols) {
            if (!existingCols.contains(col[0])) {
                stmt.execute("ALTER TABLE service_requests ADD COLUMN " + col[0] + " " + col[1] + ";");
            }
        }

        // Si la tabla existente fue creada por Hibernate con un CHECK constraint antiguo sobre 'status'
        // que no incluye 'IN_PROGRESS', 'RESOLVED' o 'REJECTED', reconstruir la tabla preservando los registros.
        String tableSql = "";
        try (ResultSet rsSql = stmt.executeQuery("SELECT sql FROM sqlite_master WHERE type='table' AND name='service_requests'")) {
            if (rsSql.next() && rsSql.getString(1) != null) {
                tableSql = rsSql.getString(1);
            }
        }
        if (tableSql.toLowerCase().contains("check") && !tableSql.contains("REJECTED")) {
            log.info("[DatabaseMigration] Updating CHECK constraint on 'service_requests.status' while preserving all rows...");
            stmt.execute("BEGIN TRANSACTION;");
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS service_requests_canonical (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    code VARCHAR(30) NOT NULL,
                    service_code VARCHAR(30) NOT NULL,
                    service_title VARCHAR(200) NOT NULL,
                    category_key VARCHAR(40) NOT NULL,
                    category_label VARCHAR(100) NOT NULL,
                    correlation_id VARCHAR(64) NOT NULL,
                    requester_user_id BIGINT NOT NULL,
                    requester_username VARCHAR(150) NOT NULL,
                    requester_email VARCHAR(150) NOT NULL,
                    requester_role VARCHAR(50) NOT NULL,
                    summary VARCHAR(200) NOT NULL,
                    lot_code VARCHAR(60) NOT NULL DEFAULT '',
                    requested_information TEXT NOT NULL DEFAULT '',
                    additional_detail TEXT,
                    document_query_type VARCHAR(120),
                    observed_field VARCHAR(200),
                    problem_description TEXT,
                    field_to_correct VARCHAR(200),
                    current_value TEXT,
                    proposed_value TEXT,
                    justification TEXT,
                    worker_name VARCHAR(150),
                    affected_user VARCHAR(150),
                    institutional_email VARCHAR(150),
                    area VARCHAR(100),
                    required_role VARCHAR(100),
                    access_request_type VARCHAR(100),
                    current_role_field VARCHAR(100),
                    required_permissions VARCHAR(150),
                    reason TEXT,
                    requires_approval BOOLEAN DEFAULT 0,
                    approval_status VARCHAR(40) DEFAULT 'NOT_REQUIRED',
                    jira_status VARCHAR(100),
                    jira_sla_summary VARCHAR(255),
                    attachment_original_name VARCHAR(255),
                    attachment_stored_path VARCHAR(500),
                    attachment_mime_type VARCHAR(100),
                    attachment_size BIGINT,
                    attachment_sha256 VARCHAR(64),
                    jira_attachment_synced BOOLEAN DEFAULT 0,
                    status VARCHAR(30) NOT NULL DEFAULT 'REGISTERED',
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    sla_target VARCHAR(100) NOT NULL DEFAULT '4 horas hábiles',
                    sla_authority VARCHAR(40) NOT NULL DEFAULT 'LOCAL_ESTIMATE',
                    jira_issue_key VARCHAR(50),
                    jira_request_id VARCHAR(50),
                    jira_request_type_id VARCHAR(30),
                    jira_last_sync_at TIMESTAMP,
                    sync_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
                    sync_attempts INTEGER NOT NULL DEFAULT 0,
                    last_sync_error TEXT,
                    version BIGINT DEFAULT 0,
                    CONSTRAINT uk_service_request_code UNIQUE (code),
                    CONSTRAINT uk_service_request_correlation_id UNIQUE (correlation_id)
                );
            """);
            stmt.execute("""
                INSERT INTO service_requests_canonical (
                    id, code, service_code, service_title, category_key, category_label, correlation_id,
                    requester_user_id, requester_username, requester_email, requester_role, summary,
                    lot_code, requested_information, additional_detail,
                    document_query_type, observed_field, problem_description,
                    field_to_correct, current_value, proposed_value, justification,
                    worker_name, affected_user, institutional_email, area, required_role,
                    access_request_type, current_role_field, required_permissions, reason,
                    requires_approval, approval_status, jira_status, jira_sla_summary,
                    attachment_original_name, attachment_stored_path, attachment_mime_type, attachment_size, attachment_sha256, jira_attachment_synced,
                    status, created_at, updated_at, sla_target, sla_authority,
                    jira_issue_key, jira_request_id, jira_request_type_id, jira_last_sync_at,
                    sync_status, sync_attempts, last_sync_error, version
                )
                SELECT
                    id, code, service_code, service_title, category_key, category_label, correlation_id,
                    requester_user_id, requester_username, requester_email, requester_role, summary,
                    COALESCE(lot_code, ''), COALESCE(requested_information, ''), additional_detail,
                    document_query_type, observed_field, problem_description,
                    field_to_correct, current_value, proposed_value, justification,
                    worker_name, affected_user, institutional_email, area, required_role,
                    access_request_type, current_role_field, required_permissions, reason,
                    COALESCE(requires_approval, 0), COALESCE(approval_status, 'NOT_REQUIRED'), jira_status, jira_sla_summary,
                    attachment_original_name, attachment_stored_path, attachment_mime_type, attachment_size, attachment_sha256, COALESCE(jira_attachment_synced, 0),
                    status, created_at, updated_at, sla_target, sla_authority,
                    jira_issue_key, jira_request_id, jira_request_type_id, jira_last_sync_at,
                    sync_status, sync_attempts, last_sync_error, COALESCE(version, 0)
                FROM service_requests;
            """);
            stmt.execute("DROP TABLE service_requests;");
            stmt.execute("ALTER TABLE service_requests_canonical RENAME TO service_requests;");
            stmt.execute("COMMIT;");
            log.info("[DatabaseMigration] Successfully updated 'service_requests' table schema.");
        }

        stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_uk_service_request_code ON service_requests(code);");
        stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_uk_service_request_correlation_id ON service_requests(correlation_id);");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_sr_requester_user ON service_requests(requester_user_id);");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_sr_service_code ON service_requests(service_code);");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_sr_status ON service_requests(status);");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_sr_sync_status ON service_requests(sync_status);");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_sr_created_at ON service_requests(created_at);");

        stmt.execute("""
            CREATE TABLE IF NOT EXISTS service_request_comments (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                service_request_id BIGINT NOT NULL,
                jira_comment_id VARCHAR(64) UNIQUE,
                author_display_name VARCHAR(150) NOT NULL,
                body TEXT NOT NULL,
                public_comment BOOLEAN NOT NULL DEFAULT 1,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                source VARCHAR(30) NOT NULL DEFAULT 'JIRA',
                CONSTRAINT uk_sr_comment_jira_id UNIQUE (jira_comment_id)
            );
        """);
        stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_uk_sr_comment_jira_id ON service_request_comments(jira_comment_id);");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_sr_comment_request_id ON service_request_comments(service_request_id);");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_sr_comment_created_at ON service_request_comments(created_at);");
    }
}
