package de.uniluebeck.itm.tr.devicedb;

import com.google.common.base.Function;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.common.EventBusService;
import de.uniluebeck.itm.tr.iwsn.messages.DeviceConfigCreatedEvent;
import de.uniluebeck.itm.tr.iwsn.messages.DeviceConfigDeletedEvent;
import de.uniluebeck.itm.tr.iwsn.messages.DeviceConfigUpdatedEvent;
import eu.wisebed.api.v3.common.NodeUrn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagate;
import static de.uniluebeck.itm.util.StringUtils.parseHexOrDecLong;

public class CachedDeviceDBServiceImpl extends AbstractService implements CachedDeviceDBService {

	private static final Logger log = LoggerFactory.getLogger(CachedDeviceDBServiceImpl.class);

	private final DeviceDBService deviceDB;

	private final EventBusService eventBusService;

	private final Function<NodeUrn, DeviceConfig> getConfigFunction = new Function<NodeUrn, DeviceConfig>() {
		@Override
		public DeviceConfig apply(final NodeUrn input) {
			final DeviceConfig config = deviceDB.getConfigByNodeUrn(input);
			if (config == null) {
				throw new RuntimeException("No config for node URN " + input + " found in DeviceDB");
			}
			return config;
		}
	};

	private final CacheLoader<NodeUrn, DeviceConfig> cacheLoader = CacheLoader.from(getConfigFunction);

	private final LoadingCache<NodeUrn, DeviceConfig> cache = CacheBuilder.newBuilder().build(cacheLoader);

	@Inject
	public CachedDeviceDBServiceImpl(final DeviceDBService deviceDB, final EventBusService eventBusService) {
		this.deviceDB = deviceDB;
		this.eventBusService = eventBusService;
	}

	@Override
	protected void doStart() {
		log.trace("CachedDeviceDBServiceImpl.doStart()");
		try {
			log.trace("CachedDeviceDBServiceImpl.doStart() => populating cache...");
			final Iterable<DeviceConfig> configs = deviceDB.getAll();
			for (DeviceConfig config : configs) {
				cache.put(config.getNodeUrn(), config);
			}
			eventBusService.register(this);
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		log.trace("CachedDeviceDBServiceImpl.doStop()");
		try {
			eventBusService.unregister(this);
			log.trace("CachedDeviceDBServiceImpl.doStop() => evicting all cache entries...");
			cache.invalidateAll();
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Subscribe
	public void on(final DeviceConfigCreatedEvent event) {
		event.getHeader().getNodeUrnsList().stream().map(NodeUrn::new).forEach(nodeUrn -> {
			cache.getUnchecked(nodeUrn);
			log.trace("Adding newly created DeviceConfig for node URN {} in cache", nodeUrn);
		});
	}

	@Subscribe
	public void on(final DeviceConfigUpdatedEvent event) {
		event.getHeader().getNodeUrnsList().stream().map(NodeUrn::new).forEach(nodeUrn -> {
			log.trace("Refreshing updated DeviceConfig for node URN {} in cache", nodeUrn);
			cache.refresh(nodeUrn);
		});
	}

	@Subscribe
	public void on(final DeviceConfigDeletedEvent event) {
		event.getHeader().getNodeUrnsList().stream().map(NodeUrn::new).forEach(nodeUrn -> {
			log.trace("Removing deleted DeviceConfig for node URN {} from cache", nodeUrn);
			cache.invalidate(nodeUrn);
		});
	}

	@Override
	public Map<NodeUrn, DeviceConfig> getConfigsByNodeUrns(final Iterable<NodeUrn> nodeUrns) {
		try {
			return cache.getAll(nodeUrns);
		} catch (ExecutionException e) {
			throw propagate(e);
		}
	}

	@Nullable
	@Override
	public DeviceConfig getConfigByUsbChipId(final String usbChipId) {
		checkNotNull(usbChipId, "usbChipId may not be null");
		for (DeviceConfig config : cache.asMap().values()) {
			if (usbChipId.equals(config.getNodeUSBChipID())) {
				return config;
			}
		}
		return null;
	}

	@Nullable
	@Override
	public DeviceConfig getConfigByNodeUrn(final NodeUrn nodeUrn) {
		try {
			return cache.get(nodeUrn);
		} catch (ExecutionException e) {
			throw propagate(e);
		}
	}

	@Nullable
	@Override
	public DeviceConfig getConfigByMacAddress(final long macAddress) {
		checkNotNull(macAddress, "macAddress may not be null");
		for (DeviceConfig config : cache.asMap().values()) {
			if (parseHexOrDecLong(config.getNodeUrn().getSuffix()) == macAddress) {
				return config;
			}
		}
		return null;
	}

	@Override
	public Iterable<DeviceConfig> getAll() {
		return cache.asMap().values();
	}

	@Override
	public void add(final DeviceConfig deviceConfig) {
		throw new UnsupportedOperationException("Writing operations are not supported on CachedDeviceDBService");
	}

	@Override
	public void update(final DeviceConfig deviceConfig) {
		throw new UnsupportedOperationException("Writing operations are not supported on CachedDeviceDBService");
	}

	@Override
	public boolean removeByNodeUrn(final NodeUrn nodeUrn) {
		throw new UnsupportedOperationException("Writing operations are not supported on CachedDeviceDBService");
	}

	@Override
	public void removeAll() {
		throw new UnsupportedOperationException("Writing operations are not supported on CachedDeviceDBService");
	}
}
