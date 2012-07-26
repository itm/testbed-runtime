package de.uniluebeck.itm.tr.iwsn.overlay.connection;

public class ConnectionAcceptedEvent {

	private final ServerConnection serverConnection;

	private final Connection connection;

	public ConnectionAcceptedEvent(final ServerConnection serverConnection, final Connection connection) {
		this.serverConnection = serverConnection;
		this.connection = connection;
	}

	public Connection getConnection() {
		return connection;
	}

	public ServerConnection getServerConnection() {
		return serverConnection;
	}
}
