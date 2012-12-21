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

package de.uniluebeck.itm.tr.iwsn.nodeapi;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Service;


public interface NodeApi extends Service {

	ListenableFuture<NodeApiCallResult> sendVirtualLinkMessage(byte RSSI, byte LQI, long destination, long source,
													 byte payload[]);

	ListenableFuture<NodeApiCallResult> sendVirtualLinkMessage(long destination, long source, byte payload[]);

	ListenableFuture<NodeApiCallResult> sendByteMessage(byte binaryType, byte payload[]);

	ListenableFuture<NodeApiCallResult> flashProgram(byte payload[]);

	/**
	 * Set up a virtual link between the two nodes identified by the unique this node and destNode. The reserved broadcast
	 * ID is not allowed as parameter.
	 *
	 * @param destNode end point of the virtual link
	 *
	 * @return a {@link java.util.concurrent.Future} instance indicating the result of the call
	 */
	ListenableFuture<NodeApiCallResult> setVirtualLink(long destNode);

	/**
	 * Destroy a virtual link between this node and destNode. The reserved broadcast ID is not allowed as parameter.
	 *
	 * @param destNode end point of the virtual link
	 *
	 * @return a {@link java.util.concurrent.Future} instance indicating the result of the call
	 */
	ListenableFuture<NodeApiCallResult> destroyVirtualLink(long destNode);

	/**
	 * Enable the physical radio link between this node and nodeB (if possible). The reserved broadcast ID is not allowed
	 * as parameter.
	 *
	 * @param nodeB end point of the link
	 *
	 * @return a {@link java.util.concurrent.Future} instance indicating the result of the call
	 */
	ListenableFuture<NodeApiCallResult> enablePhysicalLink(long nodeB);

	/**
	 * Disable the physical radio link between this node and nodeB. The reserved broadcast ID is not allowed as parameter.
	 *
	 * @param nodeB end point of the link
	 *
	 * @return a {@link java.util.concurrent.Future} instance indicating the result of the call
	 */
	ListenableFuture<NodeApiCallResult> disablePhysicalLink(long nodeB);

	/**
	 * Request this Node for a special property value
	 *
	 * @param property request the property specified by this value
	 *
	 * @return a {@link java.util.concurrent.Future} instance indicating the result of the call
	 */
	ListenableFuture<NodeApiCallResult> getPropertyValue(byte property);

	/**
	 * Request a Neighborhoodlist from this node
	 *
	 * @return a {@link java.util.concurrent.Future} instance indicating the result of the call
	 */
	ListenableFuture<NodeApiCallResult> getNeighborhood();

	/**
	 * Enables this node. The node is reactivating the radio and start interacting with the environment.
	 *
	 * @return a {@link java.util.concurrent.Future} instance indicating the result of the call
	 */
	ListenableFuture<NodeApiCallResult> enableNode();

	/**
	 * Disable this node. The node does not longer send out messages or interact with the environment (e.g. a mobile node
	 * or via an actuator).
	 *
	 * @return a {@link java.util.concurrent.Future} instance indicating the result of the call
	 */
	ListenableFuture<NodeApiCallResult> disableNode();

	/**
	 * Reset this node in time milliseconds
	 *
	 * @param time the time in milliseconds after which to reset the node
	 *
	 * @return a {@link java.util.concurrent.Future} instance indicating the result of the call
	 */
	ListenableFuture<NodeApiCallResult> resetNode(int time);

	/**
	 * Sets the starttime of the nodes app to in time milliseconds
	 *
	 * @param time the time in milliseconds after which to start
	 *
	 * @return a {@link java.util.concurrent.Future} instance indicating the result of the call
	 */
	ListenableFuture<NodeApiCallResult> setStartTime(int time);

	/**
	 * Sets a new virtualNodeID. In default virtualID == natural nodeID
	 *
	 * @param virtualNodeID the nodes' new virtualNodeId
	 *
	 * @return a {@link java.util.concurrent.Future} instance indicating the result of the call
	 */
	ListenableFuture<NodeApiCallResult> setVirtualID(long virtualNodeID);

	/**
	 * Asks the connected node for its ID. In default virtualID == natural nodeID
	 *
	 * @return a {@link java.util.concurrent.Future} instance indicating the result of the call
	 */
	ListenableFuture<NodeApiCallResult> getVirtualID();

	/**
	 * Check if this node is alive.
	 *
	 * @return a {@link java.util.concurrent.Future} instance indicating the result of the call
	 */
	ListenableFuture<NodeApiCallResult> areNodesAlive();

}