package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.resources;

import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.Response;

public interface CookieResource {

	@GET
	@Path("get")
	Response getCookie();

	@GET
	@Path("check")
	Response checkCookie(@CookieParam("testCookie") Cookie cookie);
}
