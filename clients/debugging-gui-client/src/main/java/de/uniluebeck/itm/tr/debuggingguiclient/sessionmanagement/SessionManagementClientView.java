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

package de.uniluebeck.itm.tr.debuggingguiclient.sessionmanagement;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;


public class SessionManagementClientView extends JPanel {

    private JTextField endpointUrlTextField;

	private JTextArea secretReservationKeysTextArea;

    private JTextField controllerEndpointTextField;

	private JButton getInstanceButton;

    private JButton freeButton;

	private JTextField getInstanceResultTextField;

    private JButton getInstanceResultCopyButton;

    private JButton getNetworkButton;

    public SessionManagementClientView() {

        super(new FlowLayout());
        ((FlowLayout) super.getLayout()).setAlignment(FlowLayout.LEFT);

		final JPanel panel = new JPanel(new GridLayout(9, 2));

        {
            endpointUrlTextField = new JTextField();

            panel.add(new JLabel("Endpoint URL"));
            panel.add(endpointUrlTextField);
        }
        {
			secretReservationKeysTextArea = new JTextArea();
            JScrollPane secretReservationKeysScrollPane = new JScrollPane(secretReservationKeysTextArea);
            secretReservationKeysScrollPane.setPreferredSize(new Dimension(400, 50));

            panel.add(new JLabel("Secret Reservation Keys (one tuple per line: urnPrefix,secretReservationKey)"));
            panel.add(secretReservationKeysScrollPane);
        }
        {
            controllerEndpointTextField = new JTextField();

            panel.add(new JLabel("Controller Endpoint URL"));
            panel.add(controllerEndpointTextField);
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
            getNetworkButton = new JButton("getNetwork()");

            panel.add(new JLabel());
            panel.add(getNetworkButton);
        }
        {
            getInstanceResultTextField = new JTextField();

            panel.add(new JLabel("GetInstance() result"));
            panel.add(getInstanceResultTextField);
        }
        {
            getInstanceResultCopyButton = new JButton("Copy to WSN client");

            panel.add(new JLabel());
            panel.add(getInstanceResultCopyButton);
        }


        add(panel);

    }

    public JTextField getEndpointUrlTextField() {
        return endpointUrlTextField;
    }

    public JTextArea getSecretReservationKeysTextArea() {
        return secretReservationKeysTextArea;
    }

    public JTextField getControllerEndpointTextField() {
        return controllerEndpointTextField;
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

    public JButton getGetNetworkButton() {
        return getNetworkButton;
    }
}
