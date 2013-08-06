/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.smartsantander.cea.certificate;

import java.security.cert.X509Certificate;



/*******************************************************************************
* Copyright (c) 2013 CEA LIST.
* Contributor:
*   ROUX Pierre
*   Kim Thuat NGUYEN
*******************************************************************************/

public class X509CertificateOnlyAuthenticationToken implements X509CertificateAuthenticationToken {

    private String userName;
    private X509Certificate certificate;
    private boolean rememberMe;

    public X509CertificateOnlyAuthenticationToken(String userName, X509Certificate certificate) {
        this.userName = userName;
        this.certificate = certificate;
        this.rememberMe = false;
    }
            
    
    @Override
    public Object getPrincipal() {
        return getUserName();
    }

    @Override
    public Object getCredentials() {
        return getCertificate();
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Override
    public X509Certificate getCertificate() {
        return certificate;
    }

    public void setCertificate(X509Certificate certificate) {
        this.certificate = certificate;
    }

    public boolean isRememberMe() {
        return rememberMe;
    }

    public void setRememberMe(boolean rememberMe) {
        this.rememberMe = rememberMe;
    }
    
}
