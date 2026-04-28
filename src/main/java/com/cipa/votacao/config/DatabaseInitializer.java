package com.cipa.votacao.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Automatically creates the PostgreSQL database if it does not exist.
 * 
 * Runs BEFORE Hibernate/JPA initialization.
 * Connects to the default "postgres" database, checks if the target DB exists,
 * and creates it if needed.
 * 
 * Tables/columns are handled by spring.jpa.hibernate.ddl-auto=update.
 */
@Configuration
@Slf4j
public class DatabaseInitializer {

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username:postgres}")
    private String username;

    @Value("${spring.datasource.password:}")
    private String password;

    @PostConstruct
    public void initialize() {
        // Skip auto-creation when running on a managed database (e.g. Render)
        // where the DB is already provisioned by the provider.
        String dbUrlEnv = System.getenv("DB_URL");
        if (dbUrlEnv != null && !dbUrlEnv.isEmpty()) {
            log.info("DB_URL detectada — banco gerenciado pelo provedor. Pulando criação automática.");
            return;
        }

        String dbName = extractDatabaseName(datasourceUrl);
        if (dbName == null || dbName.isEmpty()) {
            log.warn("Não foi possível extrair o nome do banco da URL: {}", datasourceUrl);
            return;
        }

        // Build URL to the default "postgres" database on the same server
        String baseUrl = datasourceUrl.substring(0, datasourceUrl.lastIndexOf("/")) + "/postgres";

        try (Connection conn = DriverManager.getConnection(baseUrl, username, password)) {
            // Check if database exists
            boolean exists = false;
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                     "SELECT 1 FROM pg_database WHERE datname = '" + dbName + "'")) {
                exists = rs.next();
            }

            if (!exists) {
                log.info("Banco de dados '{}' não encontrado. Criando automaticamente...", dbName);
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("CREATE DATABASE " + dbName
                            + " ENCODING 'UTF8'"
                            + " LC_COLLATE 'pt_BR.UTF-8'"
                            + " LC_CTYPE 'pt_BR.UTF-8'"
                            + " TEMPLATE template0");
                } catch (Exception e) {
                    // If locale fails (Windows), try without locale
                    log.debug("Criação com locale pt_BR falhou, tentando sem locale...");
                    try (Statement stmt = conn.createStatement()) {
                        stmt.executeUpdate("CREATE DATABASE " + dbName + " ENCODING 'UTF8'");
                    }
                }
                log.info("Banco de dados '{}' criado com sucesso!", dbName);
            } else {
                log.info("Banco de dados '{}' já existe.", dbName);
            }

        } catch (Exception e) {
            log.error("Erro ao verificar/criar banco de dados '{}': {}", dbName, e.getMessage());
            log.error("Certifique-se de que o PostgreSQL está rodando e as credenciais estão corretas.");
        }
    }

    /**
     * Extracts the database name from a JDBC URL like:
     * jdbc:postgresql://localhost:5432/cipa → cipa
     */
    private String extractDatabaseName(String url) {
        if (url == null) return null;
        // Remove query parameters
        String clean = url.contains("?") ? url.substring(0, url.indexOf("?")) : url;
        int lastSlash = clean.lastIndexOf("/");
        if (lastSlash >= 0 && lastSlash < clean.length() - 1) {
            return clean.substring(lastSlash + 1);
        }
        return null;
    }
}
