package de.uniluebeck.itm.wisebed.cmdlineclient.wrapper;

import com.google.common.collect.Lists;
import de.uniluebeck.itm.tr.util.SecureIdGenerator;
import eu.wisebed.testbed.api.wsn.v22.*;

import javax.jws.WebParam;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class WorkingWSN implements WSN {

	private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

	private SecureIdGenerator secureIdGenerator = new SecureIdGenerator();

	private Controller controller;

	public WorkingWSN(final Controller controller) {
		this.controller = controller;
	}

    private Random random = new Random();

	private void scheduleReply(final List<String> nodeIds, final String requestId, final int value) {
		for (final String nodeId : nodeIds) {
			scheduler.schedule(new Runnable() {
				@Override
				public void run() {
					RequestStatus requestStatus = new RequestStatus();
					requestStatus.setRequestId(requestId);
					Status status = new Status();
					status.setNodeId(nodeId);
					status.setValue(value);
					requestStatus.getStatus().add(status);
					controller.receiveStatus(Lists.newArrayList(requestStatus));
				}
			}, random.nextInt(100), TimeUnit.MILLISECONDS
			);
		}
	}

	@Override
	public void addController(
			@WebParam(name = "controllerEndpointUrl", targetNamespace = "") final String controllerEndpointUrl) {
	}

	@Override
	public void removeController(
			@WebParam(name = "controllerEndpointUrl", targetNamespace = "") final String controllerEndpointUrl) {
	}

	@Override
	public String send(@WebParam(name = "nodeIds", targetNamespace = "") final List<String> nodeIds,
					   @WebParam(name = "message", targetNamespace = "") final Message message) {
		final String requestId = secureIdGenerator.getNextId();
		scheduleReply(nodeIds, requestId, 1);
		return requestId;
	}

	@Override
	public String getVersion() {
		return "2.1";
	}

	@Override
	public String areNodesAlive(@WebParam(name = "nodes", targetNamespace = "") final List<String> nodes) {
		final String requestId = secureIdGenerator.getNextId();
		scheduleReply(nodes, requestId, 1);
		return requestId;
	}

	@Override
	public String defineNetwork(@WebParam(name = "newNetwork", targetNamespace = "") final String newNetwork) {
		throw new RuntimeException("Not yet implemented.");
	}

	@Override
	public String describeCapabilities(@WebParam(name = "capability", targetNamespace = "") final String capability)
			throws UnsupportedOperationException_Exception {
		throw new RuntimeException("Not yet implemented.");
	}

	@Override
	public String destroyVirtualLink(@WebParam(name = "sourceNode", targetNamespace = "") final String sourceNode,
									 @WebParam(name = "targetNode", targetNamespace = "") final String targetNode) {
		final String requestId = secureIdGenerator.getNextId();
		scheduleReply(Lists.newArrayList(sourceNode), requestId, 1);
		return requestId;
	}

	@Override
	public String disableNode(@WebParam(name = "node", targetNamespace = "") final String node) {
		final String requestId = secureIdGenerator.getNextId();
		scheduleReply(Lists.newArrayList(node), requestId, 1);
		return requestId;
	}

	@Override
	public String disablePhysicalLink(@WebParam(name = "nodeA", targetNamespace = "") final String nodeA,
									  @WebParam(name = "nodeB", targetNamespace = "") final String nodeB) {
		final String requestId = secureIdGenerator.getNextId();
		scheduleReply(Lists.newArrayList(nodeA), requestId, 1);
		return requestId;
	}

	@Override
	public String enableNode(@WebParam(name = "node", targetNamespace = "") final String node) {
		final String requestId = secureIdGenerator.getNextId();
		scheduleReply(Lists.newArrayList(node), requestId, 1);
		return requestId;
	}

	@Override
	public String enablePhysicalLink(@WebParam(name = "nodeA", targetNamespace = "") final String nodeA,
									 @WebParam(name = "nodeB", targetNamespace = "") final String nodeB) {
		final String requestId = secureIdGenerator.getNextId();
		scheduleReply(Lists.newArrayList(nodeA), requestId, 1);
		return requestId;
	}

	@Override
	public String flashPrograms(@WebParam(name = "nodeIds", targetNamespace = "") final List<String> nodeIds,
								@WebParam(name = "programIndices", targetNamespace = "")
								final List<Integer> programIndices,
								@WebParam(name = "programs", targetNamespace = "") final List<Program> programs) {
		final String requestId = secureIdGenerator.getNextId();
		scheduleReply(Lists.newArrayList(nodeIds), requestId, 100);
		return requestId;
	}

	@Override
	public List<String> getFilters() {
		throw new RuntimeException("Not yet implemented.");
	}

	@Override
	public List<String> getNeighbourhood(@WebParam(name = "node", targetNamespace = "") final String node)
			throws UnknownNodeUrnException_Exception {
		throw new RuntimeException("Not yet implemented.");
	}

	@Override
	public String getNetwork() {
		return "<wiseml><setup><node id=\"firstNode\"></node></setup></wiseml>";
	}

	@Override
	public String getPropertyValueOf(@WebParam(name = "node", targetNamespace = "") final String node,
									 @WebParam(name = "propertyName", targetNamespace = "")
									 final String propertyName)
			throws UnknownNodeUrnException_Exception {
		throw new RuntimeException("Not yet implemented.");
	}

	@Override
	public String resetNodes(@WebParam(name = "nodes", targetNamespace = "") final List<String> nodes) {
		final String requestId = secureIdGenerator.getNextId();
		scheduleReply(nodes, requestId, 1);
		return requestId;
	}

	@Override
	public String setStartTime(@WebParam(name = "time", targetNamespace = "") final XMLGregorianCalendar time) {
		throw new RuntimeException("Not yet implemented.");
	}

	@Override
	public String setVirtualLink(@WebParam(name = "sourceNode", targetNamespace = "") final String sourceNode,
								 @WebParam(name = "targetNode", targetNamespace = "") final String targetNode,
								 @WebParam(name = "remoteServiceInstance", targetNamespace = "") final
								 String remoteServiceInstance,
								 @WebParam(name = "parameters", targetNamespace = "") final List<String> parameters,
								 @WebParam(name = "filters", targetNamespace = "") final List<String> filters) {
		final String requestId = secureIdGenerator.getNextId();
		scheduleReply(Lists.newArrayList(sourceNode), requestId, 1);
		return requestId;
	}

}
