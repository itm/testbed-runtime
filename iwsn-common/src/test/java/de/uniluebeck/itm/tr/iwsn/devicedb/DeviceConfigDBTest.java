package de.uniluebeck.itm.tr.iwsn.devicedb;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

import de.uniluebeck.itm.tr.iwsn.devicedb.dao.DeviceConfigDAO;
import de.uniluebeck.itm.tr.iwsn.devicedb.entity.DeviceConfig;

import eu.wisebed.api.v3.common.NodeUrn;

public class DeviceConfigDBTest {

	@Inject
	static DeviceConfigDAO db;

	private static final NodeUrn NODE_URN1 = new NodeUrn(
			"urn:wisebed:uzl1:0x2087");
	private static final NodeUrn NODE_URN2 = new NodeUrn(
			"urn:wisebed:uzl1:0x2088");
	private static final NodeUrn NODE_URN3 = new NodeUrn(
			"urn:wisebed:uzl1:0x2089");
	private static final String NODE_CHIPID1 = "XBQTBYH2";

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Injector injector = Guice.createInjector(new DeviceConfigDBModule());
		db = injector.getInstance(DeviceConfigDAO.class);
	}
	
	@Before
	public void setUp() {
		db.getEntityManager().getTransaction().begin();
	}

	@After
	public void tearDown() throws Exception {
		db.getEntityManager().getTransaction().rollback();
	}

	@Test
	public void basics() {

		DeviceConfig config = new DeviceConfig(NODE_URN1,
				"isense48", false, NODE_CHIPID1, null, null,
				TimeUnit.SECONDS.toMillis(1), TimeUnit.MINUTES.toMillis(2),
				TimeUnit.SECONDS.toMillis(5), TimeUnit.SECONDS.toMillis(2));
		db.save(config);
		
		Assert.assertTrue(db.getEntityManager().contains(config));
		Assert.assertEquals(1, db.find().size());
		Assert.assertEquals(NODE_URN1.toString(), db.getKey(config));
		
		db.delete(config);
		Assert.assertEquals(0, db.find().size());
	}
	
	private void insertTestNodes() {
		DeviceConfig config1 = new DeviceConfig(NODE_URN1,
				"isense48", false, NODE_CHIPID1, null, null,
				TimeUnit.SECONDS.toMillis(1), TimeUnit.MINUTES.toMillis(2),
				TimeUnit.SECONDS.toMillis(5), TimeUnit.SECONDS.toMillis(2));
		DeviceConfig config2 = new DeviceConfig(NODE_URN2,
				"isense48", false, null, null, null,
				TimeUnit.SECONDS.toMillis(1), TimeUnit.MINUTES.toMillis(2),
				TimeUnit.SECONDS.toMillis(5), TimeUnit.SECONDS.toMillis(2));
		DeviceConfig config3 = new DeviceConfig(NODE_URN3,
				"isense48", false, null, null, null,
				TimeUnit.SECONDS.toMillis(1), TimeUnit.MINUTES.toMillis(2),
				TimeUnit.SECONDS.toMillis(5), TimeUnit.SECONDS.toMillis(2));
		
		db.save(config1);
		db.save(config2);
		db.save(config3);
	}
	
	@Test
	public void getByNodeUrnsTest() {
		insertTestNodes();
		
		List<NodeUrn> nodeUrns = Arrays.asList(NODE_URN1, NODE_URN3);
		Map<NodeUrn, DeviceConfig> res = db.getByNodeUrns(nodeUrns);
		
		Assert.assertEquals(2, res.size());
		Assert.assertNotNull(res.get(NODE_URN1));
		Assert.assertNull(res.get(NODE_URN2));
		Assert.assertNotNull(res.get(NODE_URN3));
	}
	
	@Test
	public void getByUsbChipIdTest() {
		insertTestNodes();
		
		DeviceConfig config = db.getByUsbChipId(NODE_CHIPID1);
		
		Assert.assertEquals(NODE_URN1, config.getNodeUrn());
	}

	@Test
	public void getByNodeUrnTest() {
		insertTestNodes();
		
		DeviceConfig config = db.getByNodeUrn(NODE_URN2);
		
		Assert.assertEquals(NODE_URN2, config.getNodeUrn());
	}

	@Test
	public void getByMacAddressTest() {
		insertTestNodes();
		
		DeviceConfig config = db.getByMacAddress(8327L);
		
		Assert.assertEquals(NODE_URN1, config.getNodeUrn());
	}

}
