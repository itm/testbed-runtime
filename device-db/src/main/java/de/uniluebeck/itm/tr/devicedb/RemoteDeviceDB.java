package de.uniluebeck.itm.tr.devicedb;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import de.uniluebeck.itm.tr.common.EventBusService;
import de.uniluebeck.itm.tr.common.dto.DeviceConfigDto;
import de.uniluebeck.itm.tr.iwsn.messages.*;
import eu.wisebed.api.v3.common.NodeUrn;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryPlus;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.codehaus.jackson.map.ObjectMapper;

import javax.annotation.Nullable;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Functions.toStringFunction;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Optional.empty;

public class RemoteDeviceDB extends AbstractService implements DeviceDBService {

	private static final Function<DeviceConfigDto, DeviceConfig> DTO_TO_CONFIG_FUNCTION = DeviceConfigHelper::fromDto;

	private final EventBusService eventBusService;

	private final MessageFactory mf;

	private final URI remoteDeviceDBUri;

	private final URI remoteDeviceDBAdminUri;

	private final String adminUsername;

	private final String adminPassword;

	@Inject
	public RemoteDeviceDB(final EventBusService eventBusService,
						  final MessageFactory mf,
						  @Named(DeviceDBConfig.DEVICEDB_REMOTE_URI) final URI remoteDeviceDBUri,
						  @Nullable @Named(DeviceDBConfig.DEVICEDB_REMOTE_ADMIN_URI) final URI remoteDeviceDBAdminUri,
						  @Nullable @Named(DeviceDBConfig.DEVICEDB_REMOTE_ADMIN_USERNAME) final String adminUsername,
						  @Nullable @Named(DeviceDBConfig.DEVICEDB_REMOTE_ADMIN_PASSWORD) final String adminPassword) {
		this.eventBusService = eventBusService;
		this.mf = mf;
		this.remoteDeviceDBUri = remoteDeviceDBUri;
		this.remoteDeviceDBAdminUri = remoteDeviceDBAdminUri;
		this.adminUsername = adminUsername;
		this.adminPassword = adminPassword;
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
			adminClient().add(config, config.getNodeUrn());
			final DeviceConfigCreatedEvent event = mf.deviceConfigCreatedEvent(deviceConfig.getNodeUrn(), empty());

			eventBusService.post(event);

		} catch (Exception e) {
			throw propagate(e);
		}
	}

	@Override
	public void update(DeviceConfig deviceConfig) {

		checkState(isRunning());

		try {

			adminClient().update(DeviceConfigHelper.toDto(deviceConfig), deviceConfig.getNodeUrn().toString());
			DeviceConfigUpdatedEvent event = mf.deviceConfigUpdatedEvent(deviceConfig.getNodeUrn(), empty());
			eventBusService.post(event);

		} catch (Exception e) {
			throw propagate(e);
		}

	}

	@Override
	public boolean removeByNodeUrn(final NodeUrn nodeUrn) {

		checkState(isRunning());

		try {

			adminClient().delete(nodeUrn.toString());
			final DeviceConfigDeletedEvent event = mf.deviceConfigDeletedEvent(nodeUrn, empty());
			eventBusService.post(event);
			return true;

		} catch (Exception e) {
			throw propagate(e);
		}
	}

	@Override
	public void removeAll() {

		checkState(isRunning());

		try {

			Set<NodeUrn> nodeUrns = newHashSet();
			for (DeviceConfig config : getAll()) {
				nodeUrns.add(config.getNodeUrn());
			}

			adminClient().delete(Lists.<String>newArrayList());

			for (NodeUrn nodeUrn : nodeUrns) {

				final DeviceConfigDeletedEvent event = mf.deviceConfigDeletedEvent(nodeUrn, empty());
				eventBusService.post(event);
			}

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

	private DeviceDBRestAdminResource adminClient() {
		assert remoteDeviceDBAdminUri != null;
		return JAXRSClientFactoryPlus.create(
				remoteDeviceDBAdminUri.toString(),
				DeviceDBRestAdminResource.class,
				newArrayList(new JacksonJsonProvider(new ObjectMapper())),
				adminUsername,
				adminPassword
		);
	}
}
