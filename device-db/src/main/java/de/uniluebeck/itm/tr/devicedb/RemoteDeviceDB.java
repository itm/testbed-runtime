package de.uniluebeck.itm.tr.devicedb;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.devicedb.dto.DeviceConfigDto;
import eu.wisebed.api.v3.common.NodeUrn;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.provider.json.JSONProvider;

import javax.annotation.Nullable;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Functions.toStringFunction;
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

			return client().getByUsbChipId(usbChipId).readEntity(DeviceConfigDto.class).toDeviceConfig();

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

			final Response response = client().getByMacAddress(macAddress);
			final Response.Status status = Response.Status.fromStatusCode(response.getStatus());
			if (status == Response.Status.OK) {
				return response.readEntity(DeviceConfigDto.class).toDeviceConfig();
			} else if (status == Response.Status.NOT_FOUND) {
				return null;
			} else {
				throw new RuntimeException("Unexpected status retrieved from DeviceDB: " + response);
			}

		} catch (Exception e) {
			throw propagate(e);
		}
	}

	@Override
	public Iterable<DeviceConfig> getAll() {
		final List<DeviceConfigDto> dtos = getDeviceConfigDtos(ImmutableList.<NodeUrn>of());
		if (dtos != null) {
			return transform(dtos, DTO_TO_CONFIG_FUNCTION);
		} else {
			return newArrayList();
		}
	}

	@Override
	public void add(final DeviceConfig deviceConfig) {

		try {

			final DeviceConfigDto config = DeviceConfigDto.fromDeviceConfig(deviceConfig);
			client().add(config, config.getNodeUrn());

		} catch (Exception e) {
			throw propagate(e);
		}
	}

	@Override
	public void update(DeviceConfig deviceConfig) {

		try {

			client().update(DeviceConfigDto.fromDeviceConfig(deviceConfig), deviceConfig.getNodeUrn().toString());

		} catch (Exception e) {
			throw propagate(e);
		}

	}

	@Override
	public boolean removeByNodeUrn(final NodeUrn nodeUrn) {

		try {

			client().delete(nodeUrn.toString());
			return true;

		} catch (Exception e) {
			throw propagate(e);
		}
	}

	@Override
	public void removeAll() {
		try {

			client().delete(Lists.<String>newArrayList());

		} catch (Exception e) {
			throw propagate(e);
		}
	}

	private List<DeviceConfigDto> getDeviceConfigDtos(final Iterable<NodeUrn> nodeUrns) {
		try {

			return client().getByNodeUrn(newArrayList(Iterables.transform(nodeUrns, toStringFunction()))).getConfigs();

		} catch (Exception e) {
			throw propagate(e);
		}
	}

	private DeviceDBRestResource client() {
		final JSONProvider jsonProvider = new JSONProvider();
		jsonProvider.setDropRootElement(true);
		jsonProvider.setSupportUnwrapped(true);
		jsonProvider.setDropCollectionWrapperElement(false);
		return JAXRSClientFactory.create(config.uri.toString(), DeviceDBRestResource.class, newArrayList(jsonProvider));
	}
}
