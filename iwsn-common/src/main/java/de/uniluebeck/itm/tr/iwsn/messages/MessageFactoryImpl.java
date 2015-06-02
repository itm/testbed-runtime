package de.uniluebeck.itm.tr.iwsn.messages;

import com.google.common.collect.Multimap;
import com.google.protobuf.ByteString;
import com.google.protobuf.MessageLite;
import de.uniluebeck.itm.tr.common.IdProvider;
import de.uniluebeck.itm.tr.common.TimestampProvider;
import eu.wisebed.api.v3.common.NodeUrn;
import org.joda.time.DateTime;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.isEmpty;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static de.uniluebeck.itm.tr.iwsn.messages.MessageType.*;
import static java.util.Optional.empty;
import static java.util.Optional.of;

public class MessageFactoryImpl implements MessageFactory {

	private final IdProvider idProvider;

	private final TimestampProvider timestampProvider;

	@Inject
	public MessageFactoryImpl(IdProvider idProvider, TimestampProvider timestampProvider) {
		this.idProvider = idProvider;
		this.timestampProvider = timestampProvider;
	}

	@SuppressWarnings("CodeBlock2Expr")
	private static List<Link> toLinks(final Multimap<NodeUrn, NodeUrn> linkMap) {

		List<Link> links = newArrayList();

		linkMap.keySet().forEach(source -> {
			linkMap.get(source).forEach(target -> {
				links.add(Link.newBuilder()
								.setSourceNodeUrn(source.toString())
								.setTargetNodeUrn(target.toString())
								.build()
				);
			});
		});
		return links;
	}

	@Override
	public UpstreamMessageEvent upstreamMessageEvent(Optional<Long> timestamp,
													 NodeUrn nodeUrn,
													 byte[] bytes) {
		return upstreamMessageEventBuilder(timestamp, empty(), nodeUrn, bytes).build();
	}

	private UpstreamMessageEvent.Builder upstreamMessageEventBuilder(Optional<Long> timestamp,
																	 Optional<Long> correlationId,
																	 NodeUrn nodeUrn,
																	 byte[] bytes) {
		return UpstreamMessageEvent
				.newBuilder()
				.setHeader(header(empty(), correlationId, timestamp, EVENT_UPSTREAM_MESSAGE, of(newArrayList(nodeUrn)), true, false))
				.setMessageBytes(ByteString.copyFrom(bytes));
	}

	@Override
	public NotificationEvent notificationEvent(Optional<Iterable<NodeUrn>> nodeUrns,
											   Optional<Long> timestamp,
											   String message) {
		return NotificationEvent
				.newBuilder()
				.setHeader(header(empty(), empty(), timestamp, EVENT_NOTIFICATION, nodeUrns, true, false))
				.setMessage(message)
				.build();
	}

	@Override
	public DevicesAttachedEvent devicesAttachedEvent(Optional<Long> timestamp, Iterable<NodeUrn> nodeUrns) {

		checkArgument(!isEmpty(nodeUrns));

		return devicesAttachedEventBuilder(timestamp, nodeUrns)
				.build();
	}

	@Override
	public DevicesAttachedEvent devicesAttachedEvent(Optional<Long> timestamp, NodeUrn... nodeUrns) {

		checkNotNull(nodeUrns);
		checkArgument(nodeUrns.length > 0);

		return devicesAttachedEventBuilder(timestamp, newArrayList(nodeUrns)).build();
	}

	private DevicesAttachedEvent.Builder devicesAttachedEventBuilder(Optional<Long> timestamp, Iterable<NodeUrn> nodeUrns) {
		return DevicesAttachedEvent.newBuilder()
				.setHeader(header(empty(), empty(), timestamp, EVENT_DEVICES_ATTACHED, of(nodeUrns), true, false));
	}

	@Override
	public DevicesDetachedEvent devicesDetachedEvent(Optional<Long> timestamp, Iterable<NodeUrn> nodeUrns) {

		checkArgument(!isEmpty(nodeUrns));

		return devicesDetachedEventBuilder(timestamp, nodeUrns).build();
	}

	@Override
	public DevicesDetachedEvent devicesDetachedEvent(Optional<Long> timestamp, NodeUrn... nodeUrns) {

		checkNotNull(nodeUrns);
		checkArgument(nodeUrns.length > 0);

		return devicesDetachedEventBuilder(timestamp, newArrayList(nodeUrns)).build();
	}

	private DevicesDetachedEvent.Builder devicesDetachedEventBuilder(Optional<Long> timestamp, Iterable<NodeUrn> value) {
		return DevicesDetachedEvent
				.newBuilder()
				.setHeader(header(empty(), empty(), timestamp, EVENT_DEVICES_DETACHED, of(value), true, false));
	}

	@Override
	public GatewayConnectedEvent gatewayConnectedEvent(Optional<Long> timestamp, String hostname) {

		return GatewayConnectedEvent.newBuilder()
				.setHeader(header(empty(), empty(), timestamp, EVENT_GATEWAY_CONNECTED, empty(), true, false))
				.setHostname(hostname)
				.build();
	}

	@Override
	public GatewayDisconnectedEvent gatewayDisconnectedEvent(Optional<Long> timestamp,
															 String hostname,
															 Iterable<NodeUrn> nodeUrns) {
		return GatewayDisconnectedEvent
				.newBuilder()
				.setHeader(header(empty(), empty(), timestamp, EVENT_GATEWAY_DISCONNECTED, of(nodeUrns), true, false))
				.setHostname(hostname)
				.build();
	}

	@Override
	public SetChannelPipelinesRequest setChannelPipelinesRequest(Optional<String> serializedReservationKey,
																 Optional<Long> timestamp,
																 Iterable<NodeUrn> nodeUrns,
																 Iterable<? extends ChannelHandlerConfiguration> channelHandlerConfigurations) {
		return setChannelPipelinesRequestBuilder(serializedReservationKey, empty(), timestamp, nodeUrns, channelHandlerConfigurations).build();
	}

	private SetChannelPipelinesRequest.Builder setChannelPipelinesRequestBuilder(Optional<String> serializedReservationKey,
																				 Optional<Long> correlationId,
																				 Optional<Long> timestamp,
																				 Iterable<NodeUrn> nodeUrns,
																				 Iterable<? extends ChannelHandlerConfiguration> channelHandlerConfigurations) {
		return SetChannelPipelinesRequest
				.newBuilder()
				.setHeader(header(serializedReservationKey, correlationId, timestamp, REQUEST_SET_CHANNEL_PIPELINES, of(nodeUrns), false, true))
				.addAllChannelHandlerConfigurations(channelHandlerConfigurations);
	}

	@Override
	public Progress progress(Optional<String> serializedReservationKey,
							 Optional<Long> timestamp,
							 long requestId,
							 Iterable<NodeUrn> nodeUrn,
							 int progressInPercent) {

		checkArgument(
				progressInPercent >= 0 && progressInPercent <= 100,
				"A progress in percent can only be between 0 and 100 (actual value: " + progressInPercent + ")"
		);

		return Progress.newBuilder()
				.setHeader(header(serializedReservationKey, of(requestId), timestamp, PROGRESS, of(newArrayList(nodeUrn)), true, false))
				.setProgressInPercent(progressInPercent)
				.build();
	}

	@Override
	public Response response(Optional<String> serializedReservationKey,
							 Optional<Long> timestamp,
							 long requestId,
							 Iterable<NodeUrn> nodeUrn,
							 int statusCode,
							 Optional<String> errorMessage,
							 Optional<byte[]> response) {

		final Response.Builder builder = Response.newBuilder()
				.setHeader(header(serializedReservationKey, of(requestId), timestamp, RESPONSE, of(newArrayList(nodeUrn)), true, false))
				.setStatusCode(statusCode);

		if (errorMessage.isPresent()) {
			builder.setErrorMessage(errorMessage.get());
		}

		if (response.isPresent()) {
			builder.setResponse(ByteString.copyFrom(response.get()));
		}

		return builder.build();
	}

	@Override
	public ReservationStartedEvent reservationStartedEvent(Optional<Long> timestamp, String serializedReservationKey) {

		final Header.Builder header = header(of(serializedReservationKey), empty(), timestamp, EVENT_RESERVATION_STARTED, empty(), true, true)
				.setBroadcast(true);

		return ReservationStartedEvent.newBuilder().setHeader(header).build();
	}

	@Override
	public ReservationEndedEvent reservationEndedEvent(Optional<Long> timestamp, String serializedReservationKey) {

		final Header.Builder header = header(of(serializedReservationKey), empty(), timestamp, EVENT_RESERVATION_ENDED, empty(), true, true)
				.setBroadcast(true);

		return ReservationEndedEvent.newBuilder().setHeader(header).build();
	}

	@Override
	public ReservationMadeEvent reservationMadeEvent(Optional<Long> timestamp, String serializedReservationKey) {

		final Header.Builder header = header(of(serializedReservationKey), empty(), timestamp, EVENT_RESERVATION_MADE, empty(), true, true)
				.setBroadcast(true);

		return ReservationMadeEvent.newBuilder().setHeader(header).build();
	}

	@Override
	public ReservationCancelledEvent reservationCancelledEvent(Optional<Long> timestamp, String serializedReservationKey) {

		final Header.Builder header = header(of(serializedReservationKey), empty(), timestamp, EVENT_RESERVATION_CANCELLED, empty(), true, true)
				.setBroadcast(true);

		return ReservationCancelledEvent.newBuilder().setHeader(header).build();
	}

	@Override
	public ReservationOpenedEvent reservationOpenedEvent(Optional<Long> timestamp, String serializedReservationKey) {

		final Header.Builder header = header(of(serializedReservationKey), empty(), timestamp, EVENT_RESERVATION_OPENED, empty(), true, true)
				.setBroadcast(true);

		return ReservationOpenedEvent.newBuilder().setHeader(header).build();
	}

	@Override
	public ReservationClosedEvent reservationClosedEvent(Optional<Long> timestamp, String serializedReservationKey) {

		final Header.Builder header = header(of(serializedReservationKey), empty(), timestamp, EVENT_RESERVATION_CLOSED, empty(), true, true)
				.setBroadcast(true);

		return ReservationClosedEvent.newBuilder().setHeader(header).build();
	}

	@Override
	public ReservationFinalizedEvent reservationFinalizedEvent(Optional<Long> timestamp, String serializedReservationKey) {

		final Header.Builder header = header(of(serializedReservationKey), empty(), timestamp, EVENT_RESERVATION_FINALIZED, empty(), true, true)
				.setBroadcast(true);

		return ReservationFinalizedEvent.newBuilder().setHeader(header).build();
	}

	@Override
	public EventAck eventAck(Header eventHeader, Optional<Long> timestamp) {

		Header.Builder header = Header.newBuilder()
				.setCorrelationId(eventHeader.getCorrelationId())
				.setTimestamp(timestamp.orElse(timestampProvider.get()))
				.setDownstream(!eventHeader.getDownstream()) // inverse direction
				.setUpstream(!eventHeader.getUpstream());    // inverse direction

		return EventAck.newBuilder().setHeader(header).build();
	}

	@Override
	public AreNodesConnectedRequest areNodesConnectedRequest(Optional<String> serializedReservationKey,
															 Optional<Long> timestamp,
															 Iterable<NodeUrn> nodeUrns) {
		return areNodesConnectedRequestBuilder(serializedReservationKey, empty(), timestamp, nodeUrns).build();
	}

	private AreNodesConnectedRequest.Builder areNodesConnectedRequestBuilder(Optional<String> serializedReservationKey,
																			 Optional<Long> correlationId,
																			 Optional<Long> timestamp,
																			 Iterable<NodeUrn> nodeUrns) {
		return AreNodesConnectedRequest
				.newBuilder()
				.setHeader(header(serializedReservationKey, correlationId, timestamp, REQUEST_ARE_NODES_CONNECTED, of(nodeUrns), false, true));
	}

	@Override
	public DisableNodesRequest disableNodesRequest(Optional<String> serializedReservationKey,
												   Optional<Long> timestamp,
												   Iterable<NodeUrn> nodeUrns) {
		return disableNodesRequestBuilder(serializedReservationKey, empty(), timestamp, nodeUrns).build();
	}

	private DisableNodesRequest.Builder disableNodesRequestBuilder(Optional<String> serializedReservationKey,
																   Optional<Long> correlationId,
																   Optional<Long> timestamp,
																   Iterable<NodeUrn> nodeUrns) {
		return DisableNodesRequest
				.newBuilder()
				.setHeader(header(serializedReservationKey, correlationId, timestamp, REQUEST_DISABLE_NODES, of(nodeUrns), false, true));
	}

	@Override
	public EnableNodesRequest enableNodesRequest(Optional<String> serializedReservationKey,
												 Optional<Long> timestamp,
												 Iterable<NodeUrn> nodeUrns) {
		return enableNodesRequestBuilder(serializedReservationKey, empty(), timestamp, nodeUrns).build();
	}

	private EnableNodesRequest.Builder enableNodesRequestBuilder(Optional<String> serializedReservationKey,
																 Optional<Long> correlationId,
																 Optional<Long> timestamp,
																 Iterable<NodeUrn> nodeUrns) {
		return EnableNodesRequest
				.newBuilder()
				.setHeader(header(serializedReservationKey, correlationId, timestamp, REQUEST_ENABLE_NODES, of(nodeUrns), false, true));
	}

	@Override
	public ResetNodesRequest resetNodesRequest(Optional<String> serializedReservationKey,
											   Optional<Long> timestamp,
											   Iterable<NodeUrn> nodeUrns) {
		return resetNodesRequestBuilder(serializedReservationKey, empty(), timestamp, nodeUrns).build();
	}

	private ResetNodesRequest.Builder resetNodesRequestBuilder(Optional<String> serializedReservationKey,
															   Optional<Long> correlationId,
															   Optional<Long> timestamp,
															   Iterable<NodeUrn> nodeUrns) {
		return ResetNodesRequest
				.newBuilder()
				.setHeader(header(serializedReservationKey, correlationId, timestamp, REQUEST_RESET_NODES, of(nodeUrns), false, true));
	}

	@Override
	public SendDownstreamMessagesRequest sendDownstreamMessageRequest(Optional<String> serializedReservationKey,
																	  Optional<Long> timestamp,
																	  Iterable<NodeUrn> nodeUrns,
																	  byte[] bytes) {
		return sendDownstreamMessageRequestBuilder(serializedReservationKey, empty(), timestamp, nodeUrns, ByteString.copyFrom(bytes)).build();
	}

	private SendDownstreamMessagesRequest.Builder sendDownstreamMessageRequestBuilder(Optional<String> serializedReservationKey,
																					  Optional<Long> correlationId,
																					  Optional<Long> timestamp,
																					  Iterable<NodeUrn> nodeUrns,
																					  ByteString bytes) {
		return SendDownstreamMessagesRequest
				.newBuilder()
				.setHeader(header(serializedReservationKey, correlationId, timestamp, REQUEST_SEND_DOWNSTREAM_MESSAGES, of(nodeUrns), false, true))
				.setMessageBytes(bytes);
	}

	@Override
	public FlashImagesRequest flashImagesRequest(Optional<String> serializedReservationKey,
												 Optional<Long> timestamp,
												 Iterable<NodeUrn> nodeUrns,
												 byte[] image) {
		return flashImagesRequestBuilder(serializedReservationKey, empty(), timestamp, nodeUrns, ByteString.copyFrom(image)).build();
	}

	private FlashImagesRequest.Builder flashImagesRequestBuilder(Optional<String> serializedReservationKey,
																 Optional<Long> correlationId,
																 Optional<Long> timestamp,
																 Iterable<NodeUrn> nodeUrns,
																 ByteString image) {
		return FlashImagesRequest
				.newBuilder()
				.setHeader(header(serializedReservationKey, correlationId, timestamp, REQUEST_FLASH_IMAGES, of(nodeUrns), false, true))
				.setImage(image);
	}

	@Override
	public DisableVirtualLinksRequest disableVirtualLinksRequest(Optional<String> serializedReservationKey,
																 Optional<Long> timestamp,
																 Multimap<NodeUrn, NodeUrn> links) {
		return disableVirtualLinksRequestBuilder(serializedReservationKey, empty(), timestamp, links.keySet(), toLinks(links)).build();
	}

	private DisableVirtualLinksRequest.Builder disableVirtualLinksRequestBuilder(Optional<String> serializedReservationKey,
																				 Optional<Long> correlationId,
																				 Optional<Long> timestamp,
																				 Set<NodeUrn> sourceNodeUrns,
																				 List<Link> links) {
		return DisableVirtualLinksRequest
				.newBuilder()
				.setHeader(header(serializedReservationKey, correlationId, timestamp, REQUEST_DISABLE_VIRTUAL_LINKS, of(sourceNodeUrns), false, true))
				.addAllLinks(links);
	}

	@Override
	public EnableVirtualLinksRequest enableVirtualLinksRequest(Optional<String> serializedReservationKey,
															   Optional<Long> timestamp,
															   Multimap<NodeUrn, NodeUrn> links) {
		return enableVirtualLinksRequestBuilder(serializedReservationKey, empty(), timestamp, links.keySet(), toLinks(links)).build();
	}

	private EnableVirtualLinksRequest.Builder enableVirtualLinksRequestBuilder(Optional<String> serializedReservationKey,
																			   Optional<Long> correlationId,
																			   Optional<Long> timestamp,
																			   Set<NodeUrn> sourceNodeUrns,
																			   List<Link> links) {
		return EnableVirtualLinksRequest
				.newBuilder()
				.setHeader(header(serializedReservationKey, correlationId, timestamp, REQUEST_ENABLE_VIRTUAL_LINKS, of(sourceNodeUrns), false, true))
				.addAllLinks(links);
	}

	@Override
	public DisablePhysicalLinksRequest disablePhysicalLinksRequest(Optional<String> serializedReservationKey,
																   Optional<Long> timestamp,
																   Multimap<NodeUrn, NodeUrn> links) {
		return disablePhysicalLinksRequestBuilder(serializedReservationKey, empty(), timestamp, links.keySet(), toLinks(links)).build();
	}

	private DisablePhysicalLinksRequest.Builder disablePhysicalLinksRequestBuilder(Optional<String> serializedReservationKey,
																				   Optional<Long> correlationId,
																				   Optional<Long> timestamp,
																				   Set<NodeUrn> sourceNodeUrns,
																				   List<Link> links) {
		return DisablePhysicalLinksRequest
				.newBuilder()
				.setHeader(header(serializedReservationKey, correlationId, timestamp, REQUEST_DISABLE_PHYSICAL_LINKS, of(sourceNodeUrns), false, true))
				.addAllLinks(links);
	}

	@Override
	public EnablePhysicalLinksRequest enablePhysicalLinksRequest(Optional<String> serializedReservationKey,
																 Optional<Long> timestamp,
																 Multimap<NodeUrn, NodeUrn> links) {
		return enablePhysicalLinksRequestBuilder(serializedReservationKey, empty(), timestamp, links.keySet(), toLinks(links)).build();
	}

	private EnablePhysicalLinksRequest.Builder enablePhysicalLinksRequestBuilder(Optional<String> serializedReservationKey,
																				 Optional<Long> correlationId,
																				 Optional<Long> timestamp,
																				 Set<NodeUrn> sourceNodeUrns,
																				 List<Link> links) {
		return EnablePhysicalLinksRequest
				.newBuilder()
				.setHeader(header(serializedReservationKey, correlationId, timestamp, REQUEST_ENABLE_PHYSICAL_LINKS, of(sourceNodeUrns), false, true))
				.addAllLinks(links);
	}

	@Override
	public GetChannelPipelinesRequest getChannelPipelinesRequest(Optional<String> serializedReservationKey,
																 Optional<Long> timestamp,
																 Iterable<NodeUrn> nodeUrns) {
		return getChannelPipelinesRequestBuilder(serializedReservationKey, empty(), timestamp, nodeUrns).build();
	}

	private GetChannelPipelinesRequest.Builder getChannelPipelinesRequestBuilder(Optional<String> serializedReservationKey,
																				 Optional<Long> correlationId,
																				 Optional<Long> timestamp,
																				 Iterable<NodeUrn> nodeUrns) {
		return GetChannelPipelinesRequest
				.newBuilder()
				.setHeader(header(serializedReservationKey, correlationId, timestamp, REQUEST_GET_CHANNEL_PIPELINES, of(nodeUrns), false, true));
	}

	@Override
	public GetChannelPipelinesResponse getChannelPipelinesResponse(Optional<String> serializedReservationKey,
																   Optional<Long> timestamp,
																   Map<NodeUrn, List<ChannelHandlerConfiguration>> channelHandlerConfigurationMap) {

		Header.Builder header = header(
				serializedReservationKey,
				empty(),
				timestamp,
				RESPONSE_GET_CHANNELPIPELINES,
				of(channelHandlerConfigurationMap.keySet()),
				false,
				true
		);

		GetChannelPipelinesResponse.Builder builder = GetChannelPipelinesResponse
				.newBuilder()
				.setHeader(header);

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
	public AreNodesAliveRequest areNodesAliveRequest(Optional<String> serializedReservationKey,
													 Optional<Long> timestamp,
													 Iterable<NodeUrn> nodeUrns) {
		return areNodesAliveRequestBuilder(serializedReservationKey, empty(), timestamp, nodeUrns).build();
	}

	private AreNodesAliveRequest.Builder areNodesAliveRequestBuilder(Optional<String> serializedReservationKey,
																	 Optional<Long> correlationId,
																	 Optional<Long> timestamp,
																	 Iterable<NodeUrn> nodeUrns) {
		return AreNodesAliveRequest
				.newBuilder()
				.setHeader(header(serializedReservationKey, correlationId, timestamp, REQUEST_ARE_NODES_ALIVE, of(nodeUrns), false, true));
	}

	@Override
	public DeviceConfigCreatedEvent deviceConfigCreatedEvent(NodeUrn nodeUrn, Optional<Long> timestamp) {

		final Header.Builder header = header(empty(), empty(), timestamp, EVENT_DEVICE_CONFIG_CREATED, of(newArrayList(nodeUrn)), false, true)
				.setBroadcast(true);

		return DeviceConfigCreatedEvent.newBuilder().setHeader(header).build();
	}

	@Override
	public DeviceConfigUpdatedEvent deviceConfigUpdatedEvent(NodeUrn nodeUrn, Optional<Long> timestamp) {

		final Header.Builder header = header(empty(), empty(), timestamp, EVENT_DEVICE_CONFIG_UPDATED, of(newArrayList(nodeUrn)), false, true)
				.setBroadcast(true);

		return DeviceConfigUpdatedEvent.newBuilder().setHeader(header).build();
	}

	@Override
	public DeviceConfigDeletedEvent deviceConfigDeletedEvent(NodeUrn nodeUrn, Optional<Long> timestamp) {

		final Header.Builder header = header(empty(), empty(), timestamp, EVENT_DEVICE_CONFIG_DELETED, of(newArrayList(nodeUrn)), false, true)
				.setBroadcast(true);

		return DeviceConfigDeletedEvent.newBuilder().setHeader(header).build();
	}

	@Override
	public DeviceConfigDeletedEvent deviceConfigDeletedEvent(Iterable<NodeUrn> nodeUrns, Optional<Long> timestamp) {

		final Header.Builder header = header(empty(), empty(), timestamp, EVENT_DEVICE_CONFIG_DELETED, of(nodeUrns), false, true)
				.setBroadcast(true);

		return DeviceConfigDeletedEvent.newBuilder().setHeader(header).build();
	}

	@Override
	public MessageHeaderPair splitRequest(MessageHeaderPair pair, Set<NodeUrn> subRequestNodeUrns) {

		final Header header = pair.header;
		final MessageLite message = pair.message;

		if (!MessageHeaderPair.isRequest(header.getType())) {
			throw new IllegalArgumentException("Only request types are allowed, got \"" + header.getType() + "\".");
		}

		List<Link> originalLinks;
		List<Link> subRequestLinks;

		switch (header.getType()) {

			case REQUEST_ARE_NODES_ALIVE:
				final AreNodesAliveRequest areNodesAliveRequest = areNodesAliveRequestBuilder(
						of(header.getSerializedReservationKey()),
						of(header.getCorrelationId()),
						of(header.getTimestamp()),
						subRequestNodeUrns
				).build();
				return new MessageHeaderPair(areNodesAliveRequest.getHeader(), areNodesAliveRequest);

			case REQUEST_ARE_NODES_CONNECTED:
				final AreNodesConnectedRequest areNodesConnectedRequest = areNodesConnectedRequestBuilder(
						of(header.getSerializedReservationKey()),
						of(header.getCorrelationId()),
						of(header.getTimestamp()),
						subRequestNodeUrns
				).build();
				return new MessageHeaderPair(areNodesConnectedRequest.getHeader(), areNodesConnectedRequest);

			case REQUEST_DISABLE_NODES:
				final DisableNodesRequest disableNodesRequest = disableNodesRequestBuilder(
						of(header.getSerializedReservationKey()),
						of(header.getCorrelationId()),
						of(header.getTimestamp()),
						subRequestNodeUrns
				).build();
				return new MessageHeaderPair(disableNodesRequest.getHeader(), disableNodesRequest);

			case REQUEST_DISABLE_VIRTUAL_LINKS:
				originalLinks = ((DisableVirtualLinksRequest) message).getLinksList();
				subRequestLinks = originalLinks.stream()
						.filter(link -> subRequestNodeUrns.contains(new NodeUrn(link.getSourceNodeUrn())))
						.collect(Collectors.toList());
				final DisableVirtualLinksRequest disableVirtualLinksRequest = disableVirtualLinksRequestBuilder(
						of(header.getSerializedReservationKey()),
						of(header.getCorrelationId()),
						of(header.getTimestamp()),
						subRequestNodeUrns,
						subRequestLinks
				).build();
				return new MessageHeaderPair(disableVirtualLinksRequest.getHeader(), disableVirtualLinksRequest);

			case REQUEST_DISABLE_PHYSICAL_LINKS:
				originalLinks = ((DisableVirtualLinksRequest) message).getLinksList();
				subRequestLinks = originalLinks.stream()
						.filter(link -> subRequestNodeUrns.contains(new NodeUrn(link.getSourceNodeUrn())))
						.collect(Collectors.toList());
				final DisablePhysicalLinksRequest disablePhysicalLinksRequest = disablePhysicalLinksRequestBuilder(
						of(header.getSerializedReservationKey()),
						of(header.getCorrelationId()),
						of(header.getTimestamp()),
						subRequestNodeUrns,
						subRequestLinks
				).build();
				return new MessageHeaderPair(disablePhysicalLinksRequest.getHeader(), disablePhysicalLinksRequest);

			case REQUEST_ENABLE_NODES:
				final EnableNodesRequest enableNodesRequest = enableNodesRequestBuilder(
						of(header.getSerializedReservationKey()),
						of(header.getCorrelationId()),
						of(header.getTimestamp()),
						subRequestNodeUrns
				).build();
				return new MessageHeaderPair(enableNodesRequest.getHeader(), enableNodesRequest);

			case REQUEST_ENABLE_PHYSICAL_LINKS:
				originalLinks = ((DisableVirtualLinksRequest) message).getLinksList();
				subRequestLinks = originalLinks.stream()
						.filter(link -> subRequestNodeUrns.contains(new NodeUrn(link.getSourceNodeUrn())))
						.collect(Collectors.toList());
				final EnablePhysicalLinksRequest enablePhysicalLinksRequest = enablePhysicalLinksRequestBuilder(
						of(header.getSerializedReservationKey()),
						of(header.getCorrelationId()),
						of(header.getTimestamp()),
						subRequestNodeUrns,
						subRequestLinks
				).build();
				return new MessageHeaderPair(enablePhysicalLinksRequest.getHeader(), enablePhysicalLinksRequest);

			case REQUEST_ENABLE_VIRTUAL_LINKS:
				originalLinks = ((DisableVirtualLinksRequest) message).getLinksList();
				subRequestLinks = originalLinks.stream()
						.filter(link -> subRequestNodeUrns.contains(new NodeUrn(link.getSourceNodeUrn())))
						.collect(Collectors.toList());
				final EnableVirtualLinksRequest enableVirtualLinksRequest = enableVirtualLinksRequestBuilder(
						of(header.getSerializedReservationKey()),
						of(header.getCorrelationId()),
						of(header.getTimestamp()),
						subRequestNodeUrns,
						subRequestLinks
				).build();
				return new MessageHeaderPair(enableVirtualLinksRequest.getHeader(), enableVirtualLinksRequest);

			case REQUEST_FLASH_IMAGES:
				final FlashImagesRequest flashImagesRequest = flashImagesRequestBuilder(
						of(header.getSerializedReservationKey()),
						of(header.getCorrelationId()),
						of(header.getTimestamp()),
						subRequestNodeUrns,
						((FlashImagesRequest) message).getImage()
				).build();
				return new MessageHeaderPair(flashImagesRequest.getHeader(), flashImagesRequest);

			case REQUEST_GET_CHANNEL_PIPELINES:
				final GetChannelPipelinesRequest getChannelPipelinesRequest = getChannelPipelinesRequestBuilder(
						of(header.getSerializedReservationKey()),
						of(header.getCorrelationId()),
						of(header.getTimestamp()),
						subRequestNodeUrns
				).build();
				return new MessageHeaderPair(getChannelPipelinesRequest.getHeader(), getChannelPipelinesRequest);

			case REQUEST_RESET_NODES:
				final ResetNodesRequest resetNodesRequest = resetNodesRequestBuilder(
						of(header.getSerializedReservationKey()),
						of(header.getCorrelationId()),
						of(header.getTimestamp()),
						subRequestNodeUrns
				).build();
				return new MessageHeaderPair(resetNodesRequest.getHeader(), resetNodesRequest);

			case REQUEST_SEND_DOWNSTREAM_MESSAGES:
				final SendDownstreamMessagesRequest sendDownstreamMessagesRequest = sendDownstreamMessageRequestBuilder(
						of(header.getSerializedReservationKey()),
						of(header.getCorrelationId()),
						of(header.getTimestamp()),
						subRequestNodeUrns,
						((SendDownstreamMessagesRequest) message).getMessageBytes()
				).build();
				return new MessageHeaderPair(sendDownstreamMessagesRequest.getHeader(), sendDownstreamMessagesRequest);

			case REQUEST_SET_CHANNEL_PIPELINES:
				final SetChannelPipelinesRequest setChannelPipelinesRequest = setChannelPipelinesRequestBuilder(
						of(header.getSerializedReservationKey()),
						of(header.getCorrelationId()),
						of(header.getTimestamp()),
						subRequestNodeUrns,
						((SetChannelPipelinesRequest) message).getChannelHandlerConfigurationsList()
				).build();
				return new MessageHeaderPair(setChannelPipelinesRequest.getHeader(), setChannelPipelinesRequest);
		}

		throw new IllegalArgumentException("Unknown message type \"" + header.getType() + "\"!");
	}

	@Override
	public MessageHeaderPair scopeEvent(MessageHeaderPair pair, Collection<NodeUrn> subEventNodeUrns) {

		final Header header = pair.header;
		final MessageLite message = pair.message;

		if (!MessageHeaderPair.isScopeableEvent(header.getType())) {
			throw new IllegalArgumentException("Only scopeable event types are allowed, got \"" + header.getType());
		}


		switch (header.getType()) {
			case EVENT_DEVICES_ATTACHED:
				return MessageHeaderPair.fromUnwrapped(
						devicesAttachedEventBuilder(of(pair.header.getTimestamp()), subEventNodeUrns)
				);
			case EVENT_DEVICES_DETACHED:
				return MessageHeaderPair.fromUnwrapped(
						devicesDetachedEvent(of(pair.header.getTimestamp()), subEventNodeUrns)
				);
			case EVENT_NOTIFICATION:
				return MessageHeaderPair.fromUnwrapped(notificationEvent(
						of(subEventNodeUrns),
						of(header.getTimestamp()),
						((NotificationEvent) message).getMessage()
				));
		}

		throw new IllegalArgumentException("Unknown message type \"" + header.getType() + "\"!");
	}

	/**
	 * Creates a new request/response header.
	 *
	 * @param serializedReservationKey an (optional) reservation ID that identifies the reservation to which this
	 *                                 request belongs
	 * @param correlationId            a correlation ID or Optional.empty() to generate one
	 * @param timestamp                a Unix timestamp. If absent the timestamp will be set to the current time
	 * @param type                     the type of the message wrapping this header
	 * @param nodeUrns                 zero or more node URNs
	 * @param upstream                 if the message wrapping this header should be sent upstream
	 * @param downstream               if the message wrapping this header should be sent downstream
	 * @return the header
	 */
	private Header.Builder header(Optional<String> serializedReservationKey,
								  Optional<Long> correlationId,
								  Optional<Long> timestamp,
								  MessageType type,
								  Optional<Iterable<NodeUrn>> nodeUrns,
								  boolean upstream,
								  boolean downstream) {

		Header.Builder builder = Header.newBuilder()
				.setCorrelationId(correlationId.orElse(idProvider.get()))
				.setDownstream(downstream)
				.setTimestamp(timestamp.orElse(DateTime.now().getMillis()))
				.setType(type)
				.setUpstream(upstream);

		if (nodeUrns.isPresent()) {
			builder.addAllNodeUrns(transform(nodeUrns.get(), NodeUrn::toString));
		}

		if (serializedReservationKey.isPresent()) {
			builder.setSerializedReservationKey(serializedReservationKey.get());
		}

		return builder;
	}
}
