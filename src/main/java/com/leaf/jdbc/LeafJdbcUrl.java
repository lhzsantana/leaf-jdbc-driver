package com.leaf.jdbc;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

final class LeafJdbcUrl {
  private final String username;
  private final String password;

  private LeafJdbcUrl(String username, String password) {
    this.username = username;
    this.password = password;
  }

  static LeafJdbcUrl parse(String url, Properties info) throws SQLException {
    if (url == null || !url.startsWith(LeafDriver.JDBC_URL_PREFIX)) {
      throw new SQLException("Invalid JDBC URL, expected prefix: " + LeafDriver.JDBC_URL_PREFIX);
    }

    String rest = url.substring(LeafDriver.JDBC_URL_PREFIX.length());

    Map<String, String> params = new HashMap<>();
    if (!rest.isEmpty()) {
      // Supported formats:
      // jdbc:leaf:?user=...&password=...
      // jdbc:leaf:user=...;password=...
      String query = rest;
      if (rest.startsWith("?")) {
        query = rest.substring(1);
        for (String pair : query.split("&")) {
          if (pair.isEmpty()) continue;
          String[] kv = pair.split("=", 2);
          String k = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
          String v = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
          params.put(k, v);
        }
      } else {
        for (String pair : query.split(";")) {
          if (pair.isEmpty()) continue;
          String[] kv = pair.split("=", 2);
          String k = kv[0];
          String v = kv.length > 1 ? kv[1] : "";
          params.put(k, v);
        }
      }
    }

    // Support both 'user'/'username' and 'password'
    String username =
        firstNonEmpty(
            info.getProperty("user"),
            firstNonEmpty(
                info.getProperty("username"),
                firstNonEmpty(params.get("user"), params.get("username"))));
    String password =
        firstNonEmpty(
            info.getProperty("password"),
            firstNonEmpty(params.get("password"), params.get("pass")));

    if (username == null || username.isBlank()) {
      throw new SQLException("Missing required property 'user' or 'username'");
    }
    if (password == null || password.isBlank()) {
      throw new SQLException("Missing required property 'password'");
    }

    return new LeafJdbcUrl(username, password);
  }

  private static String firstNonEmpty(String a, String b) {
    if (a != null && !a.isBlank()) return a;
    if (b != null && !b.isBlank()) return b;
    return null;
  }

  String username() {
    return username;
  }

  String password() {
    return password;
  }
}
