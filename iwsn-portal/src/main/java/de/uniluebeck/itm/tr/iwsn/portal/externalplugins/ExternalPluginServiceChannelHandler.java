package de.uniluebeck.itm.tr.iwsn.portal.externalplugins;

import com.google.inject.Inject;
import com.google.protobuf.MessageLite;
import de.uniluebeck.itm.tr.iwsn.messages.*;
import de.uniluebeck.itm.tr.iwsn.portal.PortalEventBus;
import de.uniluebeck.itm.tr.iwsn.portal.Reservation;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationManager;
import de.uniluebeck.itm.tr.iwsn.messages.ReservationEndedEvent;
import de.uniluebeck.itm.tr.iwsn.messages.ReservationStartedEvent;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Iterables.transform;
import static de.uniluebeck.itm.tr.common.NodeUrnHelper.NODE_URN_TO_STRING;

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
		allChannels.add(ctx.getChannel());
		super.channelConnected(ctx, e);
        replayLifeCycleEvents(ctx.getChannel());
	}

    private void replayLifeCycleEvents(Channel channel) {
        Collection<Reservation> reservations = reservationManager.getNonFinalizedReservations();
        for(Reservation reservation : reservations) {
			if(reservation.isFinalized()) {
                continue;
            }
			reservation.getPastLifecycleEvents().forEach(evt -> Channels.write(channel, evt));
        }
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

		if (!(e.getMessage() instanceof MessageHeaderPair)) {
			throw new RuntimeException("Expected message of type MessageHeaderPair, got: " + e.getMessage().getClass());
		}

		MessageHeaderPair pair = (MessageHeaderPair) e.getMessage();
		
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

	public void onEvent(MessageHeaderPair pair) {
		allChannels.write(pair.message);
	}
}