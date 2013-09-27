package de.uniluebeck.itm.tr.federator;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AbstractService;
import de.uniluebeck.itm.tr.federator.iwsn.WSNFederatorManager;
import de.uniluebeck.itm.tr.iwsn.common.EventBusService;
import de.uniluebeck.itm.tr.iwsn.messages.Request;
import de.uniluebeck.itm.tr.iwsn.portal.PortalEventBus;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.SecretReservationKey;
import eu.wisebed.api.v3.sm.UnknownSecretReservationKeyFault;
import eu.wisebed.api.v3.wsn.AuthorizationFault;
import eu.wisebed.api.v3.wsn.ReservationNotRunningFault_Exception;

import java.util.List;

import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static de.uniluebeck.itm.tr.common.NodeUrnHelper.STRING_TO_NODE_URN;

public class FederatorPortalEventBus extends AbstractService implements PortalEventBus {

	private final EventBusService eventBusService;

	private final WSNFederatorManager wsnFederatorManager;

	public FederatorPortalEventBus(final EventBusService eventBusService,
								   final WSNFederatorManager wsnFederatorManager) {
		this.eventBusService = eventBusService;
		this.wsnFederatorManager = wsnFederatorManager;
	}

	@Override
	protected void doStart() {
		try {
			eventBusService.startAndWait();
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {
			eventBusService.stopAndWait();
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	public void register(final Object object) {
		eventBusService.register(object);
	}

	@Override
	public void unregister(final Object object) {
		eventBusService.unregister(object);
	}

	@Override
	public void post(final Object event) {
		if (event instanceof Request) {

			final Request request = (Request) event;
			final String reservationId = request.getReservationId();
			final long requestId = request.getRequestId();

			switch (request.getType()) {
				case ARE_NODES_ALIVE:
					try {
						final List<NodeUrn> nodeUrns = newArrayList(
								transform(request.getAreNodesAliveRequest().getNodeUrnsList(), STRING_TO_NODE_URN)
						);
						wsnFederatorManager
								.getWsnFederatorService(Lists.<SecretReservationKey>newArrayList())
								.areNodesAlive(requestId, nodeUrns);

					} catch (AuthorizationFault authorizationFault) {
						authorizationFault.printStackTrace();  // TODO implement
					} catch (ReservationNotRunningFault_Exception e) {
						e.printStackTrace();  // TODO implement
					} catch (UnknownSecretReservationKeyFault unknownSecretReservationKeyFault) {
						unknownSecretReservationKeyFault.printStackTrace();  // TODO implement
					}
					break;
				case ARE_NODES_CONNECTED:
					break;
				case DISABLE_NODES:
					break;
				case DISABLE_VIRTUAL_LINKS:
					break;
				case DISABLE_PHYSICAL_LINKS:
					break;
				case ENABLE_NODES:
					break;
				case ENABLE_PHYSICAL_LINKS:
					break;
				case ENABLE_VIRTUAL_LINKS:
					break;
				case FLASH_IMAGES:
					break;
				case GET_CHANNEL_PIPELINES:
					break;
				case RESET_NODES:
					break;
				case SEND_DOWNSTREAM_MESSAGES:
					break;
				case SET_CHANNEL_PIPELINES:
					break;
				default:
					throw new RuntimeException("Unknown request type: " + request.getType());
			}
		}
		throw new RuntimeException("There shouldn't be other events than requests");
	}
}
