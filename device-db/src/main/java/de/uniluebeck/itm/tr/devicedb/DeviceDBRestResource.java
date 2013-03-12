package de.uniluebeck.itm.tr.devicedb;

import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static de.uniluebeck.itm.tr.iwsn.common.NodeUrnHelper.STRING_TO_NODE_URN;

import java.net.URI;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Level;

import com.google.common.base.Function;
import com.google.inject.Inject;

import de.uniluebeck.itm.tr.devicedb.dto.CoordinateDto;
import de.uniluebeck.itm.tr.devicedb.dto.DeviceConfigDto;
import de.uniluebeck.itm.tr.devicedb.dto.DeviceConfigListDto;
import de.uniluebeck.itm.tr.util.Logging;
import eu.wisebed.api.v3.common.NodeUrn;

@Path("/")
public class DeviceDBRestResource {

	static {
		Logging.setLoggingDefaults(Level.TRACE);
	}

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
	@Path("deviceConfigs") // accept "/deviceConfigs" or root
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response add(final DeviceConfigDto deviceConfig, @Context UriInfo uriInfo) {
		try {
			deviceDB.add(deviceConfig.toDeviceConfig());
		} catch (Exception e) {
			return Response.serverError().entity(e.getMessage()).build();
		}
		final URI location = UriBuilder.fromUri(uriInfo.getBaseUri()).fragment(deviceConfig.getNodeUrn()).build();
		return Response.created(location).build();
	}
	
	@PUT
	@Path("deviceConfigs/{nodeUrn}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response update(final DeviceConfigDto deviceConfig, @Context UriInfo uriInfo) {
		// check if Entity to update already exists
		if ( deviceDB.getConfigByNodeUrn(new NodeUrn(deviceConfig.getNodeUrn())) != null ) {
			deviceDB.update(deviceConfig.toDeviceConfig());
		} else {
			return Response.notModified().build();
		}

		return Response.ok("true").build();
	}
	
	@DELETE
	@Path("deviceConfigs/{nodeUrn}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response delete(@PathParam("nodeUrn")  String nodeUrnString) {

		boolean ok = deviceDB.removeByNodeUrn(new NodeUrn(nodeUrnString));

		return ok ? Response.ok("true").build() : Response.notModified().build();
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

		return Response.ok("true").build();
	}
	
	
}
