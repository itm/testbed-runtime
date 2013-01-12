package de.uniluebeck.itm.tr.iwsn.gateway.rest;

import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.gateway.GatewayDevice;
import de.uniluebeck.itm.tr.iwsn.gateway.GatewayDeviceManager;
import eu.wisebed.api.v3.common.NodeUrn;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static com.google.common.base.Functions.toStringFunction;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;

@Path("/")
public class GatewayRestService {

	private final GatewayDeviceManager gatewayDeviceManager;

	@Inject
	public GatewayRestService(final GatewayDeviceManager gatewayDeviceManager) {
		this.gatewayDeviceManager = gatewayDeviceManager;
	}

	@GET
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	public DeviceList getDevices() {
		return new DeviceList(newArrayList(transform(gatewayDeviceManager.getDeviceUrns(), toStringFunction())));
	}

	@GET
	@Path("{nodeUrn}")
	public Response getDeviceState(@PathParam("nodeUrn") String nodeUrnString) throws Exception {
		final GatewayDevice device = getGatewayDevice(nodeUrnString);
		if (device == null) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
		return Response.ok(new DeviceState(device.isNodeAlive().get(), device.isNodeConnected().get())).build();
	}

	@GET
	@Path("{nodeUrn}/reset")
	public Response reset(@PathParam("nodeUrn") String nodeUrnString) throws Exception {
		final GatewayDevice device = getGatewayDevice(nodeUrnString);
		if (device == null) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
		device.resetNode().get();
		return Response.ok().build();
	}

	private GatewayDevice getGatewayDevice(final String nodeUrnString) {
		final NodeUrn nodeUrn = new NodeUrn(nodeUrnString);
		return this.gatewayDeviceManager.getDevice(nodeUrn);
	}

}
