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


import eu.wisebed.testbed.api.wsn.Constants;
import eu.wisebed.testbed.api.wsn.WSNServiceHelper;
import eu.wisebed.testbed.api.wsn.v211.FlashPrograms;
import eu.wisebed.testbed.api.wsn.v211.Message;
import eu.wisebed.testbed.api.wsn.v211.Send;
import eu.wisebed.testbed.api.wsn.v211.WSN;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.xml.namespace.QName;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;


public class WSNClientController {

    private static final QName wsnServiceQName = new QName(Constants.NAMESPACE_WSN_SERVICE, "WSNService");

    private static final Logger log = LoggerFactory.getLogger(WSNClientController.class);

    private WSNClientView clientView;

    private WSNClientModel clientModel;

    private WSN getWSNService() {
		return WSNServiceHelper.getWSNService(clientView.getEndpointUrlTextField().getText());
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
            if (wsnService != null) {

                if (e.getSource() == clientView.getAreNodesAliveButton()) {

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

                } else if (e.getSource() == clientView.getDefineNetworkButton()) {

                    // TODO
                    JOptionPane.showMessageDialog(null, "TODO");

                } else if (e.getSource() == clientView.getDescribeCapabilitiesButton()) {

                    // TODO
                    JOptionPane.showMessageDialog(null, "TODO");

                } else if (e.getSource() == clientView.getDestroyVirtualLinkButton()) {

                    // TODO
                    JOptionPane.showMessageDialog(null, "TODO");

                } else if (e.getSource() == clientView.getDisableNodeButton()) {

                    // TODO
                    JOptionPane.showMessageDialog(null, "TODO");

                } else if (e.getSource() == clientView.getDisablePhysicalLinkButton()) {

                    // TODO
                    JOptionPane.showMessageDialog(null, "TODO");

                } else if (e.getSource() == clientView.getEnableNodeButton()) {

                    // TODO
                    JOptionPane.showMessageDialog(null, "TODO");

                } else if (e.getSource() == clientView.getEnablePhysicalLinkButton()) {

                    // TODO
                    JOptionPane.showMessageDialog(null, "TODO");

                } else if (e.getSource() == clientView.getFlashProgramsButton()) {

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

                } else if (e.getSource() == clientView.getGetFiltersButton()) {

                    // TODO
                    JOptionPane.showMessageDialog(null, "TODO");

                } else if (e.getSource() == clientView.getGetNeighborhoodButton()) {

                    // TODO
                    JOptionPane.showMessageDialog(null, "TODO");

                } else if (e.getSource() == clientView.getGetNetworkButton()) {

                    JOptionPane.showMessageDialog(null, new JTextArea(wsnService.getNetwork()));

                } else if (e.getSource() == clientView.getGetPropertyValueOfButton()) {

                    // TODO
                    JOptionPane.showMessageDialog(null, "TODO");

                } else if (e.getSource() == clientView.getGetVersionButton()) {

                    // fundamentals set
                    JOptionPane.showMessageDialog(null, wsnService.getVersion());

                } else if (e.getSource() == clientView.getResetNodesButton()) {

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

                } else if (e.getSource() == clientView.getSendButton()) {

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

                } else if (e.getSource() == clientView.getSetStartTimeButton()) {

                    // TODO
                    JOptionPane.showMessageDialog(null, "TODO");

                } else if (e.getSource() == clientView.getSetVirtualLinkButton()) {

                    // TODO
                    JOptionPane.showMessageDialog(null, "TODO");

                }
            }
        }
    };

    public WSNClientController(WSNClientView clientView, WSNClientModel clientModel) {

        this.clientView = clientView;
        this.clientModel = clientModel;

        this.clientView.getAreNodesAliveButton().addActionListener(buttonActionListener);
        this.clientView.getDefineNetworkButton().addActionListener(buttonActionListener);
        this.clientView.getDescribeCapabilitiesButton().addActionListener(buttonActionListener);
        this.clientView.getDestroyVirtualLinkButton().addActionListener(buttonActionListener);
        this.clientView.getDisableNodeButton().addActionListener(buttonActionListener);
        this.clientView.getDisablePhysicalLinkButton().addActionListener(buttonActionListener);
        this.clientView.getEnableNodeButton().addActionListener(buttonActionListener);
        this.clientView.getEnablePhysicalLinkButton().addActionListener(buttonActionListener);
        this.clientView.getFlashProgramsButton().addActionListener(buttonActionListener);
        this.clientView.getGetFiltersButton().addActionListener(buttonActionListener);
        this.clientView.getGetNeighborhoodButton().addActionListener(buttonActionListener);
        this.clientView.getGetNetworkButton().addActionListener(buttonActionListener);
        this.clientView.getGetPropertyValueOfButton().addActionListener(buttonActionListener);
        this.clientView.getGetVersionButton().addActionListener(buttonActionListener);
        this.clientView.getResetNodesButton().addActionListener(buttonActionListener);
        this.clientView.getSendButton().addActionListener(buttonActionListener);
        this.clientView.getSetStartTimeButton().addActionListener(buttonActionListener);
        this.clientView.getSetVirtualLinkButton().addActionListener(buttonActionListener);

    }

}
