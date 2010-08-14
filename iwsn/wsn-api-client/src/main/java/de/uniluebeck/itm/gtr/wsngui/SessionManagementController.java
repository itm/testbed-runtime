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

package de.uniluebeck.itm.gtr.wsngui;

import de.uniluebeck.itm.tr.util.StringUtils;
import eu.wisebed.testbed.api.wsn.WSNServiceHelper;
import eu.wisebed.testbed.api.wsn.v211.*;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class SessionManagementController {

	private SessionManagementView view;

	private SessionManagementModel model;

	private WSNClientView wsnClientView;

	private ActionListener getInstanceResultCopyActionListener = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			wsnClientView.getEndpointUrlTextField().setText(view.getGetInstanceResultTextField().getText());
		}
	};

	private ActionListener getInstanceActionListener = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			String endpointUrl = view.getEndpointUrlTextField().getText();
			String controllerEndpointUrl = view.getControllerTextField().getText();
			String secretReservationKeysString = view.getSecretReservationKeysTextArea().getText();
			List<SecretReservationKey> srkList = parseSecretReservationKeyList(secretReservationKeysString);

			try {

				SessionManagement smService = WSNServiceHelper.getSessionManagementService(endpointUrl);
				String instanceUrl = smService.getInstance(srkList, controllerEndpointUrl);

				view.getGetInstanceResultTextField().setText(instanceUrl);

			} catch (ExperimentNotRunningException_Exception e1) {
				JOptionPane.showMessageDialog(null, "Experiment is not running");
			} catch (UnknownReservationIdException_Exception e1) {
				JOptionPane.showMessageDialog(null, "Unknown reservation ID");
			} catch (Exception e1) {
				JOptionPane.showMessageDialog(null, e1.getMessage());
			}
		}
	};

	private ActionListener getNetworkActionListener = new ActionListener() {
		@Override
		public void actionPerformed(final ActionEvent e) {

			String endpointUrl = view.getEndpointUrlTextField().getText();
			try {

				SessionManagement smService = WSNServiceHelper.getSessionManagementService(endpointUrl);
				Dialogs.showTextDialog(smService.getNetwork(), true);

			} catch (Exception e1) {
				JOptionPane.showMessageDialog(null, e1.getMessage());
			}
		}
	};

	private List<SecretReservationKey> parseSecretReservationKeyList(String secretReservationKeysString) {
		List<String> srkStrings = StringUtils.parseLines(secretReservationKeysString);
		List<SecretReservationKey> srkList = new ArrayList<SecretReservationKey>(srkStrings.size());
		SecretReservationKey key;
		for (String srkString : srkStrings) {
			String[] tupleStrings = srkString.split(",");
			key = new SecretReservationKey();
			key.setUrnPrefix(tupleStrings[0]);
			key.setSecretReservationKey(tupleStrings[1]);
			srkList.add(key);
		}
		return srkList;
	}

	private ActionListener freeActionListener = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			String endpointUrl = view.getEndpointUrlTextField().getText();
			String secretReservationKeysString = view.getSecretReservationKeysTextArea().getText();
			try {

				SessionManagement smService = WSNServiceHelper.getSessionManagementService(endpointUrl);
				List<SecretReservationKey> srkList = parseSecretReservationKeyList(secretReservationKeysString);
				smService.free(srkList);
				view.getGetInstanceResultTextField().setText("");

			} catch (ExperimentNotRunningException_Exception e1) {
				JOptionPane.showMessageDialog(null, "Experiment is not running");
			} catch (UnknownReservationIdException_Exception e1) {
				JOptionPane.showMessageDialog(null, "Unknown reservation ID");
			} catch (Exception e1) {
				JOptionPane.showMessageDialog(null, e1.getMessage());
			}
		}
	};

	public SessionManagementController(SessionManagementView view, SessionManagementModel model,
									   WSNClientView wsnClientView) {

		this.view = view;
		this.model = model;
		this.wsnClientView = wsnClientView;

		this.view.getGetInstanceButton().addActionListener(getInstanceActionListener);
		this.view.getFreeButton().addActionListener(freeActionListener);
		this.view.getGetNetworkButton().addActionListener(getNetworkActionListener);

		try {
			this.view.getEndpointUrlTextField()
					.setText("http://" + InetAddress.getLocalHost().getHostName() + ":10001/sessions");
			this.view.getSecretReservationKeysTextArea().setText(""
					+ "urn:wisebed:testbeduzl1:,1234"
			);
			this.view.getControllerTextField()
					.setText("http://" + InetAddress.getLocalHost().getHostName() + ":8081/controller");
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		this.view.getGetInstanceResultCopyButton().addActionListener(getInstanceResultCopyActionListener);

	}

}
