package de.uniluebeck.itm.wisebed.cmdlineclient.protobuf;

import eu.wisebed.api.controller.Controller;



public interface ProtobufControllerClientListener extends Controller {

	void onConnectionClosed();

	void onConnectionEstablished();

}
