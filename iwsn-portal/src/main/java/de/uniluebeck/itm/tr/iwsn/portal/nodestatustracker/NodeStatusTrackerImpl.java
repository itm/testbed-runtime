package de.uniluebeck.itm.tr.iwsn.portal.nodestatustracker;

import com.google.common.collect.ImmutableMap;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.common.ServedNodeUrnsProvider;
import de.uniluebeck.itm.tr.iwsn.common.EventBusService;
import de.uniluebeck.itm.tr.iwsn.messages.Request;
import de.uniluebeck.itm.tr.iwsn.portal.PortalEventBus;
import de.uniluebeck.itm.tr.iwsn.portal.events.ReservationEndedEvent;
import de.uniluebeck.itm.tr.iwsn.portal.events.ReservationStartedEvent;
import de.uniluebeck.itm.tr.rs.RSHelper;
import de.uniluebeck.itm.util.Tuple;
import eu.wisebed.api.v3.common.NodeUrn;

import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.intersection;
import static com.google.common.collect.Sets.newHashSet;

public class NodeStatusTrackerImpl extends AbstractService implements NodeStatusTracker {

	private final RSHelper rsHelper;

	private final EventBusService portalEventBus;

	private final Map<NodeUrn, FlashStatus> flashStatusMap = newHashMap();

	private final Map<NodeUrn, ReservationStatus> reservationStatusMap = newHashMap();

	private final ServedNodeUrnsProvider servedNodeUrnsProvider;

	@Inject
	public NodeStatusTrackerImpl(final RSHelper rsHelper,
								 final PortalEventBus portalEventBus,
								 final ServedNodeUrnsProvider servedNodeUrnsProvider) {
		this.rsHelper = checkNotNull(rsHelper);
		this.portalEventBus = checkNotNull(portalEventBus);
		this.servedNodeUrnsProvider = checkNotNull(servedNodeUrnsProvider);
	}

	@Override
	protected void doStart() {
		try {

			portalEventBus.register(this);

			initFlashStatusMap();
			initReservationStatusMap();

			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {
			portalEventBus.unregister(this);
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
	public ImmutableMap<NodeUrn, Tuple<FlashStatus, ReservationStatus>> getStatusMap() {

		checkState(isRunning());

		final Set<NodeUrn> nodeUrns = servedNodeUrnsProvider.get();
		final ImmutableMap.Builder<NodeUrn, Tuple<FlashStatus, ReservationStatus>> mapBuilder = ImmutableMap.builder();

		synchronized (flashStatusMap) {
			synchronized (reservationStatusMap) {
				for (NodeUrn nodeUrn : nodeUrns) {
					mapBuilder.put(nodeUrn, new Tuple<FlashStatus, ReservationStatus>(
							getFlashStatus(nodeUrn),
							getReservationStatus(nodeUrn)
					)
					);
				}
			}
		}

		return mapBuilder.build();
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

	private void initFlashStatusMap() {
		synchronized (flashStatusMap) {
			for (NodeUrn nodeUrn : rsHelper.getNodes()) {
				flashStatusMap.put(nodeUrn, FlashStatus.UNKNOWN);
			}
		}
	}

	private void initReservationStatusMap() {
		synchronized (reservationStatusMap) {
			for (NodeUrn nodeUrn : rsHelper.getReservedNodes()) {
				reservationStatusMap.put(nodeUrn, ReservationStatus.RESERVED);
			}
			for (NodeUrn nodeUrn : rsHelper.getUnreservedNodes()) {
				reservationStatusMap.put(nodeUrn, ReservationStatus.UNRESERVED);
			}
		}
	}
}
