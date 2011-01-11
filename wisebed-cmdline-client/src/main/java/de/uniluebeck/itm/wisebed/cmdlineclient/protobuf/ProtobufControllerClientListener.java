package de.uniluebeck.itm.wisebed.cmdlineclient.protobuf;


import eu.wisebed.testbed.api.wsn.v211.Controller;

public interface ProtobufControllerClientListener extends Controller {

	void onConnectionClosed();

	void onConnectionEstablished();

}
