package de.uniluebeck.itm.tr.iwsn.common;

import com.google.common.util.concurrent.Service;
import eu.wisebed.api.v3.common.Message;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.controller.Notification;
import eu.wisebed.api.v3.controller.RequestStatus;

import java.util.List;
import java.util.Set;

public interface DeliveryManager extends Service {

	void addController(String endpointUrl);

	void removeController(String endpointUrl);

	void reservationEnded();

	void nodesAttached(List<NodeUrn> nodeUrns);

	void nodesDetached(List<NodeUrn> nodeUrns);

	void receive(Message... messages);

	void receive(List<Message> messages);

	void receiveNotification(Notification... notifications);

	void receiveNotification(List<Notification> notifications);

	void receiveStatus(RequestStatus... statuses);

	void receiveStatus(List<RequestStatus> statuses);

	void receiveFailureStatusMessages(List<NodeUrn> nodeUrns, long requestId, Exception e, int statusValue);

	void receiveUnknownNodeUrnRequestStatus(Set<NodeUrn> nodeUrns, String msg, long requestId);
}
