package de.uniluebeck.itm.gtr.wsngui.logcontroller;

import javax.swing.*;
import java.awt.*;

/**
 * Viewcomponent
 */
public class MessagestoreClientView extends JPanel {
    private JTextField _endpointUrlTextfield;
    private JButton _checkMessagesButton;
    private JTextField _reservationKeyTextfield;
    private JComboBox _MessageTypeCombo;
    private JTextField _limitTextField;
    private JTable _messageTable;
    private JButton _fetchButton;

    public MessagestoreClientView() {
        super(new GridLayout(1,1));

        final JPanel panelOuter = new JPanel(new GridLayout(2, 1));
        final JPanel panelInner = new JPanel(new GridLayout(6, 2));
        panelOuter.add(panelInner);
        _messageTable = new JTable();
        panelOuter.add(new JScrollPane(_messageTable));
        panelInner.add(new JLabel("Messagestore-Endpoint-Url:"));
        _endpointUrlTextfield = new JTextField();
        panelInner.add(_endpointUrlTextfield);
        panelInner.add(new JLabel("ReservationKey:"));
        _reservationKeyTextfield = new JTextField();
        panelInner.add(_reservationKeyTextfield);        
        panelInner.add(new JLabel());
        _checkMessagesButton = new JButton("Messages stored?");
        panelInner.add(_checkMessagesButton);
        panelInner.add(new JLabel("Max. Messages:"));
        _limitTextField = new JTextField();
        panelInner.add(_limitTextField);
        panelInner.add(new JLabel("Messagetype:"));
        _MessageTypeCombo = new JComboBox(new Object[]{"All", "Text", "Binary"});
        panelInner.add(_MessageTypeCombo);
        panelInner.add(new JLabel());
        _fetchButton = new JButton("Fetch Messages");
        panelInner.add(_fetchButton);
        add(panelOuter);
    }

    public JTextField get_endpointUrlTextfield() {
        return _endpointUrlTextfield;
    }

    public JButton get_checkMessagesButton() {
        return _checkMessagesButton;
    }

    public JTextField get_reservationKeyTextfield() {
        return _reservationKeyTextfield;
    }

    public JComboBox get_MessageTypeCombo() {
        return _MessageTypeCombo;
    }

    public JTextField get_limitTextField() {
        return _limitTextField;
    }

    public JTable get_messageTable() {
        return _messageTable;
    }

    public JButton get_fetchButton() {
        return _fetchButton;
    }
}
