package de.uniluebeck.itm.tr.iwsn.portal.externalplugins;

import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.messages.*;
import de.uniluebeck.itm.tr.iwsn.portal.PortalEventBus;
import de.uniluebeck.itm.tr.iwsn.portal.Reservation;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationEndedEvent;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationStartedEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.collect.Iterables.transform;
import static de.uniluebeck.itm.tr.common.NodeUrnHelper.NODE_URN_TO_STRING;

class ExternalPluginServiceChannelHandler extends SimpleChannelUpstreamHandler {

	private static final Logger log = LoggerFactory.getLogger(ExternalPluginServiceChannelHandler.class);

	private final ChannelGroup allChannels = new DefaultChannelGroup();

	private final PortalEventBus portalEventBus;

	@Inject
	public ExternalPluginServiceChannelHandler(final PortalEventBus portalEventBus) {
		this.portalEventBus = portalEventBus;
	}

	@Override
	public void channelConnected(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception {
		log.trace("ExternalPluginServiceChannelHandler.channelConnected()");
		allChannels.add(ctx.getChannel());
		super.channelConnected(ctx, e);
	}

	@Override
	public void channelDisconnected(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception {
		log.trace("ExternalPluginServiceChannelHandler.channelDisconnected()");
		allChannels.remove(ctx.getChannel());
		super.channelDisconnected(ctx, e);
	}

	@Override
	public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e) throws Exception {

		log.trace("ExternalPluginServiceChannelHandler.messageReceived({}, {})", ctx, e);

		if (!(e.getMessage() instanceof ExternalPluginMessage)) {
			throw new RuntimeException("Received unknown message type, something is wrong!");
		}

		final ExternalPluginMessage externalPluginMessage = (ExternalPluginMessage) e.getMessage();
		switch (externalPluginMessage.getType()) {
			case INTERNAL_MESSAGE:
				log.warn("Received internal message from external plugin which does not make sense. Ignoring...");
				break;
			case IWSN_MESSAGE:
				switch (externalPluginMessage.getIwsnMessage().getType()) {
					case REQUEST:
						portalEventBus.post(externalPluginMessage.getIwsnMessage().getRequest());
						break;
					default:
						log.warn("Received {} message from external plugin which does not make sense. Ignoring...",
								externalPluginMessage.getIwsnMessage().getType()
						);
						break;
				}
				break;
		}

		super.messageReceived(ctx, e);
	}

	public void onRequest(final Request request) {
		final ExternalPluginMessage externalPluginMessage = ExternalPluginMessage.newBuilder()
				.setType(ExternalPluginMessage.Type.IWSN_MESSAGE)
				.setIwsnMessage(Message.newBuilder()
						.setType(Message.Type.REQUEST)
						.setRequest(request)
				).build();
		allChannels.write(externalPluginMessage);
	}

	public void onEvent(final Event event) {
		final ExternalPluginMessage externalPluginMessage = ExternalPluginMessage.newBuilder()
				.setType(ExternalPluginMessage.Type.IWSN_MESSAGE)
				.setIwsnMessage(Message.newBuilder()
						.setType(Message.Type.EVENT)
						.setEvent(event)
				).build();
		allChannels.write(externalPluginMessage);
	}

	public void onEventAck(final EventAck eventAck) {
		final ExternalPluginMessage externalPluginMessage = ExternalPluginMessage.newBuilder()
				.setType(ExternalPluginMessage.Type.IWSN_MESSAGE)
				.setIwsnMessage(Message.newBuilder()
						.setType(Message.Type.EVENT_ACK)
						.setEventAck(eventAck)
				).build();
		allChannels.write(externalPluginMessage);
	}

	public void onGetChannelPipelinesResponse(final GetChannelPipelinesResponse response) {
		final ExternalPluginMessage externalPluginMessage = ExternalPluginMessage.newBuilder()
				.setType(ExternalPluginMessage.Type.IWSN_MESSAGE)
				.setIwsnMessage(Message.newBuilder()
						.setType(Message.Type.GET_CHANNELPIPELINES_RESPONSE)
						.setGetChannelPipelinesResponse(response)
				).build();
		allChannels.write(externalPluginMessage);
	}

	public void onSingleNodeResponse(final SingleNodeResponse response) {
		final ExternalPluginMessage externalPluginMessage = ExternalPluginMessage.newBuilder()
				.setType(ExternalPluginMessage.Type.IWSN_MESSAGE)
				.setIwsnMessage(Message.newBuilder()
						.setType(Message.Type.RESPONSE)
						.setResponse(response)
				).build();
		allChannels.write(externalPluginMessage);
	}

	public void onSingleNodeProgress(final SingleNodeProgress progress) {
		final ExternalPluginMessage externalPluginMessage = ExternalPluginMessage.newBuilder()
				.setType(ExternalPluginMessage.Type.IWSN_MESSAGE)
				.setIwsnMessage(Message.newBuilder()
						.setType(Message.Type.PROGRESS)
						.setProgress(progress)
				).build();
		allChannels.write(externalPluginMessage);
	}

	public void onReservationStartedEvent(final ReservationStartedEvent event) {
		allChannels.write(createReservationEvent(event.getReservation(), ReservationEvent.Type.STARTED));
	}

	public void onReservationEndedEvent(final ReservationEndedEvent event) {
		allChannels.write(createReservationEvent(event.getReservation(), ReservationEvent.Type.ENDED));
	}

	private ExternalPluginMessage createReservationEvent(final Reservation reservation,
														 final ReservationEvent.Type type) {

		final ReservationEvent.Builder reservationEvent = ReservationEvent
				.newBuilder()
				.setType(type)
				.addAllNodeUrns(transform(reservation.getNodeUrns(), NODE_URN_TO_STRING))
				.setIntervalStart(reservation.getInterval().getStart().toString())
				.setIntervalEnd(reservation.getInterval().getEnd().toString());


		for (Reservation.Entry entry : reservation.getEntries()) {
			reservationEvent.addSecretReservationKeys(ReservationEvent.SecretReservationKey.newBuilder()
					.setKey(entry.getKey())
					.setUsername(entry.getUsername())
					.setNodeUrnPrefix(entry.getNodeUrnPrefix().toString()));
		}

		return ExternalPluginMessage.newBuilder()
				.setType(ExternalPluginMessage.Type.INTERNAL_MESSAGE)
				.setInternalMessage(InternalMessage.newBuilder()
						.setType(InternalMessage.Type.RESERVATION_EVENT)
						.setReservationEvent(reservationEvent)
				).build();
	}
}