package eu.wisebed.api.controller;


import eu.wisebed.api.common.Message;

import javax.jws.WebParam;
import java.util.List;

public class ControllerAdapter implements Controller {

	@Override
	public void experimentEnded() {
	}

	@Override
	public void receive(@WebParam(name = "msg", targetNamespace = "") final List<Message> msg) {
	}

	@Override
	public void receiveNotification(@WebParam(name = "msg", targetNamespace = "") final List<String> msg) {
	}

	@Override
	public void receiveStatus(@WebParam(name = "status", targetNamespace = "") final List<RequestStatus> status) {
	}
}
