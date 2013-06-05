package de.uniluebeck.itm.tr.snaa.shibboleth;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import de.uniluebeck.itm.tr.snaa.SNAAProperties;

import java.sql.*;

public class MySQLConnection {

	private final Connection connection;

	@Inject
	public MySQLConnection(
			@Named(SNAAProperties.SHIBBOLETH_AUTHORIZATION_ATTRIBUTEBASED_DATASOURCE_URL) final String url,
			@Named(SNAAProperties.SHIBBOLETH_AUTHORIZATION_ATTRIBUTEBASED_DATASOURCE_USERNAME) final String user,
			@Named(SNAAProperties.SHIBBOLETH_AUTHORIZATION_ATTRIBUTEBASED_DATASOURCE_PASSWORD) final String password)
			throws ClassNotFoundException, SQLException {
		Class.forName("com.mysql.jdbc.Driver");
		connection = DriverManager.getConnection(url, user, password);
	}

	public ResultSet getQuery(String sql) throws SQLException {
		Statement statement = connection.createStatement();
		return statement.executeQuery(sql);
	}

	/**
	 * Returns integer-value for query. If result is empty a NullPointerException is thrown.
	 *
	 * @param query
	 * 		query to execute
	 * @param column
	 * 		column to read int from
	 *
	 * @return an integer value
	 *
	 * @throws SQLException
	 * @throws NullPointerException
	 */
	public int getSingleInt(String query, String column) throws SQLException, NullPointerException {
		ResultSet rs = getQuery(query);
		if (rs.next()) {
			return rs.getInt(column);
		}
		throw new NullPointerException("Warning: Result of query: " + query + " is empty");
	}

	public void disconnect() throws SQLException {
		connection.close();
	}
}