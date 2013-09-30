package de.uniluebeck.itm.tr.federator.iwsn;

import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractService;
import de.uniluebeck.itm.tr.iwsn.common.DeliveryManagerInternalController;
import de.uniluebeck.itm.tr.iwsn.messages.Request;
import eu.wisebed.api.v3.common.Message;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.controller.Controller;
import eu.wisebed.api.v3.controller.Notification;
import eu.wisebed.api.v3.controller.RequestStatus;
import org.joda.time.DateTime;

import javax.jws.WebParam;
import java.util.List;

public class FederatedEventBusApiAdapter extends AbstractService implements Controller {

	private final DeliveryManagerInternalController dmController = new DeliveryManagerInternalController(this);

	private final FederatedReservationEventBus federatedReservationEventBus;

	private final WSNFederatorController wsnFederatorController;

	public FederatedEventBusApiAdapter(
			final FederatedReservationEventBus federatedReservationEventBus,
			final WSNFederatorController wsnFederatorController) {
		this.federatedReservationEventBus = federatedReservationEventBus;
		this.wsnFederatorController = wsnFederatorController;
	}


	@Override
	protected void doStart() {
		try {
			wsnFederatorController.addController(dmController);
			federatedReservationEventBus.register(this);
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {
			federatedReservationEventBus.unregister(this);
			wsnFederatorController.removeController(dmController);
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Subscribe
	public void onRequest(final Request request) {
		// this call comes from another internal or external TR component (e.g., the REST API or an external plugin)
		// TODO implement
	}

	@Override
	public void nodesAttached(@WebParam(name = "timestamp", targetNamespace = "") final DateTime timestamp,
							  @WebParam(name = "nodeUrns", targetNamespace = "") final List<NodeUrn> nodeUrns) {
		// this call comes from a federated testbed
		// TODO implement
	}

	@Override
	public void nodesDetached(@WebParam(name = "timestamp", targetNamespace = "") final DateTime timestamp,
							  @WebParam(name = "nodeUrns", targetNamespace = "") final List<NodeUrn> nodeUrns) {
		// this call comes from a federated testbed
		// TODO implement
	}

	@Override
	public void reservationStarted(@WebParam(name = "timestamp", targetNamespace = "") final DateTime timestamp) {
		// this call comes from a federated testbed
		// TODO implement
	}

	@Override
	public void reservationEnded(@WebParam(name = "timestamp", targetNamespace = "") final DateTime timestamp) {
		// this call comes from a federated testbed
		// TODO implement
	}

	@Override
	public void receive(@WebParam(name = "msg", targetNamespace = "") final List<Message> msg) {
		// this call comes from a federated testbed
		// TODO implement
	}

	@Override
	public void receiveNotification(
			@WebParam(name = "notifications", targetNamespace = "") final List<Notification> notifications) {
		// this call comes from a federated testbed
		// TODO implement
	}

	@Override
	public void receiveStatus(@WebParam(name = "status", targetNamespace = "") final List<RequestStatus> status) {
		// this call comes from a federated testbed
		// TODO implement
	}
}
