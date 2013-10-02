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
import javax.jws.WebService;
import javax.xml.ws.RequestWrapper;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

@WebService(name = "Controller", targetNamespace = "http://wisebed.eu/api/v3/controller")
public class DeliveryManagerTestbedClientController implements DeliveryManagerController {

	@Nullable
	private final String endpointUrl;

	private final Controller endpoint;

	public DeliveryManagerTestbedClientController(final Controller endpoint, @Nullable final String endpointUrl) {
		this.endpoint = checkNotNull(endpoint);
		this.endpointUrl = checkNotNull(endpointUrl);
	}

	@Override
	@Nullable
	public String getEndpointUrl() {
		return endpointUrl;
	}

	@Override
	@WebMethod
	@Oneway
	@RequestWrapper(localName = "nodesAttached", targetNamespace = "http://wisebed.eu/api/v3/controller", className = "eu.wisebed.api.v3.controller.NodesAttached")
	public void nodesAttached(
			@WebParam(name = "timestamp", targetNamespace = "")
			DateTime timestamp,
			@WebParam(name = "nodeUrns", targetNamespace = "")
			List<NodeUrn> nodeUrns) {
		endpoint.nodesAttached(timestamp, nodeUrns);
	}

	@Override
	@WebMethod
	@Oneway
	@RequestWrapper(localName = "nodesDetached", targetNamespace = "http://wisebed.eu/api/v3/controller", className = "eu.wisebed.api.v3.controller.NodesDetached")
	public void nodesDetached(
			@WebParam(name = "timestamp", targetNamespace = "")
			DateTime timestamp,
			@WebParam(name = "nodeUrns", targetNamespace = "")
			List<NodeUrn> nodeUrns) {
		endpoint.nodesDetached(timestamp, nodeUrns);
	}

	@Override
	@WebMethod
	@Oneway
	@RequestWrapper(localName = "receive", targetNamespace = "http://wisebed.eu/api/v3/controller", className = "eu.wisebed.api.v3.controller.Receive")
	public void receive(
			@WebParam(name = "msg", targetNamespace = "")
			List<Message> msg) {
		endpoint.receive(msg);
	}

	@Override
	@WebMethod
	@Oneway
	@RequestWrapper(localName = "receiveNotification", targetNamespace = "http://wisebed.eu/api/v3/controller", className = "eu.wisebed.api.v3.controller.ReceiveNotification")
	public void receiveNotification(
			@WebParam(name = "notifications", targetNamespace = "")
			List<Notification> notifications) {
		endpoint.receiveNotification(notifications);
	}

	@Override
	@WebMethod
	@Oneway
	@RequestWrapper(localName = "receiveStatus", targetNamespace = "http://wisebed.eu/api/v3/controller", className = "eu.wisebed.api.v3.controller.ReceiveStatus")
	public void receiveStatus(
			@WebParam(name = "status", targetNamespace = "")
			List<RequestStatus> status) {
		endpoint.receiveStatus(status);
	}

	@Override
	@WebMethod
	@Oneway
	@RequestWrapper(localName = "reservationEnded", targetNamespace = "http://wisebed.eu/api/v3/controller", className = "eu.wisebed.api.v3.controller.ReservationEnded")
	public void reservationEnded(
			@WebParam(name = "timestamp", targetNamespace = "")
			DateTime timestamp) {
		endpoint.reservationEnded(timestamp);
	}

	@Override
	@WebMethod
	@Oneway
	@RequestWrapper(localName = "reservationStarted", targetNamespace = "http://wisebed.eu/api/v3/controller", className = "eu.wisebed.api.v3.controller.ReservationStarted")
	public void reservationStarted(
			@WebParam(name = "timestamp", targetNamespace = "")
			DateTime timestamp) {
		endpoint.reservationStarted(timestamp);
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		final DeliveryManagerTestbedClientController that = (DeliveryManagerTestbedClientController) o;
		assert endpointUrl != null;
		return endpointUrl.equals(that.endpointUrl);

	}

	@Override
	public int hashCode() {
		assert endpointUrl != null;
		return endpointUrl.hashCode();
	}

	@Override
	public String toString() {
		return endpointUrl;
	}
}
