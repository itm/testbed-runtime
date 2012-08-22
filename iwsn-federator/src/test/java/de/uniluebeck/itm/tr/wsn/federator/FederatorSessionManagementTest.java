package de.uniluebeck.itm.tr.wsn.federator;

import com.google.common.collect.ImmutableSet;
import de.uniluebeck.itm.tr.federatorutils.FederationManager;
import de.uniluebeck.itm.tr.iwsn.common.SessionManagementPreconditions;
import de.uniluebeck.itm.tr.util.Logging;
import eu.wisebed.api.v3.common.KeyValuePair;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.sm.ChannelHandlerDescription;
import eu.wisebed.api.v3.sm.SessionManagement;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.net.URI;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FederatorSessionManagementTest {

	static {
		Logging.setLoggingDefaults();
	}

	private static final NodeUrnPrefix TESTBED_1_URN_PREFIX = new NodeUrnPrefix("urn:testbed1:");

	private static final NodeUrnPrefix TESTBED_2_URN_PREFIX = new NodeUrnPrefix("urn:testbed2:");

	private static final URI TESTBED_1_ENDPOINT_URL = URI.create("http://localhost:1234/");

	private static final URI TESTBED_2_ENDPOINT_URL = URI.create("http://localhost:2345/");

	@Mock
	private SessionManagement testbed1SM;

	@Mock
	private SessionManagement testbed2SM;

	@Mock
	private FederationManager<SessionManagement> federationManager;

	@Mock
	private SessionManagementPreconditions preconditions;

	@Mock
	private FederatorController federatorController;

	@Mock
	private FederatorWSNConfig config;

	private FederatorSessionManagement federatorSM;

	@Before
	public void setUp() throws Exception {
		federatorSM = new FederatorSessionManagement(federationManager, preconditions, federatorController, config);
	}

	/**
	 * Tests if calling {@link eu.wisebed.api.v3.sm.SessionManagement#getSupportedChannelHandlers()} on the federator
	 * returns only the handlers that are supported on all federated testbeds.
	 *
	 * @throws Exception
	 * 		if anything goes wrong
	 */
	@Test
	public void testGetSupportedChannelHandlersReturnsOnlyHandlersSupportedByAllFederatedTestbeds() throws Exception {

		final ImmutableSet<FederationManager.Entry<SessionManagement>> smEndpointUrlPrefixSet = ImmutableSet.of(
				new FederationManager.Entry<SessionManagement>(
						testbed1SM,
						TESTBED_1_ENDPOINT_URL,
						ImmutableSet.of(TESTBED_1_URN_PREFIX)
				),
				new FederationManager.Entry<SessionManagement>(
						testbed2SM,
						TESTBED_2_ENDPOINT_URL,
						ImmutableSet.of(TESTBED_2_URN_PREFIX)
				)
		);

		when(federationManager.getEntries()).thenReturn(smEndpointUrlPrefixSet);

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

		when(testbed1SM.getSupportedChannelHandlers()).thenReturn(supportedChannelHandlersTestbed1);
		when(testbed2SM.getSupportedChannelHandlers()).thenReturn(supportedChannelHandlersTestbed2);

		final List<ChannelHandlerDescription> supportedChannelHandlers = federatorSM.getSupportedChannelHandlers();

		verify(testbed1SM).getSupportedChannelHandlers();
		verify(testbed2SM).getSupportedChannelHandlers();

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

}
