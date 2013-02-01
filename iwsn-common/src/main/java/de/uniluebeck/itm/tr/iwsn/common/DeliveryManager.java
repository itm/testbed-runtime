package de.uniluebeck.itm.tr.iwsn.common;

import com.google.common.util.concurrent.Service;
import eu.wisebed.api.v3.common.Message;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.controller.Notification;
import eu.wisebed.api.v3.controller.RequestStatus;

public interface DeliveryManager extends Service {

	void addController(String endpointUrl);

	void removeController(String endpointUrl);

	void reservationStarted();

	void reservationEnded();

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
