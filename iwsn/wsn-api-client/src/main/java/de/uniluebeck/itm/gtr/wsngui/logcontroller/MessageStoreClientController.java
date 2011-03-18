package de.uniluebeck.itm.gtr.wsngui.logcontroller;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import de.uniluebeck.itm.gtr.wsngui.WSNClientProperties;
import eu.wisebed.testbed.api.messagestore.MessageStoreServiceHelper;
import eu.wisebed.testbed.api.messagestore.v1.MessageStore;
import eu.wisebed.testbed.api.wsn.v22.Message;
import eu.wisebed.testbed.api.wsn.v22.SecretReservationKey;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class MessageStoreClientController extends DefaultTableModel {

	private MessageStoreClientView view;

	private ActionListener hasMessagesActionListener = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				SecretReservationKey key = new SecretReservationKey();
				key.setSecretReservationKey(view.getReservationKeyTextField().getText());
				JOptionPane.showMessageDialog(null, getStore().hasMessages(convert(key)));
			} catch (Exception ex) {
				JOptionPane.showMessageDialog(null, ex.getMessage());
			}
		}
	};

	private ActionListener fetchMessagesActionListener = new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				SecretReservationKey key = new SecretReservationKey();
				key.setSecretReservationKey(view.getReservationKeyTextField().getText());
				List<SecretReservationKey> list = ImmutableList.of(key);
				int limit = 0;
				try {
					limit = Integer.parseInt(view.getLimitTextField().getText());
				} catch (NumberFormatException ex) {
					JOptionPane.showMessageDialog(null, ex.getMessage());
				}
				List<Message> mes = convertMessages(getStore().fetchMessages(convert(list), limit));
				newMessagesFetched(mes);
			} catch (Exception ex) {
				ex.printStackTrace();
				JOptionPane.showMessageDialog(null, ex.getMessage());
			}
		}

	};

	private List<Message> convertMessages(final List<eu.wisebed.testbed.api.messagestore.v1.Message> messages) {
		ArrayList<Message> retList = Lists.newArrayListWithCapacity(messages.size());
		for (eu.wisebed.testbed.api.messagestore.v1.Message message : messages) {
			retList.add(convertMessage(message));
		}
		return retList;
	}

	private Message convertMessage(final eu.wisebed.testbed.api.messagestore.v1.Message message) {
		Message ret = new Message();
		ret.setBinaryData(message.getBinaryData());
		ret.setSourceNodeId(message.getSourceNodeId());
		ret.setTimestamp(message.getTimestamp());
		return ret;
	}

	private List<eu.wisebed.testbed.api.messagestore.v1.SecretReservationKey> convert(
			final List<SecretReservationKey> secretReservationKeys) {
		ArrayList<eu.wisebed.testbed.api.messagestore.v1.SecretReservationKey> retList =
				Lists.newArrayListWithCapacity(secretReservationKeys.size());
		for (SecretReservationKey secretReservationKey : secretReservationKeys) {
			retList.add(convert(secretReservationKey));
		}
		return retList;
	}

	private eu.wisebed.testbed.api.messagestore.v1.SecretReservationKey convert(
			final SecretReservationKey secretReservationKey) {
		eu.wisebed.testbed.api.messagestore.v1.SecretReservationKey ret =
				new eu.wisebed.testbed.api.messagestore.v1.SecretReservationKey();
		ret.setSecretReservationKey(secretReservationKey.getSecretReservationKey());
		ret.setUrnPrefix(secretReservationKey.getUrnPrefix());
		return ret;
	}

	private void newMessagesFetched(List<Message> mes) {
		super.dataVector.clear();
		Object binData;
		for (Message m : mes) {
			binData = m.getBinaryData();
			super.addRow(new Object[]{m.getSourceNodeId(), m.getTimestamp(), binData});
		}
		super.fireTableDataChanged();
	}

	private MessageStore getStore() {
		String endpoint = view.getEndpointUrlTextField().getText();
		if (endpoint != null) {
			return MessageStoreServiceHelper.getMessageStoreService(endpoint);
		}
		return null;
	}

	public MessageStoreClientController(MessageStoreClientView messageStoreView, Properties properties) {
		initMessageData();
		view = messageStoreView;
		try {
			view.getEndpointUrlTextField().setText(
					properties.getProperty(WSNClientProperties.LOGCONTROLLER_STORE_ENDPOINTURL,
							"http://" + InetAddress.getLocalHost().getHostName()
									+ ":8887/messagestore"
					)
			);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		view.getCheckMessagesButton().addActionListener(hasMessagesActionListener);
		view.getLimitTextField().setText("-1");
		view.getFetchButton().addActionListener(fetchMessagesActionListener);
		view.getMessageTable().setModel(this);
		view.getMessageTable().setAutoCreateColumnsFromModel(true);
		super.fireTableChanged(new TableModelEvent(this));
	}

	private void initMessageData() {
		Object[] columns = {"Node", "Timestamp", "Binarydata"};
		for (Object col : columns) {
			super.addColumn(col);
		}
	}
}
