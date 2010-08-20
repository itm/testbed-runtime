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

package de.uniluebeck.itm.gtr.wsngui.wsn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


public class WSNServiceDummyView extends JPanel {

	private static final Logger log = LoggerFactory.getLogger(WSNServiceDummyView.class);

	private JPanel panel;

	private JTextField endpointUrlTextField;

	private JCheckBox startServiceCheckbox;

	private WSNServiceDummyImpl wsnService;

	public WSNServiceDummyView() {

		super(new FlowLayout());
		((FlowLayout) super.getLayout()).setAlignment(FlowLayout.LEFT);

		this.panel = new JPanel(new GridLayout(2, 2));

		{
			panel.add(new JLabel("WSN API Endpoint URL"));
			endpointUrlTextField = new JTextField("http://localhost:8082/wsn/dummy");
			panel.add(endpointUrlTextField);
		}
		{
			startServiceCheckbox = new JCheckBox("Start service");
			startServiceCheckbox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					if (startServiceCheckbox.isSelected()) {

						String endpointUrl = endpointUrlTextField.getText();
						wsnService = new WSNServiceDummyImpl(endpointUrl);

						try {
							wsnService.start();
						} catch (Exception e1) {
							JOptionPane.showMessageDialog(
									null,
									"Failed to start controller service on URL " + endpointUrl,
									"Failed to start controller service!",
									JOptionPane.WARNING_MESSAGE
							);
							log.error("Exception while starting controller service", e1);
						}

					} else {
						wsnService.stop();
					}
				}
			});
			panel.add(startServiceCheckbox);
		}

		add(panel);

	}

}
