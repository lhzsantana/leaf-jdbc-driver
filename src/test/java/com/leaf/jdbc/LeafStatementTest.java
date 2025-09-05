package com.leaf.jdbc;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LeafStatementTest {
  private HttpServer server;
  private String apiPrefix;

  @BeforeEach
  void setup() throws Exception {
    server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext("/api/pointlake/query", this::handleQuery);
    server.start();
    apiPrefix = "http://localhost:" + server.getAddress().getPort() + "/api";
  }

  @AfterEach
  void teardown() {
    server.stop(0);
  }

  private void handleQuery(HttpExchange exchange) throws java.io.IOException {
    String json =
        "{\n" + "  \"columns\": [\"a\", \"b\"],\n" + "  \"rows\": [[1, \"x\"], [2, \"y\"]]\n" + "}";
    byte[] bytes = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    exchange.getResponseHeaders().add("Content-Type", "application/json");
    exchange.sendResponseHeaders(200, bytes.length);
    try (OutputStream os = exchange.getResponseBody()) {
      os.write(bytes);
    }
  }

  @Test
  void testQueryColumnsRowsShape() throws Exception {
    Properties p = new Properties();
    p.setProperty("apiPrefix", apiPrefix);
    p.setProperty("token", "dummy");

    try (Connection c = DriverManager.getConnection("jdbc:leaf:", p);
        Statement s = c.createStatement();
        ResultSet rs = s.executeQuery("select 1")) {
      assertNotNull(rs);
      assertTrue(rs.next());
      assertEquals(1, rs.getInt(1));
      assertEquals("x", rs.getString(2));
      assertTrue(rs.next());
      assertEquals(2, rs.getInt(1));
      assertEquals("y", rs.getString(2));
      assertFalse(rs.next());
    }
  }
}
