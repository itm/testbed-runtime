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

package de.uniluebeck.itm.gtr.messaging.server;

import de.uniluebeck.itm.tr.util.Service;


/**
 * A service that allows to open {@link de.uniluebeck.itm.gtr.connection.ServerConnection}s and listens for incoming
 * messages. If a message is received it is then dispatched to all {@link de.uniluebeck.itm.gtr.messaging.event.MessageEventListener}
 * instances that registered with the {@link de.uniluebeck.itm.gtr.messaging.event.MessageEventService}.
 */
public interface MessageServerService extends Service {

	/**
	 * Creates a {@link de.uniluebeck.itm.gtr.connection.ServerConnection} of type {@code type} with the address {@code
	 * address} and starts to listen for incoming messages on the {@link de.uniluebeck.itm.gtr.connection.ServerConnection}s
	 * {@link java.io.InputStream} instance. The connection will be established immediately or after {@link
	 * de.uniluebeck.itm.tr.util.Service#start()} was invoked on this service.
	 *
	 * @param type	the type of the {@link de.uniluebeck.itm.gtr.connection.ServerConnection} instance
	 * @param address the address of the {@link de.uniluebeck.itm.gtr.connection.ServerConnection} instance
	 */
	void addMessageServer(String type, String address, MessageFilter... filters);

	/**
	 * Stops to listen for incoming messages on the {@link de.uniluebeck.itm.gtr.connection.ServerConnection}s {@link
	 * java.io.InputStream} instance that corresponds to type {@code type} with the address {@code address}. The
	 * connection will be close immediately if it is currently open.
	 *
	 * @param type	the type of the {@link de.uniluebeck.itm.gtr.connection.ServerConnection} instance
	 * @param address the address of the {@link de.uniluebeck.itm.gtr.connection.ServerConnection} instance
	 */
	void removeMessageServer(String type, String address);

}
