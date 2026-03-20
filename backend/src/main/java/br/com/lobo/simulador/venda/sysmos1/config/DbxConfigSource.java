package br.com.lobo.simulador.venda.sysmos1.config;

import org.eclipse.microprofile.config.spi.ConfigSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DbxConfigSource implements ConfigSource {

    private static final String DBX_PATH = "C:\\SysmoVs\\dbxconnections.ini";
    private static final String DEFAULT_USER = "sysdba";
    private static final String DEFAULT_PASS = "L$5a7*(B";

    private final Map<String, String> properties;

    public DbxConfigSource() {
        properties = new HashMap<>();
        loadFromIni();
    }

    private void loadFromIni() {
        Path path = Paths.get(DBX_PATH);
        if (!Files.exists(path)) {
            System.err.println("[DbxConfigSource] Arquivo nao encontrado: " + DBX_PATH);
            return;
        }

        String host = null;
        String port = null;
        String database = null;

        try (BufferedReader reader = Files.newBufferedReader(path, Charset.forName("UTF-8"))) {
            String currentSection = "";
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("[") && line.endsWith("]")) {
                    currentSection = line.substring(1, line.length() - 1);
                    continue;
                }

                if ("Delphi".equals(currentSection) && line.startsWith("Database=")) {
                    String value = line.substring("Database=".length()).trim();
                    int slashIdx = value.indexOf('/');
                    if (slashIdx > 0) {
                        String hostPort = value.substring(0, slashIdx);
                        database = value.substring(slashIdx + 1);
                        int colonIdx = hostPort.indexOf(':');
                        if (colonIdx > 0) {
                            host = hostPort.substring(0, colonIdx);
                            port = hostPort.substring(colonIdx + 1);
                        } else {
                            host = hostPort;
                            port = "5432";
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[DbxConfigSource] Erro ao ler " + DBX_PATH + ": " + e.getMessage());
            return;
        }

        if (host != null && database != null) {
            String jdbcUrl = "jdbc:postgresql://" + host + ":" + (port != null ? port : "5432") + "/" + database;
            properties.put("dbx.jdbc.url", jdbcUrl);
            properties.put("dbx.username", DEFAULT_USER);
            properties.put("dbx.password", DEFAULT_PASS);
            System.out.println("[DbxConfigSource] Conexao: " + jdbcUrl + " (usuario: " + DEFAULT_USER + ")");
        } else {
            System.err.println("[DbxConfigSource] Nao foi possivel extrair conexao de " + DBX_PATH);
        }
    }

    @Override
    public Map<String, String> getProperties() { return properties; }

    @Override
    public Set<String> getPropertyNames() { return properties.keySet(); }

    @Override
    public String getValue(String propertyName) { return properties.get(propertyName); }

    @Override
    public String getName() { return "DbxConnectionsIniConfigSource"; }

    @Override
    public int getOrdinal() { return 275; }
}
