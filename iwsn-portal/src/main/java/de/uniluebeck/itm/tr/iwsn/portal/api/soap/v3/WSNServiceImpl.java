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
import eu.wisebed.api.v3.wsn.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jws.WebService;
import java.net.URI;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newLinkedList;
import static de.uniluebeck.itm.tr.iwsn.devicedb.WiseMLConverter.convertToWiseML;
import static de.uniluebeck.itm.tr.iwsn.messages.MessagesHelper.*;
import static eu.wisebed.wiseml.WiseMLHelper.serialize;

@WebService
public class WSNServiceImpl extends AbstractService implements WSNService {

	private static final Logger log = LoggerFactory.getLogger(WSNServiceImpl.class);

	private final ServicePublisher servicePublisher;

	private final DeviceConfigDB deviceConfigDB;

	private final String secretReservationKey;

	private final Reservation reservation;

	private final DeliveryManager deliveryManager;

	private ServicePublisherService jaxWsService;

	@Inject
	public WSNServiceImpl(final ServicePublisher servicePublisher,
						  final DeviceConfigDB deviceConfigDB,
						  @Assisted final String secretReservationKey,
						  @Assisted final Reservation reservation,
						  @Assisted final DeliveryManager deliveryManager) {

		this.servicePublisher = checkNotNull(servicePublisher);
		this.deviceConfigDB = checkNotNull(deviceConfigDB);
		this.secretReservationKey = checkNotNull(secretReservationKey);
		this.reservation = checkNotNull(reservation);
		this.deliveryManager = checkNotNull(deliveryManager);
	}

	@Override
	protected void doStart() {
		try {
			jaxWsService = servicePublisher.createJaxWsService("/soap/v3/wsn/" + secretReservationKey, this);
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
	public void addController(final String controllerEndpointUrl) {

		log.debug("WSNServiceImpl.addController({})", controllerEndpointUrl);

		if (!"NONE".equals(controllerEndpointUrl)) {
			NetworkUtils.checkConnectivity(controllerEndpointUrl);
		}

		deliveryManager.addController(controllerEndpointUrl);
	}

	@Override
	public void areNodesAlive(final long requestId, final List<NodeUrn> nodeUrns) {
		reservation.getReservationEventBus().post(newAreNodesAliveRequest(requestId, nodeUrns));
		throw new RuntimeException("Assure that requestIds do not interfere with other concurrently running requests");
	}

	@Override
	public void destroyVirtualLinks(final long requestId, final List<Link> links) {
		reservation.getReservationEventBus().post(newDisableVirtualLinksRequest(requestId, convert(links)));
		throw new RuntimeException("Assure that requestIds do not interfere with other concurrently running requests");
	}

	@Override
	public void disableNodes(final long requestId, final List<NodeUrn> nodeUrns) {
		reservation.getReservationEventBus().post(newDisableNodesRequest(requestId, nodeUrns));
		throw new RuntimeException("Assure that requestIds do not interfere with other concurrently running requests");
	}

	@Override
	public void disablePhysicalLinks(final long requestId, final List<Link> links) {
		reservation.getReservationEventBus().post(newDisablePhysicalLinksRequest(requestId, convert(links)));
		throw new RuntimeException("Assure that requestIds do not interfere with other concurrently running requests");
	}

	@Override
	public void disableVirtualization() throws VirtualizationNotSupported_Exception {
		throw new RuntimeException("TODO implement");
	}

	@Override
	public void enableVirtualization() throws VirtualizationNotSupported_Exception {
		throw new RuntimeException("TODO implement");
	}

	@Override
	public void enableNodes(final long requestId, final List<NodeUrn> nodeUrns) {
		reservation.getReservationEventBus().post(newEnableNodesRequest(requestId, nodeUrns));
		throw new RuntimeException("Assure that requestIds do not interfere with other concurrently running requests");
	}

	@Override
	public void enablePhysicalLinks(final long requestId, final List<Link> links) {
		reservation.getReservationEventBus().post(newEnablePhysicalLinksRequest(requestId, convert(links)));
		throw new RuntimeException("Assure that requestIds do not interfere with other concurrently running requests");
	}

	@Override
	public void flashPrograms(final long requestId, final List<FlashProgramsConfiguration> configurations) {
		throw new RuntimeException("TODO implement");
	}

	@Override
	public List<ChannelPipelinesMap> getChannelPipelines(final List<NodeUrn> nodeUrns) {
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
		reservation.getReservationEventBus().post(newResetNodesRequest(requestId, nodeUrns));
		throw new RuntimeException("Assure that requestIds do not interfere with other concurrently running requests");
	}

	@Override
	public void send(final long requestId, final List<NodeUrn> nodeUrns, final byte[] message) {
		reservation.getReservationEventBus().post(newSendDownstreamMessageRequest(requestId, nodeUrns, message));
		throw new RuntimeException("Assure that requestIds do not interfere with other concurrently running requests");
	}

	@Override
	public void setChannelPipeline(final long requestId, final List<NodeUrn> nodeUrns,
								   final List<ChannelHandlerConfiguration> channelHandlerConfigurations) {
		reservation.getReservationEventBus().post(
				newSetChannelPipelinesRequest(requestId, nodeUrns, convert(channelHandlerConfigurations))
		);
		throw new RuntimeException("Assure that requestIds do not interfere with other concurrently running requests");
	}

	@Override
	public void setSerialPortParameters(final List<NodeUrn> nodeUrns, final SerialPortParameters parameters) {
		throw new RuntimeException("TODO implement");
	}

	@Override
	public void setVirtualLinks(final long requestId, final List<VirtualLink> links) {
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

	private Iterable<? extends SetChannelPipelinesRequest.ChannelHandlerConfiguration> convert(
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
}
