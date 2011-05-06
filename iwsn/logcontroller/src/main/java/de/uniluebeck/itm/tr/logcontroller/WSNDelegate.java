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
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or        *
 *   promote products derived from this software without specific prior written permission.                           *
 *                                                                                                                    *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, *
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE      *
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,         *
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE *
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF    *
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY   *
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.                                *
 **********************************************************************************************************************/

package de.uniluebeck.itm.tr.logcontroller;

import java.util.List;

import javax.jws.WebParam;
import javax.jws.WebService;

import eu.wisebed.api.common.Message;
import eu.wisebed.api.wsn.ChannelHandlerConfiguration;
import eu.wisebed.api.wsn.ChannelHandlerDescription;
import eu.wisebed.api.wsn.Program;
import eu.wisebed.api.wsn.WSN;
import eu.wisebed.testbed.api.wsn.Constants;

/**
 * Delegate for the WSN service.
 */
@WebService(
		serviceName = "WSNService",
		targetNamespace = Constants.NAMESPACE_WSN_SERVICE,
		portName = "WSNPort",
		endpointInterface = Constants.ENDPOINT_INTERFACE_WSN_SERVICE
)
public class WSNDelegate implements WSN {

	private WSN delegate;

	public WSNDelegate(WSN delegate) {
		this.delegate = delegate;
	}

	public void addController(
			@WebParam(name = "controllerEndpointUrl", targetNamespace = "") String controllerEndpointUrl) {
		delegate.addController(controllerEndpointUrl);
	}

	public void removeController(
			@WebParam(name = "controllerEndpointUrl", targetNamespace = "") String controllerEndpointUrl) {
		delegate.removeController(controllerEndpointUrl);
	}

	public String send(@WebParam(name = "nodeIds", targetNamespace = "") List<String> nodeIds,
					   @WebParam(name = "message", targetNamespace = "") Message message) {
		return delegate.send(nodeIds, message);
	}

	@Override
	public String setChannelPipeline(@WebParam(name = "nodes", targetNamespace = "") final List<String> nodes,
									 @WebParam(name = "channelHandlerConfigurations", targetNamespace = "") final
									 List<ChannelHandlerConfiguration> channelHandlerConfigurations) {
		return delegate.setChannelPipeline(nodes, channelHandlerConfigurations);
	}

	public String getVersion() {
		return delegate.getVersion();
	}

	public String areNodesAlive(@WebParam(name = "nodes", targetNamespace = "") List<String> nodes) {
		return delegate.areNodesAlive(nodes);
	}

	public String destroyVirtualLink(@WebParam(name = "sourceNode", targetNamespace = "") String sourceNode,
									 @WebParam(name = "targetNode", targetNamespace = "") String targetNode) {
		return delegate.destroyVirtualLink(sourceNode, targetNode);
	}

	public String disableNode(@WebParam(name = "node", targetNamespace = "") String node) {
		return delegate.disableNode(node);
	}

	public String disablePhysicalLink(@WebParam(name = "nodeA", targetNamespace = "") String nodeA,
									  @WebParam(name = "nodeB", targetNamespace = "") String nodeB) {
		return delegate.disablePhysicalLink(nodeA, nodeB);
	}

	public String enableNode(@WebParam(name = "node", targetNamespace = "") String node) {
		return delegate.enableNode(node);
	}

	public String enablePhysicalLink(@WebParam(name = "nodeA", targetNamespace = "") String nodeA,
									 @WebParam(name = "nodeB", targetNamespace = "") String nodeB) {
		return delegate.enablePhysicalLink(nodeA, nodeB);
	}

	public String flashPrograms(@WebParam(name = "nodeIds", targetNamespace = "") List<String> nodeIds,
								@WebParam(name = "programIndices", targetNamespace = "") List<Integer> programIndices,
								@WebParam(name = "programs", targetNamespace = "") List<Program> programs) {
		return delegate.flashPrograms(nodeIds, programIndices, programs);
	}

	@Override
	public List<ChannelHandlerDescription> getSupportedChannelHandlers() {
		return delegate.getSupportedChannelHandlers();
	}

	public List<String> getFilters() {
		return delegate.getFilters();
	}

	public String getNetwork() {
		return delegate.getNetwork();
	}

	public String resetNodes(@WebParam(name = "nodes", targetNamespace = "") List<String> nodes) {
		return delegate.resetNodes(nodes);
	}

	public String setVirtualLink(@WebParam(name = "sourceNode", targetNamespace = "") String sourceNode,
								 @WebParam(name = "targetNode", targetNamespace = "") String targetNode,
								 @WebParam(name = "remoteServiceInstance", targetNamespace = "")
								 String remoteServiceInstance,
								 @WebParam(name = "parameters", targetNamespace = "") List<String> parameters,
								 @WebParam(name = "filters", targetNamespace = "") List<String> filters) {
		return delegate.setVirtualLink(sourceNode, targetNode, remoteServiceInstance, parameters, filters);
	}
}
