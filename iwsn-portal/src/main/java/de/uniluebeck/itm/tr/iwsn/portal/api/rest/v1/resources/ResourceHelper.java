package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.resources;

import de.uniluebeck.itm.tr.common.json.JSONHelper;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto.LoginData;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto.SnaaSecretAuthenticationKeyList;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.exceptions.NotLoggedInException;
import eu.wisebed.api.v3.common.SecretAuthenticationKey;
import eu.wisebed.api.v3.rs.AuthenticationFault;
import eu.wisebed.api.v3.snaa.Authenticate;
import eu.wisebed.api.v3.snaa.SNAA;
import eu.wisebed.api.v3.snaa.SNAAFault_Exception;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import java.util.List;

import static de.uniluebeck.itm.tr.common.Base64Helper.decode;
import static de.uniluebeck.itm.tr.common.json.JSONHelper.fromJSON;

public class ResourceHelper {

	private static final Logger log = LoggerFactory.getLogger(ResourceHelper.class);

	public static final String COOKIE_SECRET_AUTH_KEY = "wisebed-secret-authentication-key";

	private static final String HTTP_AUTH_HEADER = "X-WISEBED-Authentication-Triple";

	public static List<SecretAuthenticationKey> getSAKsFromCookieOrHeader(final HttpHeaders httpHeaders, final SNAA snaa)
			throws AuthenticationFault {
		final List<SecretAuthenticationKey> saks = getSAKsFromCookie(httpHeaders);
		return saks != null ? saks : getSAKsFromHeader(httpHeaders, snaa);
	}

	public static List<SecretAuthenticationKey> getSAKsFromHeader(final HttpHeaders httpHeaders, final SNAA snaa) throws
			AuthenticationFault {

		final List<SecretAuthenticationKey> saks;
		final LoginData loginData = getLoginDataFromHeader(httpHeaders);
		if (loginData == null) {
			throw new NotLoggedInException();
		}

		final Authenticate authenticate = new Authenticate().withAuthenticationData(loginData.authenticationData);
		try {
			saks = snaa.authenticate(authenticate).getSecretAuthenticationKey();
		} catch (eu.wisebed.api.v3.snaa.AuthenticationFault authenticationFault) {
			throw new AuthenticationFault(authenticationFault.getMessage(), authenticationFault.getFaultInfo());
		} catch (SNAAFault_Exception e) {
			final eu.wisebed.api.v3.common.AuthenticationFault authenticationFault =
					new eu.wisebed.api.v3.common.AuthenticationFault();
			authenticationFault.setMessage(e.getFaultInfo().getMessage());
			throw new AuthenticationFault(e.getMessage(), authenticationFault);
		}
		return saks;
	}

	public static LoginData getLoginDataFromHeader(final HttpHeaders httpHeaders) {

		// if set this should return a comma-separated list of authentication triples
		// Example: urn:wisebed:tb1:;username1;password1,urn:wisebed:tb2:;username2;password2
		// password are base64-encoded to allow special characters

		final String headerString = httpHeaders.getHeaderString(HTTP_AUTH_HEADER);

		if (headerString == null || "".equals(headerString)) {
			return null;
		}

		try {

			return JSONHelper.fromJSON(headerString, LoginData.class);

		} catch (Exception e) {
			log.warn("Exception trying to get login data from " + HTTP_AUTH_HEADER + " HTTP header: ", e);
			return null;
		}
	}

	public static List<SecretAuthenticationKey> getSAKsFromCookie(final HttpHeaders httpHeaders) {

		try {

			Cookie snaaSecretAuthCookie = httpHeaders.getCookies().get(
					COOKIE_SECRET_AUTH_KEY
			);

			return snaaSecretAuthCookie == null ?
					null :
					fromJSON(
							decode(snaaSecretAuthCookie.getValue()),
							SnaaSecretAuthenticationKeyList.class
					).secretAuthenticationKeys;

		} catch (Exception e) {
			return null;
		}
	}
}
