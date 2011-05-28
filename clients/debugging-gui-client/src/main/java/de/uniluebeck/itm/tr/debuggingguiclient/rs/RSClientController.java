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

package de.uniluebeck.itm.tr.debuggingguiclient.rs;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uniluebeck.itm.tr.debuggingguiclient.Dialogs;
import de.uniluebeck.itm.tr.debuggingguiclient.WSNClientProperties;
import de.uniluebeck.itm.tr.debuggingguiclient.sessionmanagement.SessionManagementClientView;
import eu.wisebed.testbed.api.rs.RSServiceHelper;
import eu.wisebed.api.rs.ConfidentialReservationData;
import eu.wisebed.api.rs.Data;
import eu.wisebed.api.rs.GetReservations;
import eu.wisebed.api.rs.PublicReservationData;
import eu.wisebed.api.rs.RS;
import eu.wisebed.api.rs.SecretAuthenticationKey;
import eu.wisebed.api.rs.SecretReservationKey;


public class RSClientController {

	private static final Logger log = LoggerFactory.getLogger(RSClientController.class);

	private RSClientView view;

	private SessionManagementClientView sessionManagementClientView;

	private ActionListener makeReservationButtonActionListener = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {

			try {

				String endpointUrl = view.getEndpointUrlTextField().getText();
				RS rsService = RSServiceHelper.getRSService(endpointUrl);
				List<SecretAuthenticationKey> authenticationData = parseAuthenticationData();
				ConfidentialReservationData reservation = parseConfidentialReservationData();
				List<SecretReservationKey> secretReservationKeyList =
						rsService.makeReservation(authenticationData, reservation);
				displaySecretReservationKeysResult(secretReservationKeyList);

			} catch (Exception e1) {
				JOptionPane.showMessageDialog(null, e1.getMessage());
			}


		}

	};

	private ActionListener copySecretReservationKeysButtonActionListener = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			sessionManagementClientView.getSecretReservationKeysTextArea()
					.setText(view.getSecretReservationKeysTextArea().getText());
		}
	};

	private ActionListener getReservationsButtonActionListener = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {

			try {

				DatatypeFactory datatypeFactory = DatatypeFactory.newInstance();
				String endpointUrl = view.getEndpointUrlTextField().getText();
				RS rsService = RSServiceHelper.getRSService(endpointUrl);
				List<PublicReservationData> reservations = rsService.getReservations(parseFrom(), parseUntil());

				String[] columns = new String[]{"From", "Until", "Node-URNs", "UserData"};
				String[][] rows = new String[reservations.size()][];

				for (int row = 0; row < reservations.size(); row++) {
					rows[row] = new String[4];
					rows[row][0] = reservations.get(row).getFrom().toString();
					rows[row][1] = reservations.get(row).getTo().toString();
					StringBuilder nodeUrnString = new StringBuilder();
					List<String> rowNodeUrnList = reservations.get(row).getNodeURNs();
					for (int nodeUrnIndex = 0; nodeUrnIndex < rowNodeUrnList.size(); nodeUrnIndex++) {
						nodeUrnString.append(rowNodeUrnList.get(nodeUrnIndex));
						if (nodeUrnIndex < rowNodeUrnList.size() - 1) {
							nodeUrnString.append(",");
						}
					}
					rows[row][2] = nodeUrnString.toString();
					rows[row][3] = reservations.get(row).getUserData();
				}

				JTable table = new JTable(rows, columns);
				JScrollPane scrollPane = new JScrollPane(table);
				table.setFillsViewportHeight(true);
				JOptionPane.showMessageDialog(null, scrollPane);

			} catch (Exception e1) {
				JOptionPane.showMessageDialog(null, e1.getMessage());
			}

		}
	};

	private ActionListener selectNodeUrnsButtonActionListener = new ActionListener() {
		@Override
		public void actionPerformed(final ActionEvent e) {
            Set<String> nodeUrns = Dialogs.showNodeUrnSelectionDialog();
            if (nodeUrns != null) {
                String nodeUrnString = "";
                for (Iterator<String> nodeUrnIterator = nodeUrns.iterator(); nodeUrnIterator.hasNext();) {
                    String nodeUrn = nodeUrnIterator.next();
                    nodeUrnString += nodeUrn;
                    if (nodeUrnIterator.hasNext()) {
                        nodeUrnString += ",";
                    }
                }
                view.getNodeUrnsTextArea().setText(nodeUrnString);
            }
        }
	};

	private XMLGregorianCalendar parseFrom() {

		try {

			DatatypeFactory datatypeFactory = DatatypeFactory.newInstance();
			DateTime from = new DateTime(view.getFromDateTextField().getValue());
			return datatypeFactory.newXMLGregorianCalendar(from.toGregorianCalendar());

		} catch (Exception e) {
			log.error("" + e, e);
			throw new RuntimeException(e);
		}

	}

	private XMLGregorianCalendar parseUntil() {

		try {

			DatatypeFactory datatypeFactory = DatatypeFactory.newInstance();
			DateTime until = new DateTime(view.getUntilDateTextField().getValue());
			return datatypeFactory.newXMLGregorianCalendar(until.toGregorianCalendar());

		} catch (Exception e) {
			log.error("" + e, e);
			throw new RuntimeException(e);
		}

	}

	private GetReservations parsePeriod() {

		GetReservations period = new GetReservations();
		period.setFrom(parseFrom());
		period.setTo(parseUntil());

		return period;
	}

	private ActionListener getConfidentialReservationsButtonActionListener = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {

			try {

				String endpointUrl = view.getEndpointUrlTextField().getText();
				RS rsService = RSServiceHelper.getRSService(endpointUrl);
				List<SecretAuthenticationKey> authenticationData = parseAuthenticationData();

				List<ConfidentialReservationData> reservations =
						rsService.getConfidentialReservations(authenticationData, parsePeriod());

				String[] columns = new String[]{"From", "Until", "Node-URNs", "UserData", "Secret Reservation Keys"};
				String[][] rows = new String[reservations.size()][];

				for (int row = 0; row < reservations.size(); row++) {
					rows[row] = new String[5];
					rows[row][0] = reservations.get(row).getFrom().toString();
					rows[row][1] = reservations.get(row).getTo().toString();
					StringBuilder nodeUrnString = new StringBuilder();
					List<String> rowNodeUrnList = reservations.get(row).getNodeURNs();
					for (int nodeUrnIndex = 0; nodeUrnIndex < rowNodeUrnList.size(); nodeUrnIndex++) {
						nodeUrnString.append(rowNodeUrnList.get(nodeUrnIndex));
						if (nodeUrnIndex < rowNodeUrnList.size() - 1) {
							nodeUrnString.append(",");
						}
					}
					rows[row][2] = nodeUrnString.toString();
					rows[row][3] = reservations.get(row).getUserData();
					StringBuilder srks = new StringBuilder();
					List<Data> rowData = reservations.get(row).getData();
					for (int i = 0; i < rowData.size(); i++) {
						srks.append(rowData.get(i).getUrnPrefix());
						srks.append(",");
						srks.append(rowData.get(i).getUsername());
						srks.append(",");
						srks.append(rowData.get(i).getSecretReservationKey());
						if (i < rowData.size() - 1) {
							srks.append(";");
						}
					}
					rows[row][4] = srks.toString();
				}

				JTable table = new JTable(rows, columns);
				JScrollPane scrollPane = new JScrollPane(table);
				table.setFillsViewportHeight(true);
				JOptionPane.showMessageDialog(null, scrollPane);

			} catch (Exception e1) {
				JOptionPane.showMessageDialog(null, e1.getMessage());
			}

		}
	};

	private void displaySecretReservationKeysResult(List<SecretReservationKey> secretReservationKeyList) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < secretReservationKeyList.size(); i++) {
			builder.append(secretReservationKeyList.get(i).getUrnPrefix());
			builder.append(",");
			builder.append(secretReservationKeyList.get(i).getSecretReservationKey());
			if (i < secretReservationKeyList.size() - 1) {
				builder.append("\n");
			}
		}
		view.getSecretReservationKeysTextArea().setText(builder.toString());
	}

	private ConfidentialReservationData parseConfidentialReservationData() {


		try {

			ConfidentialReservationData data = new ConfidentialReservationData();
			DatatypeFactory datatypeFactory = DatatypeFactory.newInstance();
			DateTime fromDate = new DateTime(view.getFromDateTextField().getValue());
			DateTime toDate = new DateTime(view.getUntilDateTextField().getValue());
			data.setFrom(datatypeFactory.newXMLGregorianCalendar(fromDate.toGregorianCalendar()));
			data.setTo(datatypeFactory.newXMLGregorianCalendar(toDate.toGregorianCalendar()));
			data.setUserData("");
			data.getNodeURNs().addAll(parseNodeUrns());

			return data;

		} catch (Exception e) {
			log.warn("" + e, e);
			JOptionPane.showMessageDialog(null, e.getMessage());
		}

		return null;

	}

	private List<String> parseNodeUrns() {
		return Arrays.asList(view.getNodeUrnsTextArea().getText().split(","));
	}

	private List<SecretAuthenticationKey> parseAuthenticationData() {

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

	public RSClientController(final RSClientView view, SessionManagementClientView sessionManagementClientView,
							  Properties properties) {

		this.view = view;
		this.sessionManagementClientView = sessionManagementClientView;

		try {

			view.getEndpointUrlTextField().setText(
					properties.getProperty(
							WSNClientProperties.RS_CLIENT_ENDPOINTURL,
							"http://" + InetAddress.getLocalHost().getHostName() + ":8889/rs"
					)
			);

		} catch (UnknownHostException e) {
			log.error("" + e, e);
		}

		DateTime now = new DateTime();
		DateTime then = now.plusMinutes(30);

		String presetFromDateStr = properties.getProperty(WSNClientProperties.RS_CLIENT_DATEFROM);
		String presetUntilDateStr = properties.getProperty(WSNClientProperties.RS_CLIENT_DATEUNTIL);

		boolean presetFromDateSet = presetFromDateStr != null && !"".equals(presetFromDateStr);
		boolean presetUntilDateSet = presetUntilDateStr != null && !"".equals(presetUntilDateStr);

		if (presetFromDateSet) {
			this.view.getFromDateTextField().setText(presetFromDateStr);
		} else {
			this.view.getFromDateTextField().setValue(now.toDate());
		}
		this.view.getSecretAuthenticationKeysTextArea().setText(
				WSNClientProperties.readList(properties, WSNClientProperties.RS_CLIENT_SECRETAUTHENTICATIONKEYS, "")
		);

		if (presetUntilDateSet) {
			this.view.getUntilDateTextField().setText(presetUntilDateStr);
		} else {
			this.view.getUntilDateTextField().setValue(then.toDate());
		}
		this.view.getNodeUrnsTextArea().setText(
				properties.getProperty(WSNClientProperties.RS_CLIENT_NODEURNS, "")
		);

		this.view.getMakeReservationButton().addActionListener(makeReservationButtonActionListener);
		this.view.getSelectNodeUrnsButton().addActionListener(selectNodeUrnsButtonActionListener);
		this.view.getGetReservationsButton().addActionListener(getReservationsButtonActionListener);
		this.view.getGetConfidentialReservationsButton()
				.addActionListener(getConfidentialReservationsButtonActionListener);

		this.view.getCopySecretReservationKeysButton().addActionListener(copySecretReservationKeysButtonActionListener);

	}

}
