package com.leaf.jdbc;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;

public final class LeafDriver implements Driver {
  public static final String JDBC_URL_PREFIX = "jdbc:leaf:";

  static {
    try {
      DriverManager.registerDriver(new LeafDriver());
    } catch (SQLException e) {
      throw new RuntimeException("Failed to register LeafDriver", e);
    }
  }

  @Override
  public Connection connect(String url, Properties info) throws SQLException {
    if (!acceptsURL(url)) {
      return null;
    }

    LeafJdbcUrl parsed = LeafJdbcUrl.parse(url, info);
    return new LeafConnection(parsed);
  }

  @Override
  public boolean acceptsURL(String url) {
    return url != null && url.startsWith(JDBC_URL_PREFIX);
  }

  @Override
  public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
    DriverPropertyInfo apiPrefix =
        new DriverPropertyInfo("apiPrefix", info.getProperty("apiPrefix", ""));
    apiPrefix.required = true;
    apiPrefix.description = "Base API prefix, e.g. https://api.withleaf.io/api/v1";

    DriverPropertyInfo token = new DriverPropertyInfo("token", info.getProperty("token", ""));
    token.required = true;
    token.description = "JWT Bearer token for authentication";

    return new DriverPropertyInfo[] {apiPrefix, token};
  }

  @Override
  public int getMajorVersion() {
    return 0;
  }

  @Override
  public int getMinorVersion() {
    return 1;
  }

  @Override
  public boolean jdbcCompliant() {
    return false;
  }

  @Override
  public Logger getParentLogger() {
    return Logger.getLogger("com.leaf.jdbc");
  }
}
