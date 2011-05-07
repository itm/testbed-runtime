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

package de.uniluebeck.itm.gtr.connection;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.uniluebeck.itm.gtr.naming.NamingEntry;
import de.uniluebeck.itm.gtr.naming.NamingService;
import de.uniluebeck.itm.gtr.routing.RoutingTableService;
import de.uniluebeck.itm.tr.util.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
class ConnectionServiceImpl implements ConnectionService, ConnectionListener {

	private static final Logger log = LoggerFactory.getLogger(ConnectionService.class);

	private final Map<String, ConnectionFactory> connectionFactories = new HashMap<String, ConnectionFactory>();

	private final Map<String, ServerConnectionFactory> serverConnectionFactories = new HashMap<String, ServerConnectionFactory>();

	private final RoutingTableService routing;
	private NamingService naming;

	private final Map<Tuple<String, String>, ServerConnection> serverConnectionInstances = new HashMap<Tuple<String, String>, ServerConnection>();

	private final Map<String, Set<Connection>> connectionInstances = new HashMap<String, Set<Connection>>();

	@Inject
	public ConnectionServiceImpl(Set<ConnectionFactory> connectionFactories,
								 Set<ServerConnectionFactory> serverConnectionFactories,
								 RoutingTableService routing,
								 NamingService naming) {

		this.routing = routing;
		this.naming = naming;

		for (ConnectionFactory connectionFactory : connectionFactories) {
			this.connectionFactories.put(connectionFactory.getType(), connectionFactory);
		}

		for (ServerConnectionFactory serverConnectionFactory : serverConnectionFactories) {
			this.serverConnectionFactories.put(serverConnectionFactory.getType(), serverConnectionFactory);
		}
	}

	@Override
	public Connection getConnection(String nodeName) throws ConnectionInvalidAddressException, ConnectionTypeUnavailableException, IOException {

		checkNotNull(nodeName);
		checkArgument(!"".equals(nodeName), "Parameter nodeName must not be empty");

		String nextHop = routing.getNextHop(nodeName);

		synchronized (connectionInstances) {

			Set<Connection> nodeConnections = connectionInstances.get(nextHop);
			return nodeConnections == null || nodeConnections.size() == 0 ?
					createConnection(naming.getEntries(nextHop)) :
					nodeConnections.iterator().next();
		}
	}

	private Connection createConnection(SortedSet<NamingEntry> namingEntries) {

		if (namingEntries == null)
			return null;

		for (NamingEntry namingEntry : namingEntries) {

			ConnectionFactory connectionFactory = connectionFactories.get(namingEntry.getIface().getType());

			if (connectionFactory == null) {
				throw new RuntimeException(
						"No ConnectionFactory implementation found for interface type " +
								namingEntry.getIface().getType()
				);
			}

			try {

				Connection connection = connectionFactory.create(
						namingEntry.getNodeName(),
						Connection.Direction.OUT,
						namingEntry.getIface().getAddress()
				);
				connection.addListener(this);
				connection.connect();
				return connection;

			} catch (java.net.UnknownHostException e) {
				log.error(
						"Unkown host {}.",
						namingEntry.getIface().getAddress()
				);
			} catch (Exception e) {
				log.info(
						"Exception while trying to establish connection to {}. Cause: {}",
						namingEntry.getIface().getAddress(),
						e.getCause()
				);
			}

		}

		return null;

	}

	@Override
	public ServerConnection getServerConnection(String type, String address) throws ConnectionInvalidAddressException, ConnectionTypeUnavailableException, IOException {

		synchronized (serverConnectionInstances) {
			Tuple<String, String> typeAddressTuple = new Tuple<String, String>(type, address);
			ServerConnection instance = serverConnectionInstances.get(typeAddressTuple);
			return instance != null ? instance : createServerConnection(type, address);
		}

	}

	private ServerConnection createServerConnection(String type, String address) throws ConnectionInvalidAddressException {
		ServerConnectionFactory serverConnectionFactory = serverConnectionFactories.get(type);
		ServerConnection serverConnection = serverConnectionFactory.create(address);
		serverConnectionInstances.put(new Tuple<String, String>(type, address), serverConnection);
		return serverConnection;
	}

	@Override
	public void start() throws Exception {
		// nothing to do
	}

	@Override
	public void stop() {
		closeConnections();
		closeServerConnections();
	}

	private void closeServerConnections() {
		for (ServerConnection serverConnection : serverConnectionInstances.values()) {
			serverConnection.unbind();
		}
	}

	private void closeConnections() {
		for (Set<Connection> connections : this.connectionInstances.values()) {
			for (Connection connection : connections) {
				connection.disconnect();
			}
		}
	}

	@Override
	public void connectionOpened(Connection connection) {

		if (Connection.Direction.OUT == connection.getDirection()) {
			synchronized (connectionInstances) {
				Set<Connection> nodeConnections = connectionInstances.get(connection.getRemoteNodeName());
				if (nodeConnections == null) {
					nodeConnections = new HashSet<Connection>();
					connectionInstances.put(connection.getRemoteNodeName(), nodeConnections);
				}
				nodeConnections.add(connection);
			}
		}

	}

	@Override
	public void connectionClosed(Connection connection) {

		if (Connection.Direction.OUT == connection.getDirection()) {

			synchronized (connectionInstances) {
				Set<Connection> nodeConnections = connectionInstances.get(connection.getRemoteNodeName());
				if (nodeConnections != null) {
					nodeConnections.remove(connection);
				}
			}
		}
	}

}
