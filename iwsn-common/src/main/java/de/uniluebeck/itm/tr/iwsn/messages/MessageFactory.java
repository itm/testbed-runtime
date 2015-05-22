package de.uniluebeck.itm.tr.iwsn.messages;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.protobuf.ByteString;
import eu.wisebed.api.v3.common.NodeUrn;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.isEmpty;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;

public abstract class MessageFactory {

	public static AreNodesAliveRequest areNodesAliveRequest(Optional<String> reservationId,
															long requestId,
															Optional<Long> timestamp,
															Iterable<NodeUrn> nodeUrns) {
		return AreNodesAliveRequest
				.newBuilder()
				.setHeader(header(reservationId, requestId, timestamp, nodeUrns))
				.build();
	}

	public static AreNodesConnectedRequest areNodesConnectedRequest(Optional<String> reservationId,
																	long requestId,
																	Optional<Long> timestamp,
																	Iterable<NodeUrn> nodeUrns) {
		return AreNodesConnectedRequest
				.newBuilder()
				.setHeader(header(reservationId, requestId, timestamp, nodeUrns))
				.build();
	}

	public static DisableNodesRequest disableNodesRequest(Optional<String> reservationId,
														  long requestId,
														  Optional<Long> timestamp,
														  Iterable<NodeUrn> nodeUrns) {
		return DisableNodesRequest
				.newBuilder()
				.setHeader(header(reservationId, requestId, timestamp, nodeUrns))
				.build();
	}

	public static EnableNodesRequest enableNodesRequest(Optional<String> reservationId,
														long requestId,
														Optional<Long> timestamp,
														Iterable<NodeUrn> nodeUrns) {
		return EnableNodesRequest
				.newBuilder()
				.setHeader(header(reservationId, requestId, timestamp, nodeUrns))
				.build();
	}

	public static ResetNodesRequest resetNodesRequest(Optional<String> reservationId,
													  long requestId,
													  Optional<Long> timestamp,
													  Iterable<NodeUrn> nodeUrns) {
		return ResetNodesRequest
				.newBuilder()
				.setHeader(header(reservationId, requestId, timestamp, nodeUrns))
				.build();
	}

	public static SendDownstreamMessagesRequest sendDownstreamMessageRequest(Optional<String> reservationId,
																			 long requestId,
																			 Optional<Long> timestamp,
																			 Iterable<NodeUrn> nodeUrns,
																			 ByteString bytes) {
		return SendDownstreamMessagesRequest
				.newBuilder()
				.setHeader(header(reservationId, requestId, timestamp, nodeUrns))
				.setMessageBytes(bytes)
				.build();
	}

	public static FlashImagesRequest flashImagesRequest(Optional<String> reservationId,
														long requestId,
														Optional<Long> timestamp,
														Iterable<NodeUrn> nodeUrns,
														ByteString image) {
		return FlashImagesRequest
				.newBuilder()
				.setHeader(header(reservationId, requestId, timestamp, nodeUrns))
				.setImage(image)
				.build();
	}

	public static DisableVirtualLinksRequest disableVirtualLinksRequest(Optional<String> reservationId,
																		long requestId,
																		Optional<Long> timestamp,
																		Multimap<NodeUrn, NodeUrn> links) {
		return DisableVirtualLinksRequest
				.newBuilder()
				.setHeader(header(reservationId, requestId, timestamp, links.keySet()))
				.addAllLinks(toLinks(links))
				.build();
	}

	public static EnableVirtualLinksRequest enableVirtualLinksRequest(Optional<String> reservationId,
																	  long requestId,
																	  Optional<Long> timestamp,
																	  Multimap<NodeUrn, NodeUrn> links) {
		return EnableVirtualLinksRequest
				.newBuilder()
				.setHeader(header(reservationId, requestId, timestamp, links.keySet()))
				.addAllLinks(toLinks(links))
				.build();
	}

	public static DisablePhysicalLinksRequest disablePhysicalLinksRequest(Optional<String> reservationId,
																		  long requestId,
																		  Optional<Long> timestamp,
																		  Multimap<NodeUrn, NodeUrn> links) {
		return DisablePhysicalLinksRequest
				.newBuilder()
				.setHeader(header(reservationId, requestId, timestamp, links.keySet()))
				.addAllLinks(toLinks(links))
				.build();
	}

	public static EnablePhysicalLinksRequest enablePhysicalLinksRequest(Optional<String> reservationId,
																		long requestId,
																		Optional<Long> timestamp,
																		Multimap<NodeUrn, NodeUrn> links) {
		return EnablePhysicalLinksRequest
				.newBuilder()
				.setHeader(header(reservationId, requestId, timestamp, links.keySet()))
				.addAllLinks(toLinks(links))
				.build();
	}

	public static GetChannelPipelinesRequest getChannelPipelinesRequest(Optional<String> reservationId,
																		long requestId,
																		Optional<Long> timestamp,
																		Iterable<NodeUrn> nodeUrns) {
		return GetChannelPipelinesRequest
				.newBuilder()
				.setHeader(header(reservationId, requestId, timestamp, nodeUrns))
				.build();
	}

	public static GetChannelPipelinesResponse getChannelPipelinesResponse(Optional<String> reservationId,
																		  long requestId,
																		  Optional<Long> timestamp,
																		  Map<NodeUrn, List<ChannelHandlerConfiguration>> channelHandlerConfigurationMap) {

		GetChannelPipelinesResponse.Builder builder = GetChannelPipelinesResponse
				.newBuilder()
				.setHeader(header(reservationId, requestId, timestamp, channelHandlerConfigurationMap.keySet()));

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

	public static SetChannelPipelinesRequest setChannelPipelinesRequest(Optional<String> reservationId,
																		long requestId,
																		Optional<Long> timestamp,
																		Iterable<NodeUrn> nodeUrns,
																		Iterable<? extends ChannelHandlerConfiguration> channelHandlerConfigurations) {
		return SetChannelPipelinesRequest
				.newBuilder()
				.setHeader(header(reservationId, requestId, timestamp, nodeUrns))
				.addAllChannelHandlerConfigurations(channelHandlerConfigurations)
				.build();
	}

	public static DevicesAttachedEvent devicesAttachedEvent(long eventId,
															Optional<Long> timestamp,
															Iterable<NodeUrn> nodeUrns) {

		checkArgument(!isEmpty(nodeUrns));

		return DevicesAttachedEvent.newBuilder()
				.setHeader(eventHeader(eventId, timestamp, Optional.of(nodeUrns)))
				.build();
	}

	public static DevicesAttachedEvent devicesAttachedEvent(long eventId,
															Optional<Long> timestamp,
															NodeUrn... nodeUrns) {

		checkNotNull(nodeUrns);
		checkArgument(nodeUrns.length > 0);

		return DevicesAttachedEvent
				.newBuilder()
				.setHeader(eventHeader(eventId, timestamp, Optional.of(newArrayList(nodeUrns))))
				.build();
	}

	public static DevicesDetachedEvent devicesDetachedEvent(long eventId,
															Optional<Long> timestamp,
															Iterable<NodeUrn> nodeUrns) {

		checkArgument(!isEmpty(nodeUrns));

		return DevicesDetachedEvent
				.newBuilder()
				.setHeader(eventHeader(eventId, timestamp, Optional.of(nodeUrns)))
				.build();
	}

	public static DevicesDetachedEvent devicesDetachedEvent(long eventId,
															Optional<Long> timestamp,
															NodeUrn... nodeUrns) {

		checkNotNull(nodeUrns);
		checkArgument(nodeUrns.length > 0);

		return DevicesDetachedEvent
				.newBuilder()
				.setHeader(eventHeader(eventId, timestamp, Optional.of(newArrayList(nodeUrns))))
				.build();
	}

	public static GatewayConnectedEvent gatewayConnectedEvent(long eventId, Optional<Long> timestamp, String hostname) {

		return GatewayConnectedEvent.newBuilder()
				.setHeader(eventHeader(eventId, timestamp, Optional.empty()))
				.setHostname(hostname)
				.build();
	}

	public static GatewayDisconnectedEvent gatewayDisconnectedEvent(long eventId,
																	Optional<Long> timestamp,
																	String hostname,
																	Iterable<NodeUrn> nodeUrns) {
		return GatewayDisconnectedEvent
				.newBuilder()
				.setHeader(eventHeader(eventId, timestamp, Optional.of(nodeUrns)))
				.setHostname(hostname)
				.build();
	}

	public static boolean equals(final DevicesAttachedEvent event1, final DevicesAttachedEvent event2) {
		return event1.getHeader().getTimestamp() == event2.getHeader().getTimestamp() &&
				newHashSet(event1.getHeader().getNodeUrnsList()).equals(newHashSet(event2.getHeader().getNodeUrnsList()));
	}

	public static boolean equals(final DevicesDetachedEvent event1, final DevicesDetachedEvent event2) {
		return event1.getHeader().getTimestamp() == event2.getHeader().getTimestamp() &&
				newHashSet(event1.getHeader().getNodeUrnsList()).equals(newHashSet(event2.getHeader().getNodeUrnsList()));
	}

	public static NotificationEvent notificationEvent(long eventId,
													  Optional<NodeUrn> nodeUrn,
													  Optional<Long> timestamp,
													  String message) {

		Optional<Iterable<NodeUrn>> nodeUrns = nodeUrn.isPresent() ?
				Optional.of(newArrayList(nodeUrn.get())) :
				Optional.empty();

		return NotificationEvent
				.newBuilder()
				.setHeader(eventHeader(eventId, timestamp, nodeUrns))
				.setMessage(message)
				.build();
	}

	public static SingleNodeProgress singleNodeProgress(Optional<String> reservationId,
														Optional<Long> timestamp,
														long requestId,
														NodeUrn nodeUrn,
														int progressInPercent) {
		checkArgument(
				progressInPercent >= 0 && progressInPercent <= 100,
				"A progress in percent can only be between 0 and 100 (actual value: " + progressInPercent + ")"
		);

		return SingleNodeProgress
				.newBuilder()
				.setHeader(header(reservationId, requestId, timestamp, newArrayList(nodeUrn)))
				.setProgressInPercent(progressInPercent)
				.build();
	}

	public static SingleNodeResponse singleNodeResponse(Optional<String> reservationId,
														Optional<Long> timestamp,
														long requestId,
														NodeUrn nodeUrn,
														int statusCode,
														Optional<String> errorMessage) {

		final SingleNodeResponse.Builder builder = SingleNodeResponse.newBuilder()
				.setHeader(header(reservationId, requestId, timestamp, newArrayList(nodeUrn)))
				.setStatusCode(statusCode);

		if (errorMessage.isPresent()) {
			builder.setErrorMessage(errorMessage.get());
		}

		return builder.build();
	}

	public static UpstreamMessageEvent upstreamMessageEvent(long eventId,
															Optional<Long> timestamp,
															NodeUrn nodeUrn,
															byte[] bytes) {
		return UpstreamMessageEvent
				.newBuilder()
				.setHeader(eventHeader(eventId, timestamp, Optional.of(newArrayList(nodeUrn))))
				.setMessageBytes(ByteString.copyFrom(bytes))
				.build();
	}

	/**
	 * Creates a new request/response header.
	 *
	 * @param reservationId an (optional) reservation ID that identifies the reservation to which this request belongs
	 * @param requestId     a request ID
	 * @param timestamp     a Unix timestamp. If absent the timestamp will be set to the current time
	 * @param nodeUrns      zero or more node URNs
	 * @return the header
	 */
	private static RequestResponseHeader.Builder header(Optional<String> reservationId,
														long requestId,
														Optional<Long> timestamp,
														Iterable<NodeUrn> nodeUrns) {

		RequestResponseHeader.Builder builder = RequestResponseHeader
				.newBuilder()
				.setRequestId(requestId)
				.setTimestamp(timestamp.orElse(DateTime.now().getMillis()))
				.addAllNodeUrns(transform(nodeUrns, NodeUrn::toString));

		if (reservationId.isPresent()) {
			builder.setReservationId(reservationId.get());
		}

		return builder;
	}

	private static EventHeader eventHeader(long eventId,
										   Optional<Long> timestamp,
										   Optional<Iterable<NodeUrn>> nodeUrns) {
		EventHeader.Builder builder = EventHeader
				.newBuilder()
				.setEventId(eventId)
				.setTimestamp(timestamp.orElse(DateTime.now().getMillis()));

		if (nodeUrns.isPresent()) {
			builder.addAllNodeUrns(transform(nodeUrns.get(), NodeUrn::toString));
		}

		return builder.build();
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
}
