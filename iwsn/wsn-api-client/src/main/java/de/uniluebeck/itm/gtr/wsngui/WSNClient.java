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

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;

/**
 * Created by:
 * User: bimschas
 * Date: 04.03.2010
 * Time: 15:29:14
 */
public class WSNClient {

	private static final Logger log = LoggerFactory.getLogger(WSNClient.class);

	private JPanel panel;

	private JTabbedPane tabs;

	private JFrame frame;

	private JTextArea outputTextPane;

	private JScrollPane outputScrollPane;

	public WSNClient(Map<String, Object> properties) {

		panel = new JPanel(new BorderLayout());

		tabs = new JTabbedPane();

		panel.add(tabs, BorderLayout.NORTH);

		{
			ControllerModel controllerModel = new ControllerModel();
			ControllerView controllerView = new ControllerView(controllerModel);
			ControllerController controllerController = new ControllerController(controllerView, controllerModel, properties);
			tabs.addTab("Controller Client", controllerView);
		}

		{
			ControllerServiceDummyView controllerServiceDummyView = new ControllerServiceDummyView(properties);
			tabs.addTab("Controller Service Dummy", controllerServiceDummyView);
		}

		{
			WSNClientModel wsnClientModel = new WSNClientModel();
			WSNClientView wsnClientView = new WSNClientView(wsnClientModel);
			WSNClientController wsnClientController = new WSNClientController(wsnClientView, wsnClientModel);

			SessionManagementModel sessionManagementModel = new SessionManagementModel();
			SessionManagementView sessionManagementView = new SessionManagementView(sessionManagementModel);
			SessionManagementController sessionManagementController = new SessionManagementController(sessionManagementView, sessionManagementModel, wsnClientView, properties);

			tabs.addTab("Session Management Client", sessionManagementView);
			tabs.addTab("WSN Client", wsnClientView);
		}

		{
			WSNServiceDummyView wsnServiceDummyView = new WSNServiceDummyView(properties);
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

		outputScrollPane = new JScrollPane(outputTextPane);
		outputScrollPane.setPreferredSize(new Dimension(800, 400));
		outputScrollPane.setAutoscrolls(true);

		panel.add(outputScrollPane, BorderLayout.CENTER);

		TextAreaAppender.setTextArea(outputTextPane);

		frame = new JFrame("WISEBED Web Service API Testing Tool");
		frame.setContentPane(panel);
		frame.pack();

	}

	public static void main(String[] args) {
        String propertyFile = null;
        // create the command line parser
        CommandLineParser parser = new PosixParser();
        Options options = new Options();
        options.addOption("f", "file", true, "The properties file");

        CommandLine line = null;
        Map<String, Object> properties = null;
        try {
            line = parser.parse(options, args);

            if (line.hasOption('f')) {
                propertyFile = line.getOptionValue('f');
                properties = WSNClientProperties.getPropertyMap(propertyFile);
            }

        } catch (Exception e) {
            log.error(e.getMessage() + "\nProperties-file ignored!");
        }
		new WSNClient(properties).frame.setVisible(true);
	}

}
