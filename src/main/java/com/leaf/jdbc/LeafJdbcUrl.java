package com.leaf.jdbc;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

final class LeafJdbcUrl {
  private final String apiPrefix;
  private final String token;

  private LeafJdbcUrl(String apiPrefix, String token) {
    this.apiPrefix = apiPrefix;
    this.token = token;
  }

  static LeafJdbcUrl parse(String url, Properties info) throws SQLException {
    if (url == null || !url.startsWith(LeafDriver.JDBC_URL_PREFIX)) {
      throw new SQLException("Invalid JDBC URL, expected prefix: " + LeafDriver.JDBC_URL_PREFIX);
    }

    String rest = url.substring(LeafDriver.JDBC_URL_PREFIX.length());

    Map<String, String> params = new HashMap<>();
    if (!rest.isEmpty()) {
      // Supported formats:
      // jdbc:leaf:?apiPrefix=...&token=...
      // jdbc:leaf:apiPrefix=...;token=...
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

    String apiPrefix = firstNonEmpty(info.getProperty("apiPrefix"), params.get("apiPrefix"));
    String token = firstNonEmpty(info.getProperty("token"), params.get("token"));

    if (apiPrefix == null || apiPrefix.isBlank()) {
      throw new SQLException("Missing required property 'apiPrefix'");
    }
    if (token == null || token.isBlank()) {
      throw new SQLException("Missing required property 'token'");
    }

    return new LeafJdbcUrl(apiPrefix, token);
  }

  private static String firstNonEmpty(String a, String b) {
    if (a != null && !a.isBlank()) return a;
    if (b != null && !b.isBlank()) return b;
    return null;
  }

  String apiPrefix() {
    return apiPrefix;
  }

  String token() {
    return token;
  }
}
