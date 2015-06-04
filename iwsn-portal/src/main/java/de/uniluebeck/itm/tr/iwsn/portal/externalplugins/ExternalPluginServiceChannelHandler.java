package de.uniluebeck.itm.tr.iwsn.portal.externalplugins;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.messages.MessageHeaderPair;
import de.uniluebeck.itm.tr.iwsn.portal.PortalEventBus;
import de.uniluebeck.itm.tr.iwsn.portal.Reservation;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationManager;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

class ExternalPluginServiceChannelHandler extends SimpleChannelUpstreamHandler {

	private static final Logger log = LoggerFactory.getLogger(ExternalPluginServiceChannelHandler.class);

	private final ChannelGroup allChannels = new DefaultChannelGroup();

	private final PortalEventBus portalEventBus;

	private final ReservationManager reservationManager;

	@Inject
	public ExternalPluginServiceChannelHandler(final PortalEventBus portalEventBus,
											   final ReservationManager reservationManager) {
		this.portalEventBus = portalEventBus;
		this.reservationManager = reservationManager;
	}

	@Override
	public void channelConnected(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception {
		log.trace("ExternalPluginServiceChannelHandler.channelConnected()");
		synchronized (allChannels) {
			if (allChannels.isEmpty()) {
				portalEventBus.register(this);
			}
			allChannels.add(ctx.getChannel());
		}
		super.channelConnected(ctx, e);
		replayLifeCycleEvents(ctx.getChannel());
	}

	@Override
	public void channelDisconnected(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception {
		log.trace("ExternalPluginServiceChannelHandler.channelDisconnected()");

		synchronized (allChannels) {
			allChannels.remove(ctx.getChannel());
			if (allChannels.isEmpty()) {
				portalEventBus.unregister(this);
			}
		}

		super.channelDisconnected(ctx, e);
	}

	@Override
	public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e) throws Exception {

		log.trace("ExternalPluginServiceChannelHandler.messageReceived({}, {})", ctx, e);

		if (!(e.getMessage() instanceof MessageHeaderPair)) {
			throw new RuntimeException("Expected message of type MessageHeaderPair, got: " + e.getMessage().getClass());
		}

		MessageHeaderPair pair = (MessageHeaderPair) e.getMessage();
		portalEventBus.post(pair.message);
	}

	@Subscribe
	public void onEvent(final Object obj) {
		if (MessageHeaderPair.isUnwrappedMessageEvent(obj)) {
			allChannels.write(MessageHeaderPair.fromUnwrapped(obj));
		}
	}

	private void replayLifeCycleEvents(Channel channel) {
		Collection<Reservation> reservations = reservationManager.getNonFinalizedReservations();
		for (Reservation reservation : reservations) {
			if (reservation.isFinalized()) {
				continue;
			}
			reservation.getPastLifecycleEvents().forEach(evt -> Channels.write(channel, evt));
		}
	}
}