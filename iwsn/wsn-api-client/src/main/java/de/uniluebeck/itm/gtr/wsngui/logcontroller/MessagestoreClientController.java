package de.uniluebeck.itm.gtr.wsngui.logcontroller;

import com.google.common.collect.ImmutableList;
import de.uniluebeck.itm.gtr.wsngui.WSNClientProperties;
import de.uniluebeck.itm.tr.logcontroller.MessageStoreHelper;
import de.uniluebeck.itm.tr.logcontroller.client.Message;
import de.uniluebeck.itm.tr.logcontroller.client.MessageStore;
import de.uniluebeck.itm.tr.logcontroller.client.MessageType;
import de.uniluebeck.itm.tr.logcontroller.client.SecretReservationKey;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Properties;

/**
 * Controller for MessageStoreClienView
 */
public class MessagestoreClientController extends DefaultTableModel {
    private MessagestoreClientView _view;

    private ActionListener _hasMessagesActionListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                SecretReservationKey key = new SecretReservationKey();
                key.setSecretReservationKey(_view.get_reservationKeyTextfield().getText());
                JOptionPane.showMessageDialog(null,
                        getStore().hasMessages(key));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, ex.getMessage());
            }
        }
    };

    private ActionListener _fetchMessagesActionListener = new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                SecretReservationKey key = new SecretReservationKey();
                key.setSecretReservationKey(_view.get_reservationKeyTextfield().getText());
                List<SecretReservationKey> list = ImmutableList.of(key);
                int limit = 0;
                try {
                    limit = Integer.parseInt(_view.get_limitTextField().getText());
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(null, ex.getMessage());
                }
                MessageType type;
                if (_view.get_MessageTypeCombo().getSelectedItem() == "All")
                    type = null;
                else if (_view.get_MessageTypeCombo().getSelectedItem() == "Text")
                    type = MessageType.TEXT;
                else
                    type = MessageType.BINARY;
                List<Message> mes = getStore().fetchMessages(list, type, limit);
                newMessagesFetched(mes);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, ex.getMessage());
            }
        }
    };

    private void newMessagesFetched(List<Message> mes) {
        super.dataVector.clear();
        Object msg, msgLevel;
        Object binData, binType;
        for (Message m : mes) {
            msg = m.getTextMessage() == null ? null :
                    m.getTextMessage().getMsg();
            msgLevel = m.getTextMessage() == null ? null :
                    m.getTextMessage().getMessageLevel();
            binData = m.getBinaryMessage() == null ? null :
                    m.getBinaryMessage().getBinaryData();
            binType = m.getBinaryMessage() == null ? null :
                    m.getBinaryMessage().getBinaryType();
            super.addRow(new Object[]{m.getSourceNodeId(), m.getTimestamp(),
                    msg, msgLevel,
                    binData, binType});
        }
        super.fireTableDataChanged();
    }

    private MessageStore getStore() {
        String endpoint = _view.get_endpointUrlTextfield().getText();
        if (endpoint != null)
            return MessageStoreHelper.getMessageStoreService(endpoint);
        return null;
    }

    public MessagestoreClientController(MessagestoreClientView messageStoreView, Properties properties) {
        initMessageData();
        _view = messageStoreView;
        try {
            _view.get_endpointUrlTextfield().setText(
                    properties.getProperty(WSNClientProperties.LOGCONTROLLER_STORE_ENDPOINTURL,
                            "http://" + InetAddress.getLocalHost().getHostName()
                                    + ":8887/messagestore"));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        _view.get_checkMessagesButton().addActionListener(_hasMessagesActionListener);
        _view.get_limitTextField().setText("-1");
        _view.get_fetchButton().addActionListener(_fetchMessagesActionListener);
        _view.get_messageTable().setModel(this);
        _view.get_messageTable().setAutoCreateColumnsFromModel(true);
        super.fireTableChanged(new TableModelEvent(this));
    }

    private void initMessageData() {
        for (Object col : new Object[]{"Node", "Timestamp", "Text",
                "Messagelevel", "Binarydata", "Binarytype"})
            super.addColumn(col);
    }
}
