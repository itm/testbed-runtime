package de.uniluebeck.itm.tr.iwsn.messages;

import com.google.common.collect.Multimap;
import com.google.protobuf.ByteString;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public abstract class MessagesHelper {

	public static Message newAreNodesAliveRequestMessage(final long requestId,
														 final Iterable<String> nodeUrns) {
		return newMessage(newAreNodesAliveRequest(requestId, nodeUrns));
	}

	public static Request newAreNodesAliveRequest(final long requestId, final Iterable<String> nodeUrns) {
		return Request.newBuilder()
				.setRequestId(requestId)
				.setType(Request.Type.ARE_NODES_ALIVE)
				.setAreNodesAliveRequest(AreNodesAliveRequest.newBuilder().addAllNodeUrns(nodeUrns))
				.build();
	}

	public static Request newAreNodesConnectedRequest(final long requestId,
													  final Iterable<String> nodeUrns) {
		return Request.newBuilder()
				.setRequestId(requestId)
				.setType(Request.Type.ARE_NODES_CONNECTED)
				.setAreNodesConnectedRequest(AreNodesConnectedRequest.newBuilder().addAllNodeUrns(nodeUrns))
				.build();
	}

	public static Message newAreNodesConnectedRequestMessage(final long requestId,
															 final Iterable<String> nodeUrns) {
		return newMessage(newAreNodesConnectedRequest(requestId, nodeUrns));
	}

	public static Message newDisableNodesRequestMessage(final long requestId,
														final Iterable<String> nodeUrns) {
		return newMessage(newDisableNodesRequest(requestId, nodeUrns));
	}

	public static Request newDisableNodesRequest(final long requestId, final Iterable<String> nodeUrns) {
		return Request.newBuilder()
				.setRequestId(requestId)
				.setType(Request.Type.DISABLE_NODES)
				.setDisableNodesRequest(DisableNodesRequest.newBuilder().addAllNodeUrns(nodeUrns))
				.build();
	}

	public static Request newEnableNodesRequest(final long requestId,
												final Iterable<String> nodeUrns) {
		return Request.newBuilder()
				.setRequestId(requestId)
				.setType(Request.Type.ENABLE_NODES)
				.setEnableNodesRequest(EnableNodesRequest.newBuilder().addAllNodeUrns(nodeUrns))
				.build();
	}

	public static Message newEnableNodesRequestMessage(final long requestId,
													   final Iterable<String> nodeUrns) {
		return newMessage(newEnableNodesRequest(requestId, nodeUrns));
	}

	public static Request newResetNodesRequest(final long requestId,
											   final Iterable<String> nodeUrns) {
		return Request.newBuilder()
				.setRequestId(requestId)
				.setType(Request.Type.RESET_NODES)
				.setResetNodesRequest(ResetNodesRequest.newBuilder().addAllNodeUrns(nodeUrns))
				.build();
	}

	public static Message newResetNodesRequestMessage(final long requestId,
													  final Iterable<String> nodeUrns) {
		return newMessage(newResetNodesRequest(requestId, nodeUrns));
	}

	public static Request newSendDownstreamMessageRequest(final long requestId,
														  final Iterable<String> nodeUrns,
														  final byte[] bytes) {
		return newSendDownstreamMessageRequest(requestId, nodeUrns, ByteString.copyFrom(bytes));
	}

	public static Request newSendDownstreamMessageRequest(final long requestId,
														  final Iterable<String> nodeUrns,
														  final ByteString bytes) {
		return Request.newBuilder()
				.setRequestId(requestId)
				.setType(Request.Type.SEND_DOWNSTREAM_MESSAGES)
				.setSendDownstreamMessagesRequest(SendDownstreamMessagesRequest.newBuilder()
						.addAllTargetNodeUrns(nodeUrns)
						.setMessageBytes(bytes)
				)
				.build();
	}

	public static Message newSendDownstreamMessageRequestMessage(final long requestId,
																 final Iterable<String> nodeUrns,
																 final byte[] bytes) {
		return newMessage(newSendDownstreamMessageRequest(requestId, nodeUrns, ByteString.copyFrom(bytes)));
	}

	public static Request newFlashImagesRequest(final long requestId,
												final Iterable<String> nodeUrns,
												final byte[] imageBytes) {
		return newFlashImagesRequest(requestId, nodeUrns, ByteString.copyFrom(imageBytes));
	}

	public static Request newFlashImagesRequest(final long requestId,
												final Iterable<String> nodeUrns,
												final ByteString image) {
		return Request.newBuilder()
				.setRequestId(requestId)
				.setType(Request.Type.FLASH_IMAGES)
				.setFlashImagesRequest(FlashImagesRequest.newBuilder()
						.addAllNodeUrns(nodeUrns)
						.setImage(image)
				)
				.build();
	}

	public static Message newFlashImagesRequestMessage(final long requestId,
													   final Iterable<String> nodeUrns,
													   final byte[] imageBytes) {
		return newMessage(newFlashImagesRequest(requestId, nodeUrns, ByteString.copyFrom(imageBytes)));
	}

	public static Request newDisableVirtualLinksRequest(final long requestId, final Multimap<String, String> links) {
		return Request.newBuilder()
				.setRequestId(requestId)
				.setType(Request.Type.DISABLE_VIRTUAL_LINKS)
				.setDisableVirtualLinksRequest(
						DisableVirtualLinksRequest.newBuilder().addAllLinks(toLinks(links)).build()
				)
				.build();
	}

	public static Message newDisableVirtualLinksRequestMessage(final long requestId,
															   final Multimap<String, String> links) {
		return newMessage(newDisableVirtualLinksRequest(requestId, links));
	}

	public static Request newEnableVirtualLinksRequest(final long requestId, final Multimap<String, String> links) {
		return Request.newBuilder()
				.setRequestId(requestId)
				.setType(Request.Type.ENABLE_VIRTUAL_LINKS)
				.setEnableVirtualLinksRequest(
						EnableVirtualLinksRequest.newBuilder().addAllLinks(toLinks(links)).build()
				)
				.build();
	}

	public static Message newEnableVirtualLinksRequestMessage(final long requestId,
															  final Multimap<String, String> links) {
		return newMessage(newEnableVirtualLinksRequest(requestId, links));
	}

	public static Message newMessage(Request request) {
		return Message.newBuilder()
				.setType(Message.Type.REQUEST)
				.setRequest(request)
				.build();
	}

	private static Iterable<Link> toLinks(final Multimap<String, String> linkMap) {
		List<Link> links = newArrayList();
		for (String sourceNodeUrn : linkMap.keySet()) {
			for (String targetNodeUrn : linkMap.get(sourceNodeUrn)) {
				links.add(Link.newBuilder().setSourceNodeUrn(sourceNodeUrn).setTargetNodeUrn(targetNodeUrn).build());
			}
		}
		return links;
	}
}
