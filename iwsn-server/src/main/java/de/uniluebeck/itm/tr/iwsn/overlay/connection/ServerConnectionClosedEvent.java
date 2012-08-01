package de.uniluebeck.itm.tr.iwsn.overlay.connection;

public class ServerConnectionClosedEvent {

	private final ServerConnection serverConnection;

	public ServerConnectionClosedEvent(final ServerConnection serverConnection) {
		this.serverConnection = serverConnection;
	}

	public ServerConnection getServerConnection() {
		return serverConnection;
	}
}
