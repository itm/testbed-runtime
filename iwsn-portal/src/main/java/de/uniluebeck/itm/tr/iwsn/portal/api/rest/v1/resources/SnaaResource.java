package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.resources;

import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto.LoginData;
import eu.wisebed.api.v3.snaa.AuthenticationFault;
import eu.wisebed.api.v3.snaa.SNAAFault_Exception;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public interface SnaaResource {

	@GET
	@Path("isLoggedIn")
	Response isLoggedIn() throws SNAAFault_Exception;

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
	Response login(LoginData loginData) throws SNAAFault_Exception, AuthenticationFault;

	@GET
	@Path("logout")
	Response deleteCookie();
}
