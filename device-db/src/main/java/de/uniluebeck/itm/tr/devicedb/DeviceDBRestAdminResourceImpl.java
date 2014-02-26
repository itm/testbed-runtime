package de.uniluebeck.itm.tr.devicedb;

import com.google.inject.Inject;
import de.uniluebeck.itm.tr.common.dto.DeviceConfigDto;
import eu.wisebed.api.v3.common.NodeUrn;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;

public class DeviceDBRestAdminResourceImpl implements DeviceDBRestAdminResource {

	private final DeviceDBService deviceDBService;

	@Context
	private UriInfo uriInfo;

	@Inject
	public DeviceDBRestAdminResourceImpl(final DeviceDBService deviceDBService) {
		this.deviceDBService = deviceDBService;
	}

	@Override
	public Response add(final DeviceConfigDto deviceConfig, String nodeUrnString) {

		if (deviceConfig.getNodeUrn() == null || !deviceConfig.getNodeUrn().equalsIgnoreCase(nodeUrnString)) {
			return Response
					.status(Response.Status.BAD_REQUEST)
					.entity("Node URN encoded in request (\"" + nodeUrnString + "\") does not match node URN in entity (\"" + deviceConfig
							.getNodeUrn() + "\")!"
					)
					.build();
		}

		try {
			deviceDBService.add(DeviceConfigHelper.fromDto(deviceConfig));
		} catch (Exception e) {
			e.printStackTrace();
			return Response.serverError().entity(e.getMessage()).build();
		}
		final URI location = UriBuilder.fromUri(uriInfo.getBaseUri()).fragment(deviceConfig.getNodeUrn()).build();
		return Response.created(location).entity("true").build();
	}

	@Override
	public Response update(final DeviceConfigDto deviceConfig, String nodeUrnString) {

		if (!deviceConfig.getNodeUrn().equalsIgnoreCase(nodeUrnString)) {
			return Response.status(Response.Status.BAD_REQUEST).build();
		}

		// check if Entity to update already exists
		if (deviceDBService.getConfigByNodeUrn(new NodeUrn(deviceConfig.getNodeUrn())) != null) {
			deviceDBService.update(DeviceConfigHelper.fromDto(deviceConfig));
		} else {
			return Response.notModified().build();
		}

		return Response.ok("true").build();
	}

	@Override
	public Response delete(final String nodeUrnString) {

		boolean ok = deviceDBService.removeByNodeUrn(new NodeUrn(nodeUrnString));

		return ok ? Response.ok("true").build() : Response.notModified().build();
	}

	@Override
	public Response delete(final List<String> nodeUrnStrings) {

		if (nodeUrnStrings.isEmpty()) {

			deviceDBService.removeAll();

		} else {

			for (String nodeUrnString : nodeUrnStrings) {
				deviceDBService.removeByNodeUrn(new NodeUrn(nodeUrnString));
			}

		}

		return Response.ok("true").build();
	}
}
