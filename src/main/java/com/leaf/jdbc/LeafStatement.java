package com.leaf.jdbc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetMetaDataImpl;
import javax.sql.rowset.RowSetProvider;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;

final class LeafStatement implements Statement {
  private final LeafConnection connection;
  private final OkHttpClient client = new OkHttpClient();
  private final ObjectMapper mapper = new ObjectMapper();
  private boolean closed = false;
  private int fetchSize = 0;
  private ResultSet lastResultSet = null;

  LeafStatement(LeafConnection connection) {
    this.connection = connection;
  }

  @Override
  public ResultSet executeQuery(String sql) throws SQLException {
    ensureOpen();
    // Optional validation: Apache Calcite may not support all Spark SQL syntaxes
    // The API will perform final validation anyway
    try {
      validateSql(sql);
    } catch (SQLException e) {
      // Ignore Calcite validation errors to allow Spark SQL-specific syntaxes
      // The Leaf API will validate the SQL anyway
    }

    try {
      // Use system property for testing, otherwise use production endpoint
      String queryBase =
          System.getProperty(
              "leaf.query.base", "https://api.withleaf.io/services/pointlake/api/v2/query");
      HttpUrl base = Objects.requireNonNull(HttpUrl.parse(queryBase));
      HttpUrl url = base.newBuilder().addQueryParameter("sqlEngine", "SPARK_SQL").build();

      // Create request body with SQL query as plain text
      MediaType mediaType = MediaType.parse("text/plain; charset=utf-8");
      RequestBody body = RequestBody.create(sql, mediaType);

      Request request =
          new Request.Builder()
              .url(url)
              .addHeader("Authorization", "Bearer " + connection.token())
              .addHeader("Content-Type", "text/plain; charset=utf-8")
              .post(body)
              .build();

      try (Response response = client.newCall(request).execute()) {
        if (!response.isSuccessful()) {
          throw new SQLException(
              "HTTP "
                  + response.code()
                  + ": "
                  + (response.body() != null ? response.body().string() : ""));
        }
        String responseBody = response.body() != null ? response.body().string() : "";
        ResultSet rs = parseJsonToResultSet(responseBody);
        this.lastResultSet = rs;
        return rs;
      }
    } catch (IOException e) {
      throw new SQLException("I/O error on HTTP call", e);
    }
  }

  private void validateSql(String sql) throws SQLException {
    try {
      SqlParser parser = SqlParser.create(sql);
      parser.parseStmt();
    } catch (SqlParseException e) {
      throw new SQLException("Invalid SQL: " + e.getMessage(), e);
    }
  }

  private ResultSet parseJsonToResultSet(String body) throws SQLException {
    try {
      JsonNode root = mapper.readTree(body);

      List<String> columns = new ArrayList<>();
      List<List<Object>> rows = new ArrayList<>();

      if (root == null || root.isNull()) {
        return buildRowSet(columns, rows);
      }

      if (root.isObject() && root.has("columns") && root.has("rows")) {
        for (JsonNode c : root.get("columns")) {
          columns.add(c.asText());
        }
        for (JsonNode r : root.get("rows")) {
          List<Object> row = new ArrayList<>();
          for (int i = 0; i < columns.size(); i++) {
            JsonNode cell = r.get(i);
            row.add(jsonToJava(cell));
          }
          rows.add(row);
        }
        return buildRowSet(columns, rows);
      }

      JsonNode dataNode = root.isArray() ? root : root.get("data");
      if (dataNode != null && dataNode.isArray()) {
        Set<String> cols = new LinkedHashSet<>();
        for (JsonNode obj : dataNode) {
          if (obj.isObject()) {
            Iterator<String> names = obj.fieldNames();
            while (names.hasNext()) cols.add(names.next());
          }
        }
        columns.addAll(cols);
        for (JsonNode obj : dataNode) {
          List<Object> row = new ArrayList<>();
          for (String c : columns) {
            JsonNode cell = obj.get(c);
            row.add(jsonToJava(cell));
          }
          rows.add(row);
        }
        return buildRowSet(columns, rows);
      }

      return buildRowSet(columns, rows);
    } catch (IOException e) {
      throw new SQLException("Failed to parse JSON", e);
    }
  }

  private ResultSet buildRowSet(List<String> columns, List<List<Object>> rows) throws SQLException {
    CachedRowSet crs = RowSetProvider.newFactory().createCachedRowSet();
    RowSetMetaDataImpl md = new RowSetMetaDataImpl();
    // Fallback: DBeaver doesn't accept ResultSet without columns. If empty, create a generic
    // column.
    if (columns == null || columns.isEmpty()) {
      columns = new ArrayList<>();
      columns.add("result");
    }
    if (rows == null) {
      rows = new ArrayList<>();
    }
    md.setColumnCount(columns.size());
    // Infer column types from first non-null value per column
    int[] colTypes = new int[columns.size()];
    for (int i = 0; i < columns.size(); i++) {
      colTypes[i] = inferSqlType(findFirstNonNull(rows, i));
    }
    for (int i = 0; i < columns.size(); i++) {
      md.setColumnName(i + 1, columns.get(i));
      md.setColumnType(i + 1, colTypes[i]);
      md.setNullable(i + 1, ResultSetMetaData.columnNullable);
    }
    crs.setMetaData(md);
    for (int r = rows.size() - 1; r >= 0; r--) {
      List<Object> row = rows.get(r);
      crs.moveToInsertRow();
      for (int i = 0; i < columns.size(); i++) {
        crs.updateObject(i + 1, i < row.size() ? row.get(i) : null);
      }
      crs.insertRow();
      crs.moveToCurrentRow();
    }
    crs.beforeFirst();
    return crs;
  }

  private Object findFirstNonNull(List<List<Object>> rows, int columnIndex) {
    for (List<Object> row : rows) {
      if (columnIndex < row.size()) {
        Object v = row.get(columnIndex);
        if (v != null) return v;
      }
    }
    return null;
  }

  private int inferSqlType(Object value) {
    if (value == null) return Types.VARCHAR;
    if (value instanceof Integer) return Types.INTEGER;
    if (value instanceof Long) return Types.BIGINT;
    if (value instanceof java.math.BigDecimal) return Types.DECIMAL;
    if (value instanceof Boolean) return Types.BOOLEAN;
    if (value instanceof Double || value instanceof Float) return Types.DOUBLE;
    return Types.VARCHAR;
  }

  private Object jsonToJava(JsonNode node) {
    if (node == null || node.isNull()) return null;
    if (node.isBoolean()) return node.booleanValue();
    if (node.isInt()) return node.intValue();
    if (node.isLong()) return node.longValue();
    if (node.isFloat() || node.isDouble() || node.isBigDecimal()) return node.decimalValue();
    if (node.isTextual()) return node.asText();
    return node.toString();
  }

  @Override
  public int executeUpdate(String sql) throws SQLException {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void close() {
    this.closed = true;
    try {
      if (lastResultSet != null) {
        lastResultSet.close();
      }
    } catch (SQLException ignore) {
    } finally {
      lastResultSet = null;
    }
  }

  @Override
  public int getMaxFieldSize() {
    return 0;
  }

  @Override
  public void setMaxFieldSize(int max) {}

  @Override
  public int getMaxRows() {
    return 0;
  }

  @Override
  public void setMaxRows(int max) {}

  @Override
  public void setEscapeProcessing(boolean enable) {}

  @Override
  public int getQueryTimeout() {
    return 0;
  }

  @Override
  public void setQueryTimeout(int seconds) {}

  @Override
  public void cancel() {}

  @Override
  public SQLWarning getWarnings() {
    return null;
  }

  @Override
  public void clearWarnings() {}

  @Override
  public void setCursorName(String name) {}

  @Override
  public boolean execute(String sql) throws SQLException {
    this.lastResultSet = executeQuery(sql);
    return true;
  }

  @Override
  public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
    return execute(sql);
  }

  @Override
  public boolean execute(String sql, int[] columnIndexes) throws SQLException {
    return execute(sql);
  }

  @Override
  public boolean execute(String sql, String[] columnNames) throws SQLException {
    return execute(sql);
  }

  @Override
  public ResultSet getResultSet() {
    return lastResultSet;
  }

  @Override
  public int getUpdateCount() {
    return -1;
  }

  @Override
  public boolean getMoreResults() {
    return false;
  }

  @Override
  public void setFetchDirection(int direction) {}

  @Override
  public int getFetchDirection() {
    return ResultSet.FETCH_FORWARD;
  }

  @Override
  public void setFetchSize(int rows) {
    this.fetchSize = rows;
  }

  @Override
  public int getFetchSize() {
    return fetchSize;
  }

  @Override
  public int getResultSetConcurrency() {
    return ResultSet.CONCUR_READ_ONLY;
  }

  @Override
  public int getResultSetType() {
    return ResultSet.TYPE_FORWARD_ONLY;
  }

  @Override
  public void addBatch(String sql) throws SQLException {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void clearBatch() {}

  @Override
  public int[] executeBatch() throws SQLException {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public Connection getConnection() {
    return connection;
  }

  @Override
  public boolean getMoreResults(int current) {
    return false;
  }

  @Override
  public ResultSet getGeneratedKeys() throws SQLException {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public int executeUpdate(String sql, String[] columnNames) throws SQLException {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public int getResultSetHoldability() {
    return ResultSet.CLOSE_CURSORS_AT_COMMIT;
  }

  @Override
  public boolean isClosed() {
    return closed;
  }

  @Override
  public void setPoolable(boolean poolable) {}

  @Override
  public boolean isPoolable() {
    return false;
  }

  @Override
  public void closeOnCompletion() {}

  @Override
  public boolean isCloseOnCompletion() {
    return false;
  }

  @Override
  public <T> T unwrap(Class<T> iface) throws SQLException {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public boolean isWrapperFor(Class<?> iface) {
    return false;
  }

  private void ensureOpen() throws SQLException {
    if (closed) throw new SQLException("Statement is closed");
  }
}
