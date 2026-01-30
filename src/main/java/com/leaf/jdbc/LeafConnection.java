package com.leaf.jdbc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.ShardingKey;
import java.sql.Statement;
import java.sql.Struct;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetMetaDataImpl;
import javax.sql.rowset.RowSetProvider;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

final class LeafConnection implements Connection {
  private static final String API_BASE =
      System.getProperty("leaf.api.base", "https://api.withleaf.io");
  private final String token;
  private boolean closed = false;
  private boolean autoCommit = true;

  LeafConnection(LeafJdbcUrl parsed) throws SQLException {
    this.token = authenticate(parsed.username(), parsed.password());
  }

  private String authenticate(String username, String password) throws SQLException {
    try {
      OkHttpClient client =
          new OkHttpClient.Builder()
              .connectTimeout(30, TimeUnit.MINUTES)
              .readTimeout(30, TimeUnit.MINUTES)
              .writeTimeout(30, TimeUnit.MINUTES)
              .build();
      ObjectMapper mapper = new ObjectMapper();

      Map<String, String> authData = new HashMap<>();
      authData.put("username", username);
      authData.put("password", password);
      authData.put("rememberMe", "true");

      String jsonBody = mapper.writeValueAsString(authData);

      MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
      RequestBody body = RequestBody.create(jsonBody, mediaType);

      Request request =
          new Request.Builder()
              .url(API_BASE + "/api/authenticate")
              .addHeader("Content-Type", "application/json")
              .post(body)
              .build();

      try (Response response = client.newCall(request).execute()) {
        if (!response.isSuccessful()) {
          throw new SQLException(
              "Authentication failed: HTTP "
                  + response.code()
                  + ": "
                  + (response.body() != null ? response.body().string() : ""));
        }
        String responseBody = response.body() != null ? response.body().string() : "";
        JsonNode root = mapper.readTree(responseBody);
        if (root.has("id_token")) {
          return root.get("id_token").asText();
        }
        throw new SQLException("Invalid authentication response: missing id_token");
      }
    } catch (IOException e) {
      throw new SQLException("Failed to authenticate with Leaf API", e);
    }
  }

  String apiPrefix() {
    return API_BASE + "/api/v1";
  }

  String token() {
    return token;
  }

  @Override
  public Statement createStatement() throws SQLException {
    ensureOpen();
    return new LeafStatement(this);
  }

  @Override
  public PreparedStatement prepareStatement(String sql) throws SQLException {
    throw new SQLFeatureNotSupportedException("PreparedStatement not supported");
  }

  @Override
  public CallableStatement prepareCall(String sql) throws SQLException {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public String nativeSQL(String sql) {
    return sql;
  }

  @Override
  public void setAutoCommit(boolean autoCommit) {
    this.autoCommit = autoCommit;
  }

  @Override
  public boolean getAutoCommit() {
    return autoCommit;
  }

  @Override
  public void commit() {}

  @Override
  public void rollback() {}

  @Override
  public void close() {
    this.closed = true;
  }

  @Override
  public boolean isClosed() {
    return closed;
  }

  @Override
  public DatabaseMetaData getMetaData() throws SQLException {
    return createDatabaseMetaDataProxy();
  }

  @Override
  public void setReadOnly(boolean readOnly) {}

  @Override
  public boolean isReadOnly() {
    return true;
  }

  @Override
  public void setCatalog(String catalog) {}

  @Override
  public String getCatalog() {
    return null;
  }

  @Override
  public void setTransactionIsolation(int level) {}

  @Override
  public int getTransactionIsolation() {
    return Connection.TRANSACTION_NONE;
  }

  @Override
  public SQLWarning getWarnings() {
    return null;
  }

  @Override
  public void clearWarnings() {}

  @Override
  public Statement createStatement(int resultSetType, int resultSetConcurrency)
      throws SQLException {
    return createStatement();
  }

  @Override
  public Statement createStatement(
      int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
    return createStatement();
  }

  @Override
  public Map<String, Class<?>> getTypeMap() throws SQLException {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void setHoldability(int holdability) {}

  @Override
  public int getHoldability() {
    return 0;
  }

  @Override
  public Savepoint setSavepoint() throws SQLException {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public Savepoint setSavepoint(String name) throws SQLException {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void rollback(Savepoint savepoint) throws SQLException {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void releaseSavepoint(Savepoint savepoint) throws SQLException {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public Clob createClob() throws SQLException {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public Blob createBlob() throws SQLException {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public NClob createNClob() throws SQLException {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public SQLXML createSQLXML() throws SQLException {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public boolean isValid(int timeout) {
    return !closed;
  }

  @Override
  public void setClientInfo(String name, String value) throws SQLClientInfoException {}

  @Override
  public void setClientInfo(Properties properties) throws SQLClientInfoException {}

  @Override
  public String getClientInfo(String name) {
    return null;
  }

  @Override
  public Properties getClientInfo() {
    return new Properties();
  }

  @Override
  public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void setSchema(String schema) {}

  @Override
  public String getSchema() {
    return null;
  }

  @Override
  public void abort(Executor executor) {
    close();
  }

  @Override
  public void setNetworkTimeout(Executor executor, int milliseconds) {}

  @Override
  public int getNetworkTimeout() {
    return 0;
  }

  @Override
  public <T> T unwrap(Class<T> iface) throws SQLException {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public boolean isWrapperFor(Class<?> iface) {
    return false;
  }

  @Override
  public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
      throws SQLException {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public PreparedStatement prepareStatement(
      String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability)
      throws SQLException {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency)
      throws SQLException {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public CallableStatement prepareCall(
      String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability)
      throws SQLException {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void beginRequest() throws SQLException {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void endRequest() throws SQLException {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public boolean setShardingKeyIfValid(
      ShardingKey shardingKey, ShardingKey superShardingKey, int timeout) throws SQLException {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void setShardingKey(ShardingKey shardingKey, ShardingKey superShardingKey)
      throws SQLException {
    throw new SQLFeatureNotSupportedException();
  }

  private void ensureOpen() throws SQLException {
    if (closed) throw new SQLException("Connection is closed");
  }

  private DatabaseMetaData createDatabaseMetaDataProxy() throws SQLException {
    ClassLoader cl = DatabaseMetaData.class.getClassLoader();
    InvocationHandler handler =
        new InvocationHandler() {
          @Override
          public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            Class<?> rt = method.getReturnType();

            if (name.equals("getURL")) return LeafDriver.JDBC_URL_PREFIX;
            if (name.equals("getUserName")) return null;
            if (name.equals("getDriverName")) return "Leaf JDBC Driver";
            if (name.equals("getDriverVersion")) return "0.3.7";
            if (name.equals("getDriverMajorVersion")) return 0;
            if (name.equals("getDriverMinorVersion")) return 3;
            if (name.equals("getDatabaseProductName")) return "Leaf API";
            if (name.equals("getDatabaseProductVersion")) return "unknown";
            if (name.equals("getIdentifierQuoteString")) return "\"";
            if (name.equals("getCatalogSeparator")) return ".";
            if (name.equals("getCatalogTerm")) return "catalog";
            if (name.equals("getSchemaTerm")) return "schema";
            if (name.equals("getSearchStringEscape")) return "\\";
            if (name.equals("getDatabaseMajorVersion")) return 1;
            if (name.equals("getDatabaseMinorVersion")) return 0;

            if (name.equals("getSchemas")) {
              return emptyResultSetWithColumns("TABLE_SCHEM", "TABLE_CATALOG");
            }
            if (name.equals("getTables")) {
              return emptyResultSetWithColumns(
                  "TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "TABLE_TYPE", "REMARKS");
            }
            if (name.equals("getColumns")) {
              return emptyResultSetWithColumns(
                  "TABLE_CAT",
                  "TABLE_SCHEM",
                  "TABLE_NAME",
                  "COLUMN_NAME",
                  "DATA_TYPE",
                  "TYPE_NAME",
                  "COLUMN_SIZE",
                  "NULLABLE");
            }

            // Provide generic defaults to avoid failures in tools
            if (rt == boolean.class) return false;
            if (rt == int.class) return 0;
            if (rt == long.class) return 0L;
            if (rt == String.class) return "";
            if (ResultSet.class.isAssignableFrom(rt)) return emptyResultSetWithColumns("COLUMN");
            return null;
          }
        };
    return (DatabaseMetaData)
        Proxy.newProxyInstance(cl, new Class<?>[] {DatabaseMetaData.class}, handler);
  }

  private ResultSet emptyResultSetWithColumns(String... columnNames) throws SQLException {
    CachedRowSet crs = RowSetProvider.newFactory().createCachedRowSet();
    RowSetMetaDataImpl md = new RowSetMetaDataImpl();
    md.setColumnCount(columnNames.length);
    for (int i = 0; i < columnNames.length; i++) {
      md.setColumnName(i + 1, columnNames[i]);
      md.setColumnType(i + 1, java.sql.Types.VARCHAR);
      md.setNullable(i + 1, java.sql.ResultSetMetaData.columnNullable);
    }
    crs.setMetaData(md);
    crs.beforeFirst();
    return crs;
  }

  // Backward-compat: some paths may call this zero-column helper; ensure it returns 1+ columns
  @SuppressWarnings("unused")
  private ResultSet emptyResultSet() throws SQLException {
    return emptyResultSetWithColumns("COLUMN");
  }
}
