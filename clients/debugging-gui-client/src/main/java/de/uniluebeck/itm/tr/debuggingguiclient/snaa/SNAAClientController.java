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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.swing.JOptionPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uniluebeck.itm.tr.debuggingguiclient.WSNClientProperties;
import de.uniluebeck.itm.tr.debuggingguiclient.rs.RSClientView;
import eu.wisebed.testbed.api.snaa.helpers.SNAAServiceHelper;
import eu.wisebed.api.snaa.Action;
import eu.wisebed.api.snaa.AuthenticationTriple;
import eu.wisebed.api.snaa.SNAA;
import eu.wisebed.api.snaa.SecretAuthenticationKey;


public class SNAAClientController {

    private static final Logger log = LoggerFactory.getLogger(SNAAClientController.class);

    private SNAAClientView view;
    private RSClientView rsClientView;

    private ActionListener authenticatedActionListener = new ActionListener() {
        @Override
        public void actionPerformed(final ActionEvent e) {

            try {

                String endpointUrl = view.getEndpointUrlTextField().getText();
                List<SecretAuthenticationKey> secretAuthenticationKeys = parseSecretAuthenticationKeys();
                Action action = new Action();
                action.setAction(view.getActionTextField().getText());

                SNAA snaaService = SNAAServiceHelper.getSNAAService(endpointUrl);
                boolean result = snaaService.isAuthorized(secretAuthenticationKeys, action);
                JOptionPane.showMessageDialog(null, result);

            } catch (Exception e1) {
                JOptionPane.showMessageDialog(null, e1.getMessage());
            }

        }

    };

    private List<SecretAuthenticationKey> parseSecretAuthenticationKeys() {

        List<SecretAuthenticationKey> keys = new ArrayList<SecretAuthenticationKey>();
        String text = view.getSecretAuthenticationKeysTextArea().getText();

        for (String line : text.split("\n")) {

            String[] values = line.split(",");

            if (values.length == 3) {
                SecretAuthenticationKey key = new SecretAuthenticationKey();
                key.setUrnPrefix(values[0].trim());
                key.setUsername(values[1].trim());
                key.setSecretAuthenticationKey(values[2].trim());
                keys.add(key);
            }
        }

        return keys;
    }

    private ActionListener authenticateActionListener = new ActionListener() {
        @Override
        public void actionPerformed(final ActionEvent e) {

            try {

                String endpointUrl = view.getEndpointUrlTextField().getText();
                List<AuthenticationTriple> authenticationData = parseAuthenticationTriples();

                SNAA snaaService = SNAAServiceHelper.getSNAAService(endpointUrl);
                List<SecretAuthenticationKey> keys = snaaService.authenticate(authenticationData);

                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < keys.size(); i++) {
                    builder.append(keys.get(i).getUrnPrefix());
                    builder.append(",");
                    builder.append(keys.get(i).getUsername());
                    builder.append(",");
                    builder.append(keys.get(i).getSecretAuthenticationKey());
                    if (i < keys.size() - 1) {
                        builder.append("\n");
                    }
                }

                view.getSecretAuthenticationKeysTextArea().setText(builder.toString());

            } catch (Exception e1) {
                JOptionPane.showMessageDialog(null, e1.getMessage());
            }

        }
    };

    private List<AuthenticationTriple> parseAuthenticationTriples() {

        String text = view.getAuthenticationTriplesTextArea().getText();
        String[] lines = text.split("\n");
        ArrayList<AuthenticationTriple> triples = new ArrayList<AuthenticationTriple>(lines.length);
        for (String line : lines) {

            String[] values = line.split(",");

            if (values.length == 3) {
                AuthenticationTriple triple = new AuthenticationTriple();
                triple.setUrnPrefix(values[0].trim());
                triple.setUsername(values[1].trim());
                triple.setPassword(values[2].trim());
                triples.add(triple);
            }
        }

        return triples;

    }

    private ActionListener copyToRSButtonActionListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            rsClientView.getSecretAuthenticationKeysTextArea().setText(view.getSecretAuthenticationKeysTextArea().getText());
        }
    };

    public SNAAClientController(final SNAAClientView view, final RSClientView rsClientView, final Properties properties) {

        this.view = view;
        this.rsClientView = rsClientView;

        this.view.getAuthenticateButton().addActionListener(authenticateActionListener);
        this.view.getAuthenticatedButton().addActionListener(authenticatedActionListener);
        this.view.getCopyToRSButton().addActionListener(copyToRSButtonActionListener);

        this.view.getAuthenticationTriplesTextArea().setText(
                WSNClientProperties.readList(
                        properties,
                        WSNClientProperties.SNAA_CLIENT_CREDENTIALS,
                        ""
                )
        );
        this.view.getSecretAuthenticationKeysTextArea().setText(
                WSNClientProperties.readList(
                        properties,
                        WSNClientProperties.SNAA_CLIENT_SECRETAUTHENTICATIONKEYS,
                        ""
                )
        );
        this.view.getActionTextField().setText(properties.getProperty(WSNClientProperties.SNAA_CLIENT_ACTION, ""));

        try {

            this.view.getEndpointUrlTextField().setText(properties.getProperty(
                    WSNClientProperties.SNAA_CLIENT_ENDPOINTURL,
                    "http://" + InetAddress.getLocalHost().getHostName() + ":8890/snaa"
            ));

        } catch (UnknownHostException e) {
            log.error("" + e, e);
        }

    }

}
