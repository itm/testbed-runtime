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

package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.common.util.concurrent.Service;
import de.uniluebeck.itm.nettyprotocols.ChannelHandlerConfigList;
import de.uniluebeck.itm.tr.devicedb.DeviceConfig;
import de.uniluebeck.itm.tr.iwsn.nodeapi.NodeApiCallResult;
import de.uniluebeck.itm.util.Listenable;
import de.uniluebeck.itm.util.concurrent.ListenableFutureMap;
import de.uniluebeck.itm.util.concurrent.ProgressListenableFutureMap;
import eu.wisebed.api.v3.common.NodeUrn;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;

/**
 * Represents a set of devices that are currently "connected" to this gateway. This set may either contain one device
 * (e.g. a device that is attached via USB) or more than one (typically if the devices are "wirelessly attached" and
 * currently in communication range). The set of devices may vary over time (cf. {@link
 * de.uniluebeck.itm.tr.iwsn.gateway.DeviceAdapter#getNodeUrns()}).
 */
public interface DeviceAdapter extends Listenable<DeviceAdapterListener>, Service {

	String getDeviceType();

	String getDevicePort();

	@Nullable
	Map<String, String> getDeviceConfiguration();

	@Nullable
	DeviceConfig getDeviceConfig();

	/**
	 * Returns the set of currently "connected" nodes. The returned set is a snapshot of the current state when being
	 * called. To determine subsequent changes to the state this method has to be called again.
	 *
	 * @return the set of currently "connected" nodes.
	 */
	Set<NodeUrn> getNodeUrns();

	/**
	 * Reprograms a set of nodes with the given binary image.
	 * <p/>
	 * Depending on the type of nodes the flashing process might e.g., either be implemented using hardware functions of
	 * a serially attached node or by running a (multi-hop) over-the-air programming ((M)OTAP) algorithm. Therefore the
	 * duration needed to flash individual nodes may very strongly.
	 *
	 * @param nodeUrns
	 * 		the set of nodes to be flashed
	 * @param binaryImage
	 * 		the binary image to be flashed onto the nodes
	 *
	 * @return a map mapping from node URNs to futures of Void. As soon as a void value is set for either the complete
	 *         operation or for an individual nodes flashing operation it can be regarded as successfully completed.
	 *         Progress can be determined by asking the individual futures or by listening to progress updates.
	 */
	ProgressListenableFutureMap<NodeUrn, Void> flashProgram(Iterable<NodeUrn> nodeUrns, byte[] binaryImage);

	/**
	 * Checks if a set of nodes is alive. A node can be regarded as alive if it is either serially attached and
	 * operational or e.g., if it is responding to hello/ping messages or the last interaction with a node has occurred
	 * within a reasonable time frame.
	 * <p/>
	 * In contrast to {@link DeviceAdapter#areNodesConnected(Iterable)} this method allows active interaction with the
	 * node to determine its liveliness, thereby potentially interfering with whatever application might currently be
	 * running in the wireless sensor network.
	 *
	 * @param nodeUrns
	 * 		the set of nodes to test for liveness
	 *
	 * @return a map mapping from node URNs to futures of Boolean, indicating if a node is alive or not
	 */
	ListenableFutureMap<NodeUrn, Boolean> areNodesAlive(Iterable<NodeUrn> nodeUrns);

	/**
	 * Checks if a set of nodes is connected. In contrast to {@link DeviceAdapter#areNodesAlive(Iterable)} this method
	 * must not interfere with currently running applications in the WSN. Therefore, the connectivity state might only
	 * be determined by e.g., checking if the node is currently serially attached or the last interaction with a
	 * wireless node is within a reasonable time frame. An implication of this restriction is that this method may be
	 * called by graphical user interfaces or clients without having to reserve the nodes in order to get a current
	 * state of the testbed.
	 *
	 * @param nodeUrns
	 * 		the set of nodes to test for connectivity
	 *
	 * @return a map mapping from node URNs to futures of Boolean, indicating if a node is connected or not
	 */
	ListenableFutureMap<NodeUrn, Boolean> areNodesConnected(Iterable<NodeUrn> nodeUrns);

	/**
	 * Resets a set of nodes.
	 *
	 * @param nodeUrns
	 * 		the set of nodes to be reset
	 *
	 * @return a map mapping from node URNs to futures of Void, indicating successful operation completion
	 */
	ListenableFutureMap<NodeUrn, Void> resetNodes(Iterable<NodeUrn> nodeUrns);

	/**
	 * Sends a binary message to a set of nodes.
	 *
	 * @param nodeUrns
	 * 		the set of nodes the message shall be sent to
	 * @param messageBytes
	 * 		the message to be sent
	 *
	 * @return a map mapping from node URNs to futures of Void, indicating successful operation completion
	 */
	ListenableFutureMap<NodeUrn, Void> sendMessage(Iterable<NodeUrn> nodeUrns, byte[] messageBytes);

	/**
	 * Sets the channel pipelines for the given set of nodes according to the list of handlers passed in. The channel
	 * pipeline is responsible for preprocessing data that is sent to and received from the individual nodes. It can
	 * e.g., be used to frame or deframe messages according to a format that the operating system on the node expects.
	 *
	 * @param nodeUrns
	 * 		the set of nodes the pipeline shall be set for
	 * @param channelHandlerConfigs
	 * 		the configuration of the pipeline
	 *
	 * @return a map mapping from node URNs to futures of Void, indicating successful operation completion
	 */
	ListenableFutureMap<NodeUrn, Void> setChannelPipelines(Iterable<NodeUrn> nodeUrns,
														   ChannelHandlerConfigList channelHandlerConfigs);

	/**
	 * Returns the channel pipelines currently set on the given set of nodes.
	 *
	 * @param nodeUrns
	 * 		the set of nodes for which to retrieve the current pipeline configuration
	 *
	 * @return a map from nodes to pipeline configurations
	 *
	 * @see DeviceAdapter#setChannelPipelines(Iterable, de.uniluebeck.itm.nettyprotocols.ChannelHandlerConfigList)
	 */
	ListenableFutureMap<NodeUrn, ChannelHandlerConfigList> getChannelPipelines(Iterable<NodeUrn> nodeUrns);

	/**
	 * Enables a set of nodes by "switching on" the virtual radio of the nodes to receive and send messages over both
	 * the physical and virtual links.
	 * <p/>
	 * Optional method for nodes that implement the
	 * <a href="http://wisebed.eu/api/documentation/node/0.9/node-api.pdf">Node API</a> as defined by the WISEBED
	 * project.
	 *
	 * @param nodeUrns
	 * 		the set of node URNs to perform this operation on
	 *
	 * @return a map containing futures to the results of the calls to the individual nodes
	 */
	ListenableFutureMap<NodeUrn, NodeApiCallResult> enableNodes(Iterable<NodeUrn> nodeUrns);

	/**
	 * Enables physical links between individual nodes given by a set of links.
	 * <p/>
	 * Optional method for nodes that implement the
	 * <a href="http://wisebed.eu/api/documentation/node/0.9/node-api.pdf">Node API</a> as defined by the WISEBED
	 * project.
	 *
	 * @param sourceTargetMap
	 * 		a map mapping from source to target representing the links to be enabled
	 *
	 * @return a map containing futures to the results of the calls to the individual nodes
	 */
	ListenableFutureMap<NodeUrn, NodeApiCallResult> enablePhysicalLinks(Map<NodeUrn, NodeUrn> sourceTargetMap);

	/**
	 * Enables virtual links between individual nodes given by a set of links.
	 * <p/>
	 * Optional method for nodes that implement the
	 * <a href="http://wisebed.eu/api/documentation/node/0.9/node-api.pdf">Node API</a> as defined by the WISEBED
	 * project.
	 *
	 * @param sourceTargetMap
	 * 		a map mapping from source to target representing the links to be enabled
	 *
	 * @return a map containing futures to the results of the calls to the individual nodes
	 */
	ListenableFutureMap<NodeUrn, NodeApiCallResult> enableVirtualLinks(Map<NodeUrn, NodeUrn> sourceTargetMap);

	/**
	 * Disables a set of nodes by "switching off" the virtual radio of the nodes to receive and send messages over both
	 * the physical and virtual links.
	 *
	 * @param nodeUrns
	 * 		the set of node URNs to perform this operation on
	 *
	 * @return a map containing futures to the results of the calls to the individual nodes
	 */
	ListenableFutureMap<NodeUrn, NodeApiCallResult> disableNodes(Iterable<NodeUrn> nodeUrns);

	/**
	 * Disables physical links between individual nodes given by a set of links.
	 * <p/>
	 * Optional method for nodes that implement the
	 * <a href="http://wisebed.eu/api/documentation/node/0.9/node-api.pdf">Node API</a> as defined by the WISEBED
	 * project.
	 *
	 * @param sourceTargetMap
	 * 		a map mapping from source to target representing the links to be disabled
	 *
	 * @return a map containing futures to the results of the calls to the individual nodes
	 */
	ListenableFutureMap<NodeUrn, NodeApiCallResult> disablePhysicalLinks(Map<NodeUrn, NodeUrn> sourceTargetMap);

	/**
	 * Disables virtual links between individual nodes given by a set of links.
	 * <p/>
	 * Optional method for nodes that implement the
	 * <a href="http://wisebed.eu/api/documentation/node/0.9/node-api.pdf">Node API</a> as defined by the WISEBED
	 * project.
	 *
	 * @param sourceTargetMap
	 * 		a map mapping from source to target representing the links to be disabled
	 *
	 * @return a map containing futures to the results of the calls to the individual nodes
	 */
	ListenableFutureMap<NodeUrn, NodeApiCallResult> disableVirtualLinks(Map<NodeUrn, NodeUrn> sourceTargetMap);
}
