/*******************************************************************************
 * Copyright (c) 2013 CEA LIST.
 * Contributor:
 *   ROUX Pierre
 *   Kim Thuat NGUYEN
 ********************************************************************************/

package eu.smartsantander.cea.certificate;


import com.google.inject.Inject;
import de.uniluebeck.itm.tr.snaa.shiro.entity.Permission;
import de.uniluebeck.itm.tr.snaa.shiro.entity.Role;
import de.uniluebeck.itm.tr.snaa.shiro.entity.UsersCert;
import eu.smartsantander.cea.utils.Helper.HelperUtilities;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.subject.PrincipalCollection;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.*;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;


public class X509CertificateRealm extends AuthorizingRealm implements Realm {

	@Inject
	UserCertDao userCertDao;

	@Override
	protected SimpleAuthenticationInfo doGetAuthenticationInfo(AuthenticationToken at) throws AuthenticationException {
		X509CertificateOnlyAuthenticationToken token = (X509CertificateOnlyAuthenticationToken) at;
		UsersCert userCert = userCertDao.find(token.getUserName());
		X509Certificate certificate = token.getCertificate();
		// Verify the certificate of users: if the certificate is in the truststore
		if (!isCertificateOK(certificate)) {
			return null;
		}

		if (userCert != null && userCert.getName() != null && userCert.getOrganization().getName() != null) {
			return new SimpleAuthenticationInfo(userCert.getName(), token.getCertificate(), getName());
		}
		return null;
	}


	@Override
	protected SimpleAuthorizationInfo doGetAuthorizationInfo(PrincipalCollection pc) {
		String userId = (String) pc.fromRealm(getName()).iterator().next();
		UsersCert userCert = userCertDao.find(userId);
		if (userCert != null) {
			SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
			for (Role role : userCert.getRoles()) {
				info.addRole(role.getName());
				Set<Permission> permissions = role.getPermissions();
				Set<String> strPerm = toString(permissions);
				info.addStringPermissions(strPerm);
			}
			return info;
		}

		return null;
	}

	@Override
	public boolean supports(AuthenticationToken token) {
		if (token != null) {
			return token instanceof X509CertificateAuthenticationToken;
		}

		return false;
	}

	// Verify if the certificate is trusted or not
	public boolean isCertificateOK(X509Certificate certificate) {
		if (certificate == null) {
			return false;
		}
		try {
			FileInputStream in;
			if (HelperUtilities.isWindows()) {
				in = new FileInputStream(SNAACertificateConfig.PATH_TO_TRUST_STORE_WINDOWS);
			} else {
				in = new FileInputStream(SNAACertificateConfig.PATH_TO_TRUST_STORE_LINUX);
			}
			KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
			keyStore.load(in, SNAACertificateConfig.TRUST_STORE_PW.toCharArray());

			X509CertSelector target = new X509CertSelector();
			target.setCertificate(certificate);
			PKIXBuilderParameters parameters = new PKIXBuilderParameters(keyStore, target);

			// TODO: verify if the certificate is in the revocaton list
			// For instance, we instanciated an empty list
			Collection<X509CRL> crls = Collections.emptyList();
			CertStoreParameters revoked = new CollectionCertStoreParameters(crls);
			parameters.addCertStore(CertStore.getInstance("Collection", revoked));

			// If build() returns successfully, the certificate is valid.
			@SuppressWarnings("unused")
			CertPathBuilderResult r = CertPathBuilder.getInstance("PKIX").build(parameters);

			System.out.println("Certificate is validated sucessfully");
			return true;
		} catch (FileNotFoundException ex) {
			Logger.getLogger(X509CertificateRealm.class.getName()).log(Level.SEVERE, "Certificate validation failed", ex);
		} catch (KeyStoreException ex) {
			Logger.getLogger(X509CertificateRealm.class.getName()).log(Level.SEVERE, "Certificate validation failed", ex);
		} catch (IOException ex) {
			Logger.getLogger(X509CertificateRealm.class.getName()).log(Level.SEVERE, "Certificate validation failed", ex);
		} catch (NoSuchAlgorithmException ex) {
			Logger.getLogger(X509CertificateRealm.class.getName()).log(Level.SEVERE, "Certificate validation failed", ex);
		} catch (CertificateException ex) {
			Logger.getLogger(X509CertificateRealm.class.getName()).log(Level.SEVERE, "Certificate validation failed", ex);
		} catch (InvalidAlgorithmParameterException ex) {
			Logger.getLogger(X509CertificateRealm.class.getName()).log(Level.SEVERE, "Certificate validation failed", ex);
		} catch (CertPathBuilderException ex) {
			Logger.getLogger(X509CertificateRealm.class.getName()).log(Level.SEVERE, "Certificate validation failed", ex);
		}
		return false;
	}


	// ------------------------------------------------------------------------

	/**
	 * Converts a set of {@link Permission} objects into a set of Strings and returns the result.
	 *
	 * @param permissionses
	 * 		A set of persisted permission objects which indicate which action is allowed for which resource groups
	 * @return A set of permission stings which indicate which action is allowed for which resource groups
	 */
	private Set<String> toString(final Set<Permission> permissionses) {
		Set<String> result = new HashSet<String>();
		for (Permission permissions : permissionses) {
			result.add(permissions.getAction().getName() + ":" + permissions.getResourceGroup().getName());
		}
		return result;
	}


	public boolean doesUserExist(final String userId) {
		return userCertDao.find(userId) != null;
	}
}
