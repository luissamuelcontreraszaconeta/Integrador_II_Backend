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

                // Check row count in legacy notifications
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

                log.info("[DatabaseMigration] Successfully migrated 'notifications' table to canonical schema with created_at.");
            }

        } catch (Exception e) {
            log.error("[DatabaseMigration] Error during schema migration check: {}", e.getMessage(), e);
        }
    }
}
