/*******************************************************************************
* Copyright (c) 2013 CEA LIST.
* Contributor:
*   ROUX Pierre
*   Kim Thuat NGUYEN
*******************************************************************************/
package eu.smartsantander.cea.organizationservice.dao;


import eu.smartsantander.cea.organizationservice.utilities.HelperUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;


public class ConnnectionBD {

	private static final Logger log = LoggerFactory.getLogger(ConnnectionBD.class);

    private String dbClass = "com.mysql.jdbc.Driver";
    private String dbUrl = HelperUtilities.getProperty("organizationDbUrl");
    private String username = HelperUtilities.getProperty("organizationDB_username");
    private String password = HelperUtilities.getProperty("organizationDB_password");
    
    public Connection getConnection() {
        Connection conn = null;
        try {
            Class.forName(dbClass);
            conn = DriverManager.getConnection(dbUrl, username, password);
        } catch (ClassNotFoundException e) {
            log.error(e.getMessage(),e);
        } catch (SQLException e) {
	        log.error(e.getMessage(),e);
        }
      return conn;
       
    }
    
    public void printAllUsers() {
        try {
            String query = "Select * from USERS";
            Statement statement = getConnection().createStatement();
            ResultSet results = statement.executeQuery(query);
            while (results.next()) {
                String userId = results.getString(1);
                String userRole = results.getString(2);
                String pubKey = results.getString(3);
                log.debug("UserID:"+userId+"   Role:"+userRole+"   pubKey:"+pubKey);
            }
        } catch (Exception e) {
	        log.error(e.getMessage(),e);
        }
    }
    
    public boolean isUserIdExist(String userId) {
        Connection conn = null;
        try {
            String query = "Select userRole from USERS where userId='"+userId+"'";
            conn = getConnection();
            Statement st = conn.createStatement();
            ResultSet results = st.executeQuery(query);
            String userRole = null;
            while (results.next()) {
                userRole = results.getString("userRole");
                if (userRole!=null) return true;
            }
            
        } catch (Exception e) {
	        log.error(e.getMessage(),e);
        }
        log.debug("Userid: "+userId+ "  doesnt exist");
        return false;
    }
    
    public String getUserRole(String userId) {
        Connection conn = null;
        
        try {
            String query = "Select userRole from USERS where userId='"+userId+"'";
            conn = getConnection();
            Statement st = conn.createStatement();
            ResultSet results = st.executeQuery(query);
            String userRole = null;
            while (results.next()) {
                userRole = results.getString("userRole");
                if (userRole!=null) return userRole;
            }
            
        } catch (Exception e) {
	        log.error(e.getMessage(),e);
        }
        
        return null;
                
    }
    
    public static void main(String args[]) {
        ConnnectionBD bd = new ConnnectionBD();
        bd.printAllUsers();
    }
}
