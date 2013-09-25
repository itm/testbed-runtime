package de.uniluebeck.itm.tr.iwsn.portal.externalplugins;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.messages.*;
import de.uniluebeck.itm.tr.iwsn.portal.PortalEventBus;
import de.uniluebeck.itm.tr.iwsn.portal.Reservation;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationEndedEvent;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationStartedEvent;
import org.jboss.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.collect.Iterables.transform;
import static de.uniluebeck.itm.tr.common.NodeUrnHelper.NODE_URN_TO_STRING;

public class ExternalPluginServiceChannelHandler extends SimpleChannelUpstreamHandler {

	private static final Logger log = LoggerFactory.getLogger(ExternalPluginServiceChannelHandler.class);

	private final PortalEventBus portalEventBus;

	private volatile ChannelHandlerContext ctx;

	@Inject
	public ExternalPluginServiceChannelHandler(final PortalEventBus portalEventBus) {
		this.portalEventBus = portalEventBus;
	}

	@Override
	public void channelConnected(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception {
		log.trace("ExternalPluginServiceChannelHandler.channelConnected()");
		portalEventBus.register(this);
		this.ctx = ctx;
		super.channelConnected(ctx, e);
	}

	@Override
	public void channelDisconnected(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception {
		log.trace("ExternalPluginServiceChannelHandler.channelDisconnected()");
		portalEventBus.unregister(this);
		this.ctx = null;
		super.channelDisconnected(ctx, e);
	}

	@Override
	public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e) throws Exception {
		log.trace("ExternalPluginServiceChannelHandler.messageReceived({}, {})", ctx, e);
		// TODO implement me!
		throw new RuntimeException("Not yet implemented!");
		//super.messageReceived(ctx, e);
	}

	@Subscribe
	public void onRequest(final Request request) {
		sendToPluginsIfConnected(ExternalPluginMessage.newBuilder()
				.setType(ExternalPluginMessage.Type.IWSN_MESSAGE)
				.setIwsnMessage(Message.newBuilder()
						.setType(Message.Type.REQUEST)
						.setRequest(request)
				).build()
		);
	}

	@Subscribe
	public void onEvent(final Event event) {
		sendToPluginsIfConnected(ExternalPluginMessage.newBuilder()
				.setType(ExternalPluginMessage.Type.IWSN_MESSAGE)
				.setIwsnMessage(Message.newBuilder()
						.setType(Message.Type.EVENT)
						.setEvent(event)
				).build()
		);
	}

	@Subscribe
	public void onEventAck(final EventAck eventAck) {
		sendToPluginsIfConnected(ExternalPluginMessage.newBuilder()
				.setType(ExternalPluginMessage.Type.IWSN_MESSAGE)
				.setIwsnMessage(Message.newBuilder()
						.setType(Message.Type.EVENT_ACK)
						.setEventAck(eventAck)
				).build()
		);
	}

	@Subscribe
	public void onGetChannelPipelinesResponse(final GetChannelPipelinesResponse response) {
		sendToPluginsIfConnected(ExternalPluginMessage.newBuilder()
				.setType(ExternalPluginMessage.Type.IWSN_MESSAGE)
				.setIwsnMessage(Message.newBuilder()
						.setType(Message.Type.GET_CHANNELPIPELINES_RESPONSE)
						.setGetChannelPipelinesResponse(response)
				).build()
		);
	}

	@Subscribe
	public void onSingleNodeResponse(final SingleNodeResponse response) {
		sendToPluginsIfConnected(ExternalPluginMessage.newBuilder()
				.setType(ExternalPluginMessage.Type.IWSN_MESSAGE)
				.setIwsnMessage(Message.newBuilder()
						.setType(Message.Type.RESPONSE)
						.setResponse(response)
				).build()
		);
	}

	@Subscribe
	public void onSingleNodeProgress(final SingleNodeProgress progress) {
		sendToPluginsIfConnected(ExternalPluginMessage.newBuilder()
				.setType(ExternalPluginMessage.Type.IWSN_MESSAGE)
				.setIwsnMessage(Message.newBuilder()
						.setType(Message.Type.PROGRESS)
						.setProgress(progress)
				).build()
		);
	}

	@Subscribe
	public void onReservationStartedEvent(final ReservationStartedEvent event) {
		final Reservation reservation = event.getReservation();
		sendToPluginsIfConnected(
				ExternalPluginMessage.newBuilder()
						.setType(ExternalPluginMessage.Type.INTERNAL_MESSAGE)
						.setInternalMessage(InternalMessage.newBuilder()
								.setReservationEvent(ReservationEvent.newBuilder()
										.setIntervalEnd(reservation.getInterval().getEnd().toString())
										.setIntervalStart(reservation.getInterval().getStart().toString())
										.setKey(reservation.getKey())
										.setType(ReservationEvent.Type.STARTED)
										.setUsername(reservation.getUsername())
										.addAllNodeUrns(transform(reservation.getNodeUrns(), NODE_URN_TO_STRING))
								)
						).build()
		);
	}

	@Subscribe
	public void onReservationEndedEvent(final ReservationEndedEvent event) {
		final Reservation reservation = event.getReservation();
		sendToPluginsIfConnected(
				ExternalPluginMessage.newBuilder()
						.setInternalMessage(InternalMessage.newBuilder()
								.setReservationEvent(ReservationEvent.newBuilder()
										.setType(ReservationEvent.Type.ENDED)
										.setKey(reservation.getKey())
										.setUsername(reservation.getUsername())
										.addAllNodeUrns(transform(reservation.getNodeUrns(), NODE_URN_TO_STRING))
										.setIntervalStart(reservation.getInterval().getStart().toString())
										.setIntervalEnd(reservation.getInterval().getEnd().toString())
								)
						).build()
		);
	}

	protected void sendToPluginsIfConnected(final ExternalPluginMessage externalPluginMessage) {
		if (ctx != null) {
			Channels.write(ctx.getChannel(), externalPluginMessage);
		}
	}
}
