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

package de.uniluebeck.itm.tr.iwsn.overlay.connection;

import com.google.common.collect.ImmutableSortedSet;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.uniluebeck.itm.tr.iwsn.overlay.naming.NamingEntry;
import de.uniluebeck.itm.tr.iwsn.overlay.naming.NamingService;
import de.uniluebeck.itm.tr.iwsn.overlay.routing.RoutingTableService;
import de.uniluebeck.itm.tr.util.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
class ConnectionServiceImpl implements ConnectionService {

	private static final Logger log = LoggerFactory.getLogger(ConnectionService.class);

	private final Map<String, ConnectionFactory> connectionFactories = new HashMap<String, ConnectionFactory>();

	private final Map<String, ServerConnectionFactory> serverConnectionFactories =
			new HashMap<String, ServerConnectionFactory>();

	private final RoutingTableService routing;

	private NamingService naming;

	private final EventBus eventBus;

	private final Map<Tuple<String, String>, ServerConnection> serverConnectionInstances =
			new HashMap<Tuple<String, String>, ServerConnection>();

	private final Map<String, Set<Connection>> connectionInstances = new HashMap<String, Set<Connection>>();

	@Inject
	public ConnectionServiceImpl(final Set<ConnectionFactory> connectionFactories,
								 final Set<ServerConnectionFactory> serverConnectionFactories,
								 final RoutingTableService routing,
								 final NamingService naming,
								 final EventBus eventBus) {

		this.routing = routing;
		this.naming = naming;
		this.eventBus = eventBus;

		for (ConnectionFactory connectionFactory : connectionFactories) {
			this.connectionFactories.put(connectionFactory.getType(), connectionFactory);
		}

		for (ServerConnectionFactory serverConnectionFactory : serverConnectionFactories) {
			this.serverConnectionFactories.put(serverConnectionFactory.getType(), serverConnectionFactory);
		}
	}

	@Override
	public Connection getConnection(String nodeName)
			throws ConnectionInvalidAddressException, ConnectionTypeUnavailableException, IOException {

		log.trace("ConnectionServiceImpl.getConnection({})", nodeName);

		checkNotNull(nodeName);
		checkArgument(!"".equals(nodeName), "Parameter nodeName must not be empty");

		String nextHop = routing.getNextHop(nodeName);
		log.trace("Next hop for \"{}\": {}", nodeName, nextHop);

		synchronized (connectionInstances) {

			Set<Connection> nodeConnections = connectionInstances.get(nextHop);

			final boolean noExistingConnection = nodeConnections == null || nodeConnections.size() == 0;

			if (log.isTraceEnabled() && noExistingConnection) {
				log.trace("No existing connection to \"{}\" yet. Creating one...", nextHop);
			}

			Connection connection = noExistingConnection ?
					createConnection(nextHop) :
					nodeConnections.iterator().next();

			if (connection != null) {

				if (!connection.isConnected()) {
					log.trace("Existing connection is not connected. Creating new one...");
					connection = createConnection(nextHop);
				}

				log.trace("Connection to \"{}\" found/created: {}", nextHop, connection);
			} else {
				log.trace("Impossible to create connection to: {}", nextHop);
			}

			return connection;
		}
	}

	private Connection createConnection(final String nextHop) {

		final ImmutableSortedSet<NamingEntry> namingEntries = naming.getEntries(nextHop);

		if (namingEntries == null) {
			return null;
		}

		for (NamingEntry namingEntry : namingEntries) {

			final Connection connection = tryCreateConnection(namingEntry);
			if (connection != null) {
				return connection;
			}
		}

		return null;

	}

	private Connection tryCreateConnection(final NamingEntry namingEntry) {

		final ConnectionFactory connectionFactory = connectionFactories.get(namingEntry.getIface().getType());

		if (connectionFactory == null) {
			throw new RuntimeException(
					"No ConnectionFactory implementation found for interface type " +
							namingEntry.getIface().getType()
			);
		}

		final String nodeName = namingEntry.getNodeName();
		final String address = namingEntry.getIface().getAddress();

		try {

			final Connection connection = connectionFactory.create(
					nodeName,
					Connection.Direction.OUT,
					address,
					eventBus
			);

			connection.connect();
			return connection;

		} catch (java.net.UnknownHostException e) {
			log.error("Unknown host {} for overlay node {}.", address, nodeName);
			return null;
		} catch (Exception e) {
			log.info("Exception while trying to establish connection with {} for overlay node {}: {}",
					new Object[]{address, nodeName, e}
			);
			return null;
		}
	}

	@Override
	public ServerConnection getServerConnection(String type, String address)
			throws ConnectionInvalidAddressException, ConnectionTypeUnavailableException, IOException {

		synchronized (serverConnectionInstances) {
			Tuple<String, String> typeAddressTuple = new Tuple<String, String>(type, address);
			ServerConnection instance = serverConnectionInstances.get(typeAddressTuple);
			return instance != null ? instance : createServerConnection(type, address);
		}

	}

	private ServerConnection createServerConnection(String type, String address)
			throws ConnectionInvalidAddressException {
		ServerConnectionFactory serverConnectionFactory = serverConnectionFactories.get(type);
		ServerConnection serverConnection = serverConnectionFactory.create(address, eventBus);
		serverConnectionInstances.put(new Tuple<String, String>(type, address), serverConnection);
		return serverConnection;
	}

	@Override
	public void start() throws Exception {
		eventBus.register(this);
	}

	@Override
	public void stop() {
		closeConnections();
		closeServerConnections();
		eventBus.unregister(this);
	}

	private void closeServerConnections() {

		Set<ServerConnection> openServerConnections = new HashSet<ServerConnection>(serverConnectionInstances.values());

		for (ServerConnection serverConnection : openServerConnections) {
			serverConnection.unbind();
		}
	}

	private void closeConnections() {

		Set<Connection> openConnections = new HashSet<Connection>();

		for (Set<Connection> connections : this.connectionInstances.values()) {
			for (Connection connection : connections) {
				openConnections.add(connection);
			}
		}

		for (Connection openConnection : openConnections) {
			openConnection.disconnect();
		}
	}

	@Subscribe
	public void connectionOpened(ConnectionOpenedEvent event) {

		log.debug("ConnectionService.connectionOpened({})", event);

		if (Connection.Direction.OUT == event.getConnection().getDirection()) {
			synchronized (connectionInstances) {
				Set<Connection> nodeConnections = connectionInstances.get(event.getConnection().getRemoteNodeName());
				if (nodeConnections == null) {
					nodeConnections = new HashSet<Connection>();
					connectionInstances.put(event.getConnection().getRemoteNodeName(), nodeConnections);
				}
				nodeConnections.add(event.getConnection());
			}
		}

	}

	@Subscribe
	public void connectionClosed(ConnectionClosedEvent event) {

		log.debug("ConnectionService.connectionClosed({})", event);

		if (Connection.Direction.OUT == event.getConnection().getDirection()) {

			synchronized (connectionInstances) {
				Set<Connection> nodeConnections = connectionInstances.get(event.getConnection().getRemoteNodeName());
				if (nodeConnections != null) {
					nodeConnections.remove(event.getConnection());
				}
			}
		}
	}

}
