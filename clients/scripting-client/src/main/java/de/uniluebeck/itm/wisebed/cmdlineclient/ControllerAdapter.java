package de.uniluebeck.itm.wisebed.cmdlineclient;

import java.util.List;

import eu.wisebed.api.common.Message;
import eu.wisebed.api.controller.Controller;
import eu.wisebed.api.controller.RequestStatus;

public class ControllerAdapter implements Controller {

    @Override
    public void experimentEnded() {
    }

    @Override
    public void receive(List<Message> msg) {
    }

    @Override
    public void receiveNotification(List<String> msg) {
    }

    @Override
    public void receiveStatus(List<RequestStatus> status) {
    }

}
