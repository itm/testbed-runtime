package de.uniluebeck.itm.tr.iwsn.common;

import eu.wisebed.api.v3.common.Message;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.controller.Controller;
import eu.wisebed.api.v3.controller.Notification;
import eu.wisebed.api.v3.controller.RequestStatus;
import org.joda.time.DateTime;

import javax.annotation.Nullable;
import javax.jws.WebParam;
import java.util.List;

public class DeliveryManagerInternalController implements DeliveryManagerController {

	private final Controller controller;

	public DeliveryManagerInternalController(final Controller controller) {
		this.controller = controller;
	}

	@Override
	public void nodesAttached(@WebParam(name = "timestamp", targetNamespace = "") final DateTime timestamp,
							  @WebParam(name = "nodeUrns", targetNamespace = "") final
							  List<NodeUrn> nodeUrns) {
		controller.nodesAttached(timestamp, nodeUrns);
	}

	@Override
	public void nodesDetached(@WebParam(name = "timestamp", targetNamespace = "") final DateTime timestamp,
							  @WebParam(name = "nodeUrns", targetNamespace = "") final
							  List<NodeUrn> nodeUrns) {
		controller.nodesDetached(timestamp, nodeUrns);
	}

	@Override
	public void receive(
			@WebParam(name = "msg", targetNamespace = "") final List<Message> msg) {
		controller.receive(msg);
	}

	@Override
	public void receiveNotification(
			@WebParam(name = "notifications", targetNamespace = "") final
			List<Notification> notifications) {
		controller.receiveNotification(notifications);
	}

	@Override
	public void receiveStatus(
			@WebParam(name = "status", targetNamespace = "") final
			List<RequestStatus> status) {
		controller.receiveStatus(status);
	}

	@Override
	public void reservationEnded(
			@WebParam(name = "timestamp", targetNamespace = "") final DateTime timestamp) {
		controller.reservationEnded(timestamp);
	}

	@Override
	public void reservationStarted(
			@WebParam(name = "timestamp", targetNamespace = "") final DateTime timestamp) {
		controller.reservationStarted(timestamp);
	}

	@Nullable
	@Override
	public String getEndpointUrl() {
		return null;
	}

	@Override
	public String toString() {
		return "DeliveryManagerInternalController{" +
				"controller=" + controller +
				'}';
	}
}
