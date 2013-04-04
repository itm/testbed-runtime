package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.resources;

import com.google.inject.Inject;
import eu.wisebed.api.snaa.*;
import eu.wisebed.restws.dto.LoginData;
import eu.wisebed.restws.dto.SnaaSecretAuthenticationKeyList;
import eu.wisebed.restws.proxy.WebServiceEndpointManager;
import eu.wisebed.restws.util.Base64Helper;
import eu.wisebed.restws.util.InjectLogger;
import org.slf4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;
import java.util.LinkedList;
import java.util.List;

import static eu.wisebed.restws.resources.ResourceHelper.createSecretAuthenticationKeyCookieName;
import static eu.wisebed.restws.resources.ResourceHelper.getSnaaSecretAuthCookie;
import static eu.wisebed.restws.util.JSONHelper.toJSON;

@Path("/" + Constants.WISEBED_API_VERSION + "/{testbedId}/")
public class SnaaResource {

	@InjectLogger
	private Logger log;

	@Inject
	private WebServiceEndpointManager endpointManager;

	@Context
	private HttpHeaders httpHeaders;

	@GET
	@Path("isLoggedIn")
	public Response isLoggedIn(@PathParam("testbedId") final String testbedId) {

		SnaaSecretAuthenticationKeyList cookie = getSnaaSecretAuthCookie(httpHeaders, testbedId);
		SNAA snaa = endpointManager.getSnaaEndpoint(testbedId);

		try {

			boolean isLoggedIn = snaa.isAuthorized(cookie.secretAuthenticationKeys, new Action("isLoggedIn"));
			return isLoggedIn ? Response.ok().build() : Response.status(Status.FORBIDDEN).build();

		} catch (SNAAExceptionException e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e).build();
		}
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
	 * @param testbedId
	 *            the ID of the testbed
	 * @param loginData
	 *            login data
	 *
	 * @return a response
	 */
	@POST
	@Produces({ MediaType.APPLICATION_JSON })
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("login")
	public Response login(@PathParam("testbedId") final String testbedId, final LoginData loginData) {

		List<SecretAuthenticationKey> secretAuthenticationKeys;

		try {

			SNAA snaa = endpointManager.getSnaaEndpoint(testbedId);

			secretAuthenticationKeys = snaa.authenticate(loginData.authenticationData);
			SnaaSecretAuthenticationKeyList loginResult = new SnaaSecretAuthenticationKeyList(secretAuthenticationKeys);
			String jsonResponse = toJSON(loginResult);

			List<NewCookie> cookies = new LinkedList<NewCookie>();
			cookies.add(createCookie(testbedId, loginResult, "", false));

			log.trace("Received {}, returning {}", toJSON(loginData), jsonResponse);
			return Response.ok(jsonResponse).cookie(cookies.toArray(new NewCookie[cookies.size()])).build();

		} catch (AuthenticationExceptionException e) {
			return createLoginErrorResponse(e);
		} catch (SNAAExceptionException e) {
			return createLoginErrorResponse(e);
		}

	}

	@GET
	@Path("logout")
	public Response deleteCookie(@PathParam("testbedId") final String testbedId) {
		return Response.ok().cookie(createCookie(testbedId, null, "", true)).build();
	}

	private NewCookie createCookie(final String testbedId, SnaaSecretAuthenticationKeyList loginData, String domain, boolean isReset) {
		int maxAge = isReset ? 0 : 60 * 60 * 24;
		boolean secure = false;
		String comment = "";
		String value = (loginData == null) ? "" : Base64Helper.encode(toJSON(loginData));
		String name = createSecretAuthenticationKeyCookieName(testbedId);
		String path = "/";

		return new NewCookie(name, value, path, domain, comment, maxAge, secure);
	}

	private Response createLoginErrorResponse(Exception e) {
		log.debug("Login failed :" + e, e);
		String errorMessage = String.format("Login failed: %s (%s)", e, e.getMessage());
		return Response.status(Status.FORBIDDEN).entity(errorMessage).build();
	}

}
