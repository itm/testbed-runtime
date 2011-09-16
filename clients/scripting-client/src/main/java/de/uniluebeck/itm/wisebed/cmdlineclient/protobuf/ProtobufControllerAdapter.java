package de.uniluebeck.itm.wisebed.cmdlineclient.protobuf;

import eu.wisebed.api.common.Message;
import eu.wisebed.api.controller.Controller;
import eu.wisebed.api.controller.RequestStatus;

import javax.jws.WebParam;
import java.util.List;

public class ProtobufControllerAdapter implements ProtobufController {

	private Controller controller;

	public ProtobufControllerAdapter(final Controller controller) {
		this.controller = controller;
	}

	@Override
	public void experimentEnded() {
		controller.experimentEnded();
	}

	@Override
	public void receive(@WebParam(name = "msg", targetNamespace = "") final List<Message> msg) {
		controller.receive(msg);
	}

	@Override
	public void receiveNotification(@WebParam(name = "msg", targetNamespace = "") final List<String> msg) {
		controller.receiveNotification(msg);
	}

	@Override
	public void receiveStatus(@WebParam(name = "status", targetNamespace = "") final List<RequestStatus> status) {
		controller.receiveStatus(status);
	}

	@Override
	public void onConnectionClosed() {
	}

	@Override
	public void onConnectionEstablished() {
	}
}
