package de.uniluebeck.itm.tr.iwsn.overlay.connection;

public class ConnectionClosedEvent {

	private final Connection connection;

	public ConnectionClosedEvent(final Connection connection) {
		this.connection = connection;
	}

	public Connection getConnection() {
		return connection;
	}
}
