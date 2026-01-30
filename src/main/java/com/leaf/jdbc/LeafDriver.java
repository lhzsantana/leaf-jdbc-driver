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
    DriverPropertyInfo user =
        new DriverPropertyInfo(
            "user", firstNonEmpty(info.getProperty("user"), info.getProperty("username"), ""));
    user.required = true;
    user.description = "Leaf API username";

    DriverPropertyInfo password =
        new DriverPropertyInfo("password", info.getProperty("password", ""));
    password.required = true;
    password.description = "Leaf API password";

    return new DriverPropertyInfo[] {user, password};
  }

  private static String firstNonEmpty(String... values) {
    for (String value : values) {
      if (value != null && !value.isEmpty()) {
        return value;
      }
    }
    return "";
  }

  @Override
  public int getMajorVersion() {
    return 0;
  }

  @Override
  public int getMinorVersion() {
    return 3;
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
