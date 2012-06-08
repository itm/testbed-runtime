/**********************************************************************************************************************
 * Copyright (c) 2010, Institute of Telematics, University of Luebeck                                                 *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or        *
 *   promote products derived from this software without specific prior written permission.                           *
 *                                                                                                                    *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, *
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE      *
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,         *
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE *
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF    *
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY   *
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.                                *
 **********************************************************************************************************************/

package shiro;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.derby.drda.NetworkServerControl;
import org.apache.derby.jdbc.ClientDataSource;
import org.apache.derby.jdbc.EmbeddedDataSource;
import org.apache.derby.tools.ij;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.uniluebeck.itm.tr.snaa.shiro.TRJDBCRealm;
import de.uniluebeck.itm.tr.util.Logging;

/**
 * Creation of a test DB for Shiro JdbcRealm and basic authorization tests. Creation of Derby database based on demo
 * class {@code SimpleApp.java} of Derby project.
 * 
 * @author massel
 */
public class AuthorizationTest {
    static {
        Logging.setDebugLoggingDefaults();
    }

    private static final String EXPERIMENTER1_PASS = "Pass1";
    private static final String EXPERIMENTER1 = "Experimenter1";
    private static final UsernamePasswordToken experimenter_token = new UsernamePasswordToken(EXPERIMENTER1,
            EXPERIMENTER1_PASS);

    private static final String ADMINISTRATOR2_PASS = "Pass2";
    private static final String ADMINISTRATOR2 = "Administrator2";
    private static final UsernamePasswordToken administrator_token = new UsernamePasswordToken(ADMINISTRATOR2,
            ADMINISTRATOR2_PASS);

    private static AuthorizationTest instance;
    private String framework = "embedded";
    private String driver = "org.apache.derby.jdbc.EmbeddedDriver";
    private String protocol = "jdbc:derby:memory:";
    private String dbName = "derbyDB";
    private String dataSourceDbName = "memory:" + dbName;
    private Properties props = new Properties(); // connection properties
    private EmbeddedDataSource dataSource = new EmbeddedDataSource();
    private TRJDBCRealm realm = new TRJDBCRealm();
    private DefaultSecurityManager securityManager;

    @BeforeClass
    public static void setUp() {
        instance = new AuthorizationTest();
        instance.createDB();
        // set up Shiro classes
        instance.dataSource.setDatabaseName(instance.dataSourceDbName);
        instance.realm.setDataSource(instance.dataSource);
        instance.realm.setPermissionsLookupEnabled(true);
        instance.securityManager = new DefaultSecurityManager(instance.realm);
        // Make the SecurityManager instance available to the entire application via static memory:
        SecurityUtils.setSecurityManager(instance.securityManager);
    }

    @Test
    public void simpleSelectTest() throws SQLException {
        Connection conn = DriverManager.getConnection(protocol + dbName, props);
        Statement s = conn.createStatement();
        // If table does not exist or connection fails somehow, test fails due to SQL exception
        ResultSet rs = s.executeQuery("select * from users");
        // Assert that we have at least one user
        assertTrue(rs.next());
    }

    @Test
    public void testAuthentication() throws SQLException {

        Subject currentUser = SecurityUtils.getSubject();

        // login the current to check authentication
        currentUser.login(experimenter_token);
        currentUser.logout();

    }

    @Test
    public void testAuthorizationOKAdmin() throws SQLException {
        Subject currentUser = SecurityUtils.getSubject();

        // login the current to check authentication
        currentUser.login(administrator_token);
        // check permissions, administrator role has *:* so it is allowed for everything
        assertTrue(currentUser.isPermittedAll("l:k:j","lk:j:x"));
        currentUser.logout();
    }

    @Test
    public void testAuthorizationOKExperimenter() throws SQLException {
        Subject currentUser = SecurityUtils.getSubject();
        currentUser.login(experimenter_token);
        // check permissions, administrator role has *:* so it is allowed for everything
        assertTrue(currentUser.isPermittedAll("WSN_FLASH_PROGRAMS:EXPERIMENT_NODES"));
        currentUser.logout();
        
    }

    
    @Test
    public void testAuthorizationNOKExperimenter() throws SQLException {
        Subject currentUser = SecurityUtils.getSubject();

        // login the current to check authentication
        currentUser.login(experimenter_token);
        // check permissions, should not be allowed to flash service nodes
        assertFalse(currentUser.isPermittedAll("WSN_FLASH_PROGRAMS:SERVICE_NODES"));
        currentUser.logout();
    }
    
    
    void createDB() {
        /* load the JDBC driver */
        loadDriver();

        /*
         * We will be using Statement and PreparedStatement objects for executing SQL. These objects, as well as
         * Connections and ResultSets, are resources that should be released explicitly after use, hence the
         * try-catch-finally pattern used below. We are storing the Statement and Prepared statement object references
         * in an array list for convenience.
         */
        // TODO FMA: try to create from within the SQL script and remove conn here
        Connection conn = null;
        try {

            // providing a user name and password is optional in the embedded
            // and derby client frameworks
            // props.put("user", "user1");
            // props.put("password", "user1");

            conn = DriverManager.getConnection(protocol + dbName + ";create=true", props);

            // TODO FMA: change to logging
            System.out.println("Connected to and created database " + dbName);

            ij.main(new String[] { "./src/test/resources/createTestDB.sql" });

            NetworkServerControl server = new NetworkServerControl(InetAddress.getByName("localhost"), 1527);
            server.start(null);

        } catch (SQLException sqle) {
            printSQLException(sqle);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @AfterClass
    public static void tearDown() {

        /*
         * In embedded mode, an application should shut down the database. If the application fails to shut down the
         * database, Derby will not perform a checkpoint when the JVM shuts down. This means that it will take longer to
         * boot (connect to) the database the next time, because Derby needs to perform a recovery operation.
         * 
         * It is also possible to shut down the Derby system/engine, which automatically shuts down all booted
         * databases.
         * 
         * Explicitly shutting down the database or the Derby engine with the connection URL is preferred. This style of
         * shutdown will always throw an SQLException.
         * 
         * Not shutting down when in a client environment, see method Javadoc.
         */

        if (instance.framework.equals("embedded")) {
            try {
                // the shutdown=true attribute shuts down Derby
                DriverManager.getConnection("jdbc:derby:;shutdown=true");

                // To shut down a specific database only, but keep the
                // engine running (for example for connecting to other
                // databases), specify a database in the connection URL:
                // DriverManager.getConnection("jdbc:derby:" + dbName + ";shutdown=true");
            } catch (SQLException se) {
                if (((se.getErrorCode() == 50000) && ("XJ015".equals(se.getSQLState())))) {
                    // we got the expected exception
                    System.out.println("Derby shut down normally");
                    // Note that for single database shutdown, the expected
                    // SQL state is "08006", and the error code is 45000.
                } else {
                    // if the error code or SQLState is different, we have
                    // an unexpected exception (shutdown failed)
                    System.err.println("Derby did not shut down normally");
                    printSQLException(se);
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    /**
     * Loads the appropriate JDBC driver for this environment/framework. For example, if we are in an embedded
     * environment, we load Derby's embedded Driver, <code>org.apache.derby.jdbc.EmbeddedDriver</code>.
     */
    private void loadDriver() {
        /*
         * The JDBC driver is loaded by loading its class. If you are using JDBC 4.0 (Java SE 6) or newer, JDBC drivers
         * may be automatically loaded, making this code optional.
         * 
         * In an embedded environment, this will also start up the Derby engine (though not any databases), since it is
         * not already running. In a client environment, the Derby engine is being run by the network server framework.
         * 
         * In an embedded environment, any static Derby system properties must be set before loading the driver to take
         * effect.
         */
        try {
            Class.forName(driver).newInstance();
            System.out.println("Loaded the appropriate driver");
        } catch (ClassNotFoundException cnfe) {
            System.err.println("\nUnable to load the JDBC driver " + driver);
            System.err.println("Please check your CLASSPATH.");
            cnfe.printStackTrace(System.err);
        } catch (InstantiationException ie) {
            System.err.println("\nUnable to instantiate the JDBC driver " + driver);
            ie.printStackTrace(System.err);
        } catch (IllegalAccessException iae) {
            System.err.println("\nNot allowed to access the JDBC driver " + driver);
            iae.printStackTrace(System.err);
        }
    }

    /**
     * Reports a data verification failure to System.err with the given message.
     * 
     * @param message
     *            A message describing what failed.
     */
    private void reportFailure(String message) {
        System.err.println("\nData verification failed:");
        System.err.println('\t' + message);
    }

    /**
     * Prints details of an SQLException chain to <code>System.err</code>. Details included are SQL State, Error code,
     * Exception message.
     * 
     * @param e
     *            the SQLException from which to print details.
     */
    public static void printSQLException(SQLException e) {
        // Unwraps the entire exception chain to unveil the real cause of the
        // Exception.
        while (e != null) {
            System.err.println("\n----- SQLException -----");
            System.err.println("  SQL State:  " + e.getSQLState());
            System.err.println("  Error Code: " + e.getErrorCode());
            System.err.println("  Message:    " + e.getMessage());
            // for stack traces, refer to derby.log or uncomment this:
            // e.printStackTrace(System.err);
            e = e.getNextException();
        }
    }
}
