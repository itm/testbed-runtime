package de.uniluebeck.itm.tr.devicedb;

import com.google.common.base.Function;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.devicedb.dto.DeviceConfigDto;
import de.uniluebeck.itm.tr.devicedb.dto.DeviceConfigListDto;
import eu.wisebed.api.v3.common.NodeUrn;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;

import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static de.uniluebeck.itm.tr.common.NodeUrnHelper.STRING_TO_NODE_URN;

public class DeviceDBRestResourceImpl implements DeviceDBRestResource {

	private final DeviceDBService deviceDBService;

	@Context
	private UriInfo uriInfo;

	@Inject
	public DeviceDBRestResourceImpl(final DeviceDBService deviceDBService) {
		this.deviceDBService = deviceDBService;
	}

	@Override
	public Response getByNodeUrn(final String nodeUrnString) {
		final DeviceConfig config = deviceDBService.getConfigByNodeUrn(new NodeUrn(nodeUrnString));
		return config == null ?
				Response.status(Response.Status.NOT_FOUND).build() :
				Response.ok(DeviceConfigDto.fromDeviceConfig(config)).build();
	}

	@Override
	public DeviceConfigListDto list() {

		final List<DeviceConfigDto> list = newArrayList();
		for (DeviceConfig deviceConfig : deviceDBService.getAll()) {
			list.add(DeviceConfigDto.fromDeviceConfig(deviceConfig));
		}

		return new DeviceConfigListDto(list);
	}

	@Override
	public DeviceConfigListDto getByNodeUrn(final List<String> nodeUrnStrings) {

		if (nodeUrnStrings.isEmpty()) {

			final List<DeviceConfigDto> list = newArrayList();
			for (DeviceConfig deviceConfig : deviceDBService.getAll()) {
				list.add(DeviceConfigDto.fromDeviceConfig(deviceConfig));
			}

			return new DeviceConfigListDto(list);

		} else {

			final Iterable<NodeUrn> nodeUrns = transform(nodeUrnStrings, STRING_TO_NODE_URN);
			final Iterable<DeviceConfig> configs = deviceDBService.getConfigsByNodeUrns(nodeUrns).values();
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

	@Override
	public Response getByUsbChipId(final String usbChipIds) {
		final DeviceConfig config = deviceDBService.getConfigByUsbChipId(usbChipIds);
		return config == null ?
				Response.status(Response.Status.NOT_FOUND).build() :
				Response.ok(DeviceConfigDto.fromDeviceConfig(config)).build();
	}

	@Override
	public Response getByMacAddress(final long macAddress) {
		final DeviceConfig config = deviceDBService.getConfigByMacAddress(macAddress);
		return config == null ?
				Response.status(Response.Status.NOT_FOUND).build() :
				Response.ok(DeviceConfigDto.fromDeviceConfig(config)).build();
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
			deviceDBService.add(deviceConfig.toDeviceConfig());
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
			deviceDBService.update(deviceConfig.toDeviceConfig());
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
