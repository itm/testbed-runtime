package de.uniluebeck.itm.tr.devicedb;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import de.uniluebeck.itm.tr.common.dto.DeviceConfigDto;
import eu.wisebed.api.v3.common.NodeUrn;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.codehaus.jackson.map.ObjectMapper;

import javax.annotation.Nullable;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Functions.toStringFunction;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;
import static com.google.common.collect.Maps.newHashMap;
import static de.uniluebeck.itm.tr.devicedb.DeviceConfigHelper.fromEntity;

public class RemoteDeviceDB extends AbstractService implements DeviceDBService {

	private static final Function<DeviceConfigDto, DeviceConfig> DTO_TO_CONFIG_FUNCTION =
			new Function<DeviceConfigDto, DeviceConfig>() {
				@Override
				public DeviceConfig apply(final DeviceConfigDto input) {
					return DeviceConfigHelper.fromDto(input);
				}
			};

	private final URI remoteDeviceDBUri;

	@Inject
	public RemoteDeviceDB(@Named(DeviceDBConfig.DEVICEDB_REMOTE_URI) final URI remoteDeviceDBUri) {
		this.remoteDeviceDBUri = remoteDeviceDBUri;
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

		checkState(isRunning());

		final List<DeviceConfigDto> configs = getDeviceConfigDtos(nodeUrns);
		final Map<NodeUrn, DeviceConfig> map = newHashMap();

		for (DeviceConfigDto config : configs) {
			map.put(new NodeUrn(config.getNodeUrn()), DeviceConfigHelper.fromDto(config));
		}

		return map;
	}

	@Override
	@Nullable
	public DeviceConfig getConfigByUsbChipId(final String usbChipId) {

		checkState(isRunning());

		try {

			final Response response = client().getByUsbChipId(usbChipId);
			if (Response.Status.NOT_FOUND.getStatusCode() == response.getStatus()) {
				return null;
			}
			return DeviceConfigHelper.fromDto(response.readEntity(DeviceConfigDto.class));

		} catch (Exception e) {
			throw propagate(e);
		}
	}

	@Override
	@Nullable
	public DeviceConfig getConfigByNodeUrn(final NodeUrn nodeUrn) {

		checkState(isRunning());

		final List<DeviceConfigDto> configs = getDeviceConfigDtos(newArrayList(nodeUrn));
 		return configs.isEmpty() ? null : DeviceConfigHelper.fromDto(configs.get(0));
	}

	@Override
	public DeviceConfig getConfigByMacAddress(final long macAddress) {

		checkState(isRunning());

		try {

			final Response response = client().getByMacAddress(macAddress);
			final Response.Status status = Response.Status.fromStatusCode(response.getStatus());
			if (status == Response.Status.OK) {
				return DeviceConfigHelper.fromDto(response.readEntity(DeviceConfigDto.class));
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

		checkState(isRunning());

		final List<DeviceConfigDto> dtos = getDeviceConfigDtos(ImmutableList.<NodeUrn>of());
		if (dtos != null) {
			return transform(dtos, DTO_TO_CONFIG_FUNCTION);
		} else {
			return newArrayList();
		}
	}

	@Override
	public void add(final DeviceConfig deviceConfig) {

		checkState(isRunning());

		try {

			final DeviceConfigDto config = DeviceConfigHelper.toDto(deviceConfig);
			client().add(config, config.getNodeUrn());

		} catch (Exception e) {
			throw propagate(e);
		}
	}

	@Override
	public void update(DeviceConfig deviceConfig) {

		checkState(isRunning());

		try {

			client().update(DeviceConfigHelper.toDto(deviceConfig), deviceConfig.getNodeUrn().toString());

		} catch (Exception e) {
			throw propagate(e);
		}

	}

	@Override
	public boolean removeByNodeUrn(final NodeUrn nodeUrn) {

		checkState(isRunning());

		try {

			client().delete(nodeUrn.toString());
			return true;

		} catch (Exception e) {
			throw propagate(e);
		}
	}

	@Override
	public void removeAll() {

		checkState(isRunning());

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
		return JAXRSClientFactory.create(
				remoteDeviceDBUri.toString(),
				DeviceDBRestResource.class,
				newArrayList(new JacksonJsonProvider(new ObjectMapper()))
		);
	}
}
