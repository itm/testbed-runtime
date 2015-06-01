package de.uniluebeck.itm.tr.devicedb;

import com.google.common.base.Function;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.common.dto.DeviceConfigDto;
import de.uniluebeck.itm.tr.common.dto.DeviceConfigListDto;
import eu.wisebed.api.v3.common.NodeUrn;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.List;

import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static de.uniluebeck.itm.tr.devicedb.DeviceConfigHelper.toDto;

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
				Response.ok(toDto(config)).build();
	}

	@Override
	public DeviceConfigListDto list() {

		final List<DeviceConfigDto> list = newArrayList();
		for (DeviceConfig deviceConfig : deviceDBService.getAll()) {
			list.add(toDto(deviceConfig));
		}

		return new DeviceConfigListDto(list);
	}

	@Override
	public DeviceConfigListDto getByNodeUrn(final List<String> nodeUrnStrings) {

		if (nodeUrnStrings.isEmpty()) {

			final List<DeviceConfigDto> list = newArrayList();
			for (DeviceConfig deviceConfig : deviceDBService.getAll()) {
				list.add(toDto(deviceConfig));
			}

			return new DeviceConfigListDto(list);

		} else {

			final Iterable<NodeUrn> nodeUrns = transform(nodeUrnStrings, NodeUrn::new);
			final Iterable<DeviceConfig> configs = deviceDBService.getConfigsByNodeUrns(nodeUrns).values();
			final Iterable<DeviceConfigDto> retList = transform(configs, new Function<DeviceConfig, DeviceConfigDto>() {
				@Override
				public DeviceConfigDto apply(final DeviceConfig config) {
					return toDto(config);
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
				Response.ok(toDto(config)).build();
	}

	@Override
	public Response getByMacAddress(final long macAddress) {
		final DeviceConfig config = deviceDBService.getConfigByMacAddress(macAddress);
		return config == null ?
				Response.status(Response.Status.NOT_FOUND).build() :
				Response.ok(toDto(config)).build();
	}
}
