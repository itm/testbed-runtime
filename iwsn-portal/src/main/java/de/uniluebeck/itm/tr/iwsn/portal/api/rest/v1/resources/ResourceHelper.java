package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.resources;

import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto.SnaaSecretAuthenticationKeyList;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.exceptions.NotLoggedInException;
import eu.wisebed.api.v3.common.SecretAuthenticationKey;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import java.util.List;

import static de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.util.Base64Helper.decode;
import static de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.util.JSONHelper.fromJSON;

public class ResourceHelper {

	public static List<SecretAuthenticationKey> getSAKsFromCookie(final HttpHeaders httpHeaders) {

		try {

			Cookie snaaSecretAuthCookie = httpHeaders.getCookies().get(
					Constants.COOKIE_SECRET_AUTH_KEY
			);

			return snaaSecretAuthCookie == null ?
					null :
					fromJSON(
							decode(snaaSecretAuthCookie.getValue()),
							SnaaSecretAuthenticationKeyList.class
					).secretAuthenticationKeys;

		} catch (Exception e) {
			throw new NotLoggedInException();
		}
	}

	public static List<SecretAuthenticationKey> assertLoggedIn(final HttpHeaders httpHeaders) {
		final List<SecretAuthenticationKey> secretAuthenticationKeys = getSAKsFromCookie(
				httpHeaders
		);
		if (secretAuthenticationKeys == null) {
			throw new NotLoggedInException();
		}
		return secretAuthenticationKeys;
	}
}
