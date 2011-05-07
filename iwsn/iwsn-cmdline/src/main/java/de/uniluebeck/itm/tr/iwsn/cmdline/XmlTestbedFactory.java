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

package de.uniluebeck.itm.tr.iwsn.cmdline;

import com.google.common.collect.ImmutableList;
import de.uniluebeck.itm.gtr.TestbedRuntime;
import de.uniluebeck.itm.gtr.application.TestbedApplication;
import de.uniluebeck.itm.gtr.application.TestbedApplicationFactory;
import de.uniluebeck.itm.gtr.naming.NamingEntry;
import de.uniluebeck.itm.gtr.naming.NamingInterface;
import de.uniluebeck.itm.gtr.naming.NamingService;
import de.uniluebeck.itm.gtr.routing.RoutingTableService;
import de.uniluebeck.itm.tr.util.Triple;
import de.uniluebeck.itm.tr.util.Tuple;
import de.uniluebeck.itm.tr.xml.*;
import org.jgrapht.EdgeFactory;
import org.jgrapht.alg.BellmanFordShortestPath;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBException;
import java.io.File;
import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;


public class XmlTestbedFactory {

	private static final Logger log = LoggerFactory.getLogger(XmlTestbedFactory.class);

	public Tuple<TestbedRuntime, ImmutableList<TestbedApplication>> create(String configFileStr, String... nodeIds)
			throws JAXBException {

		checkNotNull(configFileStr);
        for (String nodeId : nodeIds) {
            checkNotNull(nodeId);
        }

		File configFile = new File(configFileStr);

		if (!configFile.exists() || configFile.isDirectory()) {
			throw new RuntimeException(
					"Config file invalid (!configFile.exists() || configFile.isDirectory()) for " + configFile
			);
		}

		Testbed testbed = JAXB.unmarshal(configFile, Testbed.class);

		TestbedRuntime testbedRuntime = null;
		ImmutableList<TestbedApplication> testbedApplications = null;

		// TODO sanity check for duplicate node names and node ids

        boolean found = false;
        for (String nodeId : nodeIds) {

            if (found) {
                break;
            }

            for (Node node : testbed.getNodes()) {

                if (found) {
                    break;
                }

                if (nodeId.equals(node.getId())) {

                    log.debug("Creating testbed instance for node ID {}", nodeId);
                    testbedRuntime = createTestbedRuntime(node);
                    configureServerConnections(testbedRuntime, node.getServerconnections());
                    configureClientConnections(testbedRuntime, node, testbed);
                    testbedApplications = configureApplications(testbedRuntime, node.getApplications());
                    found = true;
                }
            }
        }

		checkNotNull(testbedRuntime, "No configuration for one of the node IDs %s found!", Arrays.toString(nodeIds));

		return new Tuple<TestbedRuntime, ImmutableList<TestbedApplication>>(testbedRuntime, testbedApplications);
	}

	private ImmutableList<TestbedApplication> configureApplications(TestbedRuntime testbedRuntime,
																	Applications applications) {

		ImmutableList.Builder<TestbedApplication> applicationListBuilder =
				new ImmutableList.Builder<TestbedApplication>();


		if (applications != null && applications.getApplication() != null) {

			List<Application> applicationsList = applications.getApplication();

			for (Application application : applicationsList) {

				String factoryClass = application.getFactoryclass();
				try {

					log.debug("Instantiating factory class \"{}\" for application \"{}\"", factoryClass,
							application.getName()
					);
					TestbedApplicationFactory app =
							(TestbedApplicationFactory) Class.forName(factoryClass).newInstance();

					TestbedApplication testbedApplication =
							app.create(testbedRuntime, application.getName(), application.getAny());

					// only add if creation successful
					if (testbedApplication != null) {
						applicationListBuilder.add(testbedApplication);
					}

				} catch (Exception e) {
					log.warn("Exception while loading application factory class \"" + factoryClass + "\": " + e, e);
				}

			}
		}

		return applicationListBuilder.build();
	}

	private void configureClientConnections(TestbedRuntime testbedRuntime, Node node, Testbed testbed) {

		log.debug("Filling routing and naming tables for node ID {}", node.getId());

		SimpleDirectedGraph<Node, Triple<Node, Node, ClientConnection>> graph = constructNodeGraph(testbed);
		BellmanFordShortestPath<Node, Triple<Node, Node, ClientConnection>> path =
				new BellmanFordShortestPath<Node, Triple<Node, Node, ClientConnection>>(graph, node);

		RoutingTableService routingTableService = testbedRuntime.getRoutingTableService();
		NamingService namingService = testbedRuntime.getNamingService();

		String sourceNodeFirstName = node.getNames().getNodename().get(0).getName();

		for (Node remoteNode : testbed.getNodes()) {
			if (node != remoteNode) {

				List<Triple<Node, Node, ClientConnection>> edgeList = path.getPathEdgeList(remoteNode);

				if (edgeList != null && edgeList.size() > 0) {

					Triple<Node, Node, ClientConnection> nextHopEdge = edgeList.get(0);
					String nextHopName = nextHopEdge.getSecond().getNames().getNodename().get(0).getName();

					for (NodeName name : remoteNode.getNames().getNodename()) {

						String destinationNodeName = name.getName();
						log.debug("Adding routing entry: {} -> {}, next-hop: {}", new Object[]{
								sourceNodeFirstName, destinationNodeName, nextHopName
						}
						);

						routingTableService.setNextHop(destinationNodeName, nextHopName);

					}

					ClientConnection nextHopConnection = nextHopEdge.getThird();
					// TODO use priorities
					NamingInterface nextHopInterface =
							new NamingInterface(nextHopConnection.getType(), nextHopConnection.getAddress());
					int priority = 0;
					namingService.addEntry(new NamingEntry(nextHopName, nextHopInterface, priority));
					log.debug("Adding naming entry: {}={}, prio: {}", new Object[]{
							nextHopName, nextHopInterface, priority
					}
					);
				}
			}
		}

	}

	private boolean hasOptionalClientConnectionList(Node node) {
		return node.getClientconnections() != null && node.getClientconnections().getClientconnection() != null && node
				.getClientconnections().getClientconnection().size() > 0;
	}

	private EdgeFactory<Node, Triple<Node, Node, ClientConnection>> edgeFactory =
			new EdgeFactory<Node, Triple<Node, Node, ClientConnection>>() {
				@Override
				public Triple<Node, Node, ClientConnection> createEdge(Node node, Node node1) {
					throw new RuntimeException("This edgeFactory should not be used.");
				}
			};

	private SimpleDirectedGraph<Node, Triple<Node, Node, ClientConnection>> constructNodeGraph(Testbed testbed) {

		Class<Triple> clazz = Triple.class;
		SimpleDirectedGraph<Node, Triple<Node, Node, ClientConnection>> graph =
				new SimpleDirectedGraph<Node, Triple<Node, Node, ClientConnection>>(edgeFactory);
		Map<Tuple<String, String>, Node> serverConnectionsNodeMap = new HashMap<Tuple<String, String>, Node>();

		// remember all server connections and the nodes they belong to, because we need to match them with the client
		// connection list
		for (Node node : testbed.getNodes()) {
			if (node.getServerconnections() != null) {
				for (ServerConnection serverConnection : node.getServerconnections().getServerconnection()) {

					Tuple<String, String> connectionTuple =
							new Tuple<String, String>(serverConnection.getType(), serverConnection.getAddress());
					serverConnectionsNodeMap.put(connectionTuple, node);
				}
			}
		}

		for (Node node : testbed.getNodes()) {

			if (hasOptionalClientConnectionList(node)) {

				// take only hard-configured client connections
				for (ClientConnection clientConnection : node.getClientconnections().getClientconnection()) {

					Tuple<String, String> connectionTuple =
							new Tuple<String, String>(clientConnection.getType(), clientConnection.getAddress());
					Node serverNode = serverConnectionsNodeMap.get(connectionTuple);

					if (serverNode != null) {
						assureNodesContainedInGraph(graph, node, serverNode);
						Triple<Node, Node, ClientConnection> edge =
								new Triple<Node, Node, ClientConnection>(node, serverNode, clientConnection);
						graph.addEdge(node, serverNode, edge);
					}
				}

			} else {

				// fully mesh client and server connections
				for (Map.Entry<Tuple<String, String>, Node> entry : serverConnectionsNodeMap.entrySet()) {

					// only add an edge to remote nodes
					if (node != entry.getValue()) {
						assureNodesContainedInGraph(graph, node, entry.getValue());
						ClientConnection clientConnection = new ClientConnection();
						clientConnection.setType(entry.getKey().getFirst());
						clientConnection.setAddress(entry.getKey().getSecond());
						Triple<Node, Node, ClientConnection> edge =
								new Triple<Node, Node, ClientConnection>(node, entry.getValue(), clientConnection);
						graph.addEdge(node, entry.getValue(), edge);
					}
				}

			}
		}

		return graph;

	}

	private void assureNodesContainedInGraph(SimpleDirectedGraph graph, Node... nodes) {
		for (Node node : nodes) {
			if (!graph.containsVertex(node)) {
				graph.addVertex(node);
			}
		}
	}

	/**
	 * Configures the testbed runtime to open corresponding server connections (e.g. TCP server sockets) and to listen for
	 * incoming messages on them.
	 *
	 * @param testbedRuntime
	 * @param serverconnections
	 */
	private void configureServerConnections(TestbedRuntime testbedRuntime, ServerConnections serverconnections) {

		if (serverconnections == null) {
			return;
		}

		for (ServerConnection serverConnection : serverconnections.getServerconnection()) {
			// TODO implement filter support
			testbedRuntime.getMessageServerService()
					.addMessageServer(serverConnection.getType(), serverConnection.getAddress());
		}

	}

	private TestbedRuntime createTestbedRuntime(Node node) {

		List<NodeName> nodeNameList = node.getNames().getNodename();
		Set<String> nodeNameSet = new HashSet<String>();
		for (NodeName nodeName : nodeNameList) {
			nodeNameSet.add(nodeName.getName());
		}

		return TestbedRuntime.Factory.create(nodeNameSet.toArray(new String[nodeNameSet.size()]));
	}

}
