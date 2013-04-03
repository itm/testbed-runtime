package de.uniluebeck.itm.tr.iwsn.portal.api.soap.v3;

import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.SettableFuture;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import de.uniluebeck.itm.tr.devicedb.DeviceDB;
import de.uniluebeck.itm.tr.iwsn.common.DeliveryManager;
import de.uniluebeck.itm.tr.iwsn.messages.Request;
import de.uniluebeck.itm.tr.iwsn.portal.RequestIdProvider;
import de.uniluebeck.itm.tr.iwsn.portal.Reservation;
import de.uniluebeck.itm.tr.util.NetworkUtils;
import de.uniluebeck.itm.tr.util.SettableFutureMap;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.wsn.*;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Maps.newHashMap;
import static de.uniluebeck.itm.tr.devicedb.WiseMLConverter.convertToWiseML;
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

	private static final String SCHEDULING_ERROR_MESSAGE =
			"Operation scheduling is not yet implemented. Please don't provide timestamps for operation invocations!";

	private final DeviceDB deviceDB;

	private final Reservation reservation;

	private final DeliveryManager deliveryManager;

	private final String reservationId;

	private final RequestIdProvider requestIdProvider;

	@Inject
	public WSNImpl(final DeviceDB deviceDB,
				   final RequestIdProvider requestIdProvider,
				   @Assisted final String reservationId,
				   @Assisted final Reservation reservation,
				   @Assisted final DeliveryManager deliveryManager) {

		this.reservationId = checkNotNull(reservationId);
		this.requestIdProvider = checkNotNull(requestIdProvider);
		this.deviceDB = checkNotNull(deviceDB);
		this.reservation = checkNotNull(reservation);
		this.deliveryManager = checkNotNull(deliveryManager);
	}

	@Override
	public void addController(final String controllerEndpointUrl, @Nullable final DateTime timestamp) {

		log.debug("WSNImpl.addController({}, {})", controllerEndpointUrl, timestamp);

		checkArgument(timestamp == null, SCHEDULING_ERROR_MESSAGE);

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
	public void areNodesAlive(long requestId, List<NodeUrn> nodeUrns, @Nullable final DateTime timestamp)
			throws ReservationNotRunningFault_Exception {
		log.trace("WSNImpl.areNodesAlive({}, {}, {})", requestId, nodeUrns, timestamp);
		checkArgument(timestamp == null, SCHEDULING_ERROR_MESSAGE);
		assertReservationIntervalMet();
		reservation.getReservationEventBus().post(newAreNodesAliveRequest(reservationId, requestId, nodeUrns));
	}

	@Override
	public void disableVirtualLinks(long requestId, List<Link> links, @Nullable final DateTime timestamp)
			throws ReservationNotRunningFault_Exception {
		log.trace("WSNImpl.disableVirtualLinks({}, {}, {})", requestId, links, timestamp);
		checkArgument(timestamp == null, SCHEDULING_ERROR_MESSAGE);
		assertReservationIntervalMet();
		reservation.getReservationEventBus().post(
				newDisableVirtualLinksRequest(reservationId, requestId, convertLinksToMap(links))
		);
	}

	@Override
	public void disableNodes(long requestId, List<NodeUrn> nodeUrns, @Nullable final DateTime timestamp)
			throws ReservationNotRunningFault_Exception {
		log.trace("WSNImpl.disableNodes({}, {}, {})", requestId, nodeUrns, timestamp);
		checkArgument(timestamp == null, SCHEDULING_ERROR_MESSAGE);
		assertReservationIntervalMet();
		reservation.getReservationEventBus().post(newDisableNodesRequest(reservationId, requestId, nodeUrns));
	}

	@Override
	public void disablePhysicalLinks(long requestId, List<Link> links, @Nullable final DateTime timestamp)
			throws ReservationNotRunningFault_Exception {
		log.trace("WSNImpl.disablePhysicalLinks({}, {}, {})", requestId, links, timestamp);
		checkArgument(timestamp == null, SCHEDULING_ERROR_MESSAGE);
		assertReservationIntervalMet();
		reservation.getReservationEventBus()
				.post(newDisablePhysicalLinksRequest(reservationId, requestId, convertLinksToMap(links)));
	}

	@Override
	public void disableVirtualization(@Nullable final DateTime timestamp)
			throws VirtualizationNotSupportedFault_Exception, ReservationNotRunningFault_Exception {
		log.trace("WSNImpl.disableVirtualization({})", timestamp);
		checkArgument(timestamp == null, SCHEDULING_ERROR_MESSAGE);
		assertReservationIntervalMet();
		throw new RuntimeException("TODO implement");
	}

	@Override
	public void enableVirtualization(@Nullable final DateTime timestamp)
			throws VirtualizationNotSupportedFault_Exception,
			ReservationNotRunningFault_Exception {
		log.trace("WSNImpl.enableVirtualization({})", timestamp);
		checkArgument(timestamp == null, SCHEDULING_ERROR_MESSAGE);
		assertReservationIntervalMet();
		throw new RuntimeException("TODO implement");
	}

	@Override
	public void enableNodes(long requestId, List<NodeUrn> nodeUrns, @Nullable final DateTime timestamp)
			throws ReservationNotRunningFault_Exception {
		log.trace("WSNImpl.enableNodes({}, {}, {})", requestId, nodeUrns, timestamp);
		checkArgument(timestamp == null, SCHEDULING_ERROR_MESSAGE);
		assertReservationIntervalMet();
		reservation.getReservationEventBus().post(newEnableNodesRequest(reservationId, requestId, nodeUrns));
	}

	@Override
	public void enablePhysicalLinks(long requestId, List<Link> links, @Nullable final DateTime timestamp)
			throws ReservationNotRunningFault_Exception {
		log.trace("WSNImpl.enablePhysicalLinks({}, {}, {})", requestId, links, timestamp);
		checkArgument(timestamp == null, SCHEDULING_ERROR_MESSAGE);
		assertReservationIntervalMet();
		reservation.getReservationEventBus().post(
				newEnablePhysicalLinksRequest(reservationId, requestId, convertLinksToMap(links))
		);
	}

	@Override
	public void flashPrograms(long requestId, List<FlashProgramsConfiguration> configurations,
							  @Nullable final DateTime timestamp)
			throws ReservationNotRunningFault_Exception {
		log.trace("WSNImpl.flashPrograms({}, {}, {})", requestId, configurations, timestamp);
		checkArgument(timestamp == null, SCHEDULING_ERROR_MESSAGE);
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

		final long requestId = requestIdProvider.get();
		final Request request = newGetChannelPipelinesRequest(
				reservationId,
				requestId,
				nodeUrns
		);

		final
		Map<NodeUrn, SettableFuture<de.uniluebeck.itm.tr.iwsn.messages.GetChannelPipelinesResponse.GetChannelPipelineResponse>>
				map = newHashMap();
		for (NodeUrn nodeUrn : nodeUrns) {
			map.put(nodeUrn, SettableFuture
					.<de.uniluebeck.itm.tr.iwsn.messages.GetChannelPipelinesResponse.GetChannelPipelineResponse>create()
			);
		}
		final
		SettableFutureMap<NodeUrn, de.uniluebeck.itm.tr.iwsn.messages.GetChannelPipelinesResponse.GetChannelPipelineResponse>
				future =
				new SettableFutureMap<NodeUrn, de.uniluebeck.itm.tr.iwsn.messages.GetChannelPipelinesResponse.GetChannelPipelineResponse>(
						map
				);

		final Object eventBusListener = new Object() {
			@Subscribe
			public void onResponse(de.uniluebeck.itm.tr.iwsn.messages.GetChannelPipelinesResponse response) {
				if (response.getRequestId() == requestId) {
					for (de.uniluebeck.itm.tr.iwsn.messages.GetChannelPipelinesResponse.GetChannelPipelineResponse p : response
							.getPipelinesList()) {

						final
						SettableFuture<de.uniluebeck.itm.tr.iwsn.messages.GetChannelPipelinesResponse.GetChannelPipelineResponse>
								nodeFuture =
								map.get(new NodeUrn(p.getNodeUrn()));
						nodeFuture.set(p);
					}
				}
			}
		};

		reservation.getReservationEventBus().register(eventBusListener);
		reservation.getReservationEventBus().post(request);

		try {

			final
			Map<NodeUrn, de.uniluebeck.itm.tr.iwsn.messages.GetChannelPipelinesResponse.GetChannelPipelineResponse>
					resultMap =
					future.get(30, TimeUnit.SECONDS);
			reservation.getReservationEventBus().unregister(eventBusListener);
			return convert(resultMap);

		} catch (Exception e) {
			throw propagate(e);
		}
	}

	@Override
	public String getNetwork() {
		return serialize(convertToWiseML(deviceDB.getConfigsByNodeUrns(reservation.getNodeUrns()).values()));
	}

	@Override
	public void removeController(String controllerEndpointUrl, @Nullable final DateTime timestamp) {
		log.trace("WSNImpl.removeController({}, {})", controllerEndpointUrl, timestamp);
		checkArgument(timestamp == null, SCHEDULING_ERROR_MESSAGE);
		deliveryManager.removeController(controllerEndpointUrl);
	}

	@Override
	public void resetNodes(long requestId, List<NodeUrn> nodeUrns, @Nullable final DateTime timestamp)
			throws ReservationNotRunningFault_Exception {
		log.trace("WSNImpl.resetNodes({}, {}, {})", requestId, nodeUrns, timestamp);
		checkArgument(timestamp == null, SCHEDULING_ERROR_MESSAGE);
		assertReservationIntervalMet();
		reservation.getReservationEventBus().post(newResetNodesRequest(reservationId, requestId, nodeUrns));
	}

	@Override
	public void send(long requestId, List<NodeUrn> nodeUrns, byte[] message, @Nullable final DateTime timestamp)
			throws ReservationNotRunningFault_Exception {
		log.trace("WSNImpl.send({}, {}, {}, {})", requestId, nodeUrns, message, timestamp);
		checkArgument(timestamp == null, SCHEDULING_ERROR_MESSAGE);
		assertReservationIntervalMet();
		reservation.getReservationEventBus().post(
				newSendDownstreamMessageRequest(reservationId, requestId, nodeUrns, message)
		);
	}

	@Override
	public void setChannelPipeline(long requestId,
								   List<NodeUrn> nodeUrns,
								   List<ChannelHandlerConfiguration> channelHandlerConfigurations,
								   @Nullable final DateTime timestamp)
			throws ReservationNotRunningFault_Exception {
		log.trace("WSNImpl.setChannelPipeline({}, {}, {}, {})", requestId, nodeUrns, channelHandlerConfigurations,
				timestamp
		);
		checkArgument(timestamp == null, SCHEDULING_ERROR_MESSAGE);
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
	public void setSerialPortParameters(List<NodeUrn> nodeUrns, SerialPortParameters parameters,
										@Nullable final DateTime timestamp)
			throws ReservationNotRunningFault_Exception {
		log.trace("WSNImpl.setSerialPortParameters({}, {}, {})", nodeUrns, parameters, timestamp);
		checkArgument(timestamp == null, SCHEDULING_ERROR_MESSAGE);
		assertReservationIntervalMet();
		throw new RuntimeException("TODO implement");
	}

	@Override
	public void enableVirtualLinks(long requestId, List<VirtualLink> links, @Nullable final DateTime timestamp)
			throws ReservationNotRunningFault_Exception {
		log.trace("WSNImpl.enableVirtualLinks({}, {}, {})", requestId, links, timestamp);
		checkArgument(timestamp == null, SCHEDULING_ERROR_MESSAGE);
		assertReservationIntervalMet();
		reservation.getReservationEventBus().post(
				newEnableVirtualLinksRequest(reservationId, requestId, convertVirtualLinks(links))
		);
		// TODO remember virtual link mapping in specialized class that also delivers vlink messages to remote instance
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
}
