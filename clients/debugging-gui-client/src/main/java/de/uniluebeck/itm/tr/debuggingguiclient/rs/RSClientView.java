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
package de.uniluebeck.itm.tr.debuggingguiclient.rs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.text.DateFormat;

import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class RSClientView extends JPanel {

    private JTextField endpointUrlTextField;

    private JTextArea secretAuthenticationKeysTextArea;

    private JFormattedTextField fromDateTextField;

    private JFormattedTextField untilDateTextField;

    private JButton makeReservationButton;

    private JTextArea nodeUrnsTextArea;

    private JTextArea secretReservationKeysTextArea;

    private JButton copySecretReservationKeysButton;

    private JButton getReservationsButton;
    
    private JButton getConfidentialReservationsButton;

	private JButton selectNodeUrnsButton;

	public RSClientView() {
        super(new FlowLayout());
        ((FlowLayout) super.getLayout()).setAlignment(FlowLayout.LEFT);

        JPanel superPanel = new JPanel();
        JPanel panel = new JPanel(new GridLayout(10, 2));
        superPanel.add(panel);

        {
            endpointUrlTextField = new JTextField();
            panel.add(new JLabel("RS Endpoint URL"));
            panel.add(endpointUrlTextField);
        }
        {
            secretAuthenticationKeysTextArea = new JTextArea();
            JScrollPane secretAuthenticationKeysScrollPane = new JScrollPane(secretAuthenticationKeysTextArea);
            secretAuthenticationKeysScrollPane.setPreferredSize(new Dimension(400, 50));
            panel.add(new JLabel("Secret Authentication Keys (urnprefix,username,secretauthenticationkey) one triple per line"));
            panel.add(secretAuthenticationKeysScrollPane);
        }
        {
            fromDateTextField = new JFormattedTextField(DateFormat.getInstance());
            panel.add(new JLabel("Date From"));
            panel.add(fromDateTextField);
        }
        {
            untilDateTextField = new JFormattedTextField(DateFormat.getInstance());
            panel.add(new JLabel("Date Until"));
            panel.add(untilDateTextField);
        }
        {
            nodeUrnsTextArea = new JTextArea();
			nodeUrnsTextArea.setLineWrap(true);
            JScrollPane scrollPane = new JScrollPane(nodeUrnsTextArea);
            scrollPane.setPreferredSize(new Dimension(400, 50));
			panel.add(new JLabel("Node URNs"));
            panel.add(scrollPane);
        }
        {
			panel.add(new JLabel());

			JPanel horizontalPanel = new JPanel(new BorderLayout());
			makeReservationButton = new JButton("makeReservation()");
			selectNodeUrnsButton = new JButton("Select Node URNs");
			horizontalPanel.add(makeReservationButton, BorderLayout.WEST);
			horizontalPanel.add(selectNodeUrnsButton, BorderLayout.EAST);
			panel.add(horizontalPanel);
        }
        {
            getReservationsButton = new JButton("getReservations()");
            panel.add(new JLabel());
            panel.add(getReservationsButton);
        }
        {
            getConfidentialReservationsButton = new JButton("getConfidentialReservations()");
            panel.add(new JLabel());
            panel.add(getConfidentialReservationsButton);
        }
        {
            secretReservationKeysTextArea = new JTextArea();
            JScrollPane secretReservationKeysScrollPane = new JScrollPane(secretReservationKeysTextArea);
            secretReservationKeysScrollPane.setPreferredSize(new Dimension(400, 50));
            panel.add(new JLabel("Secret Reservation Keys"));
            panel.add(secretReservationKeysScrollPane);
        }
        {
            copySecretReservationKeysButton = new JButton("Copy SRKs to Session Management client");
            panel.add(new JLabel());
            panel.add(copySecretReservationKeysButton);
        }

        add(superPanel);
    }

    public JButton getCopySecretReservationKeysButton() {
        return copySecretReservationKeysButton;
    }

    public JButton getGetConfidentialReservationsButton() {
        return getConfidentialReservationsButton;
    }

    public JTextField getEndpointUrlTextField() {
        return endpointUrlTextField;
    }

    public JTextArea getSecretAuthenticationKeysTextArea() {
        return secretAuthenticationKeysTextArea;
    }

    public JFormattedTextField getFromDateTextField() {
        return fromDateTextField;
    }

    public JFormattedTextField getUntilDateTextField() {
        return untilDateTextField;
    }

    public JButton getMakeReservationButton() {
        return makeReservationButton;
    }

    public JTextArea getNodeUrnsTextArea() {
        return nodeUrnsTextArea;
    }

    public JTextArea getSecretReservationKeysTextArea() {
        return secretReservationKeysTextArea;
    }

    public JButton getGetReservationsButton() {
        return getReservationsButton;
    }

	public JButton getSelectNodeUrnsButton() {
		return selectNodeUrnsButton;
	}
}
