package de.uniluebeck.itm.tr.iwsn.common;

import com.google.common.util.concurrent.Service;
import eu.wisebed.api.v3.common.Message;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.controller.Notification;
import eu.wisebed.api.v3.controller.RequestStatus;

public interface DeliveryManager extends Service {

	void addController(String endpointUrl);

	void removeController(String endpointUrl);

	/**
	 * Asynchronously notifies all currently registered controllers that the experiment has <emph>started</emph>.
	 */
	void reservationStarted();

	/**
	 * Asynchronously notifies the controller with the endpoint URL {@code controllerEndpointUrl} that the experiment has
	 * <emph>started</emph>.
	 */
	void reservationStarted(String controllerEndpointUrl);

	/**
	 * Asynchronously notifies all currently registered controllers that the experiment has ended.
	 */
	void reservationEnded();

	/**
	 * Asynchronously notifies the controller with the endpoint URL {@code controllerEndpointUrl} that the experiment has
	 * <emph>ended</emph>.
	 */
	void reservationEnded(String controllerEndpointUrl);

	void nodesAttached(Iterable<NodeUrn> nodeUrns);

	void nodesDetached(Iterable<NodeUrn> nodeUrns);

	void receive(Message... messages);

	void receive(Iterable<Message> messages);

	void receiveNotification(Notification... notifications);

	void receiveNotification(Iterable<Notification> notifications);

	void receiveStatus(RequestStatus... statuses);

	void receiveStatus(Iterable<RequestStatus> statuses);

	void receiveFailureStatusMessages(Iterable<NodeUrn> nodeUrns, long requestId, Exception e, int statusValue);

	void receiveUnknownNodeUrnRequestStatus(Iterable<NodeUrn> nodeUrns, String msg, long requestId);
}
