package de.uniluebeck.itm.tr.iwsn.messages;

import com.google.common.collect.Multimap;
import com.google.protobuf.ByteString;
import eu.wisebed.api.v3.common.NodeUrn;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created by danbim on 22/05/15.
 */
public interface MessageFactory {
	UpstreamMessageEvent upstreamMessageEvent(Optional<Long> timestamp,
											  NodeUrn nodeUrn,
											  byte[] bytes);

	NotificationEvent notificationEvent(Optional<NodeUrn> nodeUrn,
										Optional<Long> timestamp,
										String message);

	DevicesAttachedEvent devicesAttachedEvent(Optional<Long> timestamp, Iterable<NodeUrn> nodeUrns);

	DevicesAttachedEvent devicesAttachedEvent(Optional<Long> timestamp, NodeUrn... nodeUrns);

	DevicesDetachedEvent devicesDetachedEvent(Optional<Long> timestamp, Iterable<NodeUrn> nodeUrns);

	DevicesDetachedEvent devicesDetachedEvent(Optional<Long> timestamp, NodeUrn... nodeUrns);

	GatewayConnectedEvent gatewayConnectedEvent(Optional<Long> timestamp, String hostname);

	GatewayDisconnectedEvent gatewayDisconnectedEvent(Optional<Long> timestamp,
													  String hostname,
													  Iterable<NodeUrn> nodeUrns);

	SetChannelPipelinesRequest setChannelPipelinesRequest(Optional<String> reservationId,
														  Optional<Long> timestamp,
														  Iterable<NodeUrn> nodeUrns,
														  Iterable<? extends ChannelHandlerConfiguration> channelHandlerConfigurations);

	SingleNodeProgress singleNodeProgress(Optional<String> reservationId,
										  Optional<Long> timestamp,
										  MessageType requestType,
										  long requestId,
										  NodeUrn nodeUrn,
										  int progressInPercent);

	SingleNodeResponse singleNodeResponse(Optional<String> reservationId,
										  Optional<Long> timestamp,
										  MessageType requestType,
										  long requestId,
										  NodeUrn nodeUrn,
										  int statusCode,
										  Optional<String> errorMessage);

	ReservationStartedEvent reservationStartedEvent(Optional<Long> timestamp, String serializedKey);

	ReservationEndedEvent reservationEndedEvent(Optional<Long> timestamp, String serializedKey);

	ReservationMadeEvent reservationMadeEvent(Optional<Long> timestamp, String serializedKey);

	ReservationOpenedEvent reservationOpenedEvent(Optional<Long> timestamp, String serializedKey);

	ReservationClosedEvent reservationClosedEvent(Optional<Long> timestamp, String serializedKey);

	ReservationFinalizedEvent reservationFinalizedEvent(Optional<Long> timestamp, String serializedKey);

	EventAck eventAck(long eventId, Optional<Long> timestamp);

	AreNodesConnectedRequest areNodesConnectedRequest(Optional<String> reservationId,
													  Optional<Long> timestamp,
													  Iterable<NodeUrn> nodeUrns);

	DisableNodesRequest disableNodesRequest(Optional<String> reservationId,
											Optional<Long> timestamp,
											Iterable<NodeUrn> nodeUrns);

	EnableNodesRequest enableNodesRequest(Optional<String> reservationId,
										  Optional<Long> timestamp,
										  Iterable<NodeUrn> nodeUrns);

	ResetNodesRequest resetNodesRequest(Optional<String> reservationId,
										Optional<Long> timestamp,
										Iterable<NodeUrn> nodeUrns);

	SendDownstreamMessagesRequest sendDownstreamMessageRequest(Optional<String> reservationId,
															   Optional<Long> timestamp,
															   Iterable<NodeUrn> nodeUrns,
															   ByteString bytes);

	FlashImagesRequest flashImagesRequest(Optional<String> reservationId,
										  Optional<Long> timestamp,
										  Iterable<NodeUrn> nodeUrns,
										  ByteString image);

	DisableVirtualLinksRequest disableVirtualLinksRequest(Optional<String> reservationId,
														  Optional<Long> timestamp,
														  Multimap<NodeUrn, NodeUrn> links);

	EnableVirtualLinksRequest enableVirtualLinksRequest(Optional<String> reservationId,
														Optional<Long> timestamp,
														Multimap<NodeUrn, NodeUrn> links);

	DisablePhysicalLinksRequest disablePhysicalLinksRequest(Optional<String> reservationId,
															Optional<Long> timestamp,
															Multimap<NodeUrn, NodeUrn> links);

	EnablePhysicalLinksRequest enablePhysicalLinksRequest(Optional<String> reservationId,
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
}
