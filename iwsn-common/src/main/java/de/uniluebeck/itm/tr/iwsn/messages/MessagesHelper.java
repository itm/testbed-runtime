package de.uniluebeck.itm.tr.iwsn.messages;

import com.google.protobuf.ByteString;

import java.util.Random;

public abstract class MessagesHelper {

	public static Random RANDOM = new Random();

	public static Message newAreNodesAliveRequestMessage(final Iterable<? extends String> nodeUrns) {
		return newAreNodesAliveRequestMessage(RANDOM.nextLong(), nodeUrns);
	}

	public static Message newAreNodesAliveRequestMessage(final long requestId,
														 final Iterable<? extends String> nodeUrns) {
		return newMessage(newAreNodesAliveRequest(requestId, nodeUrns));
	}

	public static Request newAreNodesAliveRequest(final long requestId, final Iterable<? extends String> nodeUrns) {
		return Request.newBuilder()
				.setRequestId(requestId)
				.setType(Request.Type.ARE_NODES_ALIVE)
				.setAreNodesAliveRequest(AreNodesAliveRequest.newBuilder().addAllNodeUrns(nodeUrns))
				.build();
	}

	public static Message newAreNodesConnectedRequestMessage(final Iterable<? extends String> nodeUrns) {
		return newAreNodesConnectedRequestMessage(RANDOM.nextLong(), nodeUrns);
	}

	public static Request newAreNodesConnectedRequest(final long requestId,
													  final Iterable<? extends String> nodeUrns) {
		return Request.newBuilder()
				.setRequestId(requestId)
				.setType(Request.Type.ARE_NODES_CONNECTED)
				.setAreNodesConnectedRequest(AreNodesConnectedRequest.newBuilder().addAllNodeUrns(nodeUrns))
				.build();
	}

	public static Message newAreNodesConnectedRequestMessage(final long requestId,
															 final Iterable<? extends String> nodeUrns) {
		return newMessage(newAreNodesConnectedRequest(requestId, nodeUrns));
	}

	public static Message newDisableNodesRequestMessage(final long requestId,
														final Iterable<? extends String> nodeUrns) {
		return newMessage(newDisableNodesRequest(requestId, nodeUrns));
	}

	public static Request newDisableNodesRequest(final long requestId, final Iterable<? extends String> nodeUrns) {
		return Request.newBuilder()
				.setRequestId(requestId)
				.setType(Request.Type.DISABLE_NODE)
				.setDisableNodesRequest(DisableNodesRequest.newBuilder().addAllNodeUrns(nodeUrns))
				.build();
	}

	public static Request newEnableNodesRequest(final long requestId,
												final Iterable<? extends String> nodeUrns) {
		return Request.newBuilder()
				.setRequestId(requestId)
				.setType(Request.Type.ENABLE_NODE)
				.setEnableNodesRequest(EnableNodesRequest.newBuilder().addAllNodeUrns(nodeUrns))
				.build();
	}

	public static Message newEnableNodesRequestMessage(final long requestId,
													   final Iterable<? extends String> nodeUrns) {
		return newMessage(newEnableNodesRequest(requestId, nodeUrns));
	}

	public static Request newResetNodesRequest(final long requestId,
											   final Iterable<? extends String> nodeUrns) {
		return Request.newBuilder()
				.setRequestId(requestId)
				.setType(Request.Type.RESET_NODES)
				.setResetNodesRequest(ResetNodesRequest.newBuilder().addAllNodeUrns(nodeUrns))
				.build();
	}

	public static Message newResetNodesRequestMessage(final long requestId,
													  final Iterable<? extends String> nodeUrns) {
		return newMessage(newResetNodesRequest(requestId, nodeUrns));
	}

	public static Request newSendDownstreamMessageRequest(final long requestId,
														  final Iterable<? extends String> nodeUrns,
														  final byte[] bytes) {
		return Request.newBuilder()
				.setRequestId(requestId)
				.setType(Request.Type.SEND_DOWNSTREAM_MESSAGE)
				.setSendDownstreamMessageRequest(SendDownstreamMessageRequest.newBuilder()
						.addAllTargetNodeUrns(nodeUrns)
						.setMessageBytes(ByteString.copyFrom(bytes))
				)
				.build();
	}

	public static Message newSendDownstreamMessageRequestMessage(final long requestId,
																 final Iterable<? extends String> nodeUrns,
																 final byte[] bytes) {
		return newMessage(newSendDownstreamMessageRequest(requestId, nodeUrns, bytes));
	}

	public static Message newMessage(Request request) {
		return Message.newBuilder()
				.setType(Message.Type.REQUEST)
				.setRequest(request)
				.build();
	}
}
