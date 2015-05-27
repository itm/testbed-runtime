package de.uniluebeck.itm.tr.iwsn.portal.pipeline;

import de.uniluebeck.itm.tr.iwsn.messages.Message;
import de.uniluebeck.itm.tr.iwsn.messages.MessageType;
import org.jboss.netty.channel.*;

public class MessageUnwrapper extends SimpleChannelUpstreamHandler {

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {

		if (!(e.getMessage() instanceof Message)) {
			throw new IllegalArgumentException("Unknown message received! This probably means that the pipeline is not configured correctly.");
		}

		Message msg = (Message) e.getMessage();

		switch (msg.getType()) {
			case KEEP_ALIVE:
				Channels.write(ctx.getChannel(), Message.newBuilder().setType(MessageType.KEEP_ALIVE_ACK).build());
				break;
			case KEEP_ALIVE_ACK:
				// nothing to do
				break;
			case REQUEST_ARE_NODES_ALIVE:
				sendUpstream(ctx, e, msg.getAreNodesAliveRequest());
				break;
			case REQUEST_ARE_NODES_CONNECTED:
				sendUpstream(ctx, e, msg.getAreNodesConnectedRequest());
				break;
			case REQUEST_DISABLE_NODES:
				sendUpstream(ctx, e, msg.getDisableNodesRequest());
				break;
			case REQUEST_DISABLE_VIRTUAL_LINKS:
				sendUpstream(ctx, e, msg.getDisableVirtualLinksRequest());
				break;
			case REQUEST_DISABLE_PHYSICAL_LINKS:
				sendUpstream(ctx, e, msg.getDisablePhysicalLinksRequest());
				break;
			case REQUEST_ENABLE_NODES:
				sendUpstream(ctx, e, msg.getEnableNodesRequest());
				break;
			case REQUEST_ENABLE_PHYSICAL_LINKS:
				sendUpstream(ctx, e, msg.getEnablePhysicalLinksRequest());
				break;
			case REQUEST_ENABLE_VIRTUAL_LINKS:
				sendUpstream(ctx, e, msg.getEnableVirtualLinksRequest());
				break;
			case REQUEST_FLASH_IMAGES:
				sendUpstream(ctx, e, msg.getFlashImagesRequest());
				break;
			case REQUEST_GET_CHANNEL_PIPELINES:
				sendUpstream(ctx, e, msg.getGetChannelPipelinesRequest());
				break;
			case REQUEST_RESET_NODES:
				sendUpstream(ctx, e, msg.getResetNodesRequest());
				break;
			case REQUEST_SEND_DOWNSTREAM_MESSAGES:
				sendUpstream(ctx, e, msg.getSendDownstreamMessagesRequest());
				break;
			case REQUEST_SET_CHANNEL_PIPELINES:
				sendUpstream(ctx, e, msg.getSetChannelPipelinesRequest());
				break;
			case PROGRESS:
				sendUpstream(ctx, e, msg.getProgress());
				break;
			case RESPONSE:
				sendUpstream(ctx, e, msg.getResponse());
				break;
			case GET_CHANNELPIPELINES_RESPONSE:
				sendUpstream(ctx, e, msg.getGetChannelPipelinesResponse());
				break;
			case EVENT_UPSTREAM_MESSAGE:
				sendUpstream(ctx, e, msg.getUpstreamMessageEvent());
				break;
			case EVENT_DEVICES_ATTACHED:
				sendUpstream(ctx, e, msg.getDevicesAttachedEvent());
				break;
			case EVENT_DEVICES_DETACHED:
				sendUpstream(ctx, e, msg.getDevicesAttachedEvent());
				break;
			case EVENT_GATEWAY_CONNECTED:
				sendUpstream(ctx, e, msg.getGatewayConnectedEvent());
				break;
			case EVENT_GATEWAY_DISCONNECTED:
				sendUpstream(ctx, e, msg.getGatewayDisconnectedEvent());
				break;
			case EVENT_NOTIFICATION:
				sendUpstream(ctx, e, msg.getNotificationEvent());
				break;
			case EVENT_RESERVATION_STARTED:
				sendUpstream(ctx, e, msg.getReservationStartedEvent());
				break;
			case EVENT_RESERVATION_ENDED:
				sendUpstream(ctx, e, msg.getReservationEndedEvent());
				break;
			case EVENT_RESERVATION_MADE:
				sendUpstream(ctx, e, msg.getReservationMadeEvent());
				break;
			case EVENT_RESERVATION_CANCELLED:
				sendUpstream(ctx, e, msg.getReservationCancelledEvent());
				break;
			case EVENT_RESERVATION_OPENED:
				sendUpstream(ctx, e, msg.getReservationOpenedEvent());
				break;
			case EVENT_RESERVATION_CLOSED:
				sendUpstream(ctx, e, msg.getReservationClosedEvent());
				break;
			case EVENT_RESERVATION_FINALIZED:
				sendUpstream(ctx, e, msg.getReservationFinalizedEvent());
				break;
			case EVENT_DEVICE_CONFIG_CREATED:
				sendUpstream(ctx, e, msg.getDeviceConfigCreatedEvent());
				break;
			case EVENT_DEVICE_CONFIG_UPDATED:
				sendUpstream(ctx, e, msg.getDeviceConfigUpdatedEvent());
				break;
			case EVENT_DEVICE_CONFIG_DELETED:
				sendUpstream(ctx, e, msg.getDeviceConfigDeletedEvent());
				break;
			case EVENT_ACK:
				sendUpstream(ctx, e, msg.getEventAck());
				break;
		}
	}

	private void sendUpstream(ChannelHandlerContext ctx, MessageEvent e, Object message) {
		ctx.sendUpstream(new UpstreamMessageEvent(e.getChannel(), message, e.getRemoteAddress()));
	}
}
