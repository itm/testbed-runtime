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

import java.nio.ByteBuffer;

import static com.google.common.base.Preconditions.checkArgument;


class NodeApiPackets {

	public static class Interaction {

		/**
		 * The virtual ID of the original sender can be found in source. The destination of the message can either be another
		 * single virtual ID or the reserved broadcast ID. The RSSI and LQI values are simulated in an earlier step or set
		 * default to 0.
		 * <p/>
		 *
		 * @param requestId   the request ID
		 * @param RSSI		the simulated rssi for this message
		 * @param LQI		 the simulated LQI for this message
		 * @param destination destination of the message (id or broadcast)
		 * @param source	  source of the message
		 * @param payload	 payload of the virtual message
		 *
		 * @return a ByteBuffer instance containing the packet
		 */
		public static ByteBuffer newVirtualLinkMessagePacket(int requestId, byte RSSI, byte LQI, long destination,
															 long source,
															 byte[] payload) {

			checkArgument(payload.length >= 0 && payload.length <= 256);

			ByteBuffer bb = ByteBuffer.allocate(payload.length + 21);
			bb.put(NodeApiMessageType.VL_MESSAGE);
			bb.put((byte) requestId);
			bb.put(RSSI);
			bb.put(LQI);
			bb.put((byte) payload.length);
			bb.putLong(destination);
			bb.putLong(source);
			bb.put(payload);
			return bb;

		}

		public static ByteBuffer newVirtualLinkMessagePacket(int requestId, long destination, long source,
															 byte[] payload) {
			return newVirtualLinkMessagePacket(requestId, (byte) 0, (byte) 0, destination, source, payload);
		}

		public static ByteBuffer newByteMessagePacket(int requestId, byte binaryType, byte[] payload) {

			ByteBuffer bb = ByteBuffer.allocate(payload.length + 4);
			bb.put(NodeApiMessageType.BYTE_MESSAGE);
			bb.put((byte) requestId);
			bb.put(binaryType);
			bb.put((byte) payload.length);
			bb.put(payload);
			return bb;

		}

		public static ByteBuffer newFlashProgramPacket(int requestId, byte[] payload) {

			ByteBuffer bb = ByteBuffer.allocate(payload.length + 3);
			bb.put(NodeApiMessageType.FLASH_MESSAGE);
			bb.put((byte) requestId);
			bb.put((byte) payload.length);
			bb.put(payload);
			return bb;

		}

		public static boolean isInteractionPacket(ByteBuffer bb) {
			return bb.array().length != 0 &&
					bb.get(0) == NodeApiMessageType.DEBUG_MESSAGE ||
					bb.get(0) == NodeApiMessageType.VL_MESSAGE ||
					bb.get(0) == NodeApiMessageType.BYTE_MESSAGE ||
					bb.get(0) == NodeApiMessageType.FLASH_MESSAGE;
		}

	}

	public static class LinkControl {

		/**
		 * Set up a virtual link between the two nodes identified by the unique this node and destinationNode. The reserved
		 * broadcast ID is not allowed as parameter.
		 *
		 * @param requestId	   the request ID
		 * @param destinationNode end point of the virtual link
		 *
		 * @return a ByteBuffer instance containing the packet
		 */
		public static ByteBuffer newSetVirtualLinkPacket(int requestId, long destinationNode) {

			ByteBuffer bb = ByteBuffer.allocate(10);
			bb.put(NodeApiMessageType.SET_VIRTUAL_LINK);
			bb.put((byte) requestId);
			bb.putLong(destinationNode);

			return bb;

		}

		/**
		 * Destroy a virtual link between this node and destinationNode. The reserved broadcast ID is not allowed as
		 * parameter.
		 *
		 * @param requestId	   the request ID
		 * @param destinationNode end point of the virtual link
		 *
		 * @return a ByteBuffer instance containing the packet
		 */
		public static ByteBuffer newDestroyVirtualLinkPacket(int requestId, long destinationNode) {

			ByteBuffer bb = ByteBuffer.allocate(10);
			bb.put(NodeApiMessageType.DESTROY_VIRTUAL_LINK);
			bb.put((byte) requestId);
			bb.putLong(destinationNode);
			return bb;

		}

		/**
		 * Enable the physical radio link between this node and nodeB (if possible). The reserved broadcast ID is not allowed
		 * as parameter.
		 *
		 * @param requestId the request ID
		 * @param nodeB	 end point of the link
		 *
		 * @return a ByteBuffer instance containing the packet
		 */
		public static ByteBuffer newEnablePhysicalLinkPacket(int requestId, long nodeB) {

			ByteBuffer bb = ByteBuffer.allocate(10);
			bb.put(NodeApiMessageType.ENABLE_PHYSICAL_LINK);
			bb.put((byte) requestId);
			bb.putLong(nodeB);
			return bb;

		}

		/**
		 * Disable the physical radio link between this node and nodeB. The reserved broadcast ID is not allowed as
		 * parameter.
		 *
		 * @param requestId the request ID
		 * @param nodeB	 end point of the link
		 *
		 * @return a ByteBuffer instance containing the packet
		 */
		public static ByteBuffer newDisablePhysicalLinkPacket(int requestId, long nodeB) {

			ByteBuffer bb = ByteBuffer.allocate(10);
			bb.put(NodeApiMessageType.DISABLE_PHYSICAL_LINK);
			bb.put((byte) requestId);
			bb.putLong(nodeB);
			return bb;

		}

		public static boolean isLinkControlPacket(ByteBuffer bb) {
			return bb.array().length != 0 &&
					bb.get(0) == NodeApiMessageType.SET_VIRTUAL_LINK ||
					bb.get(0) == NodeApiMessageType.DESTROY_VIRTUAL_LINK ||
					bb.get(0) == NodeApiMessageType.ENABLE_PHYSICAL_LINK ||
					bb.get(0) == NodeApiMessageType.DISABLE_PHYSICAL_LINK ||
					bb.get(0) == NodeApiMessageType.SEND_VIRTUAL_LINK_MESSAGE;
		}

	}

	public static class NetworkDescription {

		/**
		 * Request this Node for a special property value
		 *
		 * @param requestId the request ID
		 * @param property  request the property specified by this value
		 *
		 * @return a ByteBuffer instance containing the packet
		 */
		public static ByteBuffer newGetPropertyValuePacket(int requestId, byte property) {

			ByteBuffer bb = ByteBuffer.allocate(3);
			bb.put(NodeApiMessageType.GET_PROPERTY_VALUE);
			bb.put((byte) requestId);
			bb.put(property);
			return bb;

		}

		/**
		 * Request a neighborhood list from this node
		 *
		 * @param requestId the request ID
		 *
		 * @return a ByteBuffer instance containing the packet
		 */
		public static ByteBuffer newGetNeighborhoodPacket(int requestId) {

			ByteBuffer bb = ByteBuffer.allocate(2);
			bb.put(NodeApiMessageType.GET_NEIGHBORHOOD);
			bb.put((byte) requestId);
			return bb;

		}

		public static boolean isNetworkDescriptionPacket(ByteBuffer bb) {
			return bb.array().length != 0 &&
					bb.get(0) == NodeApiMessageType.GET_PROPERTY_VALUE ||
					bb.get(0) == NodeApiMessageType.GET_NEIGHBORHOOD;
		}

	}

	public static class NodeControl {

		/**
		 * Enables this node. The node is reactivating the radio and start interacting with the environment.
		 *
		 * @param requestId the request ID
		 *
		 * @return a ByteBuffer instance containing the packet
		 */
		public static ByteBuffer newEnableNodePacket(int requestId) {

			ByteBuffer bb = ByteBuffer.allocate(2);
			bb.put(NodeApiMessageType.ENABLE_NODE);
			bb.put((byte) requestId);
			return bb;

		}

		/**
		 * Disable this node. The node does not longer send out messages or interact with the environment (e.g. a mobile node
		 * or via an actuator).
		 *
		 * @param requestId the request ID
		 *
		 * @return a ByteBuffer instance containing the packet
		 */
		public static ByteBuffer newDisableNodePacket(int requestId) {

			ByteBuffer bb = ByteBuffer.allocate(2);
			bb.put(NodeApiMessageType.DISABLE_NODE);
			bb.put((byte) requestId);
			return bb;

		}

		/**
		 * Reset this node in time milliseconds
		 *
		 * @param requestId the request ID
		 * @param time time in milliseconds
		 *
		 * @return a ByteBuffer instance containing the packet
		 */
		public static ByteBuffer newResetNodePacket(int requestId, int time) {

			ByteBuffer bb = ByteBuffer.allocate(6);
			bb.put(NodeApiMessageType.RESET_NODE);
			bb.put((byte) requestId);
			bb.putInt(time);
			return bb;

		}

		/**
		 * Sets the start time of the nodes de.uniluebeck.itm.tr.wisebed.app to in time milliseconds
		 *
		 * @param requestId the request ID
		 * @param time time in milliseconds
		 *
		 * @return a ByteBuffer instance containing the packet
		 */
		public static ByteBuffer newSetStartTimePacket(int requestId, int time) {

			ByteBuffer bb = ByteBuffer.allocate(6);
			bb.put(NodeApiMessageType.SET_START_TIME);
			bb.put((byte) requestId);
			bb.putInt(time);
			return bb;

		}

		/**
		 * Sets a new virtualNodeID. In default virtualID == natural nodeID
		 *
		 * @param requestId	 the request ID
		 * @param virtualNodeID the virtual node ID
		 *
		 * @return a ByteBuffer instance containing the packet
		 */
		public static ByteBuffer newSetVirtualIDPacket(int requestId, long virtualNodeID) {

			ByteBuffer bb = ByteBuffer.allocate(10);
			bb.put(NodeApiMessageType.SET_VIRTUAL_ID);
			bb.put((byte) requestId);
			bb.putLong(virtualNodeID);
			return bb;

		}

		/**
		 * Asks the connected node for its ID. In default virtualID == natural nodeID
		 *
		 * @param requestId the request ID
		 *
		 * @return a ByteBuffer instance containing the packet
		 */
		public static ByteBuffer newGetIDPacket(int requestId) {

			ByteBuffer bb = ByteBuffer.allocate(2);
			bb.put(NodeApiMessageType.GET_ID);
			bb.put((byte) requestId);
			return bb;

		}

		/**
		 * Check if this node is alive.
		 *
		 * @param requestId the request ID
		 *
		 * @return a ByteBuffer instance containing the packet
		 */
		public static ByteBuffer newAreNodesAlivePacket(int requestId) {

			ByteBuffer bb = ByteBuffer.allocate(2);
			bb.put(NodeApiMessageType.IS_NODE_ALIVE);
			bb.put((byte) requestId);
			return bb;

		}

		public static boolean isNodeControlPacket(ByteBuffer bb) {
			return bb.array().length != 0 &&
					bb.get(0) == NodeApiMessageType.ENABLE_NODE ||
					bb.get(0) == NodeApiMessageType.DISABLE_NODE ||
					bb.get(0) == NodeApiMessageType.RESET_NODE ||
					bb.get(0) == NodeApiMessageType.SET_START_TIME ||
					bb.get(0) == NodeApiMessageType.SET_VIRTUAL_ID ||
					bb.get(0) == NodeApiMessageType.IS_NODE_ALIVE;
		}

	}

	public static ByteBuffer buildResponse(ByteBuffer request, byte result, byte[] payload) {
		ByteBuffer response = ByteBuffer.allocate(2 + 1 + payload.length);
		response.put(request.get(0));
		response.put(request.get(1));
		response.put(result);
		response.put(payload, 0, payload.length);
		return response;
	}

}
