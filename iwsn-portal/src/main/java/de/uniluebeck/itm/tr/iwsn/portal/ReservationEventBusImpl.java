package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import de.uniluebeck.itm.tr.iwsn.messages.*;
import eu.wisebed.api.v3.common.NodeUrn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static de.uniluebeck.itm.tr.iwsn.messages.RequestHelper.extractNodeUrns;

public class ReservationEventBusImpl extends AbstractService implements ReservationEventBus {

    private static final Logger log = LoggerFactory.getLogger(ReservationEventBus.class);

    protected final PortalEventBus portalEventBus;

    protected final EventBus eventBus;

    protected final Reservation reservation;

    protected final String reservationId;

    @Inject
    public ReservationEventBusImpl(final PortalEventBus portalEventBus,
                                   final EventBusFactory eventBusFactory,
                                   @Assisted final Reservation reservation) {
        this.portalEventBus = portalEventBus;
        this.eventBus = eventBusFactory.create("ReservationEventBus[" + reservation.getSerializedKey() + "]");
        this.reservation = reservation;
        this.reservationId = reservation.getSerializedKey();
    }

    @Override
    protected void doStart() {

        log.trace("ReservationEventBus[{}].doStart()", reservationId);
        this.register(portalEventBus);
        try {
            notifyStarted();
        } catch (Exception e) {
            notifyFailed(e);
        }
    }

    @Override
    protected void doStop() {

        log.trace("ReservationEventBus[{}].doStop()", reservationId);
        this.unregister(portalEventBus);
        try {
            notifyStopped();
        } catch (Exception e) {
            notifyFailed(e);
        }
    }

    @Override
    public void register(final Object object) {
        log.trace("ReservationEventBus[{}].register(object={})", reservationId, object);
        eventBus.register(object);
    }

    @Override
    public void unregister(final Object object) {
        log.trace("ReservationEventBus[{}].unregister(object={})", reservationId, object);
        eventBus.unregister(object);
    }

    @Override
    public void post(final Object event) {

        log.trace("ReservationEventBus[{}].post({})", reservationId, event);

        checkState(isRunning(), "ReservationEventBus is not running");

        if (event instanceof Request) {
            assertNodesArePartOfReservation(extractNodeUrns((Request) event));
        }

        eventBus.post(event);
    }

    @Override
    public void enableVirtualization() {
        throw new RuntimeException("Virtualization features are not yet implemented!");
    }

    @Override
    public void disableVirtualization() {
        throw new RuntimeException("Virtualization features are not yet implemented!");
    }

    @Override
    public boolean isVirtualizationEnabled() {
        return false;
    }

    @Override
    public String toString() {
        return getClass().getName() + "[" + reservationId + "]";
    }

    private void assertNodesArePartOfReservation(final Set<NodeUrn> nodeUrns) {
        if (!reservation.getNodeUrns().containsAll(nodeUrns)) {
            final Set<NodeUrn> unreservedNodeUrns = Sets.filter(nodeUrns, not(in(reservation.getNodeUrns())));
            throw new IllegalArgumentException("The node URNs [" + Joiner.on(",").join(unreservedNodeUrns) + "] "
                    + "are not part of the reservation."
            );
        }

    }
}
