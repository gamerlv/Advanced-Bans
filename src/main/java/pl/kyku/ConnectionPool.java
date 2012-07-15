package pl.kyku;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

public class ConnectionPool {
	private final Vector<JDCConnection> connections;
	private final String url, user, password;
	private final long timeout = 60000;
	private final ConnectionReaper reaper;
	private final int poolsize = 10;

	public ConnectionPool(String url, String user, String password) throws ClassNotFoundException {
		Class.forName("com.mysql.jdbc.Driver");
		this.url = url;
		this.user = user;
		this.password = password;
		connections = new Vector<JDCConnection>(poolsize);
		reaper = new ConnectionReaper();
		reaper.start();
	}

	public synchronized Connection getConnection() throws SQLException {
		JDCConnection conn;
		for (int i = 0; i < connections.size(); i++) {
			conn = connections.get(i);
			if (conn.lease()) {
				if (conn.isValid())
					return conn;
				else {
					connections.remove(conn);
					conn.terminate();
				}
			}
		}
		conn = new JDCConnection(DriverManager.getConnection(url, user, password));
		conn.lease();
		if (!conn.isValid()) {
			conn.terminate();
			throw new SQLException("Failed to validate a brand new connection");
		}
		connections.add(conn);
		return conn;
	}

	private synchronized void reapConnections() {
		final long stale = System.currentTimeMillis() - timeout;
		for (final JDCConnection conn : connections)
			if (conn.inUse() && stale > conn.getLastUse() && !conn.isValid())
				connections.remove(conn);
	} 

	public synchronized void closeConnections() {
		final Enumeration<JDCConnection> conns = connections.elements();
		while (conns.hasMoreElements()) {
			final JDCConnection conn = conns.nextElement();
			connections.remove(conn);
			conn.terminate();
		}
	}

	private class ConnectionReaper extends Thread
	{
		@Override
		public void run() {
			while (true) {
				try {
					Thread.sleep(300000);
				} catch (final InterruptedException e) {}
				reapConnections();
			}
		}
	}

	private class JDCConnection implements Connection
	{
		private final Connection conn;
		private boolean inuse;
		private long timestamp;

		public JDCConnection(Connection conn) {
			this.conn = conn;
			inuse = false;
			timestamp = 0;
		}

		public void terminate() {
			try {
				conn.close();
			} catch (final SQLException ex) {}
		}

		public synchronized boolean lease() {
			if (inuse)
				return false;
			else {
				inuse = true;
				timestamp = System.currentTimeMillis();
				return true;
			}
		}

		public boolean inUse() {
			return inuse;
		}

		public long getLastUse() {
			return timestamp;
		}

		@Override
		public void close() {
			inuse = false;
			try {
				if (!conn.getAutoCommit())
				conn.setAutoCommit(true);
			} catch (SQLException ex) {
				connections.remove(conn);
				terminate();
			}
		}

		@Override
		public PreparedStatement prepareStatement(String sql) throws SQLException {
			return conn.prepareStatement(sql);
		}

		@Override
		public CallableStatement prepareCall(String sql) throws SQLException {
			return conn.prepareCall(sql);
		}

		@Override
		public Statement createStatement() throws SQLException {
			return conn.createStatement();
		}

		@Override
		public String nativeSQL(String sql) throws SQLException {
			return conn.nativeSQL(sql);
		}

		@Override
		public void setAutoCommit(boolean autoCommit) throws SQLException {
			conn.setAutoCommit(autoCommit);
		}

		@Override
		public boolean getAutoCommit() throws SQLException {
			return conn.getAutoCommit();
		}

		@Override
		public void commit() throws SQLException {
			conn.commit();
		}

		@Override
		public void rollback() throws SQLException {
			conn.rollback();
		}

		@Override
		public boolean isClosed() throws SQLException {
			return conn.isClosed();
		}

		@Override
		public DatabaseMetaData getMetaData() throws SQLException {
			return conn.getMetaData();
		}

		@Override
		public void setReadOnly(boolean readOnly) throws SQLException {
			conn.setReadOnly(readOnly);
		}

		@Override
		public boolean isReadOnly() throws SQLException {
			return conn.isReadOnly();
		}

		@Override
		public void setCatalog(String catalog) throws SQLException {
			conn.setCatalog(catalog);
		}

		@Override
		public String getCatalog() throws SQLException {
			return conn.getCatalog();
		}

		@Override
		public void setTransactionIsolation(int level) throws SQLException {
			conn.setTransactionIsolation(level);
		}

		@Override
		public int getTransactionIsolation() throws SQLException {
			return conn.getTransactionIsolation();
		}

		@Override
		public SQLWarning getWarnings() throws SQLException {
			return conn.getWarnings();
		}

		@Override
		public void clearWarnings() throws SQLException {
			conn.clearWarnings();
		}

		@Override
		public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
			return conn.createArrayOf(typeName, elements);
		}

		@Override
		public Blob createBlob() throws SQLException {
			return conn.createBlob();
		}

		@Override
		public Clob createClob() throws SQLException {
			return conn.createClob();
		}

		@Override
		public NClob createNClob() throws SQLException {
			return conn.createNClob();
		}

		@Override
		public SQLXML createSQLXML() throws SQLException {
			return conn.createSQLXML();
		}

		@Override
		public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
			return conn.createStatement(resultSetType, resultSetConcurrency);
		}

		@Override
		public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
			return conn.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
		}

		@Override
		public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
			return conn.createStruct(typeName, attributes);
		}

		@Override
		public Properties getClientInfo() throws SQLException {
			return conn.getClientInfo();
		}

		@Override
		public String getClientInfo(String name) throws SQLException {
			return conn.getClientInfo(name);
		}

		@Override
		public int getHoldability() throws SQLException {
			return conn.getHoldability();
		}

		@Override
		public Map<String, Class<?>> getTypeMap() throws SQLException {
			return conn.getTypeMap();
		}

		public boolean isValid() {
			try {
				return conn.isValid(1);
			} catch (final SQLException ex) {
				return false;
			}
		}

		@Override
		public boolean isValid(int timeout) throws SQLException {
			return conn.isValid(timeout);
		}

		@Override
		public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
			return conn.prepareCall(sql, resultSetType, resultSetConcurrency);
		}

		@Override
		public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
			return conn.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
		}

		@Override
		public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
			return conn.prepareStatement(sql, autoGeneratedKeys);
		}

		@Override
		public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
			return conn.prepareStatement(sql, columnIndexes);
		}

		@Override
		public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
			return conn.prepareStatement(sql, columnNames);
		}

		@Override
		public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
			return conn.prepareStatement(sql, resultSetType, resultSetConcurrency);
		}

		@Override
		public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
			return conn.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
		}

		@Override
		public void releaseSavepoint(Savepoint savepoint) throws SQLException {
			conn.releaseSavepoint(savepoint);
		}

		@Override
		public void rollback(Savepoint savepoint) throws SQLException {
			conn.rollback(savepoint);
		}

		@Override
		public void setClientInfo(Properties properties) throws SQLClientInfoException {
			conn.setClientInfo(properties);
		}

		@Override
		public void setClientInfo(String name, String value) throws SQLClientInfoException {
			conn.setClientInfo(name, value);
		}

		@Override
		public void setHoldability(int holdability) throws SQLException {
			conn.setHoldability(holdability);
		}

		@Override
		public Savepoint setSavepoint() throws SQLException {
			return conn.setSavepoint();
		}

		@Override
		public Savepoint setSavepoint(String name) throws SQLException {
			return conn.setSavepoint(name);
		}

		@Override
		public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
			conn.setTypeMap(map);
		}

		@Override
		public boolean isWrapperFor(Class<?> iface) throws SQLException {
			return conn.isWrapperFor(iface);
		}

		@Override
		public <T> T unwrap(Class<T> iface) throws SQLException {
			return conn.unwrap(iface);
		}
	}
}