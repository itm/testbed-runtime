package de.uniluebeck.itm.tr.devicedb;

import eu.wisebed.api.v3.common.NodeUrn;
import org.hibernate.ejb.Ejb3Configuration;
import org.junit.*;

import javax.persistence.EntityManager;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DeviceDBTest {

	private static final NodeUrn NODE_URN1 = new NodeUrn("urn:wisebed:uzl1:0x2087");

	private static final NodeUrn NODE_URN2 = new NodeUrn("urn:wisebed:uzl1:0x2088");

	private static final NodeUrn NODE_URN3 = new NodeUrn("urn:wisebed:uzl1:0x2089");

	private static final String NODE_CHIP_ID1 = "XBQTBYH2";

	private static GenericDao<DeviceConfig, String> dao;

	private static DeviceDB db;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {

		final Properties properties = new Properties();

		properties.put("hibernate.connection.url", "jdbc:hsqldb:mem:unit-testing-jpa");
		properties.put("hibernate.connection.driver_class", "org.hsqldb.jdbcDriver");
		properties.put("hibernate.dialect", "org.hibernate.dialect.HSQLDialect");
		properties.put("hibernate.hbm2ddl.auto", "create-drop");
		properties.put("hibernate.hbm2ddl.auto", "create-drop");
		properties.put("hibernate.connection.username", "sa");
		properties.put("hibernate.connection.password", "");

		final EntityManager entityManager = new Ejb3Configuration()
				.addAnnotatedClass(DeviceConfig.class)
				.addProperties(properties)
				.buildEntityManagerFactory()
				.createEntityManager();

		dao = new GenericDaoImpl<DeviceConfig, String>(entityManager, DeviceConfig.class);
		db = new DeviceDBImpl(dao);
	}

	@Before
	public void setUp() {
		dao.getEntityManager().getTransaction().begin();
	}

	@After
	public void tearDown() throws Exception {
		dao.getEntityManager().getTransaction().rollback();
	}

	@Test
	public void basics() {

		DeviceConfig config = new DeviceConfig(NODE_URN1,
				"isense48", false, NODE_CHIP_ID1, null, null,
				TimeUnit.SECONDS.toMillis(1), TimeUnit.MINUTES.toMillis(2),
				TimeUnit.SECONDS.toMillis(5), TimeUnit.SECONDS.toMillis(2)
		);

		dao.save(config);

		assertTrue(dao.getEntityManager().contains(config));
		assertEquals(1, dao.find().size());
		assertEquals(NODE_URN1.toString(), dao.getKey(config));

		dao.delete(config);
		assertEquals(0, dao.find().size());
	}

	@Test
	public void getByNodeUrnsTest() {
		insertTestNodes();

		List<NodeUrn> nodeUrns = Arrays.asList(NODE_URN1, NODE_URN3);
		Map<NodeUrn, DeviceConfig> res = db.getConfigsByNodeUrns(nodeUrns);

		assertEquals(2, res.size());
		Assert.assertNotNull(res.get(NODE_URN1));
		Assert.assertNull(res.get(NODE_URN2));
		Assert.assertNotNull(res.get(NODE_URN3));
	}

	@Test
	public void getByUsbChipIdTest() {
		insertTestNodes();

		DeviceConfig config = db.getConfigByUsbChipId(NODE_CHIP_ID1);

		assertEquals(NODE_URN1, config.getNodeUrn());
	}

	@Test
	public void getByNodeUrnTest() {
		insertTestNodes();

		DeviceConfig config = db.getConfigByNodeUrn(NODE_URN2);

		assertEquals(NODE_URN2, config.getNodeUrn());
	}

	@Test
	public void getByMacAddressTest() {
		insertTestNodes();

		DeviceConfig config = db.getConfigByMacAddress(8327L);

		assertEquals(NODE_URN1, config.getNodeUrn());
	}

	private void insertTestNodes() {
		DeviceConfig config1 = new DeviceConfig(NODE_URN1,
				"isense48", false, NODE_CHIP_ID1, null, null,
				TimeUnit.SECONDS.toMillis(1), TimeUnit.MINUTES.toMillis(2),
				TimeUnit.SECONDS.toMillis(5), TimeUnit.SECONDS.toMillis(2)
		);
		DeviceConfig config2 = new DeviceConfig(NODE_URN2,
				"isense48", false, null, null, null,
				TimeUnit.SECONDS.toMillis(1), TimeUnit.MINUTES.toMillis(2),
				TimeUnit.SECONDS.toMillis(5), TimeUnit.SECONDS.toMillis(2)
		);
		DeviceConfig config3 = new DeviceConfig(NODE_URN3,
				"isense48", false, null, null, null,
				TimeUnit.SECONDS.toMillis(1), TimeUnit.MINUTES.toMillis(2),
				TimeUnit.SECONDS.toMillis(5), TimeUnit.SECONDS.toMillis(2)
		);

		dao.save(config1);
		dao.save(config2);
		dao.save(config3);
	}

}
