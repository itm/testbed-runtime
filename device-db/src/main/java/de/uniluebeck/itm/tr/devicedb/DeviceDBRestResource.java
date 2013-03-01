package de.uniluebeck.itm.tr.devicedb;

import com.google.common.base.Function;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.devicedb.dto.DeviceConfigDto;
import de.uniluebeck.itm.tr.devicedb.dto.DeviceConfigListDto;
import eu.wisebed.api.v3.common.NodeUrn;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.List;

import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static de.uniluebeck.itm.tr.iwsn.common.NodeUrnHelper.STRING_TO_NODE_URN;

@Path("/")
public class DeviceDBRestResource {

	private final DeviceDB deviceDB;

	@Inject
	public DeviceDBRestResource(final DeviceDB deviceDB) {
		this.deviceDB = deviceDB;
	}

	@GET
	@Path("{a: deviceConfigs/|}{nodeUrn}") // accept "/deviceConfigs/{nodUrn}" or just "/{nodeUrn}"
	@Produces(MediaType.APPLICATION_JSON)
	public Response getByNodeUrn(@PathParam("nodeUrn") final String nodeUrnString) {
		final DeviceConfig config = deviceDB.getConfigByNodeUrn(new NodeUrn(nodeUrnString));
		return config == null ?
				Response.status(Response.Status.NOT_FOUND).build() :
				Response.ok(DeviceConfigDto.fromDeviceConfig(config)).build();
	}
	
	@GET
	@Path("deviceConfigs")
	@Produces(MediaType.APPLICATION_JSON)
	public DeviceConfigListDto list() {
		final List<DeviceConfigDto> list = newArrayList();
		for (DeviceConfig deviceConfig : deviceDB.getAll()) {
			list.add(DeviceConfigDto.fromDeviceConfig(deviceConfig));
		}

		return new DeviceConfigListDto(list);
	}

	@GET
	@Path("byNodeUrn")
	@Produces(MediaType.APPLICATION_JSON)
	public DeviceConfigListDto getByNodeUrn(@QueryParam("nodeUrn") List<String> nodeUrnStrings) {

		if (nodeUrnStrings.isEmpty()) {

			final List<DeviceConfigDto> list = newArrayList();
			for (DeviceConfig deviceConfig : deviceDB.getAll()) {
				list.add(DeviceConfigDto.fromDeviceConfig(deviceConfig));
			}

			return new DeviceConfigListDto(list);

		} else {

			final Iterable<NodeUrn> nodeUrns = transform(nodeUrnStrings, STRING_TO_NODE_URN);
			final Iterable<DeviceConfig> configs = deviceDB.getConfigsByNodeUrns(nodeUrns).values();
			final Iterable<DeviceConfigDto> retList = transform(configs, new Function<DeviceConfig, DeviceConfigDto>() {
				@Override
				public DeviceConfigDto apply(final DeviceConfig config) {
					return DeviceConfigDto.fromDeviceConfig(config);
				}
			}
			);

			return new DeviceConfigListDto(newArrayList(retList));
		}

	}

	@GET
	@Path("byUsbChipId")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getByUsbChipId(@QueryParam("usbChipId") String usbChipIds) {
		final DeviceConfig config = deviceDB.getConfigByUsbChipId(usbChipIds);
		return config == null ?
				Response.status(Response.Status.NOT_FOUND).build() :
				Response.ok(DeviceConfigDto.fromDeviceConfig(config)).build();
	}

	@GET
	@Path("byMacAddress")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getByMacAddress(@QueryParam("macAddress") long macAddress) {
		final DeviceConfig config = deviceDB.getConfigByMacAddress(macAddress);
		return config == null ?
				Response.status(Response.Status.NOT_FOUND).build() :
				Response.ok(DeviceConfigDto.fromDeviceConfig(config)).build();
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response add(final DeviceConfigDto deviceConfig, @Context UriInfo uriInfo) {

		deviceDB.add(deviceConfig.toDeviceConfig());

		final URI location = UriBuilder.fromUri(uriInfo.getBaseUri()).fragment(deviceConfig.getNodeUrn()).build();
		return Response.created(location).build();
	}

	@DELETE
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response delete(@QueryParam("nodeUrn") List<String> nodeUrnStrings) {

		if (nodeUrnStrings.isEmpty()) {

			deviceDB.removeAll();

		} else {

			for (String nodeUrnString : nodeUrnStrings) {
				deviceDB.removeByNodeUrn(new NodeUrn(nodeUrnString));
			}

		}

		return Response.ok().build();
	}
}
