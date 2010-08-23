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
package de.uniluebeck.itm.gtr.wsngui.rs;

import javax.swing.*;
import java.awt.*;
import java.text.DateFormat;

public class RSClientView extends JPanel {

    private RSClientModel model;

    private JTextField endpointUrlTextField;

    private JTextArea secretAuthenticationKeysTextArea;

    private JFormattedTextField fromDateTextField;

    private JFormattedTextField toDateTextField;

    private JButton makeReservationButton;

    private JTextField nodeUrnsTextField;

    private JTextArea secretReservationKeysTextArea;

    private JButton copySecretReservationKeysButton;

    public RSClientView(final RSClientModel model) {
        super(new FlowLayout());
        ((FlowLayout) super.getLayout()).setAlignment(FlowLayout.LEFT);

        this.model = model;

        JPanel panel = new JPanel(new GridLayout(8, 2));

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
            toDateTextField = new JFormattedTextField(DateFormat.getInstance());
            panel.add(new JLabel("Date Until"));
            panel.add(toDateTextField);
        }
        {
            nodeUrnsTextField = new JTextField();
            panel.add(new JLabel("Node URNs"));
            panel.add(nodeUrnsTextField);
        }
        {
            makeReservationButton = new JButton("makeReservation()");
            panel.add(new JLabel());
            panel.add(makeReservationButton);
        }
        {
            secretReservationKeysTextArea = new JTextArea();
            JScrollPane secretReservationKeysScrollPane = new JScrollPane(secretReservationKeysTextArea);
            secretReservationKeysScrollPane.setPreferredSize(new Dimension(400, 50));
            panel.add(new JLabel("Secret Reservation Keys"));
            panel.add(secretReservationKeysScrollPane);
        }
        {
            copySecretReservationKeysButton = new JButton("Copy SRKs to Session Management Client");
            panel.add(new JLabel());
            panel.add(copySecretReservationKeysButton);
        }

        add(panel);
    }

    public JButton getCopySecretReservationKeysButton() {
        return copySecretReservationKeysButton;
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

    public JFormattedTextField getToDateTextField() {
        return toDateTextField;
    }

    public JButton getMakeReservationButton() {
        return makeReservationButton;
    }

    public JTextField getNodeUrnsTextField() {
        return nodeUrnsTextField;
    }

    public JTextArea getSecretReservationKeysTextArea() {
        return secretReservationKeysTextArea;
    }
}
