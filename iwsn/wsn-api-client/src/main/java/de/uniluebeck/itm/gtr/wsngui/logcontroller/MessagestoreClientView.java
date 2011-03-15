package de.uniluebeck.itm.gtr.wsngui.logcontroller;

import javax.swing.*;
import java.awt.*;

public class MessageStoreClientView extends JPanel {

	private JTextField endpointUrlTextField;

	private JButton checkMessagesButton;

	private JTextField reservationKeyTextField;

	private JTextField limitTextField;

	private JTable messageTable;

	private JButton fetchButton;

	public MessageStoreClientView() {

		super(new GridLayout(1, 1));

		final JPanel panelOuter = new JPanel(new GridLayout(2, 1));
		final JPanel panelInner = new JPanel(new GridLayout(6, 2));

		panelOuter.add(panelInner);

		messageTable = new JTable();
		panelOuter.add(new JScrollPane(messageTable));
		panelInner.add(new JLabel("Messagestore-Endpoint-Url:"));

		endpointUrlTextField = new JTextField();
		panelInner.add(endpointUrlTextField);
		panelInner.add(new JLabel("ReservationKey:"));

		reservationKeyTextField = new JTextField();
		panelInner.add(reservationKeyTextField);
		panelInner.add(new JLabel());

		checkMessagesButton = new JButton("Messages stored?");
		panelInner.add(checkMessagesButton);
		panelInner.add(new JLabel("Max. Messages:"));

		limitTextField = new JTextField();
		panelInner.add(limitTextField);
		panelInner.add(new JLabel());

		fetchButton = new JButton("Fetch Messages");
		panelInner.add(fetchButton);
		add(panelOuter);
	}

	public JTextField getEndpointUrlTextField() {
		return endpointUrlTextField;
	}

	public JButton getCheckMessagesButton() {
		return checkMessagesButton;
	}

	public JTextField getReservationKeyTextField() {
		return reservationKeyTextField;
	}

	public JTextField getLimitTextField() {
		return limitTextField;
	}

	public JTable getMessageTable() {
		return messageTable;
	}

	public JButton getFetchButton() {
		return fetchButton;
	}
}
