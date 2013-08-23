/*******************************************************************************
 * Copyright (c) 2013 CEA LIST.
 * Contributor:
 *   ROUX Pierre
 *   Kim Thuat NGUYEN
 *******************************************************************************/

package de.uniluebeck.itm.tr.snaa.certificate;

import org.apache.shiro.authc.AuthenticationToken;

import java.security.cert.X509Certificate;


public interface X509CertificateAuthenticationToken extends AuthenticationToken {
	/**
	 * Return certificate presented by user
	 *
	 * @return X509 certificate
	 */
	public abstract X509Certificate getCertificate();
}
