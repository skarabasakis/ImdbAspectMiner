// The MIT License
//
// Copyright (c) 2010 Stelios Karabasakis
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
//
package util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.logging.Level;


/**
 * Utility class that can be used to manage connections and submit queries to a MySQL database via
 * JDBC. Connection info and credentials are statically defined as class members. Queries can either
 * be executed individually (recommended for SELECT type queries) or collected in a query queue and
 * executed as a batch for better performance (recommended for INSERT/UPDATE type queries). More
 * specifically:
 * <ul>
 * <li>To control the execution of an individual SELECT type query, use member functions
 * {@link #openQuery}, {@link #getResults} and {@link #closeQuery} can be used in that order.</li>
 * <li>To create and execute a query batch, use member functions {@link #addQueryToQueue} and
 * {@link #executeQueue}</li>
 * </ul>
 * 
 * @author Stelios Karabasakis
 */
public class DatabaseConnection {
	
	private static String	protocol	= "mysql";
	private static String	host		= "localhost";
	private static String	port		= "3306";
	private static String	schema		= "jmdb";
	private static String	username	= "root";
	private static String	password	= "password"; // Enter your mysql password here
	private Connection		c			= null;
	private Statement		select		= null;
	private Statement		update		= null;
	private ResultSet		rs			= null;
	
	/** Converts Date objects into strings that are SQL-friendly and can be used inside SQL queries */
	public static SimpleDateFormat	databaseDateFormatter	= new SimpleDateFormat("y-M-dd");
	
	/**
	 * Constructor for class DatabaseConnection
	 */
	public DatabaseConnection() throws SQLException
	{
		try {

			// Import MySQL driver in JVM
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			// Create Connection
			c = DriverManager.getConnection(connectionUri(), username, password);
			// Initialize a statement for future use
			update = c.createStatement();

		} catch ( InstantiationException e ) {
			AppLogger.error.log(Level.SEVERE, "Instantiation of the JDBC driver failed. Cannot connect to database.\n"
									+ e.getMessage());
		} catch ( IllegalAccessException e ) {
			AppLogger.error.log(Level.SEVERE, "JDBC driver class is not accessible. Cannot connect to database.\n"
				+ e.getMessage());
		} catch ( ClassNotFoundException e ) {
			AppLogger.error.log(Level.SEVERE,
								"The specified JDBC driver cannot be found. Cannot connect to database.\n"
									+ e.getMessage());

		} catch ( SQLException e ) {
			throw e;
		}
	}
	
	
	/**
	 * Returns a database connection string following the jdbc URI scheme, to be used as an argument
	 * with {@link DriverManager#getConnection}
	 * 
	 * @return A string containing the connection URI for the database
	 */
	private static String connectionUri()
	{
		return "jdbc:" + protocol + "://" + host + ":" + port + "/" + schema + "?rewriteBatchedStatements=true";
	}
	
	/**
	 * Opens (i.e. submits and executes) a query on the connected database
	 * 
	 * @param sql
	 *            The query to be executed
	 * @throws SQLException
	 */
	public void openQuery(String sql) throws SQLException
	{
		try {
			select = c.createStatement();
			rs = select.executeQuery(sql);
		} catch ( SQLException e ) {
			// Closing query
			if (select != null) {
				try {
					select.close();
				} catch ( SQLException e1 ) {
					// No need to do anything, we are throwing an exception anyway
				}
			}

			throw e;
		}
	}
	
	/**
	 * Closes the query that was opened most recently with {@link #openQuery}
	 * 
	 * @throws SQLException
	 */
	public void closeQuery() throws SQLException
	{
		// Closing query
		if (select != null) {
				select.close();
		}
	}
	
	/**
	 * Returns the {@link ResultSet} of the query that was most recently opened with
	 * {@link #openQuery}
	 * 
	 * @return The ResultSet of the most recently opened query
	 */
	public ResultSet getResults()
	{
		return rs;
	}
	
	/**
	 * Adds a query to the end of the batch execution query
	 * 
	 * @param sql
	 *            The query to be added to the queue
	 * @throws SQLException
	 */
	public void addQueryToQueue(String sql) throws SQLException
	{
		update.addBatch(sql);
	}
	
	/**
	 * Executes all queries that are currently collected in the query queue and, upon successful
	 * execution, flushes the queue
	 * 
	 * @throws SQLException
	 */
	public void executeQueue() throws SQLException
	{
		int[] update_counters = update.executeBatch();
		update.close();
		update = c.createStatement();
	}
}
