package de.uniluebeck.itm.tr.devicedb;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.persist.Transactional;
import de.uniluebeck.itm.tr.common.EventBusService;
import de.uniluebeck.itm.tr.common.NodeUrnHelper;
import de.uniluebeck.itm.tr.devicedb.entity.DeviceConfigEntity;
import de.uniluebeck.itm.tr.iwsn.messages.DeviceConfigCreatedEvent;
import de.uniluebeck.itm.tr.iwsn.messages.DeviceConfigDeletedEvent;
import de.uniluebeck.itm.tr.iwsn.messages.DeviceConfigUpdatedEvent;
import eu.wisebed.api.v3.common.NodeUrn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.uniqueIndex;
import static com.google.common.collect.Sets.newHashSet;
import static de.uniluebeck.itm.tr.devicedb.DeviceConfigHelper.fromEntity;
import static de.uniluebeck.itm.tr.devicedb.DeviceConfigHelper.toEntity;

public class DeviceDBJpa extends AbstractService implements
		DeviceDBService {

	@SuppressWarnings("unused")
	private static final Logger log = LoggerFactory.getLogger(DeviceDBJpa.class);

	private static final Function<DeviceConfig, NodeUrn> CONFIG_NODE_URN_FUNCTION =
			new Function<DeviceConfig, NodeUrn>() {
				@Override
				public NodeUrn apply(DeviceConfig config) {
					return config.getNodeUrn();
				}
			};

	private static final Function<DeviceConfigEntity, DeviceConfig> ENTITY_TO_CONFIG_FUNCTION =
			new Function<DeviceConfigEntity, DeviceConfig>() {
				@Override
				public DeviceConfig apply(DeviceConfigEntity config) {
					return fromEntity(config);
				}
			};

	private final Provider<EntityManager> entityManager;

	private final EventBusService eventBusService;

	@Inject
	public DeviceDBJpa(final Provider<EntityManager> entityManager,
					   final EventBusService eventBusService) {
		this.entityManager = entityManager;
		this.eventBusService = eventBusService;
	}

	@Override
	protected void doStart() {
		log.trace("DeviceDBJpa.doStart()");
		notifyStarted();
	}

	@Override
	protected void doStop() {
		log.trace("DeviceDBJpa.doStop()");
		notifyStopped();
	}

	@Override
	@Transactional
	public Map<NodeUrn, DeviceConfig> getConfigsByNodeUrns(Iterable<NodeUrn> nodeUrns) {
		log.trace("DeviceDBJpa.getConfigsByNodeUrns({})", nodeUrns);
		checkState(isRunning());

		final List<String> nodeUrnStrings = newArrayList(transform(nodeUrns, NodeUrnHelper.NODE_URN_TO_STRING));
		final List<DeviceConfigEntity> entities = entityManager.get()
				.createQuery("SELECT d FROM DeviceConfig d WHERE d.nodeUrn IN (:urns)", DeviceConfigEntity.class)
				.setParameter("urns", nodeUrnStrings).getResultList();

		final Collection<DeviceConfig> configs = Collections2.transform(entities, ENTITY_TO_CONFIG_FUNCTION);
		return uniqueIndex(configs, CONFIG_NODE_URN_FUNCTION);
	}

	@Override
	@Nullable
	@Transactional
	public DeviceConfig getConfigByUsbChipId(String usbChipId) {
		log.trace("DeviceDBJpa.getConfigByUsbChipId({})", usbChipId);
		checkState(isRunning());

		try {
			final DeviceConfigEntity deviceConfig = entityManager.get()
					.createQuery("SELECT d FROM DeviceConfig d WHERE d.nodeUSBChipID = :usbChipId",
							DeviceConfigEntity.class
					)
					.setParameter("usbChipId", usbChipId).getSingleResult();
			return fromEntity(deviceConfig);
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	@Nullable
	@Transactional
	public DeviceConfig getConfigByNodeUrn(NodeUrn nodeUrn) {
		log.trace("DeviceDBJpa.getConfigByNodeUrn({})", nodeUrn);
		checkState(isRunning());

		try {
			final DeviceConfigEntity deviceConfig = entityManager.get()
					.createQuery("SELECT d FROM DeviceConfig d WHERE d.nodeUrn = :nodeUrn", DeviceConfigEntity.class)
					.setParameter("nodeUrn", nodeUrn.toString()).getSingleResult();
			return fromEntity(deviceConfig);
		} catch (NoResultException e) {
			return null;
		}
	}

	@Override
	@Nullable
	@Transactional
	public DeviceConfig getConfigByMacAddress(long macAddress) {
		log.trace("DeviceDBJpa.getConfigByMacAddress({})", Long.toHexString(macAddress));
		checkState(isRunning());

		try {

			String macHex = "0x" + Strings.padStart(Long.toHexString(macAddress), 4, '0');
			final DeviceConfigEntity deviceConfig = entityManager.get()
					.createQuery("SELECT d FROM DeviceConfig d WHERE d.nodeUrn LIKE :macAddress",
							DeviceConfigEntity.class
					)
					.setParameter("macAddress", "%" + macHex).getSingleResult();
			return fromEntity(deviceConfig);

		} catch (NoResultException e) {
			return null;
		}
	}

	@Override
	@Transactional
	public Iterable<DeviceConfig> getAll() {
		log.trace("DeviceDBJpa.getAll()");
		checkState(isRunning());

		final List<DeviceConfigEntity> list = entityManager.get()
				.createQuery("SELECT d FROM DeviceConfig d", DeviceConfigEntity.class)
				.getResultList();

		return Collections2.transform(list, ENTITY_TO_CONFIG_FUNCTION);
	}

	@Override
	@Transactional
	public void add(final DeviceConfig deviceConfig) {
		log.trace("DeviceDBJpa.add({})", deviceConfig);
		checkState(isRunning());
		entityManager.get().persist(toEntity(deviceConfig));
		final DeviceConfigCreatedEvent event = DeviceConfigCreatedEvent
				.newBuilder()
				.setNodeUrn(deviceConfig.getNodeUrn().toString())
				.build();
		eventBusService.post(event);
	}

	@Override
	@Transactional
	public void update(DeviceConfig deviceConfig) {
		log.trace("DeviceDBJpa.update({})", deviceConfig);
		checkState(isRunning());
		entityManager.get().merge(toEntity(deviceConfig));
		final DeviceConfigUpdatedEvent event = DeviceConfigUpdatedEvent
				.newBuilder()
				.setNodeUrn(deviceConfig.getNodeUrn().toString())
				.build();
		eventBusService.post(event);
	}

	@Override
	@Transactional
	public boolean removeByNodeUrn(final NodeUrn nodeUrn) {
		log.trace("DeviceDBJpa.removeByNodeUrn({})", nodeUrn);
		checkState(isRunning());

		try {
			DeviceConfigEntity config = entityManager.get()
					.createQuery("SELECT d FROM DeviceConfig d WHERE d.nodeUrn = :nodeUrn", DeviceConfigEntity.class)
					.setParameter("nodeUrn", nodeUrn.toString()).getSingleResult();
			entityManager.get().remove(config);
			final DeviceConfigDeletedEvent event = DeviceConfigDeletedEvent
					.newBuilder()
					.setNodeUrn(nodeUrn.toString())
					.build();
			eventBusService.post(event);
			return true;

		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			return false;
		}

	}

	@Override
	@Transactional
	public void removeAll() {
		log.trace("DeviceDBJpa.removeAll()");
		checkState(isRunning());

		Set<NodeUrn> nodeUrns = newHashSet();
		for (DeviceConfig config : getAll()) {
			nodeUrns.add(config.getNodeUrn());
		}

		// delete nodeConfiguration via native Query, because JPA doesn't support bulk delete with ElementCollections
		entityManager.get()
				.createNativeQuery("DELETE FROM DeviceConfigEntity_NODECONFIGURATION")
				.executeUpdate();

		entityManager.get()
				.createQuery("DELETE FROM DeviceConfig")
				.executeUpdate();

		for (NodeUrn nodeUrn : nodeUrns) {

			final DeviceConfigDeletedEvent event = DeviceConfigDeletedEvent
					.newBuilder()
					.setNodeUrn(nodeUrn.toString())
					.build();

			eventBusService.post(event);
		}
	}
}
