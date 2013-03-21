package de.uniluebeck.itm.tr.devicedb;

import de.uniluebeck.itm.tr.devicedb.dto.DeviceConfigDto;
import de.uniluebeck.itm.tr.devicedb.dto.DeviceConfigListDto;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/")
public interface DeviceDBRestResource {

	@GET
	@Path("{a: deviceConfigs/|}{nodeUrn}") // accept "/deviceConfigs/{nodUrn}" or just "/{nodeUrn}"
	@Produces(MediaType.APPLICATION_JSON)
	Response getByNodeUrn(@PathParam("nodeUrn") String nodeUrnString);

	@GET
	@Path("deviceConfigs")
	@Produces(MediaType.APPLICATION_JSON)
	DeviceConfigListDto list();

	@GET
	@Path("byNodeUrn")
	@Produces(MediaType.APPLICATION_JSON)
	DeviceConfigListDto getByNodeUrn(@QueryParam("nodeUrn") List<String> nodeUrnStrings);

	@GET
	@Path("byUsbChipId")
	@Produces(MediaType.APPLICATION_JSON)
	Response getByUsbChipId(@QueryParam("usbChipId") String usbChipIds);

	@GET
	@Path("byMacAddress")
	@Produces(MediaType.APPLICATION_JSON)
	Response getByMacAddress(@QueryParam("macAddress") long macAddress);

	@POST
	@Path("deviceConfigs/{nodeUrn}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	Response add(DeviceConfigDto deviceConfig, @PathParam("nodeUrn") String nodeUrnString);

	@PUT
	@Path("deviceConfigs/{nodeUrn}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	Response update(DeviceConfigDto deviceConfig, @PathParam("nodeUrn") String nodeUrnString);

	@DELETE
	@Path("deviceConfigs/{nodeUrn}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	Response delete(@PathParam("nodeUrn") String nodeUrnString);

	@DELETE
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	Response delete(@QueryParam("nodeUrn") List<String> nodeUrnStrings);
}
