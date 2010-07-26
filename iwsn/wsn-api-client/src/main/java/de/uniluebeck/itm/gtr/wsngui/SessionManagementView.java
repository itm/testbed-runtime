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

import javax.swing.*;
import java.awt.*;


public class SessionManagementView extends JPanel {

	private JTextField endpointUrlTextField;

	private JLabel endpointUrlLabel;

	private JLabel smPanelReservationIdLabel;

	private JTextArea secretReservationKeysTextArea;

	private JTextField controllerTextField;

	private JLabel smPanelControllerLabel;

	private JButton getInstanceButton;

	private JButton freeButton;

	private SessionManagementModel model;

	private JPanel panel;

	private JTextField getInstanceResultTextField;

	private JButton getInstanceResultCopyButton;

	public SessionManagementView(SessionManagementModel model) {

		super(new FlowLayout());
		((FlowLayout) super.getLayout()).setAlignment(FlowLayout.LEFT);

		this.panel = new JPanel(new GridLayout(8, 2));
		this.model = model;

		{
			endpointUrlLabel = new JLabel("Endpoint URL");
			endpointUrlTextField = new JTextField();

			panel.add(endpointUrlLabel);
			panel.add(endpointUrlTextField);
		}
		{
			smPanelReservationIdLabel = new JLabel("Secret Reservation Keys\none tuple per line\nurnPrefix,secretReservationKey");
			secretReservationKeysTextArea = new JTextArea(5, 30);

			panel.add(smPanelReservationIdLabel);
			panel.add(secretReservationKeysTextArea);
		}
		{
			smPanelControllerLabel = new JLabel("Controller Endpoint URL");
			controllerTextField = new JTextField();

			panel.add(smPanelControllerLabel);
			panel.add(controllerTextField);
		}
		{
			getInstanceButton = new JButton("getInstance()");

			panel.add(new JLabel());
			panel.add(getInstanceButton);
		}
		{
			freeButton = new JButton("free()");

			panel.add(new JLabel());
			panel.add(freeButton);
		}
		{
			getInstanceResultTextField = new JTextField();

			panel.add(new JLabel("GetInstance() result"));
			panel.add(getInstanceResultTextField);
		}
		{
			getInstanceResultCopyButton = new JButton("Copy to WSN Client");

			panel.add(new JLabel());
			panel.add(getInstanceResultCopyButton);
		}


		add(panel);
		add(Box.createVerticalGlue());

	}

	public JTextField getEndpointUrlTextField() {
		return endpointUrlTextField;
	}

	public JTextArea getSecretReservationKeysTextArea() {
		return secretReservationKeysTextArea;
	}

	public JTextField getControllerTextField() {
		return controllerTextField;
	}

	public JButton getGetInstanceButton() {
		return getInstanceButton;
	}

	public JButton getFreeButton() {
		return freeButton;
	}

	public JButton getGetInstanceResultCopyButton() {
		return getInstanceResultCopyButton;
	}

	public JTextField getGetInstanceResultTextField() {
		return getInstanceResultTextField;
	}
}
