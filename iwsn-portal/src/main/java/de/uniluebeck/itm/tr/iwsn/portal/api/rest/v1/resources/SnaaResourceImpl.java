package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.resources;

import com.google.inject.Inject;
import de.uniluebeck.itm.tr.common.ServedNodeUrnPrefixesProvider;
import de.uniluebeck.itm.tr.iwsn.common.Base64Helper;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto.LoginData;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto.SnaaSecretAuthenticationKeyList;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.exceptions.NotLoggedInException;
import eu.wisebed.api.v3.common.SecretAuthenticationKey;
import eu.wisebed.api.v3.snaa.*;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;
import java.util.Iterator;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static de.uniluebeck.itm.tr.iwsn.common.json.JSONHelper.toJSON;
import static de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.resources.ResourceHelper.getSAKsFromCookie;
import static de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.resources.ResourceHelper.getSAKsFromCookieOrHeader;


@Path("/auth/")
public class SnaaResourceImpl implements SnaaResource {

	private final SNAA snaa;

	private final ServedNodeUrnPrefixesProvider servedNodeUrnPrefixesProvider;

	@Context
	private HttpHeaders httpHeaders;

	@Inject
	public SnaaResourceImpl(final SNAA snaa, final ServedNodeUrnPrefixesProvider servedNodeUrnPrefixesProvider) {
		this.snaa = snaa;
		this.servedNodeUrnPrefixesProvider = servedNodeUrnPrefixesProvider;
	}

	@Override
	@GET
	@Path("isLoggedIn")
	public Response isLoggedIn() throws SNAAFault_Exception {

		final List<SecretAuthenticationKey> secretAuthenticationKeys;
		try {

			secretAuthenticationKeys = getSAKsFromCookieOrHeader(httpHeaders, snaa);

			if (secretAuthenticationKeys == null) {
				return Response.status(Status.FORBIDDEN).build();
			}
		} catch (NotLoggedInException e) {
			return Response.status(Status.FORBIDDEN).build();
		} catch (eu.wisebed.api.v3.rs.AuthenticationFault authenticationFault) {
			return Response.status(Status.FORBIDDEN).build();
		}

		// only look at the relevant secret authentication keys as the user might provide additional keys for other
		// testbeds that are stored in his cookies. we don't want him to fail because of that.

		final List<SecretAuthenticationKey> relevantSAKs = newArrayList();

		for (SecretAuthenticationKey sak : secretAuthenticationKeys) {
			if (servedNodeUrnPrefixesProvider.get().contains(sak.getUrnPrefix())) {
				relevantSAKs.add(sak);
			}
		}

		if (relevantSAKs.isEmpty()) {
			return Response.status(Status.FORBIDDEN).build();
		}

		try {
			for (ValidationResult validationResult : snaa.isValid(relevantSAKs)) {
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

		final List<SecretAuthenticationKey> saKsFromCookie = getSAKsFromCookie(httpHeaders);
		final Authenticate authenticate = new Authenticate().withAuthenticationData(loginData.authenticationData);
		final List<SecretAuthenticationKey> saks = snaa.authenticate(authenticate).getSecretAuthenticationKey();

		// when creating a new cookie make sure that SecretAuthenticationKey instances for other testbeds on the same
		// host are still contained within the resulting cookie after we add our SecretAuthenticationKey

		if (saKsFromCookie != null) {

			for (SecretAuthenticationKey cookieSAK : saKsFromCookie) {
				boolean alreadyIn = false;
				for (SecretAuthenticationKey sak : saks) {
					if (sak.getUrnPrefix().equals(cookieSAK.getUrnPrefix())) {
						alreadyIn = true;
					}
				}
				if (!alreadyIn) {
					saks.add(cookieSAK);
				}
			}
		}

		final SnaaSecretAuthenticationKeyList sakList = new SnaaSecretAuthenticationKeyList(saks);
		return Response
				.ok(sakList)
				.cookie(createCookie(sakList, "", false))
				.build();
	}

	@Override
	@GET
	@Path("logout")
	public Response deleteCookie() {

		// when users logs out only delete SAKs from this testbed (if multiple testbeds running on the same domain)
		final List<SecretAuthenticationKey> saKsFromCookie = getSAKsFromCookie(httpHeaders);

		for (Iterator<SecretAuthenticationKey> it = saKsFromCookie.iterator(); it.hasNext(); ) {
			SecretAuthenticationKey next = it.next();
			if (servedNodeUrnPrefixesProvider.get().contains(next.getUrnPrefix())) {
				it.remove();
			}
		}

		final SnaaSecretAuthenticationKeyList sakList = saKsFromCookie.size() == 0 ?
				null :
				new SnaaSecretAuthenticationKeyList(saKsFromCookie);

		return Response
				.ok()
				.cookie(createCookie(sakList, "", saKsFromCookie.size() == 0))
				.build();
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
