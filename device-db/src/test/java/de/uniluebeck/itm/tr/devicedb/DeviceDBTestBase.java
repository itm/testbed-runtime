package de.uniluebeck.itm.tr.devicedb;

import com.google.common.collect.Iterators;
import de.uniluebeck.itm.tr.util.StringUtils;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.wiseml.Coordinate;
import org.junit.Test;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Iterables.get;
import static com.google.common.collect.Iterables.size;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public abstract class DeviceDBTestBase {

	protected static final Properties JPA_PROPERTIES = new Properties();

	static {
		/*
		JPA_PROPERTIES.put("hibernate.connection.url", "jdbc:hsqldb:mem:unit-testing-jpa");
		JPA_PROPERTIES.put("hibernate.connection.driver_class", "org.hsqldb.jdbcDriver");
		JPA_PROPERTIES.put("hibernate.dialect", "org.hibernate.dialect.HSQLDialect");
		JPA_PROPERTIES.put("hibernate.hbm2ddl.auto", "create-drop");
		JPA_PROPERTIES.put("hibernate.connection.username", "sa");
		JPA_PROPERTIES.put("hibernate.connection.password", "");
		*/

		JPA_PROPERTIES.put("hibernate.connection.url", "jdbc:derby:memory:device-db-unit-test");
		JPA_PROPERTIES.put("hibernate.connection.driver_class", "org.apache.derby.jdbc.EmbeddedDriver");
		JPA_PROPERTIES.put("hibernate.dialect", "org.hibernate.dialect.DerbyTenSevenDialect");
		JPA_PROPERTIES.put("hibernate.hbm2ddl.auto", "create-drop");
		JPA_PROPERTIES.put("hibernate.connection.username", "");
		JPA_PROPERTIES.put("hibernate.connection.password", "");
	}

	private static final NodeUrn NODE_URN1 = new NodeUrn("urn:wisebed:uzl1:0x2087");

	private static final NodeUrn NODE_URN2 = new NodeUrn("urn:wisebed:uzl1:0x2088");

	private static final NodeUrn NODE_URN3 = new NodeUrn("urn:wisebed:uzl1:0x2089");

	private static final String NODE_CHIP_ID1 = "XBQTBYH2";

	private DeviceConfig config1;

	private DeviceConfig config2;

	private DeviceConfig config3;

	private DeviceDB db;

	public void setUp(DeviceDB db) throws Exception {

		this.db = db;

		config1 = new DeviceConfig(
				NODE_URN1,
				"isense48",
				false,
				null,
				NODE_CHIP_ID1,
				null,
				null,
				TimeUnit.SECONDS.toMillis(1),
				TimeUnit.MINUTES.toMillis(2),
				TimeUnit.SECONDS.toMillis(5),
				TimeUnit.SECONDS.toMillis(2),
				null
		);

		config2 = new DeviceConfig(
				NODE_URN2,
				"isense48",
				false,
				null,
				null,
				null,
				null,
				TimeUnit.SECONDS.toMillis(1),
				TimeUnit.MINUTES.toMillis(2),
				TimeUnit.SECONDS.toMillis(5),
				TimeUnit.SECONDS.toMillis(2),
				new Coordinate().withX(10).withY(3).withZ(5.0)
		);

		config3 = new DeviceConfig(
				NODE_URN3,
				"isense48",
				false,
				null,
				null,
				null,
				null,
				TimeUnit.SECONDS.toMillis(1),
				TimeUnit.MINUTES.toMillis(2),
				TimeUnit.SECONDS.toMillis(5),
				TimeUnit.SECONDS.toMillis(2),
				null
		);
	}

	@Test
	public void testIfOneExistsAfterAddingIt() throws Exception {

		db.add(config1);

		assertEquals(1, size(db.getAll()));
		assertEquals(config1, db.getAll().iterator().next());
	}

	@Test
	public void testIfTwoExistAfterAddingThem() throws Exception {

		db.add(config1);
		db.add(config2);

		assertEquals(2, Iterators.size(db.getAll().iterator()));
		assertEquals(newHashSet(config1, config2), newHashSet(db.getAll()));
	}

	@Test
	public void testIfOneIsRemovedByNodeUrn() throws Exception {

		db.add(config1);

		db.removeByNodeUrn(config1.getNodeUrn());
		assertEquals(0, Iterators.size(db.getAll().iterator()));
	}

	@Test
	public void testIfTwoAreRemovedByNodeUrn() throws Exception {

		db.add(config1);
		db.add(config2);

		db.removeByNodeUrn(config1.getNodeUrn());
		assertEquals(1, size(db.getAll()));
		assertEquals(config2, get(db.getAll(), 0));

		db.removeByNodeUrn(config2.getNodeUrn());
		assertEquals(0, size(db.getAll()));
	}

	@Test
	public void getByNodeUrnsTest() {

		db.add(config1);
		db.add(config2);
		db.add(config3);

		Map<NodeUrn, DeviceConfig> map = db.getConfigsByNodeUrns(newArrayList(NODE_URN1, NODE_URN3));

		assertEquals(2, map.size());
		assertEquals(config1, map.get(NODE_URN1));
		assertNull(map.get(NODE_URN2));
		assertEquals(config3, map.get(NODE_URN3));

		map = db.getConfigsByNodeUrns(newArrayList(NODE_URN2));

		assertEquals(1, map.size());
		assertNull(map.get(NODE_URN1));
		assertEquals(config2, map.get(NODE_URN2));
		assertNull(map.get(NODE_URN3));
	}

	@Test
	public void getByUsbChipIdTest() {

		db.add(config1);
		db.add(config2);
		db.add(config3);

		final DeviceConfig retrievedConfig = db.getConfigByUsbChipId(NODE_CHIP_ID1);

		assertEquals(config1, retrievedConfig);
	}

	@Test
	public void getByNodeUrnTest() {

		db.add(config1);
		db.add(config2);
		db.add(config3);

		final DeviceConfig retrievedConfig = db.getConfigByNodeUrn(NODE_URN2);

		assertEquals(config2, retrievedConfig);
	}

	@Test
	public void getByMacAddressTest() {

		db.add(config1);
		db.add(config2);
		db.add(config3);

		final Long macAddress = StringUtils.parseHexOrDecLongFromUrn(config1.getNodeUrn().toString());
		final DeviceConfig retrievedConfig = db.getConfigByMacAddress(macAddress);

		assertEquals(config1, retrievedConfig);
	}

}
