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

package de.uniluebeck.itm.gtr.connection;

import de.uniluebeck.itm.tr.util.AbstractListenable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


/**
 * Abstract base class for all connection types. A connection is either incoming, i.e. it was opened by the remote host
 * or is outgoing and thereby opened by the local host.
 * <p/>
 * A connection is, together with the {@link de.uniluebeck.itm.gtr.connection.ServerConnection} the central abstraction
 * for any kind of transport layer that can be used (e.g. TCP, UDP, ...).
 * <p/>
 * Implementations of this interface must implement {@link Object#equals(Object)} and {@link Object#hashCode()} properly
 * so it can be used in {@link java.util.HashMap}s.
 * <p/>
 * {@link de.uniluebeck.itm.gtr.connection.ConnectionListener} instances can register themselves to be notified when the
 * Connection opens or closes.
 */
public abstract class Connection extends AbstractListenable<ConnectionListener> {

	/**
	 * Enum type defining the direction of a {@link de.uniluebeck.itm.gtr.connection.Connection} instance. Incoming
	 * connections were opened by the remote host, outgoing were opened by the local host.
	 */
	public static enum Direction {
		IN, OUT
	}

	/**
	 * Tries to connect to the remote host using the underlay transport layer.
	 *
	 * @throws ConnectionTypeUnavailableException
	 *                     if this instance was created with a type for which no implementation is available
	 * @throws ConnectionInvalidAddressException
	 *                     if the address of the remote host is invalid or the format of the address string is invalid
	 * @throws IOException if any {@link java.io.IOException} occurs in the underlying transport implementation
	 */
	public abstract void connect() throws ConnectionTypeUnavailableException, ConnectionInvalidAddressException, IOException;

	/**
	 * Returns if the connection is currently established.
	 *
	 * @return {@code true} if the connection is currently established, {@code false} otherwise
	 */
	public abstract boolean isConnected();

	/**
	 * Closes the connection to the remote host. Does nothing if already disconnected.
	 */
	public abstract void disconnect();

	/**
	 * Returns the type of the underlying transport for this connection (e.g. TCP, UDP, ...).
	 *
	 * @return the type of the underlying transport for this connection (e.g. TCP, UDP, ...)
	 */
	public abstract String getType();

	/**
	 * The name of the remote host. Returns {@code null} if this is an incoming connection.
	 *
	 * @return the name of the remote host or {@code null} if this is an incoming connection
	 */
	public abstract String getRemoteNodeName();

	/**
	 * Returns the direction of this connection.
	 *
	 * @return the direction of this connection
	 */
	public abstract Direction getDirection();

	/**
	 * Returns the address that was used to create this connection.
	 *
	 * @return the address that was used to create this connection or {@code null} if this is an incoming connection
	 */
	public abstract String getAddress();

	/**
	 * Returns the {@link java.io.InputStream} for this connection over which data from the remote host is received.
	 * Concurrent access from multiple threads on this {@link java.io.InputStream} must be synchronized by the caller
	 * of this method.
	 *
	 * @return the {@link java.io.InputStream} for this connection
	 * @throws IOException if an {@link java.io.IOException} occurs in the underlying transport
	 */
	public abstract InputStream getInputStream() throws IOException;

	/**
	 * Returns the {@link java.io.OutputStream} for this connection over which data can be send to the remote host.
	 * Concurrent access from multiple threads on this {@link java.io.OutputStream} must be synchronized by the caller
	 * of this method.
	 *
	 * @return the {@link java.io.OutputStream} for this connection
	 * @throws IOException if an {@link java.io.IOException} occurs in the underlying transport
	 */
	public abstract OutputStream getOutputStream() throws IOException;

}
