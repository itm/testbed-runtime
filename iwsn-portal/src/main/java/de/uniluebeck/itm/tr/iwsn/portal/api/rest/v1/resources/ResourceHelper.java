package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.resources;

import eu.wisebed.restws.dto.SnaaSecretAuthenticationKeyList;
import eu.wisebed.restws.exceptions.NotLoggedInException;
import eu.wisebed.restws.util.Base64Helper;
import eu.wisebed.restws.util.JSONHelper;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;

public class ResourceHelper {

	public static String createSecretAuthenticationKeyCookieName(final String testbedId) {
		return Constants.COOKIE_SECRET_AUTH_KEY + "-" + testbedId;
	}

	public static SnaaSecretAuthenticationKeyList getSnaaSecretAuthCookie(final HttpHeaders httpHeaders, final String testbedId) {

		try {

			Cookie snaaSecretAuthCookie = httpHeaders.getCookies().get(
					createSecretAuthenticationKeyCookieName(testbedId)
			);

			return JSONHelper.fromJSON(
					Base64Helper.decode(snaaSecretAuthCookie.getValue()),
					SnaaSecretAuthenticationKeyList.class
			);

		} catch (Exception e) {
			throw new NotLoggedInException(testbedId);
		}
	}
}
