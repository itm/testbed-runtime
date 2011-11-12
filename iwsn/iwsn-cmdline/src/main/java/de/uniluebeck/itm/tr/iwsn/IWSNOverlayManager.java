package de.uniluebeck.itm.tr.iwsn;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import de.uniluebeck.itm.gtr.TestbedRuntime;
import de.uniluebeck.itm.gtr.naming.NamingEntry;
import de.uniluebeck.itm.gtr.naming.NamingInterface;
import de.uniluebeck.itm.gtr.naming.NamingService;
import de.uniluebeck.itm.gtr.routing.RoutingTableService;
import de.uniluebeck.itm.tr.util.Service;
import de.uniluebeck.itm.tr.util.domobserver.DOMObserver;
import de.uniluebeck.itm.tr.util.domobserver.DOMObserverListener;
import de.uniluebeck.itm.tr.util.domobserver.DOMTuple;
import de.uniluebeck.itm.tr.xml.Node;
import de.uniluebeck.itm.tr.xml.NodeName;
import de.uniluebeck.itm.tr.xml.ServerConnection;
import de.uniluebeck.itm.tr.xml.Testbed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Sets.newHashSet;

public class IWSNOverlayManager implements DOMObserverListener, Service {

	private static final Logger log = LoggerFactory.getLogger(IWSNOverlayManager.class);

	@Inject
	private TestbedRuntime overlay;

	@Inject
	private DOMObserver domObserver;

	private ScheduledFuture<?> domObserverSchedule;

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

		try {

			Object oldDOM = oldAndNew.getFirst();
			Object newDOM = oldAndNew.getSecond();

			Testbed oldConfig = oldDOM == null ? null : unmarshal((org.w3c.dom.Node) oldDOM);
			Testbed newConfig = newDOM == null ? null : unmarshal((org.w3c.dom.Node) newDOM);

			addAndRemoveOverlayNodes(oldConfig, newConfig);
			addAndRemoveOverlayNodeNames(oldConfig, newConfig);

		} catch (JAXBException e) {
			log.error("{}", e);
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
		return unmarshaller.unmarshal(rootNode, Testbed.class).getValue();
	}

	@Override
	public void start() throws Exception {
		domObserver.addListener(this);
		domObserverSchedule = overlay.getSchedulerService().scheduleWithFixedDelay(domObserver, 0, 3, TimeUnit.SECONDS);
	}

	@Override
	public void stop() {
		if (domObserverSchedule != null) {
			domObserverSchedule.cancel(false);
		}
		domObserver.removeListener(this);
	}
}
