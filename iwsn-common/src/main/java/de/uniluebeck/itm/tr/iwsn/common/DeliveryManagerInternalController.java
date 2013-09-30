package de.uniluebeck.itm.tr.iwsn.common;

import eu.wisebed.api.v3.common.Message;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.controller.Controller;
import eu.wisebed.api.v3.controller.Notification;
import eu.wisebed.api.v3.controller.RequestStatus;
import org.joda.time.DateTime;

import javax.annotation.Nullable;
import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.xml.ws.RequestWrapper;
import java.util.List;

public class DeliveryManagerInternalController implements DeliveryManagerController {

	private final Controller controller;

	public DeliveryManagerInternalController(final Controller controller) {
		this.controller = controller;
	}

	@Override
	@WebMethod
	@Oneway
	@RequestWrapper(localName = "nodesAttached", targetNamespace = "http://wisebed.eu/api/v3/controller", className = "eu.wisebed.api.v3.controller.NodesAttached")
	public void nodesAttached(@WebParam(name = "timestamp", targetNamespace = "") final DateTime timestamp,
							  @WebParam(name = "nodeUrns", targetNamespace = "") final
							  List<NodeUrn> nodeUrns) {
		controller.nodesAttached(timestamp, nodeUrns);
	}

	@Override
	@WebMethod
	@Oneway
	@RequestWrapper(localName = "nodesDetached", targetNamespace = "http://wisebed.eu/api/v3/controller", className = "eu.wisebed.api.v3.controller.NodesDetached")
	public void nodesDetached(@WebParam(name = "timestamp", targetNamespace = "") final DateTime timestamp,
							  @WebParam(name = "nodeUrns", targetNamespace = "") final
							  List<NodeUrn> nodeUrns) {
		controller.nodesDetached(timestamp, nodeUrns);
	}

	@Override
	@WebMethod
	@Oneway
	@RequestWrapper(localName = "receive", targetNamespace = "http://wisebed.eu/api/v3/controller", className = "eu.wisebed.api.v3.controller.Receive")
	public void receive(
			@WebParam(name = "msg", targetNamespace = "") final List<Message> msg) {
		controller.receive(msg);
	}

	@Override
	@WebMethod
	@Oneway
	@RequestWrapper(localName = "receiveNotification", targetNamespace = "http://wisebed.eu/api/v3/controller", className = "eu.wisebed.api.v3.controller.ReceiveNotification")
	public void receiveNotification(
			@WebParam(name = "notifications", targetNamespace = "") final
			List<Notification> notifications) {
		controller.receiveNotification(notifications);
	}

	@Override
	@WebMethod
	@Oneway
	@RequestWrapper(localName = "receiveStatus", targetNamespace = "http://wisebed.eu/api/v3/controller", className = "eu.wisebed.api.v3.controller.ReceiveStatus")
	public void receiveStatus(
			@WebParam(name = "status", targetNamespace = "") final
			List<RequestStatus> status) {
		controller.receiveStatus(status);
	}

	@Override
	@WebMethod
	@Oneway
	@RequestWrapper(localName = "reservationEnded", targetNamespace = "http://wisebed.eu/api/v3/controller", className = "eu.wisebed.api.v3.controller.ReservationEnded")
	public void reservationEnded(
			@WebParam(name = "timestamp", targetNamespace = "") final DateTime timestamp) {
		controller.reservationEnded(timestamp);
	}

	@Override
	@WebMethod
	@Oneway
	@RequestWrapper(localName = "reservationStarted", targetNamespace = "http://wisebed.eu/api/v3/controller", className = "eu.wisebed.api.v3.controller.ReservationStarted")
	public void reservationStarted(
			@WebParam(name = "timestamp", targetNamespace = "") final DateTime timestamp) {
		controller.reservationStarted(timestamp);
	}

	@Nullable
	@Override
	public String getEndpointUrl() {
		return null;
	}
}
