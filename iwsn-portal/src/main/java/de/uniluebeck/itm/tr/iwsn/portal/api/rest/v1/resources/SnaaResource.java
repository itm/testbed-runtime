package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.resources;

import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto.LoginData;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto.SnaaSecretAuthenticationKeyList;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.util.Base64Helper;
import eu.wisebed.api.v3.common.SecretAuthenticationKey;
import eu.wisebed.api.v3.snaa.Action;
import eu.wisebed.api.v3.snaa.SNAA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;
import java.util.LinkedList;
import java.util.List;

import static de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.resources.ResourceHelper.getSAKsFromCookie;


@Path("/")
public class SnaaResource {

	private static final Logger log = LoggerFactory.getLogger(SnaaResource.class);

	private final SNAA snaa;

	@Context
	private HttpHeaders httpHeaders;

	@Inject
	public SnaaResource(final SNAA snaa) {
		this.snaa = snaa;
	}

	@GET
	@Path("isLoggedIn")
	public Response isLoggedIn() {

		final List<SecretAuthenticationKey> secretAuthenticationKeys = getSAKsFromCookie(httpHeaders);

		try {

			boolean isLoggedIn = snaa.isValid(secretAuthenticationKeys);
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
	 * 		the ID of the testbed
	 * @param loginData
	 * 		login data
	 *
	 * @return a response
	 */
	@POST
	@Produces({MediaType.APPLICATION_JSON})
	@Consumes({MediaType.APPLICATION_JSON})
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

	private NewCookie createCookie(final String testbedId, SnaaSecretAuthenticationKeyList loginData, String domain,
								   boolean isReset) {
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
