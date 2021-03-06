package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.resources;

import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

@Path("/cookies/")
public class CookieResourceImpl implements CookieResource {

	@Override
	@GET
	@Path("get")
	public Response getCookie() {
		return Response.ok().cookie(new NewCookie("testCookie", "...")).build();
	}

	@Override
	@GET
	@Path("check")
	public Response checkCookie(@CookieParam("testCookie") Cookie cookie) {
		return Response.status(cookie != null ? Status.OK : Status.FORBIDDEN).build();
	}
}