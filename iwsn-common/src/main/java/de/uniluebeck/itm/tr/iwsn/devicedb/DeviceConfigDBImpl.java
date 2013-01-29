package de.uniluebeck.itm.tr.iwsn.devicedb;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import eu.wisebed.api.v3.common.NodeUrn;

import java.util.List;
import java.util.Map;

public class DeviceConfigDBImpl extends AbstractService implements DeviceConfigDB {
	
	private final GenericDao<DeviceConfig, String> dao;

	@Inject
	public DeviceConfigDBImpl(final GenericDao<DeviceConfig, String> dao) {
		this.dao = dao;
	}

	@Override
	protected void doStart() {
		// TODO implement
	}

	@Override
	protected void doStop() {
		// TODO implement
	}

	@Transactional
	@Override
	public Map<NodeUrn, DeviceConfig> getByNodeUrns(Iterable<NodeUrn> nodeUrns) {
		// TODO simplify this as soon as NodeUrn is serializable
		// prepare list of IDs
		List<String> nodeUrnStrings = Lists.newArrayList(Iterables.transform(nodeUrns, new Function<NodeUrn, String>() {

			@Override
			public String apply(NodeUrn nodeUrn) {
				return nodeUrn.toString();
			}

		}));
		// send out db request
		// TODO "IN (:param)" is not valid JPA2. Parantheses need to be ommited in newer Hibernate versions. 
		List<DeviceConfig> configs = dao.getEntityManager()
				.createQuery("SELECT d FROM DeviceConfig d WHERE d.nodeUrn IN (:urns)", DeviceConfig.class)
				.setParameter("urns", nodeUrnStrings).getResultList();
		
		// create map for final result
		Map<NodeUrn, DeviceConfig> result = Maps.uniqueIndex(configs, new Function<DeviceConfig, NodeUrn>() {

			@Override
			public NodeUrn apply(DeviceConfig config) {
				return config.getNodeUrn();
			}

		});
		return result;
	}

	@Override
	public DeviceConfig getByUsbChipId(String usbChipId) {
		return dao.getEntityManager()
				.createQuery("SELECT d FROM DeviceConfig d WHERE d.nodeUSBChipID = :usbChipId", DeviceConfig.class)
				.setParameter("usbChipId", usbChipId).getSingleResult();
	}

	@Override
	public DeviceConfig getByNodeUrn(NodeUrn nodeUrn) {
		// TODO remove toString() as soon as NodeUrn is Serializable
		return dao.getEntityManager()
				.createQuery("SELECT d FROM DeviceConfig d WHERE d.nodeUrn = :nodeUrn", DeviceConfig.class)
				.setParameter("nodeUrn", nodeUrn.toString()).getSingleResult();
	}

	@Override
	public DeviceConfig getByMacAddress(long macAddress) {
		String macHex = "0x"+Strings.padStart(Long.toHexString(macAddress), 4, '0');
		return dao.getEntityManager()
				.createQuery("SELECT d FROM DeviceConfig d WHERE d.nodeUrn LIKE :macAddress", DeviceConfig.class)
				.setParameter("macAddress", "%"+macHex).getSingleResult();
	}

	@Override
	public Iterable<DeviceConfig> getAll() {
		return dao.find();
	}
}
