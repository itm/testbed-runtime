/**********************************************************************************************************************
 * Copyright (c) 2010, Institute of Telematics, University of Luebeck                                                  *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote *
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

package de.uniluebeck.itm.wisebed.cmdlineclient;

import eu.wisebed.testbed.api.wsn.Constants;
import eu.wisebed.testbed.api.wsn.v22.*;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.List;

@WebService(
		serviceName = "WSNService",
		targetNamespace = Constants.NAMESPACE_WSN_SERVICE,
		portName = "WSNPort",
		endpointInterface = Constants.ENDPOINT_INTERFACE_WSN_SERVICE
)
public class DelegatingWsn implements WSN {

	private WSN delegate = null;

	private DelegatingWsn(WSN delegate) {
		super();
		this.delegate = delegate;
	}

	@Override
	public String areNodesAlive(List<String> nodes) {
		return delegate.areNodesAlive(nodes);
	}

	@Override
	public String defineNetwork(String newNetwork) {
		return delegate.defineNetwork(newNetwork);
	}

	@Override
	public String describeCapabilities(String capability) throws UnsupportedOperationException_Exception {
		return delegate.describeCapabilities(capability);
	}

	@Override
	public String destroyVirtualLink(String sourceNode, String targetNode) {
		return delegate.destroyVirtualLink(sourceNode, targetNode);
	}

	@Override
	public String disableNode(String node) {
		return delegate.disableNode(node);
	}

	@Override
	public String disablePhysicalLink(String nodeA, String nodeB) {
		return delegate.disablePhysicalLink(nodeA, nodeB);
	}

	@Override
	public String enableNode(String node) {
		return delegate.enableNode(node);
	}

	@Override
	public String enablePhysicalLink(String nodeA, String nodeB) {
		return delegate.enablePhysicalLink(nodeA, nodeB);
	}

	@Override
	public String flashPrograms(List<String> nodeIds, List<Integer> programIndices, List<Program> programs) {
		return delegate.flashPrograms(nodeIds, programIndices, programs);
	}

	@Override
	public List<String> getFilters() {
		return delegate.getFilters();
	}

	@Override
	public List<String> getNeighbourhood(String node) throws UnknownNodeUrnException_Exception {
		return delegate.getNeighbourhood(node);
	}

	@Override
	public String getNetwork() {
		return delegate.getNetwork();
	}

	@Override
	public String getPropertyValueOf(String node, String propertyName) throws UnknownNodeUrnException_Exception {
		return delegate.getPropertyValueOf(node, propertyName);
	}

	@Override
	public String getVersion() {
		return delegate.getVersion();
	}

	@Override
	public String resetNodes(List<String> nodes) {
		return delegate.resetNodes(nodes);
	}

	@Override
	public void addController(
			@WebParam(name = "controllerEndpointUrl", targetNamespace = "") String controllerEndpointUrl) {
		delegate.addController(controllerEndpointUrl);
	}

	@Override
	public void removeController(
			@WebParam(name = "controllerEndpointUrl", targetNamespace = "") String controllerEndpointUrl) {
		delegate.removeController(controllerEndpointUrl);
	}

	@Override
	public String send(List<String> nodeIds, Message message) {
		return delegate.send(nodeIds, message);
	}

	@Override
	public String setStartTime(XMLGregorianCalendar time) {
		return delegate.setStartTime(time);
	}

	@Override
	public String setVirtualLink(String sourceNode, String targetNode, String remoteServiceInstance,
								 List<String> parameters, List<String> filters) {
		return delegate.setVirtualLink(sourceNode, targetNode, remoteServiceInstance, parameters, filters);
	}

}
