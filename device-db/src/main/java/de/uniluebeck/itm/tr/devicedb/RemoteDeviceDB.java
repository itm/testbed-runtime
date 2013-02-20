package de.uniluebeck.itm.tr.devicedb;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import com.sun.jersey.api.client.AsyncWebResource;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.GenericType;
import de.uniluebeck.itm.tr.devicedb.dto.DeviceConfigDto;
import eu.wisebed.api.v3.common.NodeUrn;

import javax.annotation.Nullable;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;
import static com.google.common.collect.Maps.newHashMap;

public class RemoteDeviceDB extends AbstractService implements DeviceDB {

	private static final Function<DeviceConfigDto, DeviceConfig> DTO_TO_CONFIG_FUNCTION =
			new Function<DeviceConfigDto, DeviceConfig>() {
				@Override
				public DeviceConfig apply(final DeviceConfigDto input) {
					return input.toDeviceConfig();
				}
			};

	private static final GenericType<List<DeviceConfigDto>> DEVICE_CONFIG_LIST_TYPE =
			new GenericType<List<DeviceConfigDto>>() {
			};

	private final RemoteDeviceDBConfig config;

	@Inject
	public RemoteDeviceDB(final RemoteDeviceDBConfig config) {
		this.config = config;
	}

	@Override
	protected void doStart() {
		try {
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	public Map<NodeUrn, DeviceConfig> getConfigsByNodeUrns(final Iterable<NodeUrn> nodeUrns) {

		final List<DeviceConfigDto> configs = getDeviceConfigDtos(nodeUrns);
		final Map<NodeUrn, DeviceConfig> map = newHashMap();

		for (DeviceConfigDto config : configs) {
			map.put(new NodeUrn(config.getNodeUrn()), config.toDeviceConfig());
		}

		return map;
	}

	@Override
	@Nullable
	public DeviceConfig getConfigByUsbChipId(final String usbChipId) {
		try {

			return Client.create()
					.asyncResource(config.uri)
					.get(DeviceConfigDto.class)
					.get(10, TimeUnit.SECONDS)
					.toDeviceConfig();

		} catch (Exception e) {
			throw propagate(e);
		}
	}

	@Override
	@Nullable
	public DeviceConfig getConfigByNodeUrn(final NodeUrn nodeUrn) {
		final List<DeviceConfigDto> configs = getDeviceConfigDtos(newArrayList(nodeUrn));
		return configs.isEmpty() ? null : configs.get(0).toDeviceConfig();
	}

	@Override
	public DeviceConfig getConfigByMacAddress(final long macAddress) {
		try {

			return Client.create()
					.asyncResource(config.uri)
					.get(DeviceConfigDto.class)
					.get(10, TimeUnit.SECONDS)
					.toDeviceConfig();

		} catch (Exception e) {
			throw propagate(e);
		}
	}

	@Override
	public Iterable<DeviceConfig> getAll() {
		return transform(getDeviceConfigDtos(ImmutableList.<NodeUrn>of()), DTO_TO_CONFIG_FUNCTION);
	}

	@Override
	public void add(final DeviceConfig deviceConfig) {

		try {

			Client.create()
					.asyncResource(config.uri)
					.type(MediaType.APPLICATION_JSON_TYPE)
					.post(DeviceConfigDto.fromDeviceConfig(deviceConfig))
					.get(10, TimeUnit.SECONDS);

		} catch (Exception e) {
			throw propagate(e);
		}
	}

	@Override
	public boolean removeByNodeUrn(final NodeUrn nodeUrn) {

		try {

			final Response response = Client.create()
					.asyncResource(config.uri)
					.queryParam("nodeUrn", nodeUrn.toString())
					.delete(Response.class)
					.get(10, TimeUnit.SECONDS);

			return Response.Status.OK == Response.Status.fromStatusCode(response.getStatus());

		} catch (Exception e) {
			throw propagate(e);
		}
	}

	@Override
	public void removeAll() {
		try {
			Client.create().asyncResource(config.uri).delete(Response.class).get(10, TimeUnit.SECONDS);
		} catch (Exception e) {
			throw propagate(e);
		}
	}

	private List<DeviceConfigDto> getDeviceConfigDtos(final Iterable<NodeUrn> nodeUrns) {
		try {

			AsyncWebResource res = Client.create()
					.asyncResource(config.uri)
					.path("byNodeUrn");

			for (NodeUrn nodeUrn : nodeUrns) {
				res = res.queryParam("nodeUrn", nodeUrn.toString());
			}

			return res.get(DEVICE_CONFIG_LIST_TYPE).get(10, TimeUnit.SECONDS);

		} catch (Exception e) {
			throw propagate(e);
		}
	}
}
