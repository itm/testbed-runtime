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

import de.uniluebeck.itm.gtr.common.Service;
import eu.wisebed.testbed.api.wsn.Constants;
import eu.wisebed.testbed.api.wsn.v211.*;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.ws.Endpoint;
import java.util.List;

/**
 *  delegate for the wsn-service
 */
@WebService(
		serviceName = "WSNService",
		targetNamespace = Constants.NAMESPACE_WSN_SERVICE,
		portName = "WSNPort",
		endpointInterface = Constants.ENDPOINT_INTERFACE_WSN_SERVICE
)
public class WSNDelegate implements WSN {
    private WSN _delegate;
    
    public WSNDelegate(WSN delegate) {
        _delegate = delegate;
    }

    public void addController(@WebParam(name = "controllerEndpointUrl", targetNamespace = "") String controllerEndpointUrl) {
        _delegate.addController(controllerEndpointUrl);
    }

    public void removeController(@WebParam(name = "controllerEndpointUrl", targetNamespace = "") String controllerEndpointUrl) {
        _delegate.removeController(controllerEndpointUrl);
    }

    public String send(@WebParam(name = "nodeIds", targetNamespace = "") List<String> nodeIds,
                       @WebParam(name = "message", targetNamespace = "") Message message) {
        return _delegate.send(nodeIds, message);
    }

    public String getVersion() {
        return _delegate.getVersion();
    }

    public String areNodesAlive(@WebParam(name = "nodes", targetNamespace = "") List<String> nodes) {
        return _delegate.areNodesAlive(nodes);
    }

    public String defineNetwork(@WebParam(name = "newNetwork", targetNamespace = "") String newNetwork) {
        return _delegate.defineNetwork(newNetwork);
    }

    public String describeCapabilities(@WebParam(name = "capability", targetNamespace = "") String capability)
            throws UnsupportedOperationException_Exception {
        return _delegate.describeCapabilities(capability);
    }

    public String destroyVirtualLink(@WebParam(name = "sourceNode", targetNamespace = "") String sourceNode,
                                     @WebParam(name = "targetNode", targetNamespace = "") String targetNode) {
        return _delegate.destroyVirtualLink(sourceNode, targetNode);
    }

    public String disableNode(@WebParam(name = "node", targetNamespace = "") String node) {
        return _delegate.disableNode(node);
    }

    public String disablePhysicalLink(@WebParam(name = "nodeA", targetNamespace = "") String nodeA,
                                      @WebParam(name = "nodeB", targetNamespace = "") String nodeB) {
        return _delegate.disablePhysicalLink(nodeA, nodeB);
    }

    public String enableNode(@WebParam(name = "node", targetNamespace = "") String node) {
        return _delegate.enableNode(node);
    }

    public String enablePhysicalLink(@WebParam(name = "nodeA", targetNamespace = "") String nodeA,
                                     @WebParam(name = "nodeB", targetNamespace = "") String nodeB) {
        return _delegate.enablePhysicalLink(nodeA, nodeB);
    }

    public String flashPrograms(@WebParam(name = "nodeIds", targetNamespace = "") List<String> nodeIds,
                                @WebParam(name = "programIndices", targetNamespace = "") List<Integer> programIndices,
                                @WebParam(name = "programs", targetNamespace = "") List<Program> programs) {
        return _delegate.flashPrograms(nodeIds, programIndices, programs);
    }

    public List<String> getFilters() {
        return _delegate.getFilters();
    }

    public List<String> getNeighbourhood(@WebParam(name = "node", targetNamespace = "") String node)
            throws UnknownNodeUrnException_Exception {
        return _delegate.getNeighbourhood(node);
    }

    public String getNetwork() {
        return _delegate.getNetwork();
    }

    public String getPropertyValueOf(@WebParam(name = "node", targetNamespace = "") String node,
                                     @WebParam(name = "propertyName", targetNamespace = "") String propertyName)
            throws UnknownNodeUrnException_Exception {
        return _delegate.getPropertyValueOf(node, propertyName);
    }

    public String resetNodes(@WebParam(name = "nodes", targetNamespace = "") List<String> nodes) {
        return _delegate.resetNodes(nodes);
    }

    public String setStartTime(@WebParam(name = "time", targetNamespace = "") XMLGregorianCalendar time) {
        return _delegate.setStartTime(time);
    }

    public String setVirtualLink(@WebParam(name = "sourceNode", targetNamespace = "") String sourceNode,
                                 @WebParam(name = "targetNode", targetNamespace = "") String targetNode,
                                 @WebParam(name = "remoteServiceInstance", targetNamespace = "") String remoteServiceInstance,
                                 @WebParam(name = "parameters", targetNamespace = "") List<String> parameters,
                                 @WebParam(name = "filters", targetNamespace = "") List<String> filters) {
        return _delegate.setVirtualLink(sourceNode, targetNode, remoteServiceInstance, parameters, filters);
    }
}
