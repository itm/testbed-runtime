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

package de.uniluebeck.itm.tr.nodeapi;


public interface NodeControl {

	/**
	 * Enables this node. The node is reactivating the radio and start
	 * interacting with the environment.
	 */
	void enableNode(NodeApiCallback callback);

	/**
	 * Disable this node. The node does not longer send out messages
	 * or interact with the environment (e.g. a mobile node or via an actuator).
	 */
	void disableNode(NodeApiCallback callback);

	/**
	 * Reset this node in time milliseconds
	 *
	 * @param time
	 */
	void resetNode(int time, NodeApiCallback callback);

	/**
	 * Sets the starttime of the nodes de.uniluebeck.itm.tr.wisebed.app to in time milliseconds
	 *
	 * @param time
	 */
	void setStartTime(int time, NodeApiCallback callback);

	/**
	 * Sets a new virtualNodeID. In default virtualID == natural nodeID
	 *
	 * @param virtualNodeID
	 */
	void setVirtualID(long virtualNodeID, NodeApiCallback callback);

	/**
	 * Asks the connected node for its ID. In default virtualID == natural nodeID
	 */
	void getID(NodeApiCallback callback);

	/**
	 * Check if this node is alive.
	 */
	void areNodesAlive(NodeApiCallback callback);

}