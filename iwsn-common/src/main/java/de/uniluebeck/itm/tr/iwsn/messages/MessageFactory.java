package de.uniluebeck.itm.tr.iwsn.messages;

import com.google.common.collect.Multimap;
import eu.wisebed.api.v3.common.NodeUrn;

import java.util.*;

public interface MessageFactory {

	UpstreamMessageEvent upstreamMessageEvent(Optional<Long> timestamp,
											  NodeUrn nodeUrn,
											  byte[] bytes);

	NotificationEvent notificationEvent(Optional<Iterable<NodeUrn>> nodeUrn,
										Optional<Long> timestamp,
										String message);

	NotificationEvent notificationEvent(Optional<Iterable<NodeUrn>> nodeUrn,
										long correlationId,
										Optional<Long> timestamp,
										String message);

	DevicesAttachedEvent devicesAttachedEvent(Optional<Long> timestamp, Iterable<NodeUrn> nodeUrns);

	DevicesAttachedEvent devicesAttachedEvent(Optional<Long> timestamp, long correlationId, Iterable<NodeUrn> nodeUrns);

	DevicesAttachedEvent devicesAttachedEvent(Optional<Long> timestamp, NodeUrn... nodeUrns);

	DevicesAttachedEvent devicesAttachedEvent(Optional<Long> timestamp, long correlationId, NodeUrn... nodeUrns);

	DevicesDetachedEvent devicesDetachedEvent(Optional<Long> timestamp, Iterable<NodeUrn> nodeUrns);

	DevicesDetachedEvent devicesDetachedEvent(Optional<Long> timestamp, long correlationId, Iterable<NodeUrn> nodeUrns);

	DevicesDetachedEvent devicesDetachedEvent(Optional<Long> timestamp, NodeUrn... nodeUrns);

	DevicesDetachedEvent devicesDetachedEvent(Optional<Long> timestamp, long correlationId, NodeUrn... nodeUrns);

	GatewayConnectedEvent gatewayConnectedEvent(Optional<Long> timestamp, String hostname);

	GatewayDisconnectedEvent gatewayDisconnectedEvent(Optional<Long> timestamp,
													  String hostname,
													  Iterable<NodeUrn> nodeUrns);

	SetChannelPipelinesRequest setChannelPipelinesRequest(Optional<String> reservationId,
														  Optional<Long> timestamp,
														  Iterable<NodeUrn> nodeUrns,
														  Iterable<? extends ChannelHandlerConfiguration> channelHandlerConfigurations);

	SetChannelPipelinesRequest setChannelPipelinesRequest(Optional<String> reservationId,
														  long requestId,
														  Optional<Long> timestamp,
														  Iterable<NodeUrn> nodeUrns,
														  Iterable<? extends ChannelHandlerConfiguration> channelHandlerConfigurations);

	Progress progress(Optional<String> reservationId,
					  Optional<Long> timestamp,
					  long requestId,
					  Iterable<NodeUrn> nodeUrn,
					  int progressInPercent);

	Response response(Optional<String> reservationId,
					  Optional<Long> timestamp,
					  long requestId,
					  Iterable<NodeUrn> nodeUrn,
					  int statusCode,
					  Optional<String> errorMessage,
					  Optional<byte[]> response);

	ReservationStartedEvent reservationStartedEvent(Optional<Long> timestamp, String serializedReservationKey);

	ReservationEndedEvent reservationEndedEvent(Optional<Long> timestamp, String serializedReservationKey);

	ReservationMadeEvent reservationMadeEvent(Optional<Long> timestamp, String serializedReservationKey);

	ReservationCancelledEvent reservationCancelledEvent(Optional<Long> millis, String serializedReservationKey);

	ReservationOpenedEvent reservationOpenedEvent(Optional<Long> timestamp, String serializedReservationKey);

	ReservationClosedEvent reservationClosedEvent(Optional<Long> timestamp, String serializedReservationKey);

	ReservationFinalizedEvent reservationFinalizedEvent(Optional<Long> timestamp, String serializedReservationKey);

	EventAck eventAck(Header eventHeader, Optional<Long> timestamp);

	AreNodesConnectedRequest areNodesConnectedRequest(Optional<String> reservationId,
													  Optional<Long> timestamp,
													  Iterable<NodeUrn> nodeUrns);

	DisableNodesRequest disableNodesRequest(Optional<String> reservationId,
											Optional<Long> timestamp,
											Iterable<NodeUrn> nodeUrns);

	DisableNodesRequest disableNodesRequest(Optional<String> reservationId,
											long requestId,
											Optional<Long> timestamp,
											Iterable<NodeUrn> nodeUrns);

	EnableNodesRequest enableNodesRequest(Optional<String> reservationId,
										  Optional<Long> timestamp,
										  Iterable<NodeUrn> nodeUrns);

	EnableNodesRequest enableNodesRequest(Optional<String> reservationId,
										  long requestId,
										  Optional<Long> timestamp,
										  Iterable<NodeUrn> nodeUrns);

	ResetNodesRequest resetNodesRequest(Optional<String> reservationId,
										Optional<Long> timestamp,
										Iterable<NodeUrn> nodeUrns);

	ResetNodesRequest resetNodesRequest(Optional<String> reservationId,
										long requestId,
										Optional<Long> timestamp,
										Iterable<NodeUrn> nodeUrns);

	SendDownstreamMessagesRequest sendDownstreamMessageRequest(Optional<String> reservationId,
															   Optional<Long> timestamp,
															   Iterable<NodeUrn> nodeUrns,
															   byte[] bytes);

	SendDownstreamMessagesRequest sendDownstreamMessageRequest(Optional<String> reservationId,
															   long requestId,
															   Optional<Long> timestamp,
															   Iterable<NodeUrn> nodeUrns,
															   byte[] bytes);

	FlashImagesRequest flashImagesRequest(Optional<String> reservationId,
										  Optional<Long> timestamp,
										  Iterable<NodeUrn> nodeUrns,
										  byte[] image);

	FlashImagesRequest flashImagesRequest(Optional<String> reservationId,
										  long requestId,
										  Optional<Long> timestamp,
										  Iterable<NodeUrn> nodeUrns,
										  byte[] image);

	DisableVirtualLinksRequest disableVirtualLinksRequest(Optional<String> reservationId,
														  Optional<Long> timestamp,
														  Multimap<NodeUrn, NodeUrn> links);

	DisableVirtualLinksRequest disableVirtualLinksRequest(Optional<String> reservationId,
														  long requestId,
														  Optional<Long> timestamp,
														  Multimap<NodeUrn, NodeUrn> links);

	EnableVirtualLinksRequest enableVirtualLinksRequest(Optional<String> reservationId,
														Optional<Long> timestamp,
														Multimap<NodeUrn, NodeUrn> links);

	EnableVirtualLinksRequest enableVirtualLinksRequest(Optional<String> reservationId,
														long requestId,
														Optional<Long> timestamp,
														Multimap<NodeUrn, NodeUrn> links);

	DisablePhysicalLinksRequest disablePhysicalLinksRequest(Optional<String> reservationId,
															Optional<Long> timestamp,
															Multimap<NodeUrn, NodeUrn> links);

	DisablePhysicalLinksRequest disablePhysicalLinksRequest(Optional<String> reservationId,
															long requestId,
															Optional<Long> timestamp,
															Multimap<NodeUrn, NodeUrn> links);

	EnablePhysicalLinksRequest enablePhysicalLinksRequest(Optional<String> reservationId,
														  Optional<Long> timestamp,
														  Multimap<NodeUrn, NodeUrn> links);

	EnablePhysicalLinksRequest enablePhysicalLinksRequest(Optional<String> reservationId,
														  long requestId,
														  Optional<Long> timestamp,
														  Multimap<NodeUrn, NodeUrn> links);

	GetChannelPipelinesRequest getChannelPipelinesRequest(Optional<String> reservationId,
														  Optional<Long> timestamp,
														  Iterable<NodeUrn> nodeUrns);

	GetChannelPipelinesResponse getChannelPipelinesResponse(Optional<String> reservationId,
															Optional<Long> timestamp,
															Map<NodeUrn, List<ChannelHandlerConfiguration>> channelHandlerConfigurationMap);

	AreNodesAliveRequest areNodesAliveRequest(Optional<String> reservationId,
											  Optional<Long> timestamp,
											  Iterable<NodeUrn> nodeUrns);

	AreNodesAliveRequest areNodesAliveRequest(Optional<String> reservationId,
											  long requestId,
											  Optional<Long> timestamp,
											  Iterable<NodeUrn> nodeUrns);

	DeviceConfigCreatedEvent deviceConfigCreatedEvent(NodeUrn nodeUrn, Optional<Long> timestamp);

	DeviceConfigUpdatedEvent deviceConfigUpdatedEvent(NodeUrn nodeUrn, Optional<Long> timestamp);

	DeviceConfigDeletedEvent deviceConfigDeletedEvent(NodeUrn nodeUrn, Optional<Long> timestamp);

	DeviceConfigDeletedEvent deviceConfigDeletedEvent(Iterable<NodeUrn> nodeUrn, Optional<Long> timestamp);

	/**
	 * Creates a new message intended for a subset of nodes of the original node set. Consider e.g., a
	 * FlashImagesRequest for nodes A,B,C,D containing one flash image x for node A and a flash image y for nodes B,C,D.
	 * <p/>
	 * <ul>
	 * <li>Calling splitRequest(requestMessage, {A}) will return a FlashImagesRequest to A with only one image x.</li>
	 * <li>Calling splitRequest(requestMessage, {B,C}) will return a FlashImagesRequest to B,C with only one image y.</li>
	 * <li>Calling splitRequest(requestMessage, {A,D}) will return a FlashImagesRequest to A,D with images x and y.</li>
	 * </ul>
	 * <p/>
	 * This method can be used for all message types to constrain the message to a given subset of nodes. It adapts the
	 * payload to leave only what is necessary while keeping the original request (correlation) ID so the client can
	 * match the respones coming from individual endpoints.
	 *
	 * @param pair               the original (message, header) pair to be "split up"
	 * @param subRequestNodeUrns the subset of nodes
	 * @return a new (message, header) pair
	 * @throws IllegalArgumentException if called with a (message, header) pair for which it does not "make sense" to
	 *                                  splitRequest up, i.e. for non-downstream  messages and messages that are brodcasts
	 *
	 * @see MessageHeaderPair#isRequest(MessageType)
	 */
	MessageHeaderPair splitRequest(MessageHeaderPair pair, Set<NodeUrn> subRequestNodeUrns);

	/**
	 * Scopes an event message to a subset of the original node set. Consider e.g., a DevicesAttachedEvent for 4 nodes
	 * of which only 2 are part of an ongoing reservation. Calling scopeEvent with the 2 reserved nodes will result in
	 * a new DevicesAttachedEvent for these two nodes. This method can be used for all event message types.
	 *
	 * @param pair             the original (message, header) pair to be scoped
	 * @param subEventNodeUrns the subset of nodes to scope it to
	 * @return a new (message, header) pair
	 * @throws IllegalArgumentException if called with a non-event message type
	 *
	 * @see MessageHeaderPair#isScopeableEvent(MessageType)
	 */
	MessageHeaderPair scopeEvent(MessageHeaderPair pair, Collection<NodeUrn> subEventNodeUrns);
}
