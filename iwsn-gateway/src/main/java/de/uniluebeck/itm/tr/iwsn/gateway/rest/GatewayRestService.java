package de.uniluebeck.itm.tr.iwsn.gateway.rest;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.sun.jersey.api.view.Viewable;
import de.uniluebeck.itm.tr.iwsn.gateway.GatewayDevice;
import de.uniluebeck.itm.tr.iwsn.gateway.GatewayDeviceManager;
import eu.wisebed.api.v3.common.NodeUrn;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
public class GatewayRestService {

	private final GatewayDeviceManager gatewayDeviceManager;

	@Inject
	public GatewayRestService(final GatewayDeviceManager gatewayDeviceManager) {
		this.gatewayDeviceManager = gatewayDeviceManager;
	}

	@GET
	@Produces(MediaType.TEXT_HTML)
	public Viewable getState() {
		return new Viewable("/index.jsp",
				ImmutableMap.of(
						"gatewayDeviceManager", gatewayDeviceManager
				)
		);
	}

	@GET
	@Path("{nodeUrn}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getDeviceState(@PathParam("nodeUrn") String nodeUrnString) throws Exception {

		if ("favicon.ico".equals(nodeUrnString)) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}

		final GatewayDevice device = getGatewayDevice(nodeUrnString);
		if (device == null) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
		return Response.ok(new DeviceState(device.isNodeAlive().get(), device.isNodeConnected().get())).build();
	}

	private GatewayDevice getGatewayDevice(final String nodeUrnString) {
		final NodeUrn nodeUrn = new NodeUrn(nodeUrnString);
		return this.gatewayDeviceManager.getDevice(nodeUrn);
	}

}
