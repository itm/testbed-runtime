package de.uniluebeck.itm.tr.iwsn.gateway.rest;

import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.gateway.DeviceAdapter;
import de.uniluebeck.itm.tr.iwsn.gateway.DeviceManager;
import de.uniluebeck.itm.tr.iwsn.gateway.rest.dto.DeviceList;
import de.uniluebeck.itm.tr.iwsn.gateway.rest.dto.DeviceState;
import eu.wisebed.api.v3.common.NodeUrn;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Set;

import static com.google.common.base.Functions.toStringFunction;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;

@Path("/")
public class RestService {

	private final DeviceManager deviceManager;

	@Inject
	public RestService(final DeviceManager deviceManager) {
		this.deviceManager = deviceManager;
	}

	@GET
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	public DeviceList getDevices() {
		return new DeviceList(newArrayList(transform(
				deviceManager.getConnectedNodeUrns(),
				toStringFunction()
		)));
	}

	@GET
	@Path("{nodeUrn}")
	public Response getDeviceState(@PathParam("nodeUrn") String nodeUrnString) throws Exception {

		final NodeUrn nodeUrn = new NodeUrn(nodeUrnString);
		final DeviceAdapter deviceAdapter = deviceManager.getDeviceAdapter(nodeUrn);

		if (deviceAdapter == null) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}

		final Set<NodeUrn> nodeUrnSet = newHashSet(nodeUrn);
		return Response.ok(new DeviceState(
				deviceAdapter.areNodesAlive(nodeUrnSet).get(nodeUrn).get(),
				deviceAdapter.areNodesConnected(nodeUrnSet).get(nodeUrn).get()
		)).build();
	}

	@GET
	@Path("{nodeUrn}/reset")
	public Response reset(@PathParam("nodeUrn") String nodeUrnString) throws Exception {

		final NodeUrn nodeUrn = new NodeUrn(nodeUrnString);
		final DeviceAdapter deviceAdapter = deviceManager.getDeviceAdapter(nodeUrn);

		if (deviceAdapter == null) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}

		final Set<NodeUrn> nodeUrnSet = newHashSet(nodeUrn);
		try {
			deviceAdapter.resetNodes(nodeUrnSet).get(nodeUrn).get();
			return Response.ok().build();
		} catch (Exception e) {
			return Response.serverError().build();
		}
	}

}
