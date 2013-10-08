package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.resources;

import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto.LoginData;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto.SnaaSecretAuthenticationKeyList;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.exceptions.NotLoggedInException;
import de.uniluebeck.itm.tr.iwsn.common.Base64Helper;
import eu.wisebed.api.v3.common.SecretAuthenticationKey;
import eu.wisebed.api.v3.snaa.*;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;
import java.util.List;

import static de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.resources.ResourceHelper.getSAKsFromCookie;
import static de.uniluebeck.itm.tr.iwsn.common.json.JSONHelper.toJSON;


@Path("/auth/")
public class SnaaResourceImpl implements SnaaResource {

	private final SNAA snaa;

	@Context
	private HttpHeaders httpHeaders;

	@Inject
	public SnaaResourceImpl(final SNAA snaa) {
		this.snaa = snaa;
	}

	@Override
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

		try {
			for (ValidationResult validationResult : snaa.isValid(secretAuthenticationKeys)) {
				if (!validationResult.isValid()) {
					return Response.status(Status.FORBIDDEN).build();
				}
			}
		} catch (SNAAFault_Exception e) {
			return Response.status(Status.FORBIDDEN).build();
		}

		return Response.ok().build();
	}

	@Override
	@POST
	@Produces({MediaType.APPLICATION_JSON})
	@Consumes({MediaType.APPLICATION_JSON})
	@Path("login")
	public Response login(final LoginData loginData) throws SNAAFault_Exception, AuthenticationFault {

		final Authenticate authenticate = new Authenticate();
		authenticate.getAuthenticationData().addAll(loginData.authenticationData);
		final SnaaSecretAuthenticationKeyList loginResult = new SnaaSecretAuthenticationKeyList(
				snaa.authenticate(authenticate).getSecretAuthenticationKey()
		);

		return Response
				.ok(loginResult)
				.cookie(createCookie(loginResult, "", false))
				.build();
	}

	@Override
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
		String name = ResourceHelper.COOKIE_SECRET_AUTH_KEY;
		String path = "/";

		return new NewCookie(name, value, path, domain, comment, maxAge, secure);
	}

}
