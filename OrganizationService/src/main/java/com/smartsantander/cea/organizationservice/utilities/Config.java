/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.smartsantander.cea.organizationservice.utilities;

public class Config {
    
    /*****************************************************
     * SAML Configuration
     ****************************************************/
    public static int SAML_max_session_timeout = Integer.parseInt(HelperUtilities.getProperty("samlMaxSessionTimeout"));
    
    
    /**********************************************************
     * Key store configuration for generating SAML Assertion
     **********************************************************/
    public static String pathToKeyStoreFile = HelperUtilities.getProperty("pathToKeyStoreFile");
    
    public static String keystorePassword = HelperUtilities.getProperty("keystorePassword");
    
    public static String certificateAllias = HelperUtilities.getProperty("certificateAliasName");
    
    
    /*********************************************************
     * Organization DB Configuration
     ********************************************************/
    public static String organizationDBUrl = HelperUtilities.getProperty("organizationDbUrl");
    public static String userName = HelperUtilities.getProperty("organizationDB_username");
    public static String password = HelperUtilities.getProperty("organizationDB_password");
    
    
    /*****************************************************************
     * The directory name that we use to stock the client's public key
     *****************************************************************/
    public static String CLIENT_PUBKEY_DIRECTORY = HelperUtilities.getProperty("clientPubKeyDirectoryName");
}
