package de.uniluebeck.itm.tr.devicedb;

import com.google.common.base.Function;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.devicedb.dto.DeviceConfigDto;
import eu.wisebed.api.v3.common.NodeUrn;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.Arrays;
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
	@Path("/test")
	public Response test(@QueryParam("a") List<String> as, @Context UriInfo uriInfo) {
		System.out.println(Arrays.toString(as.toArray()));
		return Response.ok(uriInfo.getRequestUri()).build();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<DeviceConfigDto> get(@QueryParam("nodeUrn") List<String> nodeUrnStrings) {

		if (nodeUrnStrings.isEmpty()) {

			final List<DeviceConfigDto> list = newArrayList();
			for (DeviceConfig deviceConfig : deviceDB.getAll()) {
				list.add(DeviceConfigDto.fromDeviceConfig(deviceConfig));
			}

			return list;

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

			return newArrayList(retList);
		}

	}

	@GET
	@Path("{nodeUrn}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getByNodeUrn(@PathParam("nodeUrn") final String nodeUrnString) {
		final DeviceConfig config = deviceDB.getConfigByNodeUrn(new NodeUrn(nodeUrnString));
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
}
