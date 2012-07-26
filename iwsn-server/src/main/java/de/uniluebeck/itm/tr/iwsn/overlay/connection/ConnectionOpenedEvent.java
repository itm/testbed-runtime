package de.uniluebeck.itm.tr.iwsn.overlay.connection;

public class ConnectionOpenedEvent {

	private final Connection connection;

	public ConnectionOpenedEvent(final Connection connection) {
		this.connection = connection;
	}

	public Connection getConnection() {
		return connection;
	}
}
