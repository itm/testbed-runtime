package de.uniluebeck.itm.tr.devicedb;

import com.google.inject.Guice;
import com.google.inject.Injector;
import de.uniluebeck.itm.tr.util.StringUtils;
import eu.wisebed.api.v3.common.NodeUrn;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.core.UriInfo;
import java.io.File;
import java.io.FileWriter;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Iterables.get;
import static com.google.common.collect.Iterables.size;
import static com.google.common.collect.Iterators.size;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(MockitoJUnitRunner.class)
public class DeviceDBTest {

	private static final NodeUrn NODE_URN1 = new NodeUrn("urn:wisebed:uzl1:0x2087");

	private static final NodeUrn NODE_URN2 = new NodeUrn("urn:wisebed:uzl1:0x2088");

	private static final NodeUrn NODE_URN3 = new NodeUrn("urn:wisebed:uzl1:0x2089");

	private static final String NODE_CHIP_ID1 = "XBQTBYH2";

	private static final Properties PROPERTIES = new Properties();

	static {
		PROPERTIES.put("hibernate.connection.url", "jdbc:hsqldb:mem:unit-testing-jpa");
		PROPERTIES.put("hibernate.connection.driver_class", "org.hsqldb.jdbcDriver");
		PROPERTIES.put("hibernate.dialect", "org.hibernate.dialect.HSQLDialect");
		PROPERTIES.put("hibernate.hbm2ddl.auto", "create-drop");
		PROPERTIES.put("hibernate.connection.username", "sa");
		PROPERTIES.put("hibernate.connection.password", "");
	}

	private DeviceDB db;

	private DeviceConfig config1;

	private DeviceConfig config2;

	private DeviceConfig config3;

	@Mock
	private UriInfo uriInfo;

	@Before
	public void setUp() throws Exception {

		final File file = File.createTempFile("ldkjf", "lkjdsfl");
		final FileWriter writer = new FileWriter(file);
		PROPERTIES.store(writer, "");
		writer.close();


		final DeviceDBServiceConfig config = new DeviceDBServiceConfig();
		config.dbPropertiesFile = file;
		config.port = 1234;
		final DeviceDBServiceModule module = new DeviceDBServiceModule(config);

		/*
		final Module module = new AbstractModule() {
			@Override
			protected void configure() {
				install(new DeviceDBJpaModule(PROPERTIES));

			}
		};
		*/

		final Injector injector = Guice.createInjector(module);
		db = injector.getInstance(DeviceDB.class);

		config1 = new DeviceConfig(
				NODE_URN1,
				"isense48",
				false,
				NODE_CHIP_ID1,
				null,
				null,
				TimeUnit.SECONDS.toMillis(1),
				TimeUnit.MINUTES.toMillis(2),
				TimeUnit.SECONDS.toMillis(5),
				TimeUnit.SECONDS.toMillis(2)
		);

		config2 = new DeviceConfig(
				NODE_URN2,
				"isense48",
				false,
				null,
				null,
				null,
				TimeUnit.SECONDS.toMillis(1),
				TimeUnit.MINUTES.toMillis(2),
				TimeUnit.SECONDS.toMillis(5),
				TimeUnit.SECONDS.toMillis(2)
		);

		config3 = new DeviceConfig(
				NODE_URN3,
				"isense48",
				false,
				null,
				null,
				null,
				TimeUnit.SECONDS.toMillis(1),
				TimeUnit.MINUTES.toMillis(2),
				TimeUnit.SECONDS.toMillis(5),
				TimeUnit.SECONDS.toMillis(2)
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

		assertEquals(2, size(db.getAll().iterator()));
		assertEquals(newHashSet(config1, config2), newHashSet(db.getAll()));
	}

	@Test
	public void testIfOneIsRemovedByNodeUrn() throws Exception {

		db.add(config1);

		db.removeByNodeUrn(config1.getNodeUrn());
		assertEquals(0, size(db.getAll().iterator()));
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
