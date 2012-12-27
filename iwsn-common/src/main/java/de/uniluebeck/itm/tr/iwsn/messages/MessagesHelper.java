package de.uniluebeck.itm.tr.iwsn.messages;

import java.util.Random;

public abstract class MessagesHelper {

	public static Random RANDOM = new Random();

	public static Message newAreNodesAliveRequest(final Iterable<? extends String> nodeUrns) {
		return newAreNodesAliveRequest(RANDOM.nextLong(), nodeUrns);
	}

	public static Message newAreNodesAliveRequest(final long requestId, final Iterable<? extends String> nodeUrns) {
		return Message.newBuilder()
				.setType(Message.Type.REQUEST)
				.setRequest(Request.newBuilder()
						.setRequestId(requestId)
						.setType(Request.Type.ARE_NODES_ALIVE)
						.setAreNodesAliveRequest(AreNodesAliveRequest.newBuilder().addAllNodeUrns(nodeUrns))
				).build();
	}

	public static Message newAreNodesConnectedRequest(final Iterable<? extends String> nodeUrns) {
		return newAreNodesConnectedRequest(RANDOM.nextLong(), nodeUrns);
	}

	public static Message newAreNodesConnectedRequest(final long requestId, final Iterable<? extends String> nodeUrns) {
		return newMessage(
				Request.newBuilder()
						.setRequestId(requestId)
						.setType(Request.Type.ARE_NODES_CONNECTED)
						.setAreNodesConnectedRequest(AreNodesConnectedRequest.newBuilder().addAllNodeUrns(nodeUrns))
						.build()
		);
	}

	public static Message newMessage(Request request) {
		return Message.newBuilder()
				.setType(Message.Type.REQUEST)
				.setRequest(request)
				.build();
	}

}
