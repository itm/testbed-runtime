package de.uniluebeck.itm.tr.iwsn.overlay.connection;

public class ServerConnectionOpenedEvent {

	private final ServerConnection serverConnection;

	public ServerConnectionOpenedEvent(final ServerConnection serverConnection) {
		this.serverConnection = serverConnection;
	}

	public ServerConnection getServerConnection() {
		return serverConnection;
	}
}
