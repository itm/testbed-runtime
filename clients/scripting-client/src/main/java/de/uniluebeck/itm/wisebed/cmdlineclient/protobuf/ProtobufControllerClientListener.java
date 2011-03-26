package de.uniluebeck.itm.wisebed.cmdlineclient.protobuf;


import eu.wisebed.testbed.api.wsn.v22.Controller;

public interface ProtobufControllerClientListener extends Controller {

	void onConnectionClosed();

	void onConnectionEstablished();

}
