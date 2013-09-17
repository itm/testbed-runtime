package de.uniluebeck.itm.tr.devicedb;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.persist.Transactional;
import de.uniluebeck.itm.tr.common.NodeUrnHelper;
import de.uniluebeck.itm.tr.devicedb.entity.DeviceConfigEntity;
import eu.wisebed.api.v3.common.NodeUrn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.uniqueIndex;

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
					return config.toDeviceConfig();
				}
			};

	private final Provider<EntityManager> entityManager;

	@Inject
	public DeviceDBJpa(final Provider<EntityManager> entityManager) {
		this.entityManager = entityManager;
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
			return entityManager.get()
					.createQuery("SELECT d FROM DeviceConfig d WHERE d.nodeUSBChipID = :usbChipId",
							DeviceConfigEntity.class
					)
					.setParameter("usbChipId", usbChipId).getSingleResult().toDeviceConfig();
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
			return entityManager.get()
					.createQuery("SELECT d FROM DeviceConfig d WHERE d.nodeUrn = :nodeUrn", DeviceConfigEntity.class)
					.setParameter("nodeUrn", nodeUrn.toString()).getSingleResult().toDeviceConfig();
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
			return entityManager.get()
					.createQuery("SELECT d FROM DeviceConfig d WHERE d.nodeUrn LIKE :macAddress",
							DeviceConfigEntity.class
					)
					.setParameter("macAddress", "%" + macHex).getSingleResult().toDeviceConfig();

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
		entityManager.get().persist(new DeviceConfigEntity(deviceConfig));
	}

	@Override
	@Transactional
	public void update(DeviceConfig deviceConfig) {
		log.trace("DeviceDBJpa.update({})", deviceConfig);
		checkState(isRunning());
		entityManager.get().merge(new DeviceConfigEntity(deviceConfig));
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
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	@Override
	@Transactional
	public void removeAll() {
		log.trace("DeviceDBJpa.removeAll()");
		checkState(isRunning());

		// delete nodeConfiguration via native Query, because JPA doesn't support bulk delete with ElementCollections
		entityManager.get()
				.createNativeQuery("DELETE FROM DeviceConfigEntity_NODECONFIGURATION")
				.executeUpdate();

		entityManager.get()
				.createQuery("DELETE FROM DeviceConfig")
				.executeUpdate();
	}
}
