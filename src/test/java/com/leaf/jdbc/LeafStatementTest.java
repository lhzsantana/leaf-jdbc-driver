package com.leaf.jdbc;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LeafStatementTest {
  private HttpServer server;
  private String apiPrefix;

  @BeforeEach
  void setup() throws Exception {
    server = HttpServer.create(new InetSocketAddress(0), 0);
    // Mock authentication endpoint
    server.createContext("/api/authenticate", this::handleAuthenticate);
    // Mock query endpoint (correct path without /api/v1)
    server.createContext("/services/pointlake/api/v2/query", this::handleQuery);
    server.start();
    apiPrefix = "http://localhost:" + server.getAddress().getPort();
    // Set system properties to use mock server
    System.setProperty("leaf.api.base", apiPrefix);
    System.setProperty("leaf.query.base", apiPrefix + "/services/pointlake/api/v2/query");
  }

  @AfterEach
  void teardown() {
    System.clearProperty("leaf.api.base");
    System.clearProperty("leaf.query.base");
    server.stop(0);
  }

  private void handleAuthenticate(HttpExchange exchange) throws java.io.IOException {
    if (!"POST".equals(exchange.getRequestMethod())) {
      exchange.sendResponseHeaders(405, -1);
      return;
    }

    // Return a mock token (always succeed for testing)
    String json = "{\"id_token\":\"mock-token-12345\"}";
    byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().add("Content-Type", "application/json");
    exchange.sendResponseHeaders(200, bytes.length);
    try (OutputStream os = exchange.getResponseBody()) {
      os.write(bytes);
    }
  }

  private void handleQuery(HttpExchange exchange) throws java.io.IOException {
    // Verify it's a POST request
    if (!"POST".equals(exchange.getRequestMethod())) {
      exchange.sendResponseHeaders(405, -1);
      return;
    }

    // Verify sqlEngine query parameter
    String query = exchange.getRequestURI().getQuery();
    if (query == null || !query.contains("sqlEngine=SPARK_SQL")) {
      exchange.sendResponseHeaders(400, -1);
      return;
    }

    // Verify Authorization header (Bearer token) - accept any Bearer token for testing
    java.util.List<String> authHeaders = exchange.getRequestHeaders().get("Authorization");
    if (authHeaders == null || authHeaders.isEmpty()) {
      exchange.sendResponseHeaders(401, -1);
      return;
    }
    String authHeader = authHeaders.get(0);
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      exchange.sendResponseHeaders(401, -1);
      return;
    }

    // Read SQL from request body (after verifying headers)
    try (BufferedReader reader =
        new BufferedReader(
            new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
      reader.lines().collect(Collectors.joining("\n"));
    }

    // Return direct array of JSON objects (new format)
    String json =
        "[\n"
            + "  {\n"
            + "    \"geometry\": \"POINT (15.754155568620842 50.31069667390199)\"\n"
            + "  },\n"
            + "  {\n"
            + "    \"geometry\": \"POINT (15.754527717117158 50.3120397150037)\"\n"
            + "  }\n"
            + "]";
    byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().add("Content-Type", "application/json");
    exchange.sendResponseHeaders(200, bytes.length);
    try (OutputStream os = exchange.getResponseBody()) {
      os.write(bytes);
    }
  }

  @Test
  void testQueryArrayFormat() throws Exception {
    Properties p = new Properties();
    p.setProperty("user", "testuser");
    p.setProperty("password", "testpass");

    try (Connection c = DriverManager.getConnection("jdbc:leaf:", p);
        Statement s = c.createStatement();
        ResultSet rs =
            s.executeQuery("SELECT geometry FROM leaf.pointlake.points TABLESAMPLE(0.3 PERCENT)")) {
      assertNotNull(rs);
      assertTrue(rs.next());
      assertEquals("POINT (15.754155568620842 50.31069667390199)", rs.getString("geometry"));
      assertTrue(rs.next());
      assertEquals("POINT (15.754527717117158 50.3120397150037)", rs.getString("geometry"));
      assertFalse(rs.next());
    }
  }
}
