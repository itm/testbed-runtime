/*******************************************************************************
* Copyright (c) 2013 CEA LIST.
* Contributor:
*   ROUX Pierre
*   Kim Thuat NGUYEN
*******************************************************************************/
package eu.smartsantander.cea.utils.saml;

import java.util.Map;

public class SAMLInput {
    
    private String issuer;
    private String nameID;
    private String nameQualifier;
    private String sessionID;
    private int maxSessionTimeout; // defaut in minutes
    
    private Map attributes;

    public SAMLInput(String issuer, String nameID, String nameQualifier, String sessionID, int maxSessionTimeout, Map attributes) {
        this.issuer = issuer;
        this.nameID = nameID;
        this.nameQualifier = nameQualifier;
        this.sessionID = sessionID;
        this.maxSessionTimeout = maxSessionTimeout;
        this.attributes = attributes;
    }

        
    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getNameID() {
        return nameID;
    }

    public void setNameID(String nameID) {
        this.nameID = nameID;
    }

    public String getNameQualifier() {
        return nameQualifier;
    }

    public void setNameQualifier(String nameQualifier) {
        this.nameQualifier = nameQualifier;
    }

    public String getSessionID() {
        return sessionID;
    }

    public void setSessionID(String sessionID) {
        this.sessionID = sessionID;
    }

    public int getMaxSessionTimeout() {
        return maxSessionTimeout;
    }

    public void setMaxSessionTimeout(int maxSessionTimeout) {
        this.maxSessionTimeout = maxSessionTimeout;
    }

    
    public Map getAttributes() {
        return attributes;
    }

    public void setAttributes(Map attributes) {
        this.attributes = attributes;
    }
}
