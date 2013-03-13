package de.uniluebeck.itm.tr.iwsn.portal.api.soap.v3;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import de.uniluebeck.itm.tr.devicedb.DeviceDB;
import de.uniluebeck.itm.tr.iwsn.common.DeliveryManager;
import de.uniluebeck.itm.tr.iwsn.messages.SetChannelPipelinesRequest;
import de.uniluebeck.itm.tr.iwsn.portal.Reservation;
import de.uniluebeck.itm.tr.util.NetworkUtils;
import eu.wisebed.api.v3.common.KeyValuePair;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.wsn.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newLinkedList;
import static de.uniluebeck.itm.tr.devicedb.WiseMLConverter.convertToWiseML;
import static de.uniluebeck.itm.tr.iwsn.messages.MessagesHelper.*;
import static eu.wisebed.wiseml.WiseMLHelper.serialize;

/**
 * Instances of this class provide the functionality offered by
 * {@link WSN} interface which is part of WISEBED API without
 * authorization checks.
 */
public class WSNImpl implements WSN {

	private static final Logger log = LoggerFactory.getLogger(WSNImpl.class);

	private final DeviceDB deviceDB;

	private final Reservation reservation;

	private final DeliveryManager deliveryManager;

	private final String reservationId;

	@Inject
	public WSNImpl(final DeviceDB deviceDB,
	               @Assisted final String reservationId,
	               @Assisted final Reservation reservation,
	               @Assisted final DeliveryManager deliveryManager) {
		this.reservationId = reservationId;
		this.deviceDB = checkNotNull(deviceDB);
		this.reservation = checkNotNull(reservation);
		this.deliveryManager = checkNotNull(deliveryManager);
	}

	@Override
	public void addController(final String controllerEndpointUrl){

		log.debug("WSNImpl.addController({})", controllerEndpointUrl);

		if (!"NONE".equals(controllerEndpointUrl)) {
			NetworkUtils.checkConnectivity(controllerEndpointUrl);
		}

		deliveryManager.addController(controllerEndpointUrl);

		if (reservation.getInterval().containsNow()) {
			deliveryManager.reservationStarted(reservation.getInterval().getStart(), controllerEndpointUrl);
		}

		if (reservation.getInterval().isBeforeNow()) {
			deliveryManager.reservationEnded(reservation.getInterval().getEnd(), controllerEndpointUrl);
		}
	}

	@Override
	public void areNodesAlive(long requestId, List<NodeUrn> nodeUrns)
			throws ReservationNotRunningFault_Exception {
		assertReservationIntervalMet();
		reservation.getReservationEventBus().post(newAreNodesAliveRequest(reservationId, requestId, nodeUrns));
	}

	@Override
	public void disableVirtualLinks(long requestId, List<Link> links)
			throws ReservationNotRunningFault_Exception {
		assertReservationIntervalMet();
		reservation.getReservationEventBus().post(
				newDisableVirtualLinksRequest(reservationId, requestId, convert(links))
		);
	}

	@Override
	public void disableNodes(long requestId, List<NodeUrn> nodeUrns)
			throws ReservationNotRunningFault_Exception {
		assertReservationIntervalMet();
		reservation.getReservationEventBus().post(newDisableNodesRequest(reservationId, requestId, nodeUrns));
	}

	@Override
	public void disablePhysicalLinks(long requestId, List<Link> links)
			throws ReservationNotRunningFault_Exception {
		assertReservationIntervalMet();
		reservation.getReservationEventBus()
				.post(newDisablePhysicalLinksRequest(reservationId, requestId, convert(links)));
	}

	@Override
	public void disableVirtualization()
			throws VirtualizationNotSupportedFault_Exception, ReservationNotRunningFault_Exception {
		assertReservationIntervalMet();
		throw new RuntimeException("TODO implement");
	}

	@Override
	public void enableVirtualization() throws VirtualizationNotSupportedFault_Exception,
			ReservationNotRunningFault_Exception {
		assertReservationIntervalMet();
		throw new RuntimeException("TODO implement");
	}

	@Override
	public void enableNodes(long requestId, List<NodeUrn> nodeUrns)
			throws ReservationNotRunningFault_Exception {
		assertReservationIntervalMet();
		reservation.getReservationEventBus().post(newEnableNodesRequest(reservationId, requestId, nodeUrns));
	}

	@Override
	public void enablePhysicalLinks(long requestId, List<Link> links)
			throws ReservationNotRunningFault_Exception {
		assertReservationIntervalMet();
		reservation.getReservationEventBus().post(
				newEnablePhysicalLinksRequest(reservationId, requestId, convert(links))
		);
	}

	@Override
	public void flashPrograms(long requestId, List<FlashProgramsConfiguration> configurations)
			throws ReservationNotRunningFault_Exception {
		assertReservationIntervalMet();
		for (FlashProgramsConfiguration configuration : configurations) {
			reservation.getReservationEventBus().post(newFlashImagesRequest(
					reservationId,
					requestId,
					configuration.getNodeUrns(),
					configuration.getProgram()
			)
			);
		}
	}

	@Override
	public List<ChannelPipelinesMap> getChannelPipelines(List<NodeUrn> nodeUrns)
			throws ReservationNotRunningFault_Exception {
		assertReservationIntervalMet();
		throw new RuntimeException("TODO implement");
	}

	@Override
	public String getNetwork() {
		return serialize(convertToWiseML(deviceDB.getConfigsByNodeUrns(reservation.getNodeUrns()).values()));
	}

	@Override
	public void removeController(String controllerEndpointUrl){
		log.debug("WSNImpl.removeController({})", controllerEndpointUrl);
		deliveryManager.removeController(controllerEndpointUrl);
	}

	@Override
	public void resetNodes( long requestId, List<NodeUrn> nodeUrns)
			throws ReservationNotRunningFault_Exception {
		assertReservationIntervalMet();
		reservation.getReservationEventBus().post(newResetNodesRequest(reservationId, requestId, nodeUrns));
	}

	@Override
	public void send(long requestId, List<NodeUrn> nodeUrns, byte[] message)
			throws ReservationNotRunningFault_Exception {
		assertReservationIntervalMet();
		reservation.getReservationEventBus().post(
				newSendDownstreamMessageRequest(reservationId, requestId, nodeUrns, message)
		);
	}

	@Override
	public void setChannelPipeline(long requestId,
	                               List<NodeUrn> nodeUrns,
	                               List<ChannelHandlerConfiguration> channelHandlerConfigurations)
			throws ReservationNotRunningFault_Exception {
		assertReservationIntervalMet();
		reservation.getReservationEventBus().post(newSetChannelPipelinesRequest(
				reservationId,
				requestId,
				nodeUrns,
				convertCHCs(channelHandlerConfigurations)
		)
		);
	}

	@Override
	public void setSerialPortParameters(List<NodeUrn> nodeUrns, SerialPortParameters parameters)
			throws ReservationNotRunningFault_Exception {
		assertReservationIntervalMet();
		throw new RuntimeException("TODO implement");
	}

	@Override
	public void enableVirtualLinks(long requestId, List<VirtualLink> links)
			throws ReservationNotRunningFault_Exception {
		assertReservationIntervalMet();
		reservation.getReservationEventBus().post(
				newEnableVirtualLinksRequest(reservationId, requestId, convertVirtualLinks(links))
		);
		// TODO remember virtual link mapping in specialized class that also delivers vlink messages to remote instance
		throw new RuntimeException("TODO only partially implemented");
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

	private void assertReservationIntervalMet() throws ReservationNotRunningFault_Exception {
		if (!reservation.getInterval().containsNow()) {
			ReservationNotRunningFault fault = new ReservationNotRunningFault();
			final String message = reservation.getInterval().isBeforeNow() ?
					"Reservation interval is over" :
					"Reservation interval lies in the future";
			fault.setMessage(message);
			throw new RuntimeException(new ReservationNotRunningFault_Exception(message, fault));
		}
	}
}
