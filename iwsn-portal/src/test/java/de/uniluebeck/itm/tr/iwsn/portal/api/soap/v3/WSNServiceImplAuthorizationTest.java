package de.uniluebeck.itm.tr.iwsn.portal.api.soap.v3;

import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import de.uniluebeck.itm.tr.devicedb.DeviceDB;
import de.uniluebeck.itm.tr.iwsn.common.DeliveryManager;
import de.uniluebeck.itm.tr.iwsn.portal.*;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.common.UsernameNodeUrnsMap;
import eu.wisebed.api.v3.rs.AuthorizationFault_Exception;
import eu.wisebed.api.v3.snaa.Action;
import eu.wisebed.api.v3.snaa.AuthorizationResponse;
import eu.wisebed.api.v3.snaa.SNAA;
import eu.wisebed.api.v3.wsn.*;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.LinkedList;

import static junit.framework.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class WSNServiceImplAuthorizationTest {


	@Mock
	private SNAA snaa;

	@Mock
	private DeviceDB deviceDB;

	@Mock
	private DeliveryManager deliveryManager;

	@Mock
	private Reservation reservation;

	@Mock
	private PortalConfig portalConfig;

	private AuthorizingWSN authorizingWsn;

	private WSN wsnDelegate;

	private AuthorizationResponse authorizationDeniedResponse;

	private AuthorizationResponse authorizationGrantedResponse;


	@Before
	public void setUp() throws Exception {

		when(portalConfig.getUrnPrefix()).thenReturn(new NodeUrnPrefix("urn:unit-test:"));

		authorizationDeniedResponse = new AuthorizationResponse();
		authorizationDeniedResponse.setAuthorized(false);
		authorizationGrantedResponse = new AuthorizationResponse();
		authorizationGrantedResponse.setAuthorized(true);

		final Injector injector = Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {

				install(new FactoryModuleBuilder()
						.implement(WSN.class, WSNImpl.class)
						.build(WSNFactory.class)
				);

				install(new FactoryModuleBuilder()
						.implement(AuthorizingWSN.class, AuthorizingWSNImpl.class)
						.build(AuthorizingWSNFactory.class)
				);

				bind(SNAA.class).toInstance(snaa);
				bind(DeviceDB.class).toInstance(deviceDB);
				bind(PortalConfig.class).toInstance(portalConfig);
				bind(RequestIdProvider.class).to(RandomRequestIdProvider.class);
			}
		});


		// get a reservation which starts in the future
		final long time = new DateTime().getMillis();
		Interval interval = new Interval(time*2,time*3);
		when(reservation.getInterval()).thenReturn(interval);


		WSNFactory wsnFactory = injector.getInstance(WSNFactory.class);
		wsnDelegate = spy(wsnFactory.create("", reservation, deliveryManager));
		AuthorizingWSNFactory authorizingWSNFactory = injector.getInstance(AuthorizingWSNFactory.class);
		authorizingWsn = authorizingWSNFactory.create(reservation,wsnDelegate);

	}

	@After
	public void tearDown() throws Exception {

	}

	@Test
	public void testAddControllerIfAuthorized() throws Exception {
		when(snaa.isAuthorized(anyListOf(UsernameNodeUrnsMap.class), any(Action.class))).thenReturn(authorizationGrantedResponse);
		try{
			authorizingWsn.addController("NONE");
		} catch (RuntimeException e){
			e.printStackTrace();
			fail("An unexpected exception was thrown");
		}

		verify(wsnDelegate,times(1)).addController("NONE");
	}

	@Test
	public void testAddControllerIfNoTAuthorized() throws Exception {
		when(snaa.isAuthorized(anyListOf(UsernameNodeUrnsMap.class), any(Action.class))).thenReturn(authorizationDeniedResponse);
		try{
			authorizingWsn.addController("ABC");
		} catch (RuntimeException e){
			if (!(e.getCause() instanceof AuthorizationFault_Exception)){
				e.printStackTrace();
				fail("An unexpected exception was thrown");
			}
		}
		verify(wsnDelegate,never()).addController("ABC");
	}

	@Test
	public void testAllowAreNodesAliveIfAuthorized() throws Exception {
		when(snaa.isAuthorized(anyListOf(UsernameNodeUrnsMap.class), any(Action.class))).thenReturn(authorizationGrantedResponse);
		final ArrayList<NodeUrn> nodeUrns = Lists.newArrayList(new NodeUrn("urn:unit-test:0x0001"));
		try{
			authorizingWsn.areNodesAlive(1L, nodeUrns);
		} catch (RuntimeException e){
			if (!(e.getCause() instanceof ReservationNotRunningFault_Exception)
				&& !e.getCause().getMessage().equals("Reservation interval lies in the future")) {
				e.printStackTrace();
				fail("An unexpected exception was thrown");
			}
		}
		verify(wsnDelegate,times(1)).areNodesAlive(1L, nodeUrns);
	}

	@Test
	public void testPreventAreNodesAliveIfNotAuthorized() throws Exception {
		when(snaa.isAuthorized(anyListOf(UsernameNodeUrnsMap.class), any(Action.class))).thenReturn(authorizationDeniedResponse);
		final ArrayList<NodeUrn> nodeUrns =  Lists.newArrayList(new NodeUrn("urn:unit-test:0x0001"));
		try{
			authorizingWsn.areNodesAlive(1L, nodeUrns);
		} catch (RuntimeException e){
			if (!(e.getCause() instanceof AuthorizationFault_Exception)){
				e.printStackTrace();
				fail("An unexpected exception was thrown");
			}

		}
		verify(wsnDelegate,never()).areNodesAlive(1L, nodeUrns);
	}

	@Test
	public void testDisableVirtualLinksIfAuthorized() throws Exception {
		when(snaa.isAuthorized(anyListOf(UsernameNodeUrnsMap.class), any(Action.class))).thenReturn(authorizationGrantedResponse);
		final ArrayList<Link> links = Lists.newArrayList(new Link());
		try{
			authorizingWsn.disableVirtualLinks(0L,links);
		} catch (RuntimeException e){
			if (!(e.getCause() instanceof ReservationNotRunningFault_Exception)
					&& !e.getCause().getMessage().equals("Reservation interval lies in the future")) {
				e.printStackTrace();
				fail("An unexpected exception was thrown");
			}
		}
		verify(wsnDelegate,times(1)).disableVirtualLinks(0L,links);
	}

	@Test
	public void testDisableVirtualLinksIfNotAuthorized() throws Exception {
		when(snaa.isAuthorized(anyListOf(UsernameNodeUrnsMap.class), any(Action.class))).thenReturn(authorizationDeniedResponse);
		final ArrayList<Link> links = Lists.newArrayList(new Link());
		try{
			authorizingWsn.disableVirtualLinks(0L,links);
		} catch (RuntimeException e){
			if (!(e.getCause() instanceof AuthorizationFault_Exception)){
				e.printStackTrace();
				fail("An unexpected exception was thrown");
			}
		}
		verify(wsnDelegate,never()).disableVirtualLinks(0L, links);
	}

	@Test
	public void testDisableNodesIfAuthorized() throws Exception {
		when(snaa.isAuthorized(anyListOf(UsernameNodeUrnsMap.class), any(Action.class))).thenReturn(authorizationGrantedResponse);
		final ArrayList<NodeUrn> nodeUrns =  Lists.newArrayList(new NodeUrn("urn:unit-test:0x0001"));
		try{
			authorizingWsn.disableNodes(1L, nodeUrns);
		} catch (RuntimeException e){
			if (!(e.getCause() instanceof ReservationNotRunningFault_Exception)
					&& !e.getCause().getMessage().equals("Reservation interval lies in the future")) {
				e.printStackTrace();
				fail("An unexpected exception was thrown");
			}
		}
		verify(wsnDelegate,times(1)).disableNodes(1L, nodeUrns);
	}

	@Test
	public void testDisableNodesIfNotAuthorized() throws Exception {
		when(snaa.isAuthorized(anyListOf(UsernameNodeUrnsMap.class), any(Action.class))).thenReturn(authorizationDeniedResponse);
		final ArrayList<NodeUrn> nodeUrns =  Lists.newArrayList(new NodeUrn("urn:unit-test:0x0001"));
		try{
			authorizingWsn.disableNodes(1L, nodeUrns);
		} catch (RuntimeException e){
			if (!(e.getCause() instanceof AuthorizationFault_Exception)){
				e.printStackTrace();
				fail("An unexpected exception was thrown");
			}

		}
		verify(wsnDelegate,never()).disableNodes(1L, nodeUrns);
	}

	@Test
	public void testDisablePhysicalLinksIfAuthorized() throws Exception {
		when(snaa.isAuthorized(anyListOf(UsernameNodeUrnsMap.class), any(Action.class))).thenReturn(authorizationGrantedResponse);
		final ArrayList<Link> links = Lists.newArrayList(new Link());
		try{
			authorizingWsn.disablePhysicalLinks(0L, links);
		} catch (RuntimeException e){
			if (!(e.getCause() instanceof ReservationNotRunningFault_Exception)
					&& !e.getCause().getMessage().equals("Reservation interval lies in the future")) {
				e.printStackTrace();
				fail("An unexpected exception was thrown");
			}
		}
		verify(wsnDelegate,times(1)).disablePhysicalLinks(0L, links);
	}

	@Test
	public void testDisablePhysicalLinksIfNotAuthorized() throws Exception {
		when(snaa.isAuthorized(anyListOf(UsernameNodeUrnsMap.class), any(Action.class))).thenReturn(authorizationDeniedResponse);
		final ArrayList<Link> links = Lists.newArrayList(new Link());
		try{
			authorizingWsn.disablePhysicalLinks(0L, links);
		} catch (RuntimeException e){
			if (!(e.getCause() instanceof AuthorizationFault_Exception)){
				e.printStackTrace();
				fail("An unexpected exception was thrown");
			}
		}
		verify(wsnDelegate,never()).disableVirtualLinks(0L, links);
	}

	@Test
	public void testDisableVirtualizationIfAuthorized() throws Exception {
		when(snaa.isAuthorized(anyListOf(UsernameNodeUrnsMap.class), any(Action.class))).thenReturn(authorizationGrantedResponse);
		try{
			authorizingWsn.disableVirtualization();
		} catch (RuntimeException e){
			if (!(e.getCause() instanceof ReservationNotRunningFault_Exception)
					&& !e.getCause().getMessage().equals("Reservation interval lies in the future")) {
				e.printStackTrace();
				fail("An unexpected exception was thrown");
			}
		}
		verify(wsnDelegate,times(1)).disableVirtualization();
	}

	@Test
	public void testDisableVirtualizationIfNotAuthorized() throws Exception {
		when(snaa.isAuthorized(anyListOf(UsernameNodeUrnsMap.class), any(Action.class))).thenReturn(authorizationDeniedResponse);
		try{
			authorizingWsn.disableVirtualization();
		} catch (RuntimeException e){
			if (!(e.getCause() instanceof AuthorizationFault_Exception)){
				e.printStackTrace();
				fail("An unexpected exception was thrown");
			}
		}
		verify(wsnDelegate,never()).disableVirtualization();
	}

	@Test
	public void testEnableVirtualizationIfAuthorized() throws Exception {
		when(snaa.isAuthorized(anyListOf(UsernameNodeUrnsMap.class), any(Action.class))).thenReturn(authorizationGrantedResponse);
		try{
			authorizingWsn.enableVirtualization();
		} catch (RuntimeException e){
			if (!(e.getCause() instanceof ReservationNotRunningFault_Exception)
					&& !e.getCause().getMessage().equals("Reservation interval lies in the future")) {
				e.printStackTrace();
				fail("An unexpected exception was thrown");
			}
		}
		verify(wsnDelegate,times(1)).enableVirtualization();
	}

	@Test
	public void testEnableVirtualizationIfNotAuthorized() throws Exception {
		when(snaa.isAuthorized(anyListOf(UsernameNodeUrnsMap.class), any(Action.class))).thenReturn(authorizationDeniedResponse);
		try{
			authorizingWsn.enableVirtualization();
		} catch (RuntimeException e){
			if (!(e.getCause() instanceof AuthorizationFault_Exception)){
				e.printStackTrace();
				fail("An unexpected exception was thrown");
			}
		}
		verify(wsnDelegate,never()).enableVirtualization();
	}

	@Test
	public void testEnableNodesIfAuthorized() throws Exception {
		when(snaa.isAuthorized(anyListOf(UsernameNodeUrnsMap.class), any(Action.class))).thenReturn(authorizationGrantedResponse);
		final ArrayList<NodeUrn> nodeUrns =  Lists.newArrayList(new NodeUrn("urn:unit-test:0x0001"));
		try{
			authorizingWsn.enableNodes(1L, nodeUrns);
		} catch (RuntimeException e){
			if (!(e.getCause() instanceof ReservationNotRunningFault_Exception)
					&& !e.getCause().getMessage().equals("Reservation interval lies in the future")) {
				e.printStackTrace();
				fail("An unexpected exception was thrown");
			}
		}
		verify(wsnDelegate,times(1)).enableNodes(1L, nodeUrns);
	}

	@Test
	public void testEnableNodesIfNotAuthorized() throws Exception {
		when(snaa.isAuthorized(anyListOf(UsernameNodeUrnsMap.class), any(Action.class))).thenReturn(authorizationDeniedResponse);
		final ArrayList<NodeUrn> nodeUrns =  Lists.newArrayList(new NodeUrn("urn:unit-test:0x0001"));
		try{
			authorizingWsn.enableNodes(1L, nodeUrns);
		} catch (RuntimeException e){
			if (!(e.getCause() instanceof AuthorizationFault_Exception)){
				e.printStackTrace();
				fail("An unexpected exception was thrown");
			}

		}
		verify(wsnDelegate,never()).enableNodes(1L, nodeUrns);
	}

	@Test
	public void testEnablePhysicalLinksIfAuthorized() throws Exception {
		when(snaa.isAuthorized(anyListOf(UsernameNodeUrnsMap.class), any(Action.class))).thenReturn(authorizationGrantedResponse);
		final ArrayList<Link> links = Lists.newArrayList(new Link());
		try{
			authorizingWsn.enablePhysicalLinks(0L, links);
		} catch (RuntimeException e){
			if (!(e.getCause() instanceof ReservationNotRunningFault_Exception)
					&& !e.getCause().getMessage().equals("Reservation interval lies in the future")) {
				e.printStackTrace();
				fail("An unexpected exception was thrown");
			}
		}
		verify(wsnDelegate,times(1)).enablePhysicalLinks(0L, links);
	}

	@Test
	public void testEnablePhysicalLinksIfNotAuthorized() throws Exception {
		when(snaa.isAuthorized(anyListOf(UsernameNodeUrnsMap.class), any(Action.class))).thenReturn(authorizationDeniedResponse);
		final ArrayList<Link> links = Lists.newArrayList(new Link());
		try{
			authorizingWsn.enablePhysicalLinks(0L, links);
		} catch (RuntimeException e){
			if (!(e.getCause() instanceof AuthorizationFault_Exception)){
				e.printStackTrace();
				fail("An unexpected exception was thrown");
			}
		}
		verify(wsnDelegate,never()).enablePhysicalLinks(0L, links);
	}

	@Test
	public void testFlashProgramsIfAuthorized() throws Exception {
		when(snaa.isAuthorized(anyListOf(UsernameNodeUrnsMap.class), any(Action.class))).thenReturn(authorizationGrantedResponse);
		final ArrayList<FlashProgramsConfiguration> configs = Lists.newArrayList(new FlashProgramsConfiguration());
		try{
			authorizingWsn.flashPrograms(0L, configs);
		} catch (RuntimeException e){
			if (!(e.getCause() instanceof ReservationNotRunningFault_Exception)
					&& !e.getCause().getMessage().equals("Reservation interval lies in the future")) {
				e.printStackTrace();
				fail("An unexpected exception was thrown");
			}
		}
		verify(wsnDelegate,times(1)).flashPrograms(0L, configs);
	}

	@Test
	public void testFlashProgramsIfNotAuthorized() throws Exception {
		when(snaa.isAuthorized(anyListOf(UsernameNodeUrnsMap.class), any(Action.class))).thenReturn(authorizationDeniedResponse);
		final ArrayList<FlashProgramsConfiguration> configs = Lists.newArrayList(new FlashProgramsConfiguration());
		try{
			authorizingWsn.flashPrograms(0L, configs);
		} catch (RuntimeException e){
			if (!(e.getCause() instanceof AuthorizationFault_Exception)){
				e.printStackTrace();
				fail("An unexpected exception was thrown");
			}
		}
		verify(wsnDelegate,never()).flashPrograms(0L, configs);
	}

	@Test
	public void testGetChannelPipelinesIfAuthorized() throws Exception {
		when(snaa.isAuthorized(anyListOf(UsernameNodeUrnsMap.class), any(Action.class))).thenReturn(authorizationGrantedResponse);
		final ArrayList<NodeUrn> nodeUrns =  Lists.newArrayList(new NodeUrn("urn:unit-test:0x0001"));
		try{
			authorizingWsn.getChannelPipelines(nodeUrns);
		} catch (RuntimeException e){
			if (!(e.getCause() instanceof ReservationNotRunningFault_Exception)
					&& !e.getCause().getMessage().equals("Reservation interval lies in the future")) {
				e.printStackTrace();
				fail("An unexpected exception was thrown");
			}
		}
		verify(wsnDelegate,times(1)).getChannelPipelines(nodeUrns);
	}

	@Test
	public void testGetChannelPipelinesIfNotAuthorized() throws Exception {
		when(snaa.isAuthorized(anyListOf(UsernameNodeUrnsMap.class), any(Action.class))).thenReturn(authorizationDeniedResponse);
		final ArrayList<NodeUrn> nodeUrns =  Lists.newArrayList(new NodeUrn("urn:unit-test:0x0001"));
		try{
			authorizingWsn.getChannelPipelines(nodeUrns);
		} catch (RuntimeException e){
			if (!(e.getCause() instanceof AuthorizationFault_Exception)){
				e.printStackTrace();
				fail("An unexpected exception was thrown");
			}
		}
		verify(wsnDelegate,never()).getChannelPipelines(nodeUrns);
	}

	@Test
	public void testGetNetworkIfAuthorized() throws Exception {
		when(snaa.isAuthorized(anyListOf(UsernameNodeUrnsMap.class), any(Action.class))).thenReturn(authorizationGrantedResponse);
		try{
			authorizingWsn.getNetwork();
		} catch (RuntimeException e){
			if (!(e.getCause() instanceof ReservationNotRunningFault_Exception)
					&& !e.getCause().getMessage().equals("Reservation interval lies in the future")) {
				e.printStackTrace();
				fail("An unexpected exception was thrown");
			}
		}
		verify(wsnDelegate,times(1)).getNetwork();
	}

	@Test
	public void testGetNetworkIfNotAuthorized() throws Exception {
		when(snaa.isAuthorized(anyListOf(UsernameNodeUrnsMap.class), any(Action.class))).thenReturn(authorizationDeniedResponse);
		try{
			authorizingWsn.getNetwork();
		} catch (RuntimeException e){
			if (!(e.getCause() instanceof AuthorizationFault_Exception)){
				e.printStackTrace();
				fail("An unexpected exception was thrown");
			}
		}
		verify(wsnDelegate,never()).getNetwork();
	}

	@Test
	public void testRemoveControllerIfAuthorized() throws Exception {
		when(snaa.isAuthorized(anyListOf(UsernameNodeUrnsMap.class), any(Action.class))).thenReturn(authorizationGrantedResponse);
		try{
			authorizingWsn.removeController("ABC");
		} catch (RuntimeException e){
			if (!(e.getCause() instanceof ReservationNotRunningFault_Exception)
					&& !e.getCause().getMessage().equals("Reservation interval lies in the future")) {
				e.printStackTrace();
				fail("An unexpected exception was thrown");
			}
		}
		verify(wsnDelegate,times(1)).removeController("ABC");
	}

	@Test
	public void testRemoveControllerIfNotAuthorized() throws Exception {
		when(snaa.isAuthorized(anyListOf(UsernameNodeUrnsMap.class), any(Action.class))).thenReturn(authorizationDeniedResponse);
		try{
			authorizingWsn.removeController("ABC");
		} catch (RuntimeException e){
			if (!(e.getCause() instanceof AuthorizationFault_Exception)){
				e.printStackTrace();
				fail("An unexpected exception was thrown");
			}
		}
		verify(wsnDelegate,never()).removeController("ABC");
	}

	@Test
	public void testResetNodesIfAuthorized() throws Exception {
		when(snaa.isAuthorized(anyListOf(UsernameNodeUrnsMap.class), any(Action.class))).thenReturn(authorizationGrantedResponse);
		final ArrayList<NodeUrn> nodeUrns = Lists.newArrayList(new NodeUrn("urn:unit-test:0x0001"));
		try{
			authorizingWsn.resetNodes(0L, nodeUrns);
		} catch (RuntimeException e){
			if (!(e.getCause() instanceof ReservationNotRunningFault_Exception)
					&& !e.getCause().getMessage().equals("Reservation interval lies in the future")) {
				e.printStackTrace();
				fail("An unexpected exception was thrown");
			}
		}
		verify(wsnDelegate,times(1)).resetNodes(0L, nodeUrns);
	}

	@Test
	public void testResetNodesIfNotAuthorized() throws Exception {
		when(snaa.isAuthorized(anyListOf(UsernameNodeUrnsMap.class), any(Action.class))).thenReturn(authorizationDeniedResponse);
		final ArrayList<NodeUrn> nodeUrns = Lists.newArrayList(new NodeUrn("urn:unit-test:0x0001"));
		try{
			authorizingWsn.resetNodes(0L,nodeUrns);
		} catch (RuntimeException e){
			if (!(e.getCause() instanceof AuthorizationFault_Exception)){
				e.printStackTrace();
				fail("An unexpected exception was thrown");
			}
		}
		verify(wsnDelegate,never()).resetNodes(0L, nodeUrns);

	}

	@Test
	public void testSendIfAuthorized() throws Exception {
		when(snaa.isAuthorized(anyListOf(UsernameNodeUrnsMap.class), any(Action.class))).thenReturn(authorizationGrantedResponse);
		final ArrayList<NodeUrn> nodeUrns =  Lists.newArrayList(new NodeUrn("urn:unit-test:0x0001"));
		final byte[] bytes = new byte[10];
		try{
			authorizingWsn.send(0L, nodeUrns, bytes);
		} catch (RuntimeException e){
			if (!(e.getCause() instanceof ReservationNotRunningFault_Exception)
					&& !e.getCause().getMessage().equals("Reservation interval lies in the future")) {
				e.printStackTrace();
				fail("An unexpected exception was thrown");
			}
		}
		verify(wsnDelegate,times(1)).send(0L, nodeUrns, bytes);
	}

	@Test
	public void testSendIfNotAuthorized() throws Exception {
		when(snaa.isAuthorized(anyListOf(UsernameNodeUrnsMap.class), any(Action.class))).thenReturn(authorizationDeniedResponse);
		final ArrayList<NodeUrn> nodeUrns =  Lists.newArrayList(new NodeUrn("urn:unit-test:0x0001"));
		final byte[] bytes = new byte[10];
		try{
			authorizingWsn.send(0L, nodeUrns, bytes);
		} catch (RuntimeException e){
			if (!(e.getCause() instanceof AuthorizationFault_Exception)){
				e.printStackTrace();
				fail("An unexpected exception was thrown");
			}
		}
		verify(wsnDelegate,never()).send(0L, nodeUrns, bytes);
	}

	@Test
	public void testSetChannelPipelineIfAuthorized() throws Exception {
		when(snaa.isAuthorized(anyListOf(UsernameNodeUrnsMap.class), any(Action.class))).thenReturn(authorizationGrantedResponse);
		final ArrayList<NodeUrn> nodeUrns =  Lists.newArrayList(new NodeUrn("urn:unit-test:0x0001"));
		final LinkedList<ChannelHandlerConfiguration> channelHandlerConfigurations = new LinkedList<ChannelHandlerConfiguration>();
		try{
			authorizingWsn.setChannelPipeline(0L,nodeUrns, channelHandlerConfigurations);
		} catch (RuntimeException e){
			if (!(e.getCause() instanceof ReservationNotRunningFault_Exception)
					&& !e.getCause().getMessage().equals("Reservation interval lies in the future")) {
				e.printStackTrace();
				fail("An unexpected exception was thrown");
			}
		}
		verify(wsnDelegate,times(1)).setChannelPipeline(0L, nodeUrns, channelHandlerConfigurations);
	}

	@Test
	public void testSetChannelPipelineIfNotAuthorized() throws Exception {
		when(snaa.isAuthorized(anyListOf(UsernameNodeUrnsMap.class), any(Action.class))).thenReturn(authorizationDeniedResponse);
		final ArrayList<NodeUrn> nodeUrns =  Lists.newArrayList(new NodeUrn("urn:unit-test:0x0001"));
		final LinkedList<ChannelHandlerConfiguration> channelHandlerConfigurations = new LinkedList<ChannelHandlerConfiguration>();
		try{
			authorizingWsn.setChannelPipeline(0L,nodeUrns, channelHandlerConfigurations);
		} catch (RuntimeException e){
			if (!(e.getCause() instanceof AuthorizationFault_Exception)){
				e.printStackTrace();
				fail("An unexpected exception was thrown");
			}
		}
		verify(wsnDelegate,never()).setChannelPipeline(0L,nodeUrns, channelHandlerConfigurations);
	}

	@Test
	public void testSetSerialPortParametersIfAuthorized() throws Exception {
		when(snaa.isAuthorized(anyListOf(UsernameNodeUrnsMap.class), any(Action.class))).thenReturn(authorizationGrantedResponse);
		final ArrayList<NodeUrn> nodeUrns =  Lists.newArrayList(new NodeUrn("urn:unit-test:0x0001"));
		final SerialPortParameters serialPortParameters = new SerialPortParameters();
		try{
			authorizingWsn.setSerialPortParameters(nodeUrns,serialPortParameters);
		} catch (RuntimeException e){
			if (!(e.getCause() instanceof ReservationNotRunningFault_Exception)
					&& !e.getCause().getMessage().equals("Reservation interval lies in the future")) {
				e.printStackTrace();
				fail("An unexpected exception was thrown");
			}
		}
		verify(wsnDelegate,times(1)).setSerialPortParameters(nodeUrns,serialPortParameters);
	}

	@Test
	public void testSetSerialPortParametersIfNotAuthorized() throws Exception {
		when(snaa.isAuthorized(anyListOf(UsernameNodeUrnsMap.class), any(Action.class))).thenReturn(authorizationDeniedResponse);
		final ArrayList<NodeUrn> nodeUrns = Lists.newArrayList(new NodeUrn("urn:unit-test:0x0001"));
		final SerialPortParameters serialPortParameters = new SerialPortParameters();
		try{
			authorizingWsn.setSerialPortParameters(nodeUrns,serialPortParameters);
		} catch (RuntimeException e){
			if (!(e.getCause() instanceof AuthorizationFault_Exception)){
				e.printStackTrace();
				fail("An unexpected exception was thrown");
			}
		}
		verify(wsnDelegate,never()).setSerialPortParameters(nodeUrns,serialPortParameters);
	}

	@Test
	public void testEnableVirtualLinksIfAuthorized() throws Exception {
		when(snaa.isAuthorized(anyListOf(UsernameNodeUrnsMap.class), any(Action.class))).thenReturn(authorizationGrantedResponse);
		final ArrayList<VirtualLink> virtualLinks = Lists.newArrayList(new VirtualLink());
		try{
			authorizingWsn.enableVirtualLinks(0L,virtualLinks);
		} catch (RuntimeException e){
			if (!(e.getCause() instanceof ReservationNotRunningFault_Exception)
					&& !e.getCause().getMessage().equals("Reservation interval lies in the future")) {
				e.printStackTrace();
				fail("An unexpected exception was thrown");
			}
		}
		verify(wsnDelegate,times(1)).enableVirtualLinks(0L, virtualLinks);
	}

	@Test
	public void testEnableVirtualLinksIfNotAuthorized() throws Exception {
		when(snaa.isAuthorized(anyListOf(UsernameNodeUrnsMap.class), any(Action.class))).thenReturn(authorizationDeniedResponse);
		final ArrayList<VirtualLink> virtualLinks = Lists.newArrayList(new VirtualLink());
		try{
			authorizingWsn.enableVirtualLinks(0L,virtualLinks);
		} catch (RuntimeException e){
			if (!(e.getCause() instanceof AuthorizationFault_Exception)){
				e.printStackTrace();
				fail("An unexpected exception was thrown");
			}
		}
		verify(wsnDelegate,never()).enableVirtualLinks(0L,virtualLinks);
	}
}
