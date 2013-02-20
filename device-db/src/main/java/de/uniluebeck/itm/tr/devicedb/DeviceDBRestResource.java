package de.uniluebeck.itm.tr.devicedb;

import com.google.inject.Inject;
import de.uniluebeck.itm.tr.devicedb.dto.DeviceConfigDto;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
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
	public List<DeviceConfigDto> get() {
		final List<DeviceConfigDto> list = newArrayList();
		for (DeviceConfig deviceConfig : deviceDB.getAll()) {
			list.add(DeviceConfigDto.fromDeviceConfig(deviceConfig));
		}
		return list;
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response create(final DeviceConfigDto deviceConfig, @Context UriInfo uriInfo) {
		try {
			deviceDB.add(deviceConfig.toDeviceConfig());
			return Response.created(UriBuilder.fromUri(uriInfo.getBaseUri()).build(deviceConfig.getNodeUrn())).build();
		} catch (Exception e) {
			return Response
					.status(Response.Status.PRECONDITION_FAILED)
					.entity("An entry under Node URN \"" + deviceConfig.getNodeUrn() + "\" already exists!")
					.build();
		}
	}
}
