package de.uniluebeck.itm.tr.plugins.defaultimage;

import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import de.uniluebeck.itm.tr.iwsn.common.EventBusService;
import de.uniluebeck.itm.tr.iwsn.messages.Request;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationEndedEvent;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationStartedEvent;
import de.uniluebeck.itm.tr.rs.RSHelper;
import eu.wisebed.api.v3.common.NodeUrn;
import org.joda.time.Duration;

import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.*;

public class NodeStatusTrackerImpl extends AbstractService implements NodeStatusTracker {

	private final RSHelper rsHelper;

	private final EventBusService eventBusService;

	private final Map<NodeUrn, FlashStatus> flashStatusMap = newHashMap();

	private final Map<NodeUrn, ReservationStatus> reservationStatusMap = newHashMap();

	private final Duration minUnreservedDuration;

	@Inject
	public NodeStatusTrackerImpl(final RSHelper rsHelper,
								 final EventBusService eventBusService,
								 @Assisted final Duration minUnreservedDuration) {
		this.rsHelper = checkNotNull(rsHelper);
		this.eventBusService = checkNotNull(eventBusService);
		this.minUnreservedDuration = checkNotNull(minUnreservedDuration);
	}

	@Override
	protected void doStart() {
		try {
			eventBusService.register(this);
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {
			eventBusService.unregister(this);
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Subscribe
	public void onReservationStartedEvent(final ReservationStartedEvent event) {
		synchronized (reservationStatusMap) {
			for (NodeUrn nodeUrn : event.getReservation().getNodeUrns()) {
				reservationStatusMap.put(nodeUrn, ReservationStatus.RESERVED);
			}
		}
	}

	@Subscribe
	public void onReservationEndedEvent(final ReservationEndedEvent event) {
		synchronized (reservationStatusMap) {
			for (NodeUrn nodeUrn : event.getReservation().getNodeUrns()) {
				reservationStatusMap.put(nodeUrn, ReservationStatus.UNRESERVED);
			}
		}
	}

	@Subscribe
	public void onFlashImagesRequest(final Request request) {
		if (Request.Type.FLASH_IMAGES.equals(request.getType())) {
			synchronized (flashStatusMap) {
				for (String nodeUrnString : request.getFlashImagesRequest().getNodeUrnsList()) {
					flashStatusMap.put(new NodeUrn(nodeUrnString), FlashStatus.USER_IMAGE);
				}
			}
		}
	}

	@Override
	public void setFlashStatus(final NodeUrn nodeUrn, final FlashStatus flashStatus) {
		checkState(isRunning());
		synchronized (flashStatusMap) {
			flashStatusMap.put(nodeUrn, flashStatus);
		}
	}

	@Override
	public FlashStatus getFlashStatus(final NodeUrn nodeUrn) {
		checkState(isRunning());
		synchronized (flashStatusMap) {
			if (!flashStatusMap.containsKey(nodeUrn)) {
				return FlashStatus.UNKNOWN;
			}
			return flashStatusMap.get(nodeUrn);
		}
	}

	@Override
	public ReservationStatus getReservationStatus(final NodeUrn nodeUrn) {
		checkState(isRunning());
		synchronized (reservationStatusMap) {
			if (!reservationStatusMap.containsKey(nodeUrn)) {
				return ReservationStatus.UNKNOWN;
			}
			return reservationStatusMap.get(nodeUrn);
		}
	}

	@Override
	public Set<NodeUrn> getNodes(final FlashStatus flashStatus) {
		checkState(isRunning());
		final Set<NodeUrn> nodeUrnSet = newHashSet();
		synchronized (flashStatusMap) {
			for (NodeUrn nodeUrn : flashStatusMap.keySet()) {
				if (flashStatusMap.get(nodeUrn).equals(flashStatus)) {
					nodeUrnSet.add(nodeUrn);
				}
			}
		}
		return nodeUrnSet;
	}

	@Override
	public Set<NodeUrn> getNodes(final ReservationStatus reservationStatus) {
		checkState(isRunning());
		final Set<NodeUrn> nodeUrnSet = newHashSet();
		synchronized (reservationStatusMap) {
			for (NodeUrn nodeUrn : reservationStatusMap.keySet()) {
				if (reservationStatusMap.get(nodeUrn).equals(reservationStatus)) {
					nodeUrnSet.add(nodeUrn);
				}
			}
		}
		return nodeUrnSet;
	}

	@Override
	public Set<NodeUrn> getNodes(final FlashStatus flashStatus, final ReservationStatus reservationStatus) {
		checkState(isRunning());
		return intersection(getNodes(flashStatus), getNodes(reservationStatus));
	}

	@Override
	public void run() {
		checkState(isRunning());
		updateFlashStatusMap();
		updateReservationStatusMap();
	}

	private void updateFlashStatusMap() {
		synchronized (flashStatusMap) {
			for (NodeUrn unknownNodeUrn : difference(rsHelper.getNodes(), flashStatusMap.keySet())) {
				flashStatusMap.put(unknownNodeUrn, FlashStatus.UNKNOWN);
			}
		}
	}

	private void updateReservationStatusMap() {
		synchronized (reservationStatusMap) {
			for (NodeUrn nodeUrn : rsHelper.getReservedNodes(minUnreservedDuration)) {
				reservationStatusMap.put(nodeUrn, ReservationStatus.RESERVED);
			}
			for (NodeUrn nodeUrn : rsHelper.getUnreservedNodes(minUnreservedDuration)) {
				reservationStatusMap.put(nodeUrn, ReservationStatus.UNRESERVED);
			}
		}
	}
}