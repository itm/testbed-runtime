package de.uniluebeck.itm.tr.iwsn.messages;

import com.google.common.collect.Multimap;
import com.google.protobuf.ByteString;
import de.uniluebeck.itm.tr.common.IdProvider;
import de.uniluebeck.itm.tr.common.TimestampProvider;
import eu.wisebed.api.v3.common.NodeUrn;
import org.joda.time.DateTime;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.isEmpty;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;

public class MessageFactoryImpl implements MessageFactory {

	private final IdProvider idProvider;

	private final TimestampProvider timestampProvider;

	@Inject
	public MessageFactoryImpl(IdProvider idProvider, TimestampProvider timestampProvider) {
		this.idProvider = idProvider;
		this.timestampProvider = timestampProvider;
	}

	private static Iterable<Link> toLinks(final Multimap<NodeUrn, NodeUrn> linkMap) {
		List<Link> links = newArrayList();
		for (NodeUrn sourceNodeUrn : linkMap.keySet()) {
			for (NodeUrn targetNodeUrn : linkMap.get(sourceNodeUrn)) {
				links.add(Link.newBuilder()
								.setSourceNodeUrn(sourceNodeUrn.toString())
								.setTargetNodeUrn(targetNodeUrn.toString())
								.build()
				);
			}
		}
		return links;
	}

	@Override
	public UpstreamMessageEvent upstreamMessageEvent(Optional<Long> timestamp,
													 NodeUrn nodeUrn,
													 byte[] bytes) {
		return UpstreamMessageEvent
				.newBuilder()
				.setHeader(eventHeader(timestamp, Optional.of(newArrayList(nodeUrn))))
				.setMessageBytes(ByteString.copyFrom(bytes))
				.build();
	}

	@Override
	public NotificationEvent notificationEvent(Optional<NodeUrn> nodeUrn,
											   Optional<Long> timestamp,
											   String message) {

		Optional<Iterable<NodeUrn>> nodeUrns = nodeUrn.isPresent() ?
				Optional.of(newArrayList(nodeUrn.get())) :
				Optional.empty();

		return NotificationEvent
				.newBuilder()
				.setHeader(eventHeader(timestamp, nodeUrns))
				.setMessage(message)
				.build();
	}

	@Override
	public DevicesAttachedEvent devicesAttachedEvent(Optional<Long> timestamp, Iterable<NodeUrn> nodeUrns) {

		checkArgument(!isEmpty(nodeUrns));

		return DevicesAttachedEvent.newBuilder()
				.setHeader(eventHeader(timestamp, Optional.of(nodeUrns)))
				.build();
	}

	@Override
	public DevicesAttachedEvent devicesAttachedEvent(Optional<Long> timestamp, NodeUrn... nodeUrns) {

		checkNotNull(nodeUrns);
		checkArgument(nodeUrns.length > 0);

		return DevicesAttachedEvent
				.newBuilder()
				.setHeader(eventHeader(timestamp, Optional.of(newArrayList(nodeUrns))))
				.build();
	}

	@Override
	public DevicesDetachedEvent devicesDetachedEvent(Optional<Long> timestamp, Iterable<NodeUrn> nodeUrns) {

		checkArgument(!isEmpty(nodeUrns));

		return DevicesDetachedEvent
				.newBuilder()
				.setHeader(eventHeader(timestamp, Optional.of(nodeUrns)))
				.build();
	}

	@Override
	public DevicesDetachedEvent devicesDetachedEvent(Optional<Long> timestamp, NodeUrn... nodeUrns) {

		checkNotNull(nodeUrns);
		checkArgument(nodeUrns.length > 0);

		return DevicesDetachedEvent
				.newBuilder()
				.setHeader(eventHeader(timestamp, Optional.of(newArrayList(nodeUrns))))
				.build();
	}

	@Override
	public GatewayConnectedEvent gatewayConnectedEvent(Optional<Long> timestamp, String hostname) {

		return GatewayConnectedEvent.newBuilder()
				.setHeader(eventHeader(timestamp, Optional.empty()))
				.setHostname(hostname)
				.build();
	}

	@Override
	public GatewayDisconnectedEvent gatewayDisconnectedEvent(Optional<Long> timestamp,
															 String hostname,
															 Iterable<NodeUrn> nodeUrns) {
		return GatewayDisconnectedEvent
				.newBuilder()
				.setHeader(eventHeader(timestamp, Optional.of(nodeUrns)))
				.setHostname(hostname)
				.build();
	}

	@Override
	public SetChannelPipelinesRequest setChannelPipelinesRequest(Optional<String> reservationId,
																 Optional<Long> timestamp,
																 Iterable<NodeUrn> nodeUrns,
																 Iterable<? extends ChannelHandlerConfiguration> channelHandlerConfigurations) {
		return SetChannelPipelinesRequest
				.newBuilder()
				.setHeader(header(reservationId, timestamp, nodeUrns))
				.addAllChannelHandlerConfigurations(channelHandlerConfigurations)
				.build();
	}

	@Override
	public SingleNodeProgress singleNodeProgress(Optional<String> reservationId,
												 Optional<Long> timestamp,
												 MessageType requestType,
												 long requestId,
												 NodeUrn nodeUrn,
												 int progressInPercent) {
		checkArgument(
				progressInPercent >= 0 && progressInPercent <= 100,
				"A progress in percent can only be between 0 and 100 (actual value: " + progressInPercent + ")"
		);

		RequestResponseHeader.Builder header = header(reservationId, timestamp, newArrayList(nodeUrn))
				.setRequestId(requestId)
				.setUpstream(true)
				.setDownstream(false);

		return SingleNodeProgress
				.newBuilder()
				.setHeader(header)
				.setProgressInPercent(progressInPercent)
				.setRequestType(requestType)
				.build();
	}

	@Override
	public SingleNodeResponse singleNodeResponse(Optional<String> reservationId,
												 Optional<Long> timestamp,
												 MessageType requestType,
												 long requestId,
												 NodeUrn nodeUrn,
												 int statusCode,
												 Optional<String> errorMessage) {

		RequestResponseHeader.Builder header = header(reservationId, timestamp, newArrayList(nodeUrn))
				.setRequestId(requestId)
				.setUpstream(true)
				.setDownstream(false);

		final SingleNodeResponse.Builder builder = SingleNodeResponse.newBuilder()
				.setHeader(header)
				.setRequestType(requestType)
				.setStatusCode(statusCode);

		if (errorMessage.isPresent()) {
			builder.setErrorMessage(errorMessage.get());
		}

		return builder.build();
	}

	@Override
	public ReservationStartedEvent reservationStartedEvent(Optional<Long> timestamp, String serializedKey) {

		EventHeader.Builder header = eventHeader(timestamp, Optional.empty())
				.setDownstream(true)
				.setUpstream(true);

		return ReservationStartedEvent
				.newBuilder()
				.setHeader(header)
				.setSerializedKey(serializedKey)
				.build();
	}

	@Override
	public ReservationEndedEvent reservationEndedEvent(Optional<Long> timestamp, String serializedKey) {

		EventHeader.Builder header = eventHeader(timestamp, Optional.empty())
				.setDownstream(true)
				.setUpstream(true);

		return ReservationEndedEvent
				.newBuilder()
				.setHeader(header)
				.setSerializedKey(serializedKey)
				.build();
	}

	@Override
	public ReservationMadeEvent reservationMadeEvent(Optional<Long> timestamp, String serializedKey) {

		EventHeader.Builder header = eventHeader(timestamp, Optional.empty())
				.setDownstream(true)
				.setUpstream(true);

		return ReservationMadeEvent
				.newBuilder()
				.setHeader(header)
				.setSerializedKey(serializedKey)
				.build();
	}

	@Override
	public ReservationOpenedEvent reservationOpenedEvent(Optional<Long> timestamp, String serializedKey) {

		EventHeader.Builder header = eventHeader(timestamp, Optional.empty())
				.setDownstream(true)
				.setUpstream(true);

		return ReservationOpenedEvent
				.newBuilder()
				.setHeader(header)
				.setSerializedKey(serializedKey)
				.build();
	}

	@Override
	public ReservationClosedEvent reservationClosedEvent(Optional<Long> timestamp, String serializedKey) {

		EventHeader.Builder header = eventHeader(timestamp, Optional.empty())
				.setDownstream(true)
				.setUpstream(true);

		return ReservationClosedEvent
				.newBuilder()
				.setHeader(header)
				.setSerializedKey(serializedKey)
				.build();
	}

	@Override
	public ReservationFinalizedEvent reservationFinalizedEvent(Optional<Long> timestamp, String serializedKey) {

		EventHeader.Builder header = eventHeader(timestamp, Optional.empty())
				.setDownstream(true)
				.setUpstream(true);

		return ReservationFinalizedEvent
				.newBuilder()
				.setHeader(header)
				.setSerializedKey(serializedKey)
				.build();
	}

	@Override
	public EventAck eventAck(long eventId, Optional<Long> timestamp) {

		EventHeader.Builder header = EventHeader
				.newBuilder()
				.setEventId(eventId)
				.setTimestamp(timestamp.orElse(timestampProvider.get()))
				.setDownstream(true)
				.setUpstream(false);

		return EventAck.newBuilder().setHeader(header).build();
	}

	private EventHeader.Builder eventHeader(Optional<Long> timestamp,
											Optional<Iterable<NodeUrn>> nodeUrns) {

		EventHeader.Builder builder = EventHeader
				.newBuilder()
				.setEventId(idProvider.get())
				.setTimestamp(timestamp.orElse(timestampProvider.get()));

		if (nodeUrns.isPresent()) {
			builder.addAllNodeUrns(transform(nodeUrns.get(), NodeUrn::toString));
		}

		return builder;
	}

	@Override
	public AreNodesConnectedRequest areNodesConnectedRequest(Optional<String> reservationId,
															 Optional<Long> timestamp,
															 Iterable<NodeUrn> nodeUrns) {
		return AreNodesConnectedRequest
				.newBuilder()
				.setHeader(header(reservationId, timestamp, nodeUrns))
				.build();
	}

	@Override
	public DisableNodesRequest disableNodesRequest(Optional<String> reservationId,
												   Optional<Long> timestamp,
												   Iterable<NodeUrn> nodeUrns) {
		return DisableNodesRequest
				.newBuilder()
				.setHeader(header(reservationId, timestamp, nodeUrns))
				.build();
	}

	@Override
	public EnableNodesRequest enableNodesRequest(Optional<String> reservationId,
												 Optional<Long> timestamp,
												 Iterable<NodeUrn> nodeUrns) {
		return EnableNodesRequest
				.newBuilder()
				.setHeader(header(reservationId, timestamp, nodeUrns))
				.build();
	}

	@Override
	public ResetNodesRequest resetNodesRequest(Optional<String> reservationId,
											   Optional<Long> timestamp,
											   Iterable<NodeUrn> nodeUrns) {
		return ResetNodesRequest
				.newBuilder()
				.setHeader(header(reservationId, timestamp, nodeUrns))
				.build();
	}

	@Override
	public SendDownstreamMessagesRequest sendDownstreamMessageRequest(Optional<String> reservationId,
																	  Optional<Long> timestamp,
																	  Iterable<NodeUrn> nodeUrns,
																	  byte[] bytes) {
		return SendDownstreamMessagesRequest
				.newBuilder()
				.setHeader(header(reservationId, timestamp, nodeUrns))
				.setMessageBytes(ByteString.copyFrom(bytes))
				.build();
	}

	@Override
	public FlashImagesRequest flashImagesRequest(Optional<String> reservationId,
												 Optional<Long> timestamp,
												 Iterable<NodeUrn> nodeUrns,
												 byte[] image) {
		return FlashImagesRequest
				.newBuilder()
				.setHeader(header(reservationId, timestamp, nodeUrns))
				.setImage(ByteString.copyFrom(image))
				.build();
	}

	@Override
	public DisableVirtualLinksRequest disableVirtualLinksRequest(Optional<String> reservationId,
																 Optional<Long> timestamp,
																 Multimap<NodeUrn, NodeUrn> links) {
		return DisableVirtualLinksRequest
				.newBuilder()
				.setHeader(header(reservationId, timestamp, links.keySet()))
				.addAllLinks(toLinks(links))
				.build();
	}

	@Override
	public EnableVirtualLinksRequest enableVirtualLinksRequest(Optional<String> reservationId,
															   Optional<Long> timestamp,
															   Multimap<NodeUrn, NodeUrn> links) {
		return EnableVirtualLinksRequest
				.newBuilder()
				.setHeader(header(reservationId, timestamp, links.keySet()))
				.addAllLinks(toLinks(links))
				.build();
	}

	@Override
	public DisablePhysicalLinksRequest disablePhysicalLinksRequest(Optional<String> reservationId,
																   Optional<Long> timestamp,
																   Multimap<NodeUrn, NodeUrn> links) {
		return DisablePhysicalLinksRequest
				.newBuilder()
				.setHeader(header(reservationId, timestamp, links.keySet()))
				.addAllLinks(toLinks(links))
				.build();
	}

	@Override
	public EnablePhysicalLinksRequest enablePhysicalLinksRequest(Optional<String> reservationId,
																 Optional<Long> timestamp,
																 Multimap<NodeUrn, NodeUrn> links) {
		return EnablePhysicalLinksRequest
				.newBuilder()
				.setHeader(header(reservationId, timestamp, links.keySet()))
				.addAllLinks(toLinks(links))
				.build();
	}

	@Override
	public GetChannelPipelinesRequest getChannelPipelinesRequest(Optional<String> reservationId,
																 Optional<Long> timestamp,
																 Iterable<NodeUrn> nodeUrns) {
		return GetChannelPipelinesRequest
				.newBuilder()
				.setHeader(header(reservationId, timestamp, nodeUrns))
				.build();
	}

	@Override
	public GetChannelPipelinesResponse getChannelPipelinesResponse(Optional<String> reservationId,
																   Optional<Long> timestamp,
																   Map<NodeUrn, List<ChannelHandlerConfiguration>> channelHandlerConfigurationMap) {

		GetChannelPipelinesResponse.Builder builder = GetChannelPipelinesResponse
				.newBuilder()
				.setHeader(header(reservationId, timestamp, channelHandlerConfigurationMap.keySet()));

		for (Map.Entry<NodeUrn, List<ChannelHandlerConfiguration>> entry : channelHandlerConfigurationMap.entrySet()) {

			final GetChannelPipelinesResponse.GetChannelPipelineResponse.Builder perNodeBuilder =
					GetChannelPipelinesResponse.GetChannelPipelineResponse
							.newBuilder()
							.setNodeUrn(entry.getKey().toString())
							.addAllHandlerConfigurations(entry.getValue());

			builder.addPipelines(perNodeBuilder);
		}
		return builder.build();
	}

	@Override
	public AreNodesAliveRequest areNodesAliveRequest(Optional<String> reservationId,
													 Optional<Long> timestamp,
													 Iterable<NodeUrn> nodeUrns) {
		return AreNodesAliveRequest
				.newBuilder()
				.setHeader(header(reservationId, timestamp, nodeUrns))
				.build();
	}

	@Override
	public DeviceConfigCreatedEvent deviceConfigCreatedEvent(NodeUrn nodeUrn, Optional<Long> timestamp) {
		return  DeviceConfigCreatedEvent
				.newBuilder()
				.setHeader(eventHeader(timestamp, Optional.of(newArrayList(nodeUrn))))
				.build();
	}

	@Override
	public DeviceConfigUpdatedEvent deviceConfigUpdatedEvent(NodeUrn nodeUrn, Optional<Long> timestamp) {
		return  DeviceConfigUpdatedEvent
				.newBuilder()
				.setHeader(eventHeader(timestamp, Optional.of(newArrayList(nodeUrn))))
				.build();
	}

	@Override
	public DeviceConfigDeletedEvent deviceConfigDeletedEvent(NodeUrn nodeUrn, Optional<Long> timestamp) {
		return  DeviceConfigDeletedEvent
				.newBuilder()
				.setHeader(eventHeader(timestamp, Optional.of(newArrayList(nodeUrn))))
				.build();
	}

	@Override
	public DeviceConfigDeletedEvent deviceConfigDeletedEvent(Iterable<NodeUrn> nodeUrns, Optional<Long> timestamp) {
		return  DeviceConfigDeletedEvent
				.newBuilder()
				.setHeader(eventHeader(timestamp, Optional.of(nodeUrns)))
				.build();
	}

	/**
	 * Creates a new request/response header.
	 *
	 * @param reservationId an (optional) reservation ID that identifies the reservation to which this request belongs
	 * @param timestamp     a Unix timestamp. If absent the timestamp will be set to the current time
	 * @param nodeUrns      zero or more node URNs
	 * @return the header
	 */
	private RequestResponseHeader.Builder header(Optional<String> reservationId,
												 Optional<Long> timestamp,
												 Iterable<NodeUrn> nodeUrns) {

		RequestResponseHeader.Builder builder = RequestResponseHeader
				.newBuilder()
				.setRequestId(idProvider.get())
				.setTimestamp(timestamp.orElse(DateTime.now().getMillis()))
				.addAllNodeUrns(transform(nodeUrns, NodeUrn::toString));

		if (reservationId.isPresent()) {
			builder.setReservationId(reservationId.get());
		}

		return builder;
	}
}
