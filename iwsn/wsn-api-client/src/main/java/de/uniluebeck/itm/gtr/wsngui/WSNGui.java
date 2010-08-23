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

import com.google.common.base.Preconditions;
import de.uniluebeck.itm.gtr.wsngui.controller.ControllerClientController;
import de.uniluebeck.itm.gtr.wsngui.controller.ControllerServiceController;
import de.uniluebeck.itm.gtr.wsngui.controller.ControllerServiceView;
import de.uniluebeck.itm.gtr.wsngui.controller.ControllerClientView;
import de.uniluebeck.itm.gtr.wsngui.rs.RSClientController;
import de.uniluebeck.itm.gtr.wsngui.rs.RSClientView;
import de.uniluebeck.itm.gtr.wsngui.sessionmanagement.SessionManagementClientController;
import de.uniluebeck.itm.gtr.wsngui.sessionmanagement.SessionManagementClientView;
import de.uniluebeck.itm.gtr.wsngui.snaa.SNAAClientController;
import de.uniluebeck.itm.gtr.wsngui.snaa.SNAAClientView;
import de.uniluebeck.itm.gtr.wsngui.wsn.WSNClientController;
import de.uniluebeck.itm.gtr.wsngui.wsn.WSNClientView;
import de.uniluebeck.itm.gtr.wsngui.wsn.WSNServiceController;
import de.uniluebeck.itm.gtr.wsngui.wsn.WSNServiceView;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.FileReader;
import java.util.Properties;

public class WSNGui {

	private static final Logger log = LoggerFactory.getLogger(WSNGui.class);

    private JFrame frame;

    private JTextArea outputTextPane;

	public WSNGui(final Properties properties) {

        Preconditions.checkNotNull(properties);

        JPanel panel = new JPanel(new BorderLayout());

        JTabbedPane tabs = new JTabbedPane();

        panel.add(tabs, BorderLayout.NORTH);

        {
            ControllerClientView controllerClientView = new ControllerClientView();
            new ControllerClientController(controllerClientView, properties);

            ControllerServiceView controllerServiceView = new ControllerServiceView();
            new ControllerServiceController(controllerServiceView, properties);

            WSNClientView wsnClientView = new WSNClientView();
            new WSNClientController(wsnClientView, properties);

            SessionManagementClientView sessionManagementClientView = new SessionManagementClientView();
            new SessionManagementClientController(sessionManagementClientView, wsnClientView, properties);

            RSClientView rsClientView = new RSClientView();
            new RSClientController(rsClientView, sessionManagementClientView, properties);

            SNAAClientView snaaClientView = new SNAAClientView();
            new SNAAClientController(snaaClientView, rsClientView, properties);

            WSNServiceView wsnServiceView = new WSNServiceView();
            new WSNServiceController(wsnServiceView, properties);

            tabs.addTab("SNAA Client", snaaClientView);
            tabs.addTab("RS Client", rsClientView);
            tabs.addTab("Controller Client", controllerClientView);
            tabs.addTab("Controller Service Dummy", controllerServiceView);
            tabs.addTab("Session Management Client", sessionManagementClientView);
            tabs.addTab("WSN Client", wsnClientView);
            tabs.addTab("WSN Server Dummy", wsnServiceView);

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
        String propertyFile;
        // create the command line parser
        CommandLineParser parser = new PosixParser();
        Options options = new Options();
        options.addOption("f", "file", true, "A property file containing values to override autodetected field values (optional)");

        CommandLine line;
        Properties properties = new Properties();
        try {

            line = parser.parse(options, args);

            if (line.hasOption('f')) {
                propertyFile = line.getOptionValue('f');
				properties.load(new FileReader(propertyFile));
            }

        } catch (Exception e) {
            log.error(e.getMessage() + "\n Start aborted!");
            System.exit(1);
        }

		new WSNGui(properties).frame.setVisible(true);
	}

}
