package de.uniluebeck.itm.tr.iwsn.portal.api.soap.v3;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import de.uniluebeck.itm.tr.common.WisemlProvider;
import de.uniluebeck.itm.tr.iwsn.common.DeliveryManager;
import de.uniluebeck.itm.tr.iwsn.common.DeliveryManagerController;
import de.uniluebeck.itm.tr.iwsn.common.DeliveryManagerTestbedClientController;
import de.uniluebeck.itm.tr.iwsn.portal.RequestIdProvider;
import de.uniluebeck.itm.tr.iwsn.portal.Reservation;
import de.uniluebeck.itm.tr.iwsn.portal.api.RequestHelper;
import de.uniluebeck.itm.util.NetworkUtils;
import de.uniluebeck.itm.util.scheduler.SchedulerService;
import eu.wisebed.api.v3.WisebedServiceHelper;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.wsn.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static de.uniluebeck.itm.tr.iwsn.messages.MessagesHelper.*;
import static de.uniluebeck.itm.tr.iwsn.portal.api.soap.v3.Converters.*;
import static eu.wisebed.wiseml.WiseMLHelper.serialize;

/**
 * Instances of this class provide the functionality offered by
 * {@link WSN} interface which is part of WISEBED API without
 * authorization checks.
 */
public class WSNImpl implements WSN {

	private static final Logger log = LoggerFactory.getLogger(WSNImpl.class);

	private final Reservation reservation;

	private final DeliveryManager deliveryManager;

	private final String reservationId;

	private final RequestIdProvider requestIdProvider;

	private final WisemlProvider wisemlProvider;

	private final SchedulerService schedulerService;

	@Inject
	public WSNImpl(final RequestIdProvider requestIdProvider,
				   final WisemlProvider wisemlProvider,
				   final SchedulerService schedulerService,
				   @Assisted final String reservationId,
				   @Assisted final Reservation reservation,
				   @Assisted final DeliveryManager deliveryManager) {
		this.schedulerService = schedulerService;
		this.wisemlProvider = checkNotNull(wisemlProvider);
		this.reservationId = checkNotNull(reservationId);
		this.requestIdProvider = checkNotNull(requestIdProvider);
		this.reservation = checkNotNull(reservation);
		this.deliveryManager = checkNotNull(deliveryManager);
	}

	@Override
	public void addController(final String controllerEndpointUrl){

		log.debug("WSNImpl.addController({})", controllerEndpointUrl);

		if (!"NONE".equals(controllerEndpointUrl)) {
			NetworkUtils.checkConnectivity(controllerEndpointUrl);
		}

		final DeliveryManagerController controller = createDeliveryManagerController(controllerEndpointUrl);

		deliveryManager.addController(controller);

		if (reservation.getInterval().containsNow()) {
			deliveryManager.reservationStarted(reservation.getInterval().getStart(), controller);
		}

		if (reservation.getInterval().isBeforeNow()) {
			deliveryManager.reservationEnded(reservation.getInterval().getEnd(), controller);
		}
	}

	private DeliveryManagerController createDeliveryManagerController(final String controllerEndpointUrl) {
		return new DeliveryManagerTestbedClientController(
					WisebedServiceHelper.getControllerService(controllerEndpointUrl, schedulerService),
					controllerEndpointUrl
			);
	}

	@Override
	public void areNodesAlive(long requestId, List<NodeUrn> nodeUrns)
			throws ReservationNotRunningFault_Exception {
		assertReservationIntervalMet();
		reservation.getReservationEventBus().post(newAreNodesAliveRequest(reservationId, requestId, nodeUrns));
	}

	@Override
	public void disableVirtualLinks(long requestId, List<Link> links)
			throws ReservationNotRunningFault_Exception, VirtualizationNotEnabledFault_Exception {
		assertReservationIntervalMet();
		assertVirtualizationEnabled();
		reservation.getReservationEventBus().post(
				newDisableVirtualLinksRequest(reservationId, requestId, convertLinksToMap(links))
		);
	}

	@Override
	public void disableNodes(long requestId, List<NodeUrn> nodeUrns)
			throws ReservationNotRunningFault_Exception, VirtualizationNotEnabledFault_Exception {
		assertReservationIntervalMet();
		assertVirtualizationEnabled();
		reservation.getReservationEventBus().post(newDisableNodesRequest(reservationId, requestId, nodeUrns));
	}

	@Override
	public void disablePhysicalLinks(long requestId, List<Link> links)
			throws ReservationNotRunningFault_Exception, VirtualizationNotEnabledFault_Exception {
		assertReservationIntervalMet();
		assertVirtualizationEnabled();
		reservation.getReservationEventBus()
				.post(newDisablePhysicalLinksRequest(reservationId, requestId, convertLinksToMap(links)));
	}

	@Override
	public void disableVirtualization()
			throws VirtualizationNotSupportedFault_Exception, ReservationNotRunningFault_Exception {
		throwVirtualizationNotSupportedFault();
	}

	@Override
	public void enableVirtualization() throws VirtualizationNotSupportedFault_Exception,
			ReservationNotRunningFault_Exception {
		throwVirtualizationNotSupportedFault();
	}

	@Override
	public void enableNodes(long requestId, List<NodeUrn> nodeUrns)
			throws ReservationNotRunningFault_Exception, VirtualizationNotEnabledFault_Exception {
		assertReservationIntervalMet();
		assertVirtualizationEnabled();
		reservation.getReservationEventBus().post(newEnableNodesRequest(reservationId, requestId, nodeUrns));
	}

	@Override
	public void enablePhysicalLinks(long requestId, List<Link> links)
			throws ReservationNotRunningFault_Exception, VirtualizationNotEnabledFault_Exception {
		assertReservationIntervalMet();
		assertVirtualizationEnabled();
		reservation.getReservationEventBus().post(
				newEnablePhysicalLinksRequest(reservationId, requestId, convertLinksToMap(links))
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
		return RequestHelper.getChannelPipelines(
				nodeUrns,
				reservationId,
				requestIdProvider.get(),
				reservation.getReservationEventBus()
		);
	}

	@Override
	public String getNetwork() {
		return serialize(wisemlProvider.get(reservation.getNodeUrns()));
	}

	@Override
	public void removeController(String controllerEndpointUrl){
		log.debug("WSNImpl.removeController({})", controllerEndpointUrl);
		deliveryManager.removeController(createDeliveryManagerController(controllerEndpointUrl));
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
			throws ReservationNotRunningFault_Exception, VirtualizationNotEnabledFault_Exception {
		assertReservationIntervalMet();
		assertVirtualizationEnabled();
		reservation.getReservationEventBus().post(
				newEnableVirtualLinksRequest(reservationId, requestId, convertVirtualLinks(links))
		);
		// TODO remember virtual link mapping in specialized class that also delivers virtual link messages to remote instance
		throw new RuntimeException("TODO only partially implemented");
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

	private void assertVirtualizationEnabled() throws VirtualizationNotEnabledFault_Exception {
		if (!reservation.isVirtualizationEnabled()) {
			final String message = "Virtualization features are not enabled! Please enable them by calling WSN.enableVirtualization()";
			final VirtualizationNotEnabledFault faultInfo = new VirtualizationNotEnabledFault();
			faultInfo.setMessage(message);
			throw new VirtualizationNotEnabledFault_Exception(message, faultInfo);
		}
	}

	private void throwVirtualizationNotSupportedFault() throws VirtualizationNotSupportedFault_Exception {
		final String message = "Virtualization features are currently not supported!";
		final VirtualizationNotSupportedFault faultInfo = new VirtualizationNotSupportedFault();
		faultInfo.setMessage(message);
		throw new VirtualizationNotSupportedFault_Exception(message, faultInfo);
	}
}
