package de.uniluebeck.itm.tr.devicedb;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.ok;

@Path("/")
public class DeviceDBRestResource {

	@GET
	public Response get() {
		return ok("Hello, World").build();
	}
}
