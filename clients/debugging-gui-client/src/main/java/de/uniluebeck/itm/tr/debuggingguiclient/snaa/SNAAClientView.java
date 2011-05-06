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
package de.uniluebeck.itm.tr.debuggingguiclient.snaa;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;


public class SNAAClientView extends JPanel {

    private JTextField endpointUrlTextField;

    private JButton isAuthenticatedButton;

    private JButton authenticateButton;

    private JTextArea authenticationTriplesTextArea;

    private JTextField actionTextField;

    private JTextArea secretAuthenticationKeysTextArea;

    private JButton copyToRSButton;

    public SNAAClientView() {
        super(new FlowLayout());
        ((FlowLayout) super.getLayout()).setAlignment(FlowLayout.LEFT);

        JPanel panel = new JPanel(new GridLayout(7, 2));

        {
            JLabel endpointUrlLabel = new JLabel("SNAA Endpoint URL");
            endpointUrlTextField = new JTextField();

            panel.add(endpointUrlLabel);
            panel.add(endpointUrlTextField);
        }
        {
            JLabel authenticationTriplesLabel = new JLabel("Credentials (urnprefix,username,password) one triple per line");
            authenticationTriplesTextArea = new JTextArea();
            JScrollPane authenticationTriplesScrollPane = new JScrollPane(authenticationTriplesTextArea);
            authenticationTriplesScrollPane.setPreferredSize(new Dimension(400, 50));

            panel.add(authenticationTriplesLabel);
            panel.add(authenticationTriplesScrollPane);
        }
        {
            authenticateButton = new JButton("authenticate()");

            panel.add(new JLabel());
            panel.add(authenticateButton);
        }
        {
            JLabel resultsLabel = new JLabel("Authentication Result (urnprefix,username,secretauthenticationkey) one triple per line");
            secretAuthenticationKeysTextArea = new JTextArea();
            JScrollPane secretAuthenticationKeysScrollPane = new JScrollPane(secretAuthenticationKeysTextArea);
            secretAuthenticationKeysScrollPane.setPreferredSize(new Dimension(400, 50));

            panel.add(resultsLabel);
            panel.add(secretAuthenticationKeysScrollPane);
        }
        {
            JLabel actionLabel = new JLabel("Action");
            actionTextField = new JTextField();

            panel.add(actionLabel);
            panel.add(actionTextField);
        }
        {
            isAuthenticatedButton = new JButton("isAuthenticated()");

            panel.add(new JLabel());
            panel.add(isAuthenticatedButton);
        }
        {
            copyToRSButton = new JButton("Copy authentication Result to RS client");

            panel.add(new JLabel());
            panel.add(copyToRSButton);
        }

        add(panel);

    }

    public JTextField getEndpointUrlTextField() {
        return endpointUrlTextField;
    }

    public JTextArea getAuthenticationTriplesTextArea() {
        return authenticationTriplesTextArea;
    }

    public JTextField getActionTextField() {
        return actionTextField;
    }

    public JButton getAuthenticatedButton() {
        return isAuthenticatedButton;
    }

    public JButton getAuthenticateButton() {
        return authenticateButton;
    }

    public JTextArea getSecretAuthenticationKeysTextArea() {
        return secretAuthenticationKeysTextArea;
    }

    public JButton getCopyToRSButton() {
        return copyToRSButton;
    }
    
}
