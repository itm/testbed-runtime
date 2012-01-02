package de.uniluebeck.itm.tr.iwsn;

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import de.uniluebeck.itm.gtr.TestbedRuntime;
import de.uniluebeck.itm.gtr.naming.NamingEntry;
import de.uniluebeck.itm.gtr.naming.NamingInterface;
import de.uniluebeck.itm.gtr.naming.NamingService;
import de.uniluebeck.itm.gtr.routing.RoutingTableService;
import de.uniluebeck.itm.tr.util.ExecutorUtils;
import de.uniluebeck.itm.tr.util.Service;
import de.uniluebeck.itm.tr.util.Tuple;
import de.uniluebeck.itm.tr.util.domobserver.DOMObserver;
import de.uniluebeck.itm.tr.util.domobserver.DOMObserverListener;
import de.uniluebeck.itm.tr.util.domobserver.DOMTuple;
import de.uniluebeck.itm.tr.xml.Node;
import de.uniluebeck.itm.tr.xml.NodeName;
import de.uniluebeck.itm.tr.xml.ServerConnection;
import de.uniluebeck.itm.tr.xml.Testbed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Sets.newHashSet;

public class IWSNOverlayManager implements Service {

	private static final Logger log = LoggerFactory.getLogger(IWSNOverlayManager.class);

	final DOMObserverListener domObserverListener = new DOMObserverListener() {

		@Override
		public QName getQName() {
			return XPathConstants.NODE;
		}

		@Override
		public String getXPathExpression() {
			return "/*";
		}

		@Override
		public void onDOMChanged(final DOMTuple oldAndNew) {

			if (scheduler != null && !scheduler.isShutdown()) {

				scheduler.execute(new Runnable() {
					@Override
					public void run() {
						onDOMChangedInternal(oldAndNew);
					}
				}
				);
			} else {
				onDOMChangedInternal(oldAndNew);
			}
		}

		@Override
		public void onDOMLoadFailure(final Throwable cause) {
			log.warn("Unable to load changed configuration file {}. Maybe it is corrupt? Ignoring changes! Cause: ",
					cause
			);
		}

		@Override
		public void onXPathEvaluationFailure(final XPathExpressionException cause) {
			log.error("Failed to evaluate XPath expression on configuration file. Maybe it is corrupt? "
					+ "Ignoring changes! Cause: ", cause
			);
		}
	};

	private void onDOMChangedInternal(final DOMTuple oldAndNew) {
		try {

			Object oldDOM = oldAndNew.getFirst();
			Object newDOM = oldAndNew.getSecond();

			Testbed oldConfig = oldDOM == null ? null : unmarshal((org.w3c.dom.Node) oldDOM);
			Testbed newConfig = newDOM == null ? null : unmarshal((org.w3c.dom.Node) newDOM);

			addAndRemoveLocalNodeNames(oldConfig, newConfig);
			addAndRemoveOverlayNodes(oldConfig, newConfig);
			addAndRemoveOverlayNodeNames(oldConfig, newConfig);
			addAndRemoveOverlayServerConnections(oldConfig, newConfig);

		} catch (JAXBException e) {
			log.error("{}", e);
		}
	}

	private final TestbedRuntime overlay;

	private final DOMObserver domObserver;

	private final String nodeId;

	private ScheduledFuture<?> domObserverSchedule;

	private ScheduledExecutorService scheduler;

	IWSNOverlayManager(final TestbedRuntime overlay, final DOMObserver domObserver, final String nodeId) {
		this.overlay = overlay;
		this.domObserver = domObserver;
		this.nodeId = nodeId;
	}

	@Override
	public void start() throws Exception {
		scheduler = Executors.newScheduledThreadPool(
				1,
				new ThreadFactoryBuilder().setNameFormat("OverlayManager-Thread %d").build()
		);
		domObserver.addListener(domObserverListener);
		domObserverSchedule = scheduler.scheduleWithFixedDelay(domObserver, 0, 3, TimeUnit.SECONDS);
	}

	@Override
	public void stop() {
		if (domObserverSchedule != null) {
			domObserverSchedule.cancel(false);
		}
		domObserver.removeListener(domObserverListener);
		ExecutorUtils.shutdown(scheduler, 1, TimeUnit.SECONDS);
	}

	private void addAndRemoveLocalNodeNames(final Testbed oldConfig, final Testbed newConfig) {

		Set<String> oldLocalNodeNames = getLocalNodeNames(oldConfig);
		Set<String> newLocalNodeNames = getLocalNodeNames(newConfig);

		boolean sameLocalNodeNames = oldLocalNodeNames.equals(newLocalNodeNames);

		if (!sameLocalNodeNames) {

			Set<String> localNodeNamesRemoved = newHashSet(Sets.difference(oldLocalNodeNames, newLocalNodeNames));
			removeLocalNodeNames(localNodeNamesRemoved);

			Set<String> localNodesNamesAdded = newHashSet(Sets.difference(newLocalNodeNames, oldLocalNodeNames));
			addLocalNodeNames(localNodesNamesAdded);

		}

	}

	private void addLocalNodeNames(final Set<String> localNodesNamesAdded) {
		for (String localNodeName : localNodesNamesAdded) {
			overlay.getLocalNodeNameManager().addLocalNodeName(localNodeName);
		}
	}

	private void removeLocalNodeNames(final Set<String> localNodeNamesRemoved) {
		for (String localNodeName : localNodeNamesRemoved) {
			overlay.getLocalNodeNameManager().removeLocalNodeName(localNodeName);
		}
	}

	private void addAndRemoveOverlayNodeNames(final Testbed oldConfig, final Testbed newConfig) {

		Set<String> oldNodeNames = getOverlayNodeNames(oldConfig);
		Set<String> newNodeNames = getOverlayNodeNames(newConfig);

		boolean sameNodeNames = oldNodeNames.equals(newNodeNames);

		if (!sameNodeNames) {

			Set<String> nodesNamesRemoved = newHashSet(Sets.difference(oldNodeNames, newNodeNames));
			removeRoutingTableEntries(nodesNamesRemoved);

			Set<String> nodesNamesAdded = newHashSet(Sets.difference(newNodeNames, oldNodeNames));
			addRoutingTableEntries(nodesNamesAdded);
		}
	}

	private void addAndRemoveOverlayServerConnections(final Testbed oldConfig, final Testbed newConfig) {

		Set<Tuple<String, String>> oldConnections = getOverlayServerConnections(oldConfig);
		Set<Tuple<String, String>> newConnections = getOverlayServerConnections(newConfig);

		boolean sameConnections = oldConnections.equals(newConnections);

		if (!sameConnections) {

			Set<Tuple<String, String>> connectionsRemoved = newHashSet(Sets.difference(oldConnections, newConnections));
			removeOverlayServerConnections(connectionsRemoved);

			Set<Tuple<String, String>> connectionsAdded = newHashSet(Sets.difference(newConnections, oldConnections));
			addOverlayServerConnections(connectionsAdded);

		}
	}

	private void addOverlayServerConnections(final Set<Tuple<String, String>> connectionsAdded) {

		for (Tuple<String, String> connectionConfig : connectionsAdded) {
			try {
				overlay.getMessageServerService().addMessageServer(
						connectionConfig.getFirst(),
						connectionConfig.getSecond()
				);
			} catch (Exception e) {
				log.error("{}", e);
			}
		}
	}

	private void removeOverlayServerConnections(final Set<Tuple<String, String>> connectionsRemoved) {

		for (Tuple<String, String> connectionConfig : connectionsRemoved) {
			try {
				overlay.getMessageServerService().removeMessageServer(
						connectionConfig.getFirst(),
						connectionConfig.getSecond()
				);
			} catch (Exception e) {
				log.error("{}", e);
			}
		}
	}

	@Nonnull
	private Set<Tuple<String, String>> getOverlayServerConnections(@Nullable final Testbed config) {

		Set<Tuple<String, String>> connections = newHashSet();

		if (config == null) {
			return connections;
		}

		for (Node node : config.getNodes()) {
			if (nodeId.equals(node.getId())) {
				if (node.getServerconnections() != null && node.getServerconnections().getServerconnection() != null) {
					for (ServerConnection serverConnection : node.getServerconnections().getServerconnection()) {
						connections.add(new Tuple<String, String>(
								serverConnection.getType(),
								serverConnection.getAddress()
						)
						);
					}
				}
			}
		}

		return connections;
	}

	@Nonnull
	private Set<String> getLocalNodeNames(@Nullable final Testbed config) {

		Set<String> set = newHashSet();

		if (config == null) {
			return set;
		}

		for (Node node : config.getNodes()) {
			if (nodeId.equals(node.getId())) {
				for (NodeName nodeName : node.getNames().getNodename()) {
					set.add(nodeName.getName());
				}
			}
		}

		return set;
	}

	private Set<String> getOverlayNodeNames(final Testbed config) {

		Set<String> set = newHashSet();

		if (config == null) {
			return set;
		}

		for (Node node : config.getNodes()) {
			for (NodeName nodeName : node.getNames().getNodename()) {
				set.add(nodeName.getName());
			}
		}

		return set;
	}

	private void addAndRemoveOverlayNodes(final Testbed oldConfig, final Testbed newConfig) {

		Set<String> oldOverlayNodeIds = getOverlayNodeIds(oldConfig);
		Set<String> newOverlayNodeIds = getOverlayNodeIds(newConfig);

		boolean sameOverlayNodes = oldOverlayNodeIds.equals(newOverlayNodeIds);

		if (!sameOverlayNodes) {

			Set<String> nodesRemoved = newHashSet(Sets.difference(oldOverlayNodeIds, newOverlayNodeIds));
			removeOverlayNodes(oldConfig, nodesRemoved);

			Set<String> nodesAdded = newHashSet(Sets.difference(newOverlayNodeIds, oldOverlayNodeIds));
			addOverlayNodes(newConfig, nodesAdded);
		}
	}

	private void addOverlayNodes(final Testbed config, final Set<String> nodesAdded) {

		if (nodesAdded.isEmpty()) {
			return;
		}

		for (Node node : config.getNodes()) {
			if (nodesAdded.contains(node.getId())) {
				addNamingTableEntries(node);
				addRoutingTableEntries(getNodeNameSet(node.getNames().getNodename()));
			}
		}
	}

	private Set<String> getNodeNameSet(final List<NodeName> nodename) {
		Set<String> set = newHashSet();
		for (NodeName nodeName : nodename) {
			set.add(nodeName.getName());
		}
		return set;
	}

	private void removeOverlayNodes(final Testbed config, final Set<String> nodesRemoved) {

		if (nodesRemoved.isEmpty()) {
			return;
		}

		for (Node node : config.getNodes()) {
			if (nodesRemoved.contains(node.getId())) {
				removeRoutingTableEntries(getNodeNameSet(node.getNames().getNodename()));
				removeNamingTableEntries(node);
			}
		}
	}

	private void addNamingTableEntries(final Node node) {

		final NamingService namingService = overlay.getNamingService();

		if (node.getServerconnections() != null) {

			for (ServerConnection serverConnection : node.getServerconnections().getServerconnection()) {
				for (NodeName nodeName : node.getNames().getNodename()) {
					final NamingEntry namingEntry = new NamingEntry(
							nodeName.getName(),
							new NamingInterface(serverConnection.getType(), serverConnection.getAddress()),
							0
					);
					namingService.addEntry(namingEntry);
				}
			}
		}
	}

	private void removeNamingTableEntries(final Node node) {

		final NamingService namingService = overlay.getNamingService();

		for (NodeName nodeName : node.getNames().getNodename()) {
			NamingEntry entry = namingService.getEntry(nodeName.getName());
			if (entry != null) {
				namingService.removeEntry(entry);
			}
		}
	}

	private void removeRoutingTableEntries(final Set<String> nodeNames) {

		RoutingTableService routingTableService = overlay.getRoutingTableService();

		for (String nodeName : nodeNames) {
			routingTableService.removeNextHop(nodeName);
		}
	}

	private Set<String> getOverlayNodeIds(final Testbed config) {

		Set<String> set = newHashSet();

		if (config == null) {
			return set;
		}

		for (Node node : config.getNodes()) {
			set.add(node.getId());
		}

		return set;
	}

	private void addRoutingTableEntries(final Set<String> nodeNames) {

		RoutingTableService routingTableService = overlay.getRoutingTableService();

		for (String nodeName : nodeNames) {
			routingTableService.setNextHop(nodeName, nodeName);
		}
	}

	private Testbed unmarshal(final org.w3c.dom.Node rootNode) throws JAXBException {

		JAXBContext context = JAXBContext.newInstance(Testbed.class.getPackage().getName());
		Unmarshaller unmarshaller = context.createUnmarshaller();
		//noinspection SynchronizationOnLocalVariableOrMethodParameter
		synchronized (rootNode) {
			return unmarshaller.unmarshal(rootNode, Testbed.class).getValue();
		}
	}
}
