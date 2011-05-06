package de.uniluebeck.itm.tr.debuggingguiclient.controller;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

import javax.swing.JOptionPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uniluebeck.itm.tr.debuggingguiclient.WSNClientProperties;


public class ControllerServiceController {

	private static final Logger log = LoggerFactory.getLogger(ControllerServiceController.class);
	
	private ControllerServiceImpl controllerService;

	private ActionListener startServiceCheckboxActionListener = new ActionListener() {
		@Override
		public void actionPerformed(final ActionEvent e) {
			if (view.getStartServiceCheckbox().isSelected()) {

				String endpointUrl = view.getEndpointUrlTextField().getText();
				controllerService = new ControllerServiceImpl(endpointUrl);

				try {
					controllerService.start();
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
				controllerService.stop();
			}
		}
	};

	private ControllerServiceView view;

	public ControllerServiceController(final ControllerServiceView view, final Properties properties) {

		this.view = view;

		try {

			view.getEndpointUrlTextField().setText(
					properties.getProperty(
							WSNClientProperties.CONTROLLER_SERVICE_ENDPOINTURL,
							"http://" + InetAddress.getLocalHost().getHostName() + ":8081/controller"
					)
			);

			view.getStartServiceCheckbox().addActionListener(startServiceCheckboxActionListener);

		} catch (UnknownHostException e) {
			log.error("" + e, e);
		}
	}
}
