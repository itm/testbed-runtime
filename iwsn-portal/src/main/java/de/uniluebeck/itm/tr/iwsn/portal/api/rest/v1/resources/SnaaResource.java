package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.resources;

import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto.LoginData;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto.SnaaSecretAuthenticationKeyList;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.exceptions.NotLoggedInException;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.util.Base64Helper;
import eu.wisebed.api.v3.common.SecretAuthenticationKey;
import eu.wisebed.api.v3.snaa.AuthenticationFault;
import eu.wisebed.api.v3.snaa.SNAA;
import eu.wisebed.api.v3.snaa.SNAAFault_Exception;
import eu.wisebed.api.v3.snaa.ValidationResult;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;
import java.util.List;

import static de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.resources.ResourceHelper.getSAKsFromCookie;
import static de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.util.JSONHelper.toJSON;


@Path("/auth/")
public class SnaaResource {

	private final SNAA snaa;

	@Context
	private HttpHeaders httpHeaders;

	@Inject
	public SnaaResource(final SNAA snaa) {
		this.snaa = snaa;
	}

	@GET
	@Path("isLoggedIn")
	public Response isLoggedIn() throws SNAAFault_Exception {

		final List<SecretAuthenticationKey> secretAuthenticationKeys;
		try {
			secretAuthenticationKeys = getSAKsFromCookie(httpHeaders);
		} catch (NotLoggedInException e) {
			return Response.status(Status.FORBIDDEN).build();
		}

		if (secretAuthenticationKeys == null) {
			return Response.status(Status.FORBIDDEN).build();
		}

		for (ValidationResult validationResult : snaa.isValid(secretAuthenticationKeys)) {
			if (!validationResult.isValid()) {
				return Response.status(Status.FORBIDDEN).build();
			}
		}

		return Response.ok().build();
	}

	/**
	 * loginData example: <code>
	 * {
	 * "authenticationData":
	 * [
	 * {"password":"pass1", "urnPrefix":"urnprefix1", "username":"user1"},
	 * {"password":"pass2", "urnPrefix":"urnprefix2", "username":"user2"}
	 * ]
	 * }
	 * </code>
	 * <p/>
	 * loginResult example: <code>
	 * {
	 * "secretAuthenticationKeys":
	 * [
	 * {"username":"user","secretAuthenticationKey":"verysecret","urnPrefix":"urn"},
	 * {"username":"user","secretAuthenticationKey":"verysecret","urnPrefix":"urn"}
	 * ]
	 * }
	 * </code>
	 *
	 * @param loginData
	 * 		login data
	 *
	 * @return a response
	 */
	@POST
	@Produces({MediaType.APPLICATION_JSON})
	@Consumes({MediaType.APPLICATION_JSON})
	@Path("login")
	public Response login(final LoginData loginData) throws SNAAFault_Exception, AuthenticationFault {

		SnaaSecretAuthenticationKeyList loginResult = new SnaaSecretAuthenticationKeyList(
				snaa.authenticate(loginData.authenticationData)
		);

		return Response
				.ok(loginResult)
				.cookie(createCookie(loginResult, "", false))
				.build();
	}

	@GET
	@Path("logout")
	public Response deleteCookie() {
		return Response.ok().cookie(createCookie(null, "", true)).build();
	}

	private NewCookie createCookie(SnaaSecretAuthenticationKeyList loginData, String domain, boolean isReset) {

		int maxAge = isReset ? 0 : 60 * 60 * 24;
		boolean secure = false;
		String comment = "";
		String value = (loginData == null) ? "" : Base64Helper.encode(toJSON(loginData));
		String name = Constants.COOKIE_SECRET_AUTH_KEY;
		String path = "/";

		return new NewCookie(name, value, path, domain, comment, maxAge, secure);
	}

}
