/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.tr.certificate;

import java.security.cert.X509Certificate;
import org.apache.shiro.authc.AuthenticationToken;

/**
 *
 * 
 */
public interface X509CertificateAuthenticationToken extends AuthenticationToken {
    /**
     *Return certificate presented by user
     * @return X509 certificate
     * 
     */
    public abstract X509Certificate getCertificate();
}
