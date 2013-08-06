/*******************************************************************************
* Copyright (c) 2013 CEA LIST.
* Contributor:
*   ROUX Pierre
*   Kim Thuat NGUYEN
* 
* Java class for authenticating user based on the use of certificate
*******************************************************************************/

package eu.smartsantander.cea.certificate;

import java.security.cert.X509Certificate;
import org.apache.shiro.authc.AuthenticationToken;

/*******************************************************************************
* Copyright (c) 2013 CEA LIST.
* Contributor:
*   ROUX Pierre
*   Kim Thuat NGUYEN
*******************************************************************************/

public interface X509CertificateAuthenticationToken extends AuthenticationToken {
    /**
     *Return certificate presented by user
     * @return X509 certificate
     * 
     */
    public abstract X509Certificate getCertificate();
}
