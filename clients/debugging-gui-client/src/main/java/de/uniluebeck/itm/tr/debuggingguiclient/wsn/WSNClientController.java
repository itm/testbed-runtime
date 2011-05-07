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

package de.uniluebeck.itm.tr.debuggingguiclient.wsn;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Properties;

import javax.swing.JOptionPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uniluebeck.itm.tr.debuggingguiclient.Dialogs;
import de.uniluebeck.itm.tr.debuggingguiclient.WSNClientProperties;
import eu.wisebed.api.common.Message;
import eu.wisebed.api.wsn.FlashPrograms;
import eu.wisebed.api.wsn.Send;
import eu.wisebed.api.wsn.WSN;
import eu.wisebed.testbed.api.wsn.WSNServiceHelper;


public class WSNClientController {

    private static final Logger log = LoggerFactory.getLogger(WSNClientController.class);

    private WSNClientView view;

	private Properties properties;

	private WSN getWSNService() {
        return WSNServiceHelper.getWSNService(view.getEndpointUrlTextField().getText());
    }

    private ActionListener buttonActionListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {

            WSN wsnService = null;

            try {
                wsnService = getWSNService();
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(null, "Service is unavailable. Reason: " + e1.getMessage(), "Service Unavailable", JOptionPane.WARNING_MESSAGE);
            }

            try {
                if (wsnService != null) {

                    if (e.getSource() == view.getAreNodesAliveButton()) {

                        Dialogs.StringListPanel stringListPanel = new Dialogs.StringListPanel();
                        Dialogs.InputDialog<List<String>> dialog = new Dialogs.InputDialog<List<String>>(
                                "AreNodesAlive - Parameters",
                                stringListPanel
                        );
                        dialog.setVisible(true);
                        List<String> nodeUrns = dialog.getResult();
                        if (nodeUrns != null) {
                            String requestId = wsnService.areNodesAlive(nodeUrns);
                            log.info("AreNodesAlive called -> got requestId {}", requestId);
                        }

                    } else if (e.getSource() == view.getDefineNetworkButton()) {

                        // TODO
                        JOptionPane.showMessageDialog(null, "TODO");

                    } else if (e.getSource() == view.getDescribeCapabilitiesButton()) {

                        // TODO
                        JOptionPane.showMessageDialog(null, "TODO");

                    } else if (e.getSource() == view.getDestroyVirtualLinkButton()) {

                        // TODO
                        JOptionPane.showMessageDialog(null, "TODO");

                    } else if (e.getSource() == view.getDisableNodeButton()) {

                        // TODO
                        JOptionPane.showMessageDialog(null, "TODO");

                    } else if (e.getSource() == view.getDisablePhysicalLinkButton()) {

                        // TODO
                        JOptionPane.showMessageDialog(null, "TODO");

                    } else if (e.getSource() == view.getEnableNodeButton()) {

                        // TODO
                        JOptionPane.showMessageDialog(null, "TODO");

                    } else if (e.getSource() == view.getEnablePhysicalLinkButton()) {

                        // TODO
                        JOptionPane.showMessageDialog(null, "TODO");

                    } else if (e.getSource() == view.getFlashProgramsButton()) {

                        // fundamentals set
                        Dialogs.FlashProgramsPanel flashProgramsPanel = new Dialogs.FlashProgramsPanel();
                        Dialogs.InputDialog<FlashPrograms> dialog = new Dialogs.InputDialog<FlashPrograms>(
                                "FlashPrograms - Parameters",
                                flashProgramsPanel
                        );
                        dialog.setVisible(true);
                        FlashPrograms flashPrograms = dialog.getResult();
                        if (flashPrograms != null) {
                            String requestId = wsnService.flashPrograms(
                                    flashPrograms.getNodeIds(),
                                    flashPrograms.getProgramIndices(),
                                    flashPrograms.getPrograms()
                            );
                            log.info("FlashPrograms called -> got requestId: {}", requestId);
                        }

                    } else if (e.getSource() == view.getGetFiltersButton()) {

                        // TODO
                        JOptionPane.showMessageDialog(null, "TODO");

                    } else if (e.getSource() == view.getGetNeighborhoodButton()) {

                        // TODO
                        JOptionPane.showMessageDialog(null, "TODO");

                    } else if (e.getSource() == view.getGetNetworkButton()) {

                        Dialogs.showTextDialog(wsnService.getNetwork(), true);

                    } else if (e.getSource() == view.getGetPropertyValueOfButton()) {

                        // TODO
                        JOptionPane.showMessageDialog(null, "TODO");

                    } else if (e.getSource() == view.getGetVersionButton()) {

                        // fundamentals set
                        JOptionPane.showMessageDialog(null, wsnService.getVersion());

                    } else if (e.getSource() == view.getResetNodesButton()) {

                        Dialogs.StringListPanel stringListPanel = new Dialogs.StringListPanel();
                        Dialogs.InputDialog<List<String>> dialog = new Dialogs.InputDialog<List<String>>(
                                "ResetNodes - Parameters",
                                stringListPanel
                        );
                        dialog.setVisible(true);
                        List<String> nodeUrns = dialog.getResult();
                        if (nodeUrns != null) {
                            String requestId = wsnService.resetNodes(nodeUrns);
                            log.info("ResetNodes called -> got requestId: {}", requestId);
                        }

                    } else if (e.getSource() == view.getSendButton()) {

                        // fundamentals set
                        Dialogs.SendMessagePanel panel = new Dialogs.SendMessagePanel();
                        Dialogs.InputDialog<Send> dialog = new Dialogs.InputDialog<Send>(
                                "Send - Parameters",
                                panel
                        );
                        dialog.setVisible(true);
                        Send result = dialog.getResult();
                        if (result != null) {
                            List<String> nodeUrns = result.getNodeIds();
                            Message message = result.getMessage();
                            String requestId = wsnService.send(nodeUrns, message);
                            log.info("Send called -> got requestId: {}", requestId);
                        }

                    } else if (e.getSource() == view.getSetStartTimeButton()) {

                        // TODO
                        JOptionPane.showMessageDialog(null, "TODO");

                    } else if (e.getSource() == view.getSetVirtualLinkButton()) {

                        // TODO
                        JOptionPane.showMessageDialog(null, "TODO");

                    }
                }

            } catch (Exception e1) {
                JOptionPane.showMessageDialog(null, e1.getMessage());
            }
        }
    };

    public WSNClientController(WSNClientView view, final Properties properties) {

        this.view = view;
		this.properties = properties;

        this.view.getEndpointUrlTextField().setText(
                properties.getProperty(WSNClientProperties.WSN_CLIENT_ENDPOINTURL, "")
        );

        this.view.getAreNodesAliveButton().addActionListener(buttonActionListener);
        this.view.getDefineNetworkButton().addActionListener(buttonActionListener);
        this.view.getDescribeCapabilitiesButton().addActionListener(buttonActionListener);
        this.view.getDestroyVirtualLinkButton().addActionListener(buttonActionListener);
        this.view.getDisableNodeButton().addActionListener(buttonActionListener);
        this.view.getDisablePhysicalLinkButton().addActionListener(buttonActionListener);
        this.view.getEnableNodeButton().addActionListener(buttonActionListener);
        this.view.getEnablePhysicalLinkButton().addActionListener(buttonActionListener);
        this.view.getFlashProgramsButton().addActionListener(buttonActionListener);
        this.view.getGetFiltersButton().addActionListener(buttonActionListener);
        this.view.getGetNeighborhoodButton().addActionListener(buttonActionListener);
        this.view.getGetNetworkButton().addActionListener(buttonActionListener);
        this.view.getGetPropertyValueOfButton().addActionListener(buttonActionListener);
        this.view.getGetVersionButton().addActionListener(buttonActionListener);
        this.view.getResetNodesButton().addActionListener(buttonActionListener);
        this.view.getSendButton().addActionListener(buttonActionListener);
        this.view.getSetStartTimeButton().addActionListener(buttonActionListener);
        this.view.getSetVirtualLinkButton().addActionListener(buttonActionListener);

    }

}
