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

package de.uniluebeck.itm.tr.runtime.wsnapp;

import de.uniluebeck.itm.gtr.application.TestbedApplication;
import de.uniluebeck.itm.tr.util.Service;

import java.util.Map;
import java.util.Set;


/**
 *
 */
public interface WSNApp extends Service, TestbedApplication {

	/**
	 *
	 */
	public static final String MSG_TYPE_LISTENER_MANAGEMENT = WSNApp.class.getCanonicalName() + "/LISTENER_MANAGEMENT";

	/**
	 *
	 */
	public static final String MSG_TYPE_LISTENER_MESSAGE = WSNApp.class.getCanonicalName() + "/LISTENER_MESSAGE";

	/**
	 *
	 */
	public static final String MSG_TYPE_LISTENER_NOTIFICATION = WSNApp.class.getCanonicalName() + "/LISTENER_NOTIFICATION";

	/**
	 *
	 */
	public static final String MSG_TYPE_OPERATION_INVOCATION_REQUEST =
			WSNApp.class.getCanonicalName() + "/OPERATION_INVOCATION_REQUEST";

	/**
	 *
	 */
	public static final String MSG_TYPE_OPERATION_INVOCATION_RESPONSE =
			WSNApp.class.getCanonicalName() + "/OPERATION_INVOCATION_RESPONSE";

	/**
	 *
	 */
	public static final String MSG_TYPE_OPERATION_INVOCATION_ACK =
			WSNApp.class.getCanonicalName() + "/OPERATION_INVOCATION_ACK";

	/**
	 *
	 */
	public static interface Callback {

		/**
		 *
		 * @param requestStatus
		 */
		void receivedRequestStatus(WSNAppMessages.RequestStatus requestStatus);

		/**
		 *
		 * @param e
		 */
		void failure(Exception e);

	}

	/**
	 *
	 * @param nodeUrns
	 * @param message
	 * @param callback
	 */
	void send(Set<String> nodeUrns, WSNAppMessages.Message message, Callback callback) throws UnknownNodeUrnsException;

	/**
	 *
	 * @param nodeUrns
	 * @param callback
	 */
	void areNodesAlive(Set<String> nodeUrns, Callback callback) throws UnknownNodeUrnsException;

	/**
	 *
	 * @param programs
	 * @param callback
	 */
	void flashPrograms(Map<String, WSNAppMessages.Program> programs, Callback callback) throws UnknownNodeUrnsException;


	/**
	 *
	 * @param nodeUrns
	 * @param callback
	 */
	void resetNodes(Set<String> nodeUrns, Callback callback) throws UnknownNodeUrnsException;

	/**
	 *
	 * @param receiver
	 */
	void addNodeMessageReceiver(WSNNodeMessageReceiver receiver);

	/**
	 *
	 * @param receiver
	 */
	void removeNodeMessageReceiver(WSNNodeMessageReceiver receiver);

	/**
	 *
	 * @param sourceNodeUrn
	 * @param targetNodeUrn
	 * @param callback
	 */
	void setVirtualLink(String sourceNodeUrn, String targetNodeUrn, Callback callback) throws UnknownNodeUrnsException;

	/**
	 *
	 * @param sourceNodeUrn
	 * @param targetNodeUrn
	 * @param callback
	 */
	void destroyVirtualLink(String sourceNodeUrn, String targetNodeUrn, Callback callback) throws UnknownNodeUrnsException;

	/**
	 *
	 * @param nodeUrn
	 * @param callback
	 */
	void disableNode(String nodeUrn, Callback callback) throws UnknownNodeUrnsException;

	/**
	 *
	 * @param nodeUrn
	 * @param callback
	 */
	void enableNode(String nodeUrn, Callback callback) throws UnknownNodeUrnsException;

	/**
	 *
	 * @param nodeUrnA
	 * @param nodeUrnB
	 * @param callback
	 */
	void enablePhysicalLink(String nodeUrnA, String nodeUrnB, Callback callback) throws UnknownNodeUrnsException;

	/**
	 *
	 * @param nodeUrnA
	 * @param nodeUrnB
	 * @param callback
	 */
	void disablePhysicalLink(String nodeUrnA, String nodeUrnB, Callback callback) throws UnknownNodeUrnsException;

}
