/**********************************************************************************************************************
 * Copyright (c) 2010, Institute of Telematics, University of Luebeck                                                 *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote*
 *   products derived from this software without specific prior written permission.                                   *
 *                                                                                                                    *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, *
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE      *
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,         *
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE *
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF    *
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY   *
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.                                *
 **********************************************************************************************************************/

package de.uniluebeck.itm.tr.debuggingguiclient.controller;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import de.uniluebeck.itm.tr.debuggingguiclient.Dialogs;
import de.uniluebeck.itm.tr.debuggingguiclient.WSNClientProperties;
import eu.wisebed.api.common.Message;
import eu.wisebed.api.controller.Controller;
import eu.wisebed.api.controller.RequestStatus;
import eu.wisebed.testbed.api.wsn.WSNServiceHelper;


public class ControllerClientController {

	private static final Logger log = LoggerFactory.getLogger(ControllerClientController.class);

	private ControllerClientView view;

	private Controller getControllerService() {
		String endpointUrl = view.getEndpointUrlTextField().getText();
		Controller controllerPort = WSNServiceHelper.getControllerService(endpointUrl);
		return controllerPort;
	}

	private ActionListener receiveActionListener = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {

			Dialogs.MessagePanel panel = new Dialogs.MessagePanel();
			Dialogs.InputDialog<Message> dialog = new Dialogs.InputDialog<Message>(
					"Receive - Parameters",
					panel
			);
			dialog.setVisible(true);
			Message message = dialog.getResult();

			if (message != null) {

				getControllerService().receive(Lists.newArrayList(message));
				log.info("Called Controller.receive() successfully...");

			}

		}
	};

	private ActionListener receiveStatusActionListener = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {

			Dialogs.RequestStatusPanel panel = new Dialogs.RequestStatusPanel();
			Dialogs.InputDialog<RequestStatus> dialog = new Dialogs.InputDialog<RequestStatus>(
					"ReceiveStatus - Parameters",
					panel
			);
			dialog.setVisible(true);
			RequestStatus status = dialog.getResult();

			if (status != null) {

				getControllerService().receiveStatus(Lists.newArrayList(status));
				log.info("Called Controller.receiveStatus() successfully...");

			}

		}
	};

	public ControllerClientController(ControllerClientView view, Properties properties) {

		this.view = view;

		this.view.getReceiveButton().addActionListener(receiveActionListener);
		this.view.getReceiveStatusButton().addActionListener(receiveStatusActionListener);
		
		try {
			this.view.getEndpointUrlTextField().setText(
					properties.getProperty(
							WSNClientProperties.CONTROLLER_CLIENT_ENDPOINTURL,
							"http://" + InetAddress.getLocalHost().getHostName() + ":8081/controller"
					)
			);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

	}

}
