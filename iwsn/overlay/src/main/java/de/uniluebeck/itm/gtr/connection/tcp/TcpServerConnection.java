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

package de.uniluebeck.itm.gtr.connection.tcp;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import de.uniluebeck.itm.gtr.connection.Connection;
import de.uniluebeck.itm.gtr.connection.ConnectionInvalidAddressException;
import de.uniluebeck.itm.gtr.connection.ServerConnection;
import de.uniluebeck.itm.gtr.connection.ServerConnectionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


public class TcpServerConnection extends ServerConnection {

    private static final Logger log = LoggerFactory.getLogger(TcpServerConnection.class);

    private Runnable acceptRunnable = new Runnable() {

        public void run() {

            while (!Thread.interrupted()) {
                try {

                    // listeners will track the connection...
                    Socket socket = serverSocket.accept();
                    InetSocketAddress remoteSocketAddress = (InetSocketAddress) socket.getRemoteSocketAddress();

                    TcpConnection connection = new TcpConnection(
                            null,
                            Connection.Direction.IN,
                            remoteSocketAddress.getHostName(),
                            remoteSocketAddress.getPort()
                    );
                    connection.setSocket(socket);

                    for (ServerConnectionListener listener : listeners) {
                        listener.connectionEstablished(TcpServerConnection.this, connection);
                    }

                } catch (IOException e) {
                    // ignore
                }
            }

        }
    };

    private final ExecutorService executor = Executors.newCachedThreadPool(
            new ThreadFactoryBuilder().setNameFormat("TcpServerConnection-Thread %d").build()
    );

    private ServerSocket serverSocket;

    private InetSocketAddress socketAddress;

    private Future<?> acceptThreadFuture;

    public TcpServerConnection(String hostName, int port) {
        this.socketAddress = new InetSocketAddress(hostName, port);
    }

    public void bind() throws IOException, ConnectionInvalidAddressException {

        if (serverSocket != null && serverSocket.isBound()) {
            return;
        }

        if (serverSocket == null) {
            try {

                serverSocket = new ServerSocket();
                serverSocket.bind(socketAddress);
                log.debug("Bound overlay server socket on {}:{}.", socketAddress.getHostName(), socketAddress.getPort());

            } catch (IOException e) {
                throw new ConnectionInvalidAddressException(getAddress(), "Failed to bind ServerSocket", e);
            }
        } else if (!serverSocket.isBound()) {

            serverSocket.bind(socketAddress);

        }

        acceptThreadFuture = executor.submit(acceptRunnable);
        postEvent(true);

    }

    private void postEvent(final boolean connected) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                for (ServerConnectionListener listener : listeners) {
                    if (connected)
                        listener.serverConnectionOpened(TcpServerConnection.this);
                    else
                        listener.serverConnectionClosed(TcpServerConnection.this);
                }
            }
        });
    }

    public void unbind() {

        // acceptThread will close the socket and inform listeners about it
        acceptThreadFuture.cancel(true);

        try {

            serverSocket.close();
			log.debug("Unbound overlay server socket from {}:{}.", socketAddress.getHostName(), socketAddress.getPort());

		} catch (IOException e) {
			log.debug("IOException while closing server socket.", e);
		} finally {
            postEvent(false);
        }

    }

    public boolean isBound() {
        return serverSocket != null && serverSocket.isBound();
    }

    public String getAddress() {
        return socketAddress.getHostName() + ":" + socketAddress.getPort();
    }

    @Override
    public String getType() {
        return TcpConstants.TYPE;
    }

    @Override
    public String toString() {
        return "TcpServerConnection{" +
                "socketAddress=" + socketAddress +
                '}';
    }
}
