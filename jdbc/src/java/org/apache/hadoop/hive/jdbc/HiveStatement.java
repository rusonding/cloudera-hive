/**
 *
 */
package org.apache.hadoop.hive.jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.util.Vector;
import org.apache.hadoop.hive.service.HiveInterface;

public class HiveStatement implements java.sql.Statement {
  JdbcSessionState session;
  HiveInterface client;
  /**
   *
   */
  public HiveStatement(JdbcSessionState session, HiveInterface client) {
    this.session = session;
    this.client = client;
  }

  /* (non-Javadoc)
   * @see java.sql.Statement#addBatch(java.lang.String)
   */

  public void addBatch(String sql) throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLException("Method not supported");
  }

  /* (non-Javadoc)
   * @see java.sql.Statement#cancel()
   */

  public void cancel() throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLException("Method not supported");
  }

  /* (non-Javadoc)
   * @see java.sql.Statement#clearBatch()
   */

  public void clearBatch() throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLException("Method not supported");
  }

  /* (non-Javadoc)
   * @see java.sql.Statement#clearWarnings()
   */

  public void clearWarnings() throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLException("Method not supported");
  }

  /* (non-Javadoc)
   * @see java.sql.Statement#close()
   */

  public void close() throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLException("Method not supported");
  }

  /* (non-Javadoc)
   * @see java.sql.Statement#execute(java.lang.String)
   */

  public boolean execute(String sql) throws SQLException {
    return true;
  }

  /* (non-Javadoc)
   * @see java.sql.Statement#execute(java.lang.String, int)
   */

  public boolean execute(String sql, int autoGeneratedKeys)
      throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLException("Method not supported");
  }

  /* (non-Javadoc)
   * @see java.sql.Statement#execute(java.lang.String, int[])
   */

  public boolean execute(String sql, int[] columnIndexes) throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLException("Method not supported");
  }

  /* (non-Javadoc)
   * @see java.sql.Statement#execute(java.lang.String, java.lang.String[])
   */

  public boolean execute(String sql, String[] columnNames)
      throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLException("Method not supported");
  }

  /* (non-Javadoc)
   * @see java.sql.Statement#executeBatch()
   */

  public int[] executeBatch() throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLException("Method not supported");
  }

  /* (non-Javadoc)
   * @see java.sql.Statement#executeQuery(java.lang.String)
   */

  public ResultSet executeQuery(String sql) throws SQLException {
    try {
      client.execute(sql);
    } catch (Exception ex) {
      throw new SQLException(ex.toString());
    }
    return new HiveResultSet(client);
  }

  /* (non-Javadoc)
   * @see java.sql.Statement#executeUpdate(java.lang.String)
   */

  public int executeUpdate(String sql) throws SQLException {
    try {
      client.execute(sql);
    } catch (Exception ex) {
      throw new SQLException(ex.toString());
    }
    throw new SQLException("Method not supported");
  }

  /* (non-Javadoc)
   * @see java.sql.Statement#executeUpdate(java.lang.String, int)
   */

  public int executeUpdate(String sql, int autoGeneratedKeys)
      throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLException("Method not supported");
  }

  /* (non-Javadoc)
   * @see java.sql.Statement#executeUpdate(java.lang.String, int[])
   */

  public int executeUpdate(String sql, int[] columnIndexes)
      throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLException("Method not supported");
  }

  /* (non-Javadoc)
   * @see java.sql.Statement#executeUpdate(java.lang.String, java.lang.String[])
   */

  public int executeUpdate(String sql, String[] columnNames)
      throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLException("Method not supported");
  }

  /* (non-Javadoc)
   * @see java.sql.Statement#getConnection()
   */

  public Connection getConnection() throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLException("Method not supported");
  }

  /* (non-Javadoc)
   * @see java.sql.Statement#getFetchDirection()
   */

  public int getFetchDirection() throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLException("Method not supported");
  }

  /* (non-Javadoc)
   * @see java.sql.Statement#getFetchSize()
   */

  public int getFetchSize() throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLException("Method not supported");
  }

  /* (non-Javadoc)
   * @see java.sql.Statement#getGeneratedKeys()
   */

  public ResultSet getGeneratedKeys() throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLException("Method not supported");
  }

  /* (non-Javadoc)
   * @see java.sql.Statement#getMaxFieldSize()
   */

  public int getMaxFieldSize() throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLException("Method not supported");
  }

  /* (non-Javadoc)
   * @see java.sql.Statement#getMaxRows()
   */

  public int getMaxRows() throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLException("Method not supported");
  }

  /* (non-Javadoc)
   * @see java.sql.Statement#getMoreResults()
   */

  public boolean getMoreResults() throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLException("Method not supported");
  }

  /* (non-Javadoc)
   * @see java.sql.Statement#getMoreResults(int)
   */

  public boolean getMoreResults(int current) throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLException("Method not supported");
  }

  /* (non-Javadoc)
   * @see java.sql.Statement#getQueryTimeout()
   */

  public int getQueryTimeout() throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLException("Method not supported");
  }

  /* (non-Javadoc)
   * @see java.sql.Statement#getResultSet()
   */

  public ResultSet getResultSet() throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLException("Method not supported");
  }

  /* (non-Javadoc)
   * @see java.sql.Statement#getResultSetConcurrency()
   */

  public int getResultSetConcurrency() throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLException("Method not supported");
  }

  /* (non-Javadoc)
   * @see java.sql.Statement#getResultSetHoldability()
   */

  public int getResultSetHoldability() throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLException("Method not supported");
  }

  /* (non-Javadoc)
   * @see java.sql.Statement#getResultSetType()
   */

  public int getResultSetType() throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLException("Method not supported");
  }

  /* (non-Javadoc)
   * @see java.sql.Statement#getUpdateCount()
   */

  public int getUpdateCount() throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLException("Method not supported");
  }

  /* (non-Javadoc)
   * @see java.sql.Statement#getWarnings()
   */

  public SQLWarning getWarnings() throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLException("Method not supported");
  }

  /* (non-Javadoc)
   * @see java.sql.Statement#isClosed()
   */

  public boolean isClosed() throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLException("Method not supported");
  }

  /* (non-Javadoc)
   * @see java.sql.Statement#isPoolable()
   */

  public boolean isPoolable() throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLException("Method not supported");
  }

  /* (non-Javadoc)
   * @see java.sql.Statement#setCursorName(java.lang.String)
   */

  public void setCursorName(String name) throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLException("Method not supported");
  }

  /* (non-Javadoc)
   * @see java.sql.Statement#setEscapeProcessing(boolean)
   */

  public void setEscapeProcessing(boolean enable) throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLException("Method not supported");
  }

  /* (non-Javadoc)
   * @see java.sql.Statement#setFetchDirection(int)
   */

  public void setFetchDirection(int direction) throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLException("Method not supported");
  }

  /* (non-Javadoc)
   * @see java.sql.Statement#setFetchSize(int)
   */

  public void setFetchSize(int rows) throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLException("Method not supported");
  }

  /* (non-Javadoc)
   * @see java.sql.Statement#setMaxFieldSize(int)
   */

  public void setMaxFieldSize(int max) throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLException("Method not supported");
  }

  /* (non-Javadoc)
   * @see java.sql.Statement#setMaxRows(int)
   */

  public void setMaxRows(int max) throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLException("Method not supported");
  }

  /* (non-Javadoc)
   * @see java.sql.Statement#setPoolable(boolean)
   */

  public void setPoolable(boolean poolable) throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLException("Method not supported");
  }

  /* (non-Javadoc)
   * @see java.sql.Statement#setQueryTimeout(int)
   */

  public void setQueryTimeout(int seconds) throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLException("Method not supported");
  }

  /* (non-Javadoc)
   * @see java.sql.Wrapper#isWrapperFor(java.lang.Class)
   */

  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLException("Method not supported");
  }

  /* (non-Javadoc)
   * @see java.sql.Wrapper#unwrap(java.lang.Class)
   */

  public <T> T unwrap(Class<T> iface) throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLException("Method not supported");
  }

}
