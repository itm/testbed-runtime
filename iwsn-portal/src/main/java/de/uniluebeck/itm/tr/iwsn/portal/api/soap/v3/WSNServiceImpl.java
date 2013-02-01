package de.uniluebeck.itm.tr.iwsn.portal.api.soap.v3;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.servicepublisher.ServicePublisherService;
import de.uniluebeck.itm.tr.iwsn.common.DeliveryManager;
import de.uniluebeck.itm.tr.iwsn.devicedb.DeviceConfigDB;
import de.uniluebeck.itm.tr.iwsn.messages.SetChannelPipelinesRequest;
import de.uniluebeck.itm.tr.iwsn.portal.Reservation;
import de.uniluebeck.itm.tr.util.NetworkUtils;
import eu.wisebed.api.v3.common.KeyValuePair;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.sm.ExperimentNotRunningFault;
import eu.wisebed.api.v3.sm.ExperimentNotRunningFault_Exception;
import eu.wisebed.api.v3.wsn.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;
import java.net.URI;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newLinkedList;
import static de.uniluebeck.itm.tr.iwsn.devicedb.WiseMLConverter.convertToWiseML;
import static de.uniluebeck.itm.tr.iwsn.messages.MessagesHelper.*;
import static eu.wisebed.wiseml.WiseMLHelper.serialize;

@WebService(name = "WSN", targetNamespace = "http://wisebed.eu/api/v3/wsn")
public class WSNServiceImpl extends AbstractService implements WSNService {

	private static final Logger log = LoggerFactory.getLogger(WSNServiceImpl.class);

	private final DeviceConfigDB deviceConfigDB;

	private final Reservation reservation;

	private final DeliveryManager deliveryManager;

	private ServicePublisherService jaxWsService;

	@Inject
	public WSNServiceImpl(final ServicePublisher servicePublisher,
						  final DeviceConfigDB deviceConfigDB,
						  @Assisted final String secretReservationKey,
						  @Assisted final Reservation reservation,
						  @Assisted final DeliveryManager deliveryManager) {
		this.deviceConfigDB = checkNotNull(deviceConfigDB);
		this.reservation = checkNotNull(reservation);
		this.deliveryManager = checkNotNull(deliveryManager);
		this.jaxWsService = servicePublisher.createJaxWsService("/soap/v3/wsn/" + secretReservationKey, this);
	}

	@Override
	protected void doStart() {
		try {
			jaxWsService.startAndWait();
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {
			jaxWsService.stopAndWait();
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	@WebMethod
	@RequestWrapper(localName = "addController",
			targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.AddController"
	)
	@ResponseWrapper(localName = "addControllerResponse",
			targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.AddControllerResponse"
	)
	public void addController(@WebParam(name = "controllerEndpointUrl", targetNamespace = "")
							  final String controllerEndpointUrl) {

		log.debug("WSNServiceImpl.addController({})", controllerEndpointUrl);

		if (!"NONE".equals(controllerEndpointUrl)) {
			NetworkUtils.checkConnectivity(controllerEndpointUrl);
		}

		deliveryManager.addController(controllerEndpointUrl);

		if (reservation.getInterval().containsNow()) {
			deliveryManager.reservationStarted();
		}

		if (reservation.getInterval().isBeforeNow()) {
			deliveryManager.reservationEnded();
		}
	}

	@Override
	public void areNodesAlive(final long requestId, final List<NodeUrn> nodeUrns) {
		assertReservationIntervalMet();
		reservation.getReservationEventBus().post(newAreNodesAliveRequest(requestId, nodeUrns));
		throw new RuntimeException("Assure that requestIds do not interfere with other concurrently running requests");
	}

	@Override
	public void destroyVirtualLinks(final long requestId, final List<Link> links) {
		assertReservationIntervalMet();
		reservation.getReservationEventBus().post(newDisableVirtualLinksRequest(requestId, convert(links)));
		throw new RuntimeException("Assure that requestIds do not interfere with other concurrently running requests");
	}

	@Override
	public void disableNodes(final long requestId, final List<NodeUrn> nodeUrns) {
		assertReservationIntervalMet();
		reservation.getReservationEventBus().post(newDisableNodesRequest(requestId, nodeUrns));
		throw new RuntimeException("Assure that requestIds do not interfere with other concurrently running requests");
	}

	@Override
	public void disablePhysicalLinks(final long requestId, final List<Link> links) {
		assertReservationIntervalMet();
		reservation.getReservationEventBus().post(newDisablePhysicalLinksRequest(requestId, convert(links)));
		throw new RuntimeException("Assure that requestIds do not interfere with other concurrently running requests");
	}

	@Override
	public void disableVirtualization() throws VirtualizationNotSupported_Exception {
		assertReservationIntervalMet();
		throw new RuntimeException("TODO implement");
	}

	@Override
	public void enableVirtualization() throws VirtualizationNotSupported_Exception {
		assertReservationIntervalMet();
		throw new RuntimeException("TODO implement");
	}

	@Override
	public void enableNodes(final long requestId, final List<NodeUrn> nodeUrns) {
		assertReservationIntervalMet();
		reservation.getReservationEventBus().post(newEnableNodesRequest(requestId, nodeUrns));
		throw new RuntimeException("Assure that requestIds do not interfere with other concurrently running requests");
	}

	@Override
	public void enablePhysicalLinks(final long requestId, final List<Link> links) {
		assertReservationIntervalMet();
		reservation.getReservationEventBus().post(newEnablePhysicalLinksRequest(requestId, convert(links)));
		throw new RuntimeException("Assure that requestIds do not interfere with other concurrently running requests");
	}

	@Override
	public void flashPrograms(final long requestId, final List<FlashProgramsConfiguration> configurations) {
		assertReservationIntervalMet();
		throw new RuntimeException("TODO implement");
	}

	@Override
	public List<ChannelPipelinesMap> getChannelPipelines(final List<NodeUrn> nodeUrns) {
		assertReservationIntervalMet();
		throw new RuntimeException("TODO implement");
	}

	@Override
	public String getNetwork() {
		return serialize(convertToWiseML(deviceConfigDB.getByNodeUrns(reservation.getNodeUrns()).values()));
	}

	@Override
	public String getVersion() {
		return "3.0";
	}

	@Override
	public void removeController(final String controllerEndpointUrl) {
		log.debug("WSNServiceImpl.removeController({})", controllerEndpointUrl);
		deliveryManager.removeController(controllerEndpointUrl);
	}

	@Override
	public void resetNodes(final long requestId, final List<NodeUrn> nodeUrns) {
		assertReservationIntervalMet();
		reservation.getReservationEventBus().post(newResetNodesRequest(requestId, nodeUrns));
		throw new RuntimeException("Assure that requestIds do not interfere with other concurrently running requests");
	}

	@Override
	public void send(final long requestId, final List<NodeUrn> nodeUrns, final byte[] message) {
		assertReservationIntervalMet();
		reservation.getReservationEventBus().post(newSendDownstreamMessageRequest(requestId, nodeUrns, message));
		throw new RuntimeException("Assure that requestIds do not interfere with other concurrently running requests");
	}

	@Override
	public void setChannelPipeline(final long requestId, final List<NodeUrn> nodeUrns,
								   final List<ChannelHandlerConfiguration> channelHandlerConfigurations) {
		assertReservationIntervalMet();
		reservation.getReservationEventBus().post(
				newSetChannelPipelinesRequest(requestId, nodeUrns, convertCHCs(channelHandlerConfigurations))
		);
		throw new RuntimeException("Assure that requestIds do not interfere with other concurrently running requests");
	}

	@Override
	public void setSerialPortParameters(final List<NodeUrn> nodeUrns, final SerialPortParameters parameters) {
		assertReservationIntervalMet();
		throw new RuntimeException("TODO implement");
	}

	@Override
	public void setVirtualLinks(final long requestId, final List<VirtualLink> links) {
		assertReservationIntervalMet();
		reservation.getReservationEventBus().post(newEnableVirtualLinksRequest(requestId, convertVirtualLinks(links)));
		// TODO remember virtual link mapping in specialized class that also delivers vlink messages to remote instance
		throw new RuntimeException("Assure that requestIds do not interfere with other concurrently running requests");
	}

	@Override
	public URI getURI() {
		return jaxWsService.getURI();
	}

	private Multimap<NodeUrn, NodeUrn> convert(final List<Link> links) {
		final Multimap<NodeUrn, NodeUrn> map = HashMultimap.create();
		for (Link link : links) {
			map.put(link.getSourceNodeUrn(), link.getTargetNodeUrn());
		}
		return map;
	}

	private Multimap<NodeUrn, NodeUrn> convertVirtualLinks(final List<VirtualLink> links) {
		final Multimap<NodeUrn, NodeUrn> map = HashMultimap.create();
		for (VirtualLink link : links) {
			map.put(link.getSourceNodeUrn(), link.getTargetNodeUrn());
		}
		return map;
	}

	private Iterable<? extends SetChannelPipelinesRequest.ChannelHandlerConfiguration> convertCHCs(
			final List<ChannelHandlerConfiguration> chcs) {

		List<SetChannelPipelinesRequest.ChannelHandlerConfiguration> retList = newLinkedList();

		for (ChannelHandlerConfiguration chc : chcs) {

			final SetChannelPipelinesRequest.ChannelHandlerConfiguration.Builder builder = SetChannelPipelinesRequest
					.ChannelHandlerConfiguration
					.newBuilder()
					.setName(chc.getName());

			for (KeyValuePair keyValuePair : chc.getConfiguration()) {
				builder.addConfigurationBuilder()
						.setKey(keyValuePair.getKey())
						.setValue(keyValuePair.getValue());
			}

			retList.add(builder.build());
		}

		return retList;
	}

	private void assertReservationIntervalMet() {
		if(!reservation.getInterval().containsNow()) {
			ExperimentNotRunningFault fault = new ExperimentNotRunningFault();
			final String message = reservation.getInterval().isBeforeNow() ?
					"Reservation interval is over" :
					"Reservation interval lies in the future";
			fault.setMessage(message);
			throw new RuntimeException(new ExperimentNotRunningFault_Exception(message, fault));
		}
	}
}
