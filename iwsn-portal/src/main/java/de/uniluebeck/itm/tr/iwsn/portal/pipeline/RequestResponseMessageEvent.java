package de.uniluebeck.itm.tr.iwsn.portal.pipeline;

import com.google.protobuf.MessageLite;
import de.uniluebeck.itm.tr.iwsn.messages.*;

import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.Throwables.propagate;

public class RequestResponseMessageEvent {

	public static final Set<Class> MESSAGE_CLASS_TYPES = new HashSet<>();

	public static final Set<MessageType> MESSAGE_TYPES = new HashSet<>();

	static {
		MESSAGE_CLASS_TYPES.add(AreNodesAliveRequest.class);
		MESSAGE_CLASS_TYPES.add(AreNodesConnectedRequest.class);
		MESSAGE_CLASS_TYPES.add(DisableNodesRequest.class);
		MESSAGE_CLASS_TYPES.add(DisableVirtualLinksRequest.class);
		MESSAGE_CLASS_TYPES.add(DisablePhysicalLinksRequest.class);
		MESSAGE_CLASS_TYPES.add(EnableNodesRequest.class);
		MESSAGE_CLASS_TYPES.add(EnablePhysicalLinksRequest.class);
		MESSAGE_CLASS_TYPES.add(EnableVirtualLinksRequest.class);
		MESSAGE_CLASS_TYPES.add(FlashImagesRequest.class);
		MESSAGE_CLASS_TYPES.add(GetChannelPipelinesRequest.class);
		MESSAGE_CLASS_TYPES.add(ResetNodesRequest.class);
		MESSAGE_CLASS_TYPES.add(SendDownstreamMessagesRequest.class);
		MESSAGE_CLASS_TYPES.add(SetChannelPipelinesRequest.class);
		MESSAGE_CLASS_TYPES.add(Progress.class);
		MESSAGE_CLASS_TYPES.add(Response.class);
		MESSAGE_CLASS_TYPES.add(GetChannelPipelinesResponse.class);

		MESSAGE_TYPES.add(MessageType.REQUEST_ARE_NODES_ALIVE);
		MESSAGE_TYPES.add(MessageType.REQUEST_ARE_NODES_CONNECTED);
		MESSAGE_TYPES.add(MessageType.REQUEST_DISABLE_NODES);
		MESSAGE_TYPES.add(MessageType.REQUEST_DISABLE_PHYSICAL_LINKS);
		MESSAGE_TYPES.add(MessageType.REQUEST_DISABLE_VIRTUAL_LINKS);
		MESSAGE_TYPES.add(MessageType.REQUEST_ENABLE_NODES);
		MESSAGE_TYPES.add(MessageType.REQUEST_ENABLE_PHYSICAL_LINKS);
		MESSAGE_TYPES.add(MessageType.REQUEST_ENABLE_VIRTUAL_LINKS);
		MESSAGE_TYPES.add(MessageType.REQUEST_FLASH_IMAGES);
		MESSAGE_TYPES.add(MessageType.REQUEST_GET_CHANNEL_PIPELINES);
		MESSAGE_TYPES.add(MessageType.REQUEST_RESET_NODES);
		MESSAGE_TYPES.add(MessageType.REQUEST_SEND_DOWNSTREAM_MESSAGES);
		MESSAGE_TYPES.add(MessageType.REQUEST_SET_CHANNEL_PIPELINES);
		MESSAGE_TYPES.add(MessageType.PROGRESS);
		MESSAGE_TYPES.add(MessageType.RESPONSE);
		MESSAGE_TYPES.add(MessageType.GET_CHANNELPIPELINES_RESPONSE);
	}

	public final RequestResponseHeader header;

	public final MessageLite message;

	public RequestResponseMessageEvent(RequestResponseHeader header, MessageLite message) {
		this.header = header;
		this.message = message;
	}

	public static boolean isUnwrappedRequestResponseMessage(Object obj) {
		return MESSAGE_CLASS_TYPES.contains(obj.getClass());
	}

	public static boolean isWrappedRequestResponseMessage(Message msg) {
		return MESSAGE_TYPES.contains(msg.getType());
	}

	public static RequestResponseMessageEvent fromUnwrapped(Object obj) {

		if (!isUnwrappedRequestResponseMessage(obj)) {
			throw new IllegalArgumentException("Unknown message type \"" + obj.getClass() + "\"!");
		}

		try {
			return new RequestResponseMessageEvent(
					(RequestResponseHeader) obj.getClass().getMethod("getHeader").invoke(obj),
					(MessageLite) obj
			);
		} catch (Exception e) {
			throw propagate(e);
		}
	}

	public static RequestResponseMessageEvent fromWrapped(Message msg) {

		switch (msg.getType()) {
			case REQUEST_ARE_NODES_ALIVE:
				return new RequestResponseMessageEvent(
						msg.getAreNodesAliveRequest().getHeader(),
						msg.getAreNodesAliveRequest()
				);
			case REQUEST_ARE_NODES_CONNECTED:
				return new RequestResponseMessageEvent(
						msg.getAreNodesConnectedRequest().getHeader(),
						msg.getAreNodesConnectedRequest()
				);
			case REQUEST_DISABLE_NODES:
				return new RequestResponseMessageEvent(
						msg.getDisableNodesRequest().getHeader(),
						msg.getDisableNodesRequest()
				);
			case REQUEST_DISABLE_VIRTUAL_LINKS:
				return new RequestResponseMessageEvent(
						msg.getDisableVirtualLinksRequest().getHeader(),
						msg.getDisableVirtualLinksRequest()
				);
			case REQUEST_DISABLE_PHYSICAL_LINKS:
				return new RequestResponseMessageEvent(
						msg.getDisablePhysicalLinksRequest().getHeader(),
						msg.getDisablePhysicalLinksRequest()
				);
			case REQUEST_ENABLE_NODES:
				return new RequestResponseMessageEvent(
						msg.getEnableNodesRequest().getHeader(),
						msg.getEnableNodesRequest()
				);
			case REQUEST_ENABLE_PHYSICAL_LINKS:
				return new RequestResponseMessageEvent(
						msg.getEnablePhysicalLinksRequest().getHeader(),
						msg.getEnablePhysicalLinksRequest()
				);
			case REQUEST_ENABLE_VIRTUAL_LINKS:
				return new RequestResponseMessageEvent(
						msg.getEnableVirtualLinksRequest().getHeader(),
						msg.getEnableVirtualLinksRequest()
				);
			case REQUEST_FLASH_IMAGES:
				return new RequestResponseMessageEvent(
						msg.getFlashImagesRequest().getHeader(),
						msg.getFlashImagesRequest()
				);
			case REQUEST_GET_CHANNEL_PIPELINES:
				return new RequestResponseMessageEvent(
						msg.getGetChannelPipelinesRequest().getHeader(),
						msg.getGetChannelPipelinesRequest()
				);
			case REQUEST_RESET_NODES:
				return new RequestResponseMessageEvent(
						msg.getResetNodesRequest().getHeader(),
						msg.getResetNodesRequest()
				);
			case REQUEST_SEND_DOWNSTREAM_MESSAGES:
				return new RequestResponseMessageEvent(
						msg.getSendDownstreamMessagesRequest().getHeader(),
						msg.getSendDownstreamMessagesRequest()
				);
			case REQUEST_SET_CHANNEL_PIPELINES:
				return new RequestResponseMessageEvent(
						msg.getSetChannelPipelinesRequest().getHeader(),
						msg.getSetChannelPipelinesRequest()
				);
			case PROGRESS:
				return new RequestResponseMessageEvent(
						msg.getProgress().getHeader(),
						msg.getProgress()
				);
			case RESPONSE:
				return new RequestResponseMessageEvent(
						msg.getResponse().getHeader(),
						msg.getResponse()
				);
			case GET_CHANNELPIPELINES_RESPONSE:
				return new RequestResponseMessageEvent(
						msg.getGetChannelPipelinesResponse().getHeader(),
						msg.getGetChannelPipelinesResponse()
				);
			default:
				throw new IllegalArgumentException("Unknown message type: " + msg.getType());
		}
	}
}
