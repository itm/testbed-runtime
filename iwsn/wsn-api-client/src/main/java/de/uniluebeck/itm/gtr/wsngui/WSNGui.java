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

import de.uniluebeck.itm.gtr.wsngui.controller.ControllerController;
import de.uniluebeck.itm.gtr.wsngui.controller.ControllerModel;
import de.uniluebeck.itm.gtr.wsngui.controller.ControllerServiceDummyView;
import de.uniluebeck.itm.gtr.wsngui.controller.ControllerView;
import de.uniluebeck.itm.gtr.wsngui.rs.RSClientController;
import de.uniluebeck.itm.gtr.wsngui.rs.RSClientModel;
import de.uniluebeck.itm.gtr.wsngui.rs.RSClientView;
import de.uniluebeck.itm.gtr.wsngui.sessionmanagement.SessionManagementController;
import de.uniluebeck.itm.gtr.wsngui.sessionmanagement.SessionManagementModel;
import de.uniluebeck.itm.gtr.wsngui.sessionmanagement.SessionManagementView;
import de.uniluebeck.itm.gtr.wsngui.snaa.SNAAClientController;
import de.uniluebeck.itm.gtr.wsngui.snaa.SNAAClientModel;
import de.uniluebeck.itm.gtr.wsngui.snaa.SNAAClientView;
import de.uniluebeck.itm.gtr.wsngui.wsn.WSNClientController;
import de.uniluebeck.itm.gtr.wsngui.wsn.WSNClientModel;
import de.uniluebeck.itm.gtr.wsngui.wsn.WSNClientView;
import de.uniluebeck.itm.gtr.wsngui.wsn.WSNServiceDummyView;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class WSNGui {

    private JFrame frame;

    private JTextArea outputTextPane;

    public WSNGui() {

        JPanel panel = new JPanel(new BorderLayout());

        JTabbedPane tabs = new JTabbedPane();

        panel.add(tabs, BorderLayout.NORTH);

        {
            ControllerModel controllerModel = new ControllerModel();
            ControllerView controllerView = new ControllerView(controllerModel);
            new ControllerController(controllerView, controllerModel);

            ControllerServiceDummyView controllerServiceDummyView = new ControllerServiceDummyView();

            WSNClientModel wsnClientModel = new WSNClientModel();
            WSNClientView wsnClientView = new WSNClientView(wsnClientModel);
            new WSNClientController(wsnClientView, wsnClientModel);

            SessionManagementModel sessionManagementModel = new SessionManagementModel();
            SessionManagementView sessionManagementView = new SessionManagementView(sessionManagementModel);
            new SessionManagementController(sessionManagementView, sessionManagementModel, wsnClientView);

            RSClientModel rsClientModel = new RSClientModel();
            RSClientView rsClientView = new RSClientView(rsClientModel);
            new RSClientController(rsClientView, rsClientModel, sessionManagementView);

            SNAAClientModel snaaClientModel = new SNAAClientModel();
            SNAAClientView snaaClientView = new SNAAClientView(snaaClientModel);
            new SNAAClientController(snaaClientView, snaaClientModel, rsClientView);

            WSNServiceDummyView wsnServiceDummyView = new WSNServiceDummyView();

            tabs.addTab("SNAA Client", snaaClientView);
            tabs.addTab("RS Client", rsClientView);
            tabs.addTab("Controller Client", controllerView);
            tabs.addTab("Controller Service Dummy", controllerServiceDummyView);
            tabs.addTab("Session Management Client", sessionManagementView);
            tabs.addTab("WSN Client", wsnClientView);
            tabs.addTab("WSN Server Dummy", wsnServiceDummyView);

        }

        outputTextPane = new JTextArea();
        outputTextPane.setEditable(false);
        outputTextPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    outputTextPane.setText("");
                }
            }
        });

        JScrollPane outputScrollPane = new JScrollPane(outputTextPane);
        outputScrollPane.setPreferredSize(new Dimension(800, 400));
        outputScrollPane.setAutoscrolls(true);

        panel.add(outputScrollPane, BorderLayout.CENTER);

        TextAreaAppender.setTextArea(outputTextPane);

        frame = new JFrame("WISEBED Web Service API Testing Tool");
        frame.setContentPane(panel);
        frame.pack();

    }

    public static void main(String[] args) {
        new WSNGui().frame.setVisible(true);
    }

}
