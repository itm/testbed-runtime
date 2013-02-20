package de.uniluebeck.itm.tr.devicedb;

import com.google.inject.Inject;
import de.uniluebeck.itm.tr.devicedb.dto.DeviceConfigDto;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

@Path("/")
public class DeviceDBRestResource {

	private final DeviceDB deviceDB;

	@Inject
	public DeviceDBRestResource(final DeviceDB deviceDB) {
		this.deviceDB = deviceDB;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<DeviceConfigDto> getAll() {
		final List<DeviceConfigDto> list = newArrayList();
		for (DeviceConfig deviceConfig : deviceDB.getAll()) {
			list.add(DeviceConfigDto.fromDeviceConfig(deviceConfig));
		}
		return list;
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response add(final DeviceConfigDto deviceConfig, @Context UriInfo uriInfo) {
		deviceDB.add(deviceConfig.toDeviceConfig());
		final URI location = UriBuilder.fromUri(uriInfo.getBaseUri()).fragment(deviceConfig.getNodeUrn()).build();
		return Response.created(location).build();
	}
}
