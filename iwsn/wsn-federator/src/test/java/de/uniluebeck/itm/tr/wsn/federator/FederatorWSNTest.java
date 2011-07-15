package de.uniluebeck.itm.tr.wsn.federator;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.MoreExecutors;
import de.uniluebeck.itm.tr.federatorutils.FederationManager;
import de.uniluebeck.itm.tr.federatorutils.WebservicePublisher;
import de.uniluebeck.itm.tr.util.ExecutorUtils;
import de.uniluebeck.itm.tr.util.Logging;
import eu.wisebed.api.common.KeyValuePair;
import eu.wisebed.api.wsn.ChannelHandlerConfiguration;
import eu.wisebed.api.wsn.ChannelHandlerDescription;
import eu.wisebed.api.wsn.WSN;
import eu.wisebed.testbed.api.wsn.WSNPreconditions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FederatorWSNTest {

	static {
		Logging.setLoggingDefaults();
	}

	private static final String TESTBED_1_URN_PREFIX = "urn:testbed1:";

	private static final String TESTBED_2_URN_PREFIX = "urn:testbed2:";

	private static final String TESTBED_3_URN_PREFIX = "urn:testbed3:";

	private static final String TESTBED_1_NODE_1 = TESTBED_1_URN_PREFIX + "0x0001";

	private static final String TESTBED_1_NODE_2 = TESTBED_1_URN_PREFIX + "0x0002";

	private static final String TESTBED_2_NODE_1 = TESTBED_2_URN_PREFIX + "0x0001";

	private static final String TESTBED_2_NODE_2 = TESTBED_2_URN_PREFIX + "0x0002";

	private static final String TESTBED_3_NODE_1 = TESTBED_3_URN_PREFIX + "0x0001";

	private static final String TESTBED_3_NODE_2 = TESTBED_3_URN_PREFIX + "0x0002";

	private static final String TESTBED_1_ENDPOINT_URL = "http://localhost:1234/";

	private static final String TESTBED_2_ENDPOINT_URL = "http://localhost:2345/";

	private static final String TESTBED_3_ENDPOINT_URL = "http://localhost:3456/";

	@Mock
	private WSN testbed1WSN;

	@Mock
	private WSN testbed2WSN;

	@Mock
	private WSN testbed3WSN;

	@Mock
	private FederationManager<WSN> federationManager;

	@Mock
	private FederatorController federatorController;

	@Mock
	private WebservicePublisher<WSN> webservicePublisher;

	@Mock
	private WSNPreconditions wsnPreconditions;

	private FederatorWSN federatorWSN;

	private ExecutorService executorService;

	@Before
	public void setUp() throws Exception {

		when(federationManager.getUrnPrefixes()).thenReturn(
				ImmutableSet.of(TESTBED_1_URN_PREFIX, TESTBED_2_URN_PREFIX, TESTBED_3_URN_PREFIX)
		);

		executorService = MoreExecutors.sameThreadExecutor();

		federatorWSN = new FederatorWSN(
				federatorController,
				federationManager,
				webservicePublisher,
				wsnPreconditions,
				executorService
		);
	}

	@After
	public void tearDown() throws Exception {
		ExecutorUtils.shutdown(executorService, 0, TimeUnit.SECONDS);
	}

	/**
	 * Tests if calling {@link WSN#setChannelPipeline(java.util.List, java.util.List)} on the federator leads to calls on
	 * exactly the involved and only the involved federated testbeds.
	 *
	 * @throws Exception if anything goes wrong
	 */
	@Test
	public void testSetChannelPipelineCalledOnInvolvedTestbeds() throws Exception {

		final List<String> nodes = newArrayList(
				TESTBED_1_NODE_1,
				TESTBED_1_NODE_2,
				TESTBED_3_NODE_1
		);
		final List<ChannelHandlerConfiguration> channelHandlerConfigurations = buildSomeArbitraryChannelPipeline();

		when(federationManager.getEndpointByNodeUrn(TESTBED_1_NODE_1)).thenReturn(testbed1WSN);
		when(federationManager.getEndpointByNodeUrn(TESTBED_1_NODE_2)).thenReturn(testbed1WSN);
		when(federationManager.getEndpointByNodeUrn(TESTBED_3_NODE_1)).thenReturn(testbed3WSN);

		federatorWSN.setChannelPipeline(nodes, channelHandlerConfigurations);

		verify(federationManager, never()).getEndpointByNodeUrn(TESTBED_2_NODE_1);
		verify(federationManager, never()).getEndpointByNodeUrn(TESTBED_2_NODE_2);
		verify(federationManager, never()).getEndpointByNodeUrn(TESTBED_3_NODE_2);

		verify(testbed1WSN).setChannelPipeline(
				eq(newArrayList(TESTBED_1_NODE_1, TESTBED_1_NODE_2)),
				eq(channelHandlerConfigurations)
		);
		verify(testbed2WSN, never()).setChannelPipeline(
				Matchers.<List<String>>any(),
				Matchers.<List<ChannelHandlerConfiguration>>any()
		);
		verify(testbed3WSN).setChannelPipeline(
				eq(newArrayList(TESTBED_3_NODE_1)),
				eq(channelHandlerConfigurations)
		);
	}

	/**
	 * Tests if calling {@link eu.wisebed.api.wsn.WSN#getSupportedChannelHandlers()} on the federator returns only the
	 * handlers that are supported on all federated testbeds.
	 *
	 * @throws Exception if anything goes wrong
	 */
	@Test
	public void testGetSupportedChannelHandlersReturnsOnlyHandlersSupportedByAllFederatedTestbeds() throws Exception {

		when(federationManager.getEntries()).thenReturn(ImmutableSet.<FederationManager.Entry<WSN>>of(
				new FederationManager.Entry<WSN>(
						testbed1WSN,
						TESTBED_1_ENDPOINT_URL,
						ImmutableSet.of(TESTBED_1_URN_PREFIX)
				),
				new FederationManager.Entry<WSN>(
						testbed2WSN,
						TESTBED_2_ENDPOINT_URL,
						ImmutableSet.of(TESTBED_2_URN_PREFIX)
				)
		)
		);

		final List<ChannelHandlerDescription> supportedChannelHandlersTestbed1 = newArrayList(
				buildChannelHandlerDescription("filter1", "option11", "option12"),
				buildChannelHandlerDescription("filter2", "option21", "option22"),
				buildChannelHandlerDescription("filter3", "option31")
		);

		final List<ChannelHandlerDescription> supportedChannelHandlersTestbed2 = newArrayList(
				buildChannelHandlerDescription("filter2", "option21"),
				buildChannelHandlerDescription("filter3", "option31"),
				buildChannelHandlerDescription("filter4")
		);

		when(testbed1WSN.getSupportedChannelHandlers()).thenReturn(supportedChannelHandlersTestbed1);
		when(testbed2WSN.getSupportedChannelHandlers()).thenReturn(supportedChannelHandlersTestbed2);

		final List<ChannelHandlerDescription> supportedChannelHandlers = federatorWSN.getSupportedChannelHandlers();

		verify(testbed1WSN).getSupportedChannelHandlers();
		verify(testbed2WSN).getSupportedChannelHandlers();

		assertEquals(1, supportedChannelHandlers.size());
		assertEquals("filter3", supportedChannelHandlers.get(0).getName());
		assertEquals(1, supportedChannelHandlers.get(0).getConfigurationOptions().size());
		assertEquals("option31", supportedChannelHandlers.get(0).getConfigurationOptions().get(0).getKey());
	}

	private ChannelHandlerDescription buildChannelHandlerDescription(final String filterName,
																	 final String... optionNames) {

		ChannelHandlerDescription chd = new ChannelHandlerDescription();
		chd.setName(filterName);
		for (String optionName : optionNames) {
			final KeyValuePair keyValuePair = new KeyValuePair();
			keyValuePair.setKey(optionName);
			keyValuePair.setValue("");
			chd.getConfigurationOptions().add(keyValuePair);
		}
		return chd;
	}

	private List<ChannelHandlerConfiguration> buildSomeArbitraryChannelPipeline() {

		final ChannelHandlerConfiguration chc1 = new ChannelHandlerConfiguration();
		chc1.setName("isense-framing");

		final ChannelHandlerConfiguration chc2 = new ChannelHandlerConfiguration();
		chc2.setName("dlestxetx-framing");

		return newArrayList(
				chc1,
				chc2
		);
	}
}
