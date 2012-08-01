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

package de.uniluebeck.itm.tr.iwsn.overlay.connection.tcp;

import com.google.common.eventbus.EventBus;
import de.uniluebeck.itm.tr.iwsn.overlay.connection.Connection;
import de.uniluebeck.itm.tr.iwsn.overlay.connection.ConnectionClosedEvent;
import de.uniluebeck.itm.tr.iwsn.overlay.connection.ConnectionInvalidAddressException;
import de.uniluebeck.itm.tr.iwsn.overlay.connection.ConnectionOpenedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;


public class TcpConnection extends Connection {

	private static final Logger log = LoggerFactory.getLogger(TcpConnection.class);

	protected Socket socket;

	private InetSocketAddress socketAddress;

	private String nodeName;

	private Direction direction;

	private final EventBus eventBus;

	public TcpConnection(@Nullable final String nodeName,
						 @Nonnull final Direction direction,
						 @Nonnull final String hostName,
						 final int port,
						 @Nonnull final EventBus eventBus) {

		switch (direction) {
			case IN:
				checkArgument(nodeName == null);
				break;
			case OUT:
				checkNotNull(nodeName);
				break;
		}

		checkNotNull(eventBus);
		checkNotNull(hostName);

		this.nodeName = nodeName;
		this.direction = direction;
		this.eventBus = eventBus;

		this.socketAddress = new InetSocketAddress(hostName, port);
	}

	public void connect() throws ConnectionInvalidAddressException, IOException {

		if (socket != null && !socket.isClosed()) {
			return;
		}

		socket = new Socket();
		socket.setKeepAlive(true);
		socket.connect(socketAddress);

		eventBus.post(new ConnectionOpenedEvent(this));
	}

	public void disconnect() {

		try {

			if (socket != null && !socket.isClosed()) {
				socket.close();
				eventBus.post(new ConnectionClosedEvent(this));
			}

		} catch (IOException e) {
			// simply ignore as this connection is closed anyway and will not be used anymore
			log.debug("IOException while closing TcpConnection", e);
		}

	}

	public String getAddress() {
		return socketAddress.getHostName() + ":" + socketAddress.getPort();
	}

	public InputStream getInputStream() throws IOException {
		return socket == null ? null : socket.getInputStream();
	}

	public String getRemoteNodeName() {
		return nodeName;
	}

	public OutputStream getOutputStream() throws IOException {
		return socket == null ? null : socket.getOutputStream();
	}

	public String getType() {
		return TcpConstants.TYPE;
	}

	public Direction getDirection() {
		return direction;
	}

	public boolean isConnected() {
		return socket != null && socket.isConnected() && !socket.isClosed();
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("TcpConnection");
		sb.append("{socket=").append(socket);
		sb.append(", socketAddress=").append(socketAddress);
		sb.append(", nodeName='").append(nodeName).append('\'');
		sb.append(", direction=").append(direction);
		sb.append('}');
		return sb.toString();
	}

	void setSocket(Socket socket) {

		this.socket = socket;

		if (socket.isConnected() && !socket.isClosed()) {
			eventBus.post(new ConnectionOpenedEvent(this));
		}
	}

	@Override
	public boolean equals(Object o) {

		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		TcpConnection that = (TcpConnection) o;

		return socketAddress.equals(that.socketAddress);

	}

	@Override
	public int hashCode() {
		return socketAddress.hashCode();
	}
}
