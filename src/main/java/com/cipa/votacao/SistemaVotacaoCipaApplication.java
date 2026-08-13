package com.cipa.votacao;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.TimeZone;

@SpringBootApplication
public class SistemaVotacaoCipaApplication {

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Manaus"));
    }

    public static void main(String[] args) {
        Dotenv.configure().ignoreIfMissing().systemProperties().load();
        ensureDatabaseExists();
        SpringApplication.run(SistemaVotacaoCipaApplication.class, args);
    }

    private static void ensureDatabaseExists() {
        String dbUrl = System.getProperty("DB_URL", System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/cipa"));
        String username = System.getProperty("DB_USERNAME", System.getenv().getOrDefault("DB_USERNAME", "postgres"));
        String password = System.getProperty("DB_PASSWORD", System.getenv().getOrDefault("DB_PASSWORD", ""));

        try {
            if (dbUrl.startsWith("jdbc:postgresql://")) {
                URI uri = new URI(dbUrl.substring(5));
                String path = uri.getPath();
                if (path != null && path.length() > 1) {
                    String dbName = path.substring(1);
                    int port = uri.getPort() == -1 ? 5432 : uri.getPort();
                    String host = uri.getHost() == null ? "localhost" : uri.getHost();
                    String baseUrl = "jdbc:postgresql://" + host + ":" + port + "/postgres";

                    try (Connection conn = DriverManager.getConnection(baseUrl, username, password);
                         Statement stmt = conn.createStatement()) {
                        ResultSet rs = stmt.executeQuery("SELECT 1 FROM pg_database WHERE datname = '" + dbName + "'");
                        if (!rs.next()) {
                            System.out.println("[CIPA] Banco de dados '" + dbName + "' não existe. Criando automaticamente...");
                            stmt.executeUpdate("CREATE DATABASE " + dbName);
                            System.out.println("[CIPA] Banco de dados '" + dbName + "' criado com sucesso!");
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[CIPA] Aviso ao verificar/criar banco de dados automaticamente: " + e.getMessage());
        }
    }
}