package de.uniluebeck.itm.tr.federator.iwsn;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ListeningExecutorService;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.tr.federator.utils.FederationManager;
import de.uniluebeck.itm.tr.common.PreconditionsFactory;
import de.uniluebeck.itm.tr.common.WSNPreconditions;
import de.uniluebeck.itm.util.SecureIdGenerator;
import de.uniluebeck.itm.util.concurrent.ExecutorUtils;
import de.uniluebeck.itm.util.logging.Logging;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.wsn.ChannelHandlerConfiguration;
import eu.wisebed.api.v3.wsn.FlashProgramsConfiguration;
import eu.wisebed.api.v3.wsn.WSN;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.util.concurrent.MoreExecutors.sameThreadExecutor;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class WSNFederatorServiceImplTest {

	static {
		Logging.setLoggingDefaults();
	}

	private static final NodeUrnPrefix TESTBED_1_URN_PREFIX = new NodeUrnPrefix("urn:testbed1:");

	private static final NodeUrnPrefix TESTBED_2_URN_PREFIX = new NodeUrnPrefix("urn:testbed2:");

	private static final NodeUrnPrefix TESTBED_3_URN_PREFIX = new NodeUrnPrefix("urn:testbed3:");

	private static final HashSet<NodeUrnPrefix> SERVED_NODE_URN_PREFIXES =
			newHashSet(TESTBED_1_URN_PREFIX, TESTBED_2_URN_PREFIX, TESTBED_3_URN_PREFIX);

	private static final NodeUrn TESTBED_1_NODE_1 = new NodeUrn(TESTBED_1_URN_PREFIX + "0x0001");

	private static final NodeUrn TESTBED_1_NODE_2 = new NodeUrn(TESTBED_1_URN_PREFIX + "0x0002");

	private static final NodeUrn TESTBED_2_NODE_1 = new NodeUrn(TESTBED_2_URN_PREFIX + "0x0001");

	private static final NodeUrn TESTBED_2_NODE_2 = new NodeUrn(TESTBED_2_URN_PREFIX + "0x0002");

	private static final NodeUrn TESTBED_2_NODE_3 = new NodeUrn(TESTBED_2_URN_PREFIX + "0x0003");

	private static final NodeUrn TESTBED_3_NODE_1 = new NodeUrn(TESTBED_3_URN_PREFIX + "0x0001");

	private static final NodeUrn TESTBED_3_NODE_2 = new NodeUrn(TESTBED_3_URN_PREFIX + "0x0002");

	private static final HashSet<NodeUrn> SERVED_NODE_URNS =
			newHashSet(TESTBED_1_NODE_1, TESTBED_1_NODE_2, TESTBED_2_NODE_1, TESTBED_2_NODE_2, TESTBED_2_NODE_3,
					TESTBED_3_NODE_1, TESTBED_3_NODE_2
			);

	private final Random requestIdGenerator = new Random();

	@Mock
	private WSN testbed1WSN;

	@Mock
	private WSN testbed2WSN;

	@Mock
	private WSN testbed3WSN;

	@Mock
	private FederationManager<WSN> federationManager;

	@Mock
	private WSNFederatorControllerFactory federatorControllerFactory;

	@Mock
	private WSNFederatorController federatorController;

	@Mock
	private ServicePublisher servicePublisher;

	@Mock
	private WSNPreconditions wsnPreconditions;

	@Mock
	private IWSNFederatorServiceConfig config;

	@Mock
	private SecureIdGenerator secureIdGenerator;

	@Mock
	private PreconditionsFactory preconditionsFactory;

	@Mock
	private FederationManager<WSN> wsnFederationManager;

	private WSNFederatorServiceImpl wsnFederatorServiceImpl;

	private ListeningExecutorService executorService;

	@Before
	public void setUp() throws Exception {

		when(preconditionsFactory.createWsnPreconditions(
				Matchers.<Set<NodeUrnPrefix>>any(),
				Matchers.<Set<NodeUrn>>any()
		)
		).thenReturn(wsnPreconditions);
		when(federatorControllerFactory.create(
				wsnFederationManager,
				Matchers.<Set<NodeUrnPrefix>>any(),
				Matchers.<Set<NodeUrn>>any()
		)
		).thenReturn(federatorController);
		when(config.getFederatorWsnEndpointUriBase()).thenReturn(URI.create("http://localhost/"));
		when(federationManager.getUrnPrefixes()).thenReturn(
				ImmutableSet.of(TESTBED_1_URN_PREFIX, TESTBED_2_URN_PREFIX, TESTBED_3_URN_PREFIX)
		);

		executorService = sameThreadExecutor();

		wsnFederatorServiceImpl = new WSNFederatorServiceImpl(
				servicePublisher,
				config,
				secureIdGenerator,
				preconditionsFactory,
				executorService,
				federatorControllerFactory,
				federationManager,
				SERVED_NODE_URN_PREFIXES,
				SERVED_NODE_URNS
		);
	}

	@After
	public void tearDown() throws Exception {
		ExecutorUtils.shutdown(executorService, 0, TimeUnit.SECONDS);
	}

	@Test
	public void testFlashProgramsCalledOnInvolvedTestbedsIfEveryConfigurationCanBeForwardedToOneTestbed()
			throws Exception {

		final byte[] image1 = {0, 1, 2};
		final byte[] image2 = {1, 2, 3};

		final FlashProgramsConfiguration config1 = new FlashProgramsConfiguration();
		config1.getNodeUrns().add(TESTBED_1_NODE_1);
		config1.getNodeUrns().add(TESTBED_1_NODE_2);
		config1.setProgram(image1);

		final FlashProgramsConfiguration config2 = new FlashProgramsConfiguration();
		config2.getNodeUrns().add(TESTBED_2_NODE_1);
		config2.setProgram(image2);

		final List<FlashProgramsConfiguration> flashProgramsConfigurations = newArrayList(config1, config2);

		when(federationManager.getEndpointToNodeUrnMap(eq(config1.getNodeUrns()))).thenReturn(
				ImmutableMap.of(testbed1WSN, config1.getNodeUrns())
		);
		when(federationManager.getEndpointToNodeUrnMap(eq(config2.getNodeUrns()))).thenReturn(
				ImmutableMap.of(testbed2WSN, config2.getNodeUrns())
		);

		wsnFederatorServiceImpl.flashPrograms(requestIdGenerator.nextLong(), flashProgramsConfigurations);

		verify(testbed1WSN).flashPrograms(anyLong(), eq(newArrayList(config1)));
		verify(testbed2WSN).flashPrograms(anyLong(), eq(newArrayList(config2)));

		verify(testbed3WSN, never()).flashPrograms(anyLong(), Matchers.<List<FlashProgramsConfiguration>>any());
	}

	@Test
	public void testFlashProgramsCalledOnInvolvedTestbedsIfConfigurationsHaveToBeSplitUpForDifferentTestbeds()
			throws Exception {

		final byte[] image1 = {0, 1, 2};
		final byte[] image2 = {1, 2, 3};

		final FlashProgramsConfiguration config1 = new FlashProgramsConfiguration();
		config1.getNodeUrns().add(TESTBED_1_NODE_1);
		config1.getNodeUrns().add(TESTBED_2_NODE_1);
		config1.getNodeUrns().add(TESTBED_2_NODE_2);
		config1.setProgram(image1);

		final FlashProgramsConfiguration config2 = new FlashProgramsConfiguration();
		config2.getNodeUrns().add(TESTBED_1_NODE_2);
		config2.getNodeUrns().add(TESTBED_2_NODE_3);
		config2.setProgram(image2);

		final List<FlashProgramsConfiguration> flashProgramsConfigurations = newArrayList(config1, config2);

		when(federationManager.getEndpointToNodeUrnMap(eq(config1.getNodeUrns()))).thenReturn(
				ImmutableMap.<WSN, List<NodeUrn>>of(
						testbed1WSN, newArrayList(TESTBED_1_NODE_1),
						testbed2WSN, newArrayList(TESTBED_2_NODE_1, TESTBED_2_NODE_2)
				)
		);
		when(federationManager.getEndpointToNodeUrnMap(eq(config2.getNodeUrns()))).thenReturn(
				ImmutableMap.<WSN, List<NodeUrn>>of(
						testbed1WSN, newArrayList(TESTBED_1_NODE_2),
						testbed2WSN, newArrayList(TESTBED_2_NODE_3)
				)
		);

		wsnFederatorServiceImpl.flashPrograms(anyLong(), flashProgramsConfigurations);

		final FlashProgramsConfiguration testbed1ExpectedConfiguration1 = new FlashProgramsConfiguration();
		testbed1ExpectedConfiguration1.getNodeUrns().add(TESTBED_1_NODE_1);
		testbed1ExpectedConfiguration1.setProgram(image1);

		final FlashProgramsConfiguration testbed1ExpectedConfiguration2 = new FlashProgramsConfiguration();
		testbed1ExpectedConfiguration2.getNodeUrns().add(TESTBED_1_NODE_2);
		testbed1ExpectedConfiguration2.setProgram(image2);

		final List<FlashProgramsConfiguration> testbed1ExpectedConfigurations = newArrayList(
				testbed1ExpectedConfiguration1, testbed1ExpectedConfiguration2
		);

		final FlashProgramsConfiguration testbed2ExpectedConfiguration1 = new FlashProgramsConfiguration();
		testbed2ExpectedConfiguration1.getNodeUrns().add(TESTBED_2_NODE_1);
		testbed2ExpectedConfiguration1.getNodeUrns().add(TESTBED_2_NODE_2);
		testbed2ExpectedConfiguration1.setProgram(image1);

		final FlashProgramsConfiguration testbed2ExpectedConfiguration2 = new FlashProgramsConfiguration();
		testbed2ExpectedConfiguration2.getNodeUrns().add(TESTBED_2_NODE_3);
		testbed2ExpectedConfiguration2.setProgram(image2);

		final List<FlashProgramsConfiguration> testbed2ExpectedConfigurations = newArrayList(
				testbed2ExpectedConfiguration1, testbed2ExpectedConfiguration2
		);

		verify(testbed1WSN).flashPrograms(anyLong(), argThat(new ListAsSetMatcher(testbed1ExpectedConfigurations)));
		verify(testbed2WSN).flashPrograms(anyLong(), eq(testbed2ExpectedConfigurations));

		verify(testbed3WSN, never()).flashPrograms(anyLong(), Matchers.<List<FlashProgramsConfiguration>>any());
	}

	/**
	 * Tests if calling {@link WSN#setChannelPipeline(long, java.util.List, java.util.List)}  on the federator leads to
	 * calls on
	 * exactly the involved and only the involved federated testbeds.
	 *
	 * @throws Exception
	 * 		if anything goes wrong
	 */
	@Test
	public void testSetChannelPipelineCalledOnInvolvedTestbeds() throws Exception {

		final List<NodeUrn> nodes = newArrayList(
				TESTBED_1_NODE_1,
				TESTBED_1_NODE_2,
				TESTBED_3_NODE_1
		);
		final List<ChannelHandlerConfiguration> channelHandlerConfigurations = buildSomeArbitraryChannelPipeline();

		when(federationManager.getEndpointByNodeUrn(TESTBED_1_NODE_1)).thenReturn(testbed1WSN);
		when(federationManager.getEndpointByNodeUrn(TESTBED_1_NODE_2)).thenReturn(testbed1WSN);
		when(federationManager.getEndpointByNodeUrn(TESTBED_3_NODE_1)).thenReturn(testbed3WSN);

		wsnFederatorServiceImpl.setChannelPipeline(requestIdGenerator.nextLong(), nodes, channelHandlerConfigurations);

		verify(federationManager, never()).getEndpointByNodeUrn(TESTBED_2_NODE_1);
		verify(federationManager, never()).getEndpointByNodeUrn(TESTBED_2_NODE_2);
		verify(federationManager, never()).getEndpointByNodeUrn(TESTBED_3_NODE_2);

		verify(testbed1WSN).setChannelPipeline(
				anyLong(),
				eq(newArrayList(TESTBED_1_NODE_1, TESTBED_1_NODE_2)),
				eq(channelHandlerConfigurations)
		);
		verify(testbed2WSN, never()).setChannelPipeline(
				anyLong(),
				Matchers.<List<NodeUrn>>any(),
				Matchers.<List<ChannelHandlerConfiguration>>any()
		);
		verify(testbed3WSN).setChannelPipeline(
				anyLong(),
				eq(newArrayList(TESTBED_3_NODE_1)),
				eq(channelHandlerConfigurations)
		);
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

	private class ListAsSetMatcher extends ArgumentMatcher<List<FlashProgramsConfiguration>> {

		private final Set<FlashProgramsConfiguration> set1;

		public ListAsSetMatcher(final List<FlashProgramsConfiguration> set1) {
			this.set1 = newHashSet(set1);
		}

		@Override
		@SuppressWarnings("unchecked")
		public boolean matches(final Object o) {
			final Set<FlashProgramsConfiguration> set2 = newHashSet((List<FlashProgramsConfiguration>) o);
			return Sets.difference(set1, set2).isEmpty();
		}
	}
}
