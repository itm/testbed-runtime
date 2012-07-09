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

package de.uniluebeck.itm.tr.iwsn.overlay;

import com.google.inject.Guice;
import com.google.inject.Injector;
import de.uniluebeck.itm.tr.iwsn.overlay.connection.*;
import de.uniluebeck.itm.tr.iwsn.overlay.messaging.MessageTools;
import de.uniluebeck.itm.tr.iwsn.overlay.messaging.Messages;
import de.uniluebeck.itm.tr.iwsn.overlay.messaging.unreliable.UnreliableMessagingService;
import de.uniluebeck.itm.tr.iwsn.overlay.naming.NamingEntry;
import de.uniluebeck.itm.tr.iwsn.overlay.naming.NamingInterface;
import de.uniluebeck.itm.tr.util.ExecutorUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.Assert.*;


public class ConnectivityTests {

	private static final Logger log = LoggerFactory.getLogger(ConnectivityTests.class);

	private static final Random random = new Random();

	private ExecutorService executorService;

	private ConnectionService cs1;

	private ConnectionService cs2;

	private ScheduledExecutorService scheduledExecutorService;

	private class RequestConnectionRunnable implements Runnable {

		private ConnectionService connectionService;

		private String nodeName;

		private RequestConnectionRunnable(ConnectionService connectionService, String nodeName) {
			this.connectionService = connectionService;
			this.nodeName = nodeName;
		}

		@Override
		public void run() {
			try {
				Connection connection = connectionService.getConnection(nodeName);
				assertNotNull(connection);
			} catch (Exception e) {
				e.printStackTrace();
				assertTrue(false);
			}
		}
	}

	private class CloseConnectionRunnable implements Runnable {

		private ConnectionService connectionService;

		private String nodeName;

		private CloseConnectionRunnable(ConnectionService connectionService, String nodeName) {
			this.connectionService = connectionService;
			this.nodeName = nodeName;
		}

		@Override
		public void run() {
			try {
				Connection connection = connectionService.getConnection(nodeName);
				connection.disconnect();
			} catch (Exception e) {
				assertTrue(false);
			}
		}
	}

	private TestbedRuntime gw1;

	private TestbedRuntime gw2;

	private ServerConnection sc1;

	private ServerConnection sc2;

	@Before
	public void setUp() throws Exception {

		scheduledExecutorService = Executors.newScheduledThreadPool(2);

		Injector gw1Injector = Guice.createInjector(
				new TestbedRuntimeModule(scheduledExecutorService, scheduledExecutorService, scheduledExecutorService)
		);
		gw1 = gw1Injector.getInstance(TestbedRuntime.class);
		gw1.getLocalNodeNameManager().addLocalNodeName("gw1");

		Injector gw2Injector = Guice.createInjector(
				new TestbedRuntimeModule(scheduledExecutorService, scheduledExecutorService, scheduledExecutorService)
		);
		gw2 = gw2Injector.getInstance(TestbedRuntime.class);
		gw2.getLocalNodeNameManager().addLocalNodeName("gw2");

		// configure topology on both nodes
		gw1.getRoutingTableService().setNextHop("gw2", "gw2");
		gw1.getNamingService().addEntry(new NamingEntry("gw2", new NamingInterface("tcp", "localhost:2220"), 1));

		gw2.getRoutingTableService().setNextHop("gw1", "gw1");
		gw2.getNamingService().addEntry(new NamingEntry("gw1", new NamingInterface("tcp", "localhost:1110"), 1));

		// start both nodes' stack
		gw1.start();
		gw2.start();

		cs1 = gw1.getConnectionService();
		cs2 = gw2.getConnectionService();

		sc1 = createServerConnection(cs1, "tcp", "localhost:1110");
		sc2 = createServerConnection(cs2, "tcp", "localhost:2220");

		sc1.bind();
		sc2.bind();

		executorService = Executors.newFixedThreadPool(5);

	}

	private ServerConnection createServerConnection(ConnectionService connectionService, String type, String address)
			throws ConnectionTypeUnavailableException, IOException, ConnectionInvalidAddressException {

		ServerConnection serverConnection = connectionService.getServerConnection(type, address);
		serverConnection.addListener(new ServerConnectionListener() {
			@Override
			public void serverConnectionOpened(ServerConnection serverConnection) {
				log.debug("ConnectivityTests.serverConnectionOpened");
			}

			@Override
			public void serverConnectionClosed(ServerConnection serverConnection) {
				log.debug("ConnectivityTests.serverConnectionClosed");
			}

			@Override
			public void connectionEstablished(ServerConnection serverConnection, Connection connection) {
				log.debug("ConnectivityTests.connectionEstablished");
			}
		}
		);
		return serverConnection;

	}

	@After
	public void tearDown() {

		gw1.stop();
		gw2.stop();

		ExecutorUtils.shutdown(scheduledExecutorService, 0, TimeUnit.SECONDS);

	}

	@Test
	public void testConcurrentUnstableConnectivity() throws Exception {

		// parallel threads, some opening and some closing one of the two same connections
		int cnt = 500;
		boolean odd;

		List<Future<?>> futures = new ArrayList<Future<?>>(cnt);

		// 5 parallel threads running 500 jobs, each only requesting the connection
		for (int i = 0; i < cnt; i++) {

			odd = i % 2 == 0;

			if (random.nextInt(100) >= 5) {
				futures.add(executorService.submit(new RequestConnectionRunnable(
						odd ? cs1 : cs2,
						odd ? "gw2" : "gw1"
				)
				)
				);
			} else {
				futures.add(executorService.submit(new CloseConnectionRunnable(
						odd ? cs1 : cs2,
						odd ? "gw2" : "gw1"
				)
				)
				);
			}

		}

		for (Future<?> future : futures) {
			assertSame(null, future.get());
		}

	}

	@Test
	public void testConcurrentConnectivity() throws Exception {

		int cnt = 500;
		boolean odd;

		List<Future<?>> futures = new ArrayList<Future<?>>(cnt);

		// 5 parallel threads running 500 jobs, each only requesting the connection
		for (int i = 0; i < cnt; i++) {

			odd = i % 2 == 0;

			futures.add(executorService.submit(new RequestConnectionRunnable(
					odd ? cs1 : cs2,
					odd ? "gw2" : "gw1"
			)
			)
			);
		}

		for (Future<?> future : futures) {
			Object futureObj = future.get();
			if (futureObj instanceof Exception) {
				throw (Exception) futureObj;
			} else {
				assertSame(null, future.get());
			}
		}

	}

	@Test
	public void testConnectivity() throws Exception {

		for (int i = 0; i < 500; i++) {

			if (i % 2 == 0) {
				cs1.getConnection("gw2");
			} else {
				cs2.getConnection("gw1");
			}

			Thread.sleep(random.nextInt(5));
		}

	}

	@Test
	public void testDataStream() throws Exception {

		int cnt = 1000;

		// need to use an array, so it can be final to be able to reference it from the listener below (whacky hacky)
		final Connection[] gw2connection = new Connection[1];
		sc2.addListener(new ServerConnectionListener() {
			@Override
			public void serverConnectionOpened(ServerConnection serverConnection) {
			}

			@Override
			public void serverConnectionClosed(ServerConnection serverConnection) {
			}

			@Override
			public void connectionEstablished(ServerConnection serverConnection, Connection connection) {
				gw2connection[0] = connection;
			}
		}
		);

		final Connection gw1connection = cs1.getConnection("gw2");
		final Random random = new Random();

		final Map<Integer, byte[]> messsagesSent = Collections.synchronizedMap(new HashMap<Integer, byte[]>(cnt));
		final Map<Integer, byte[]> messsagesReceived = Collections.synchronizedMap(new HashMap<Integer, byte[]>(cnt));

		ExecutorService readerExecutor = Executors.newFixedThreadPool(5);
		ExecutorService writerExecutor = Executors.newFixedThreadPool(5);

		List<Future> readerFutures = new LinkedList<Future>();
		List<Future> writerFutures = new LinkedList<Future>();

		synchronized (this) {
			Thread.sleep(100);
		}

		for (int i = 0; i < cnt; i++) {

			final int msgNr = i;

			Callable<byte[]> readerCallable = new Callable<byte[]>() {
				@Override
				public byte[] call() throws Exception {
					try {

						synchronized (gw2connection) {

							log.debug("Getting input stream for connection");
							InputStream in = gw2connection[0].getInputStream();

							ByteBuffer msgLengthBuffer = ByteBuffer.allocate(4);
							//noinspection ResultOfMethodCallIgnored
							in.read(msgLengthBuffer.array());
							int msgLength = msgLengthBuffer.getInt();
							log.debug("= Read message length of {}", msgLength);

							ByteBuffer msgNrBuffer = ByteBuffer.allocate(4);
							//noinspection ResultOfMethodCallIgnored
							in.read(msgNrBuffer.array());
							int msgNr = msgNrBuffer.getInt();
							log.debug("= Read message nr of {}", msgNr);

							ByteBuffer msgBuffer = ByteBuffer.allocate(msgLength);
							//noinspection ResultOfMethodCallIgnored
							in.read(msgBuffer.array());
							log.debug("Read message #{} with content:", msgNr, msgBuffer.array());

							messsagesReceived.put(msgNr, msgBuffer.array());

							return msgBuffer.array();

						}

					} catch (IOException e) {
						e.printStackTrace();
						assertTrue(false);
					}

					return null;

				}
			};

			Callable<byte[]> writerCallable = new Callable<byte[]>() {
				@Override
				public byte[] call() {

					try {

						synchronized (gw1connection) {

							log.debug("=== Opening output stream for connection");

							OutputStream out = gw1connection.getOutputStream();

							int msgLength = random.nextInt(127);

							ByteBuffer buffer = ByteBuffer.allocate(msgLength + 4 + 4);

							log.debug("* Writing message #{} of length {}", msgNr, msgLength);
							buffer.putInt(msgLength);
							buffer.putInt(msgNr);

							byte[] payload = new byte[msgLength];
							random.nextBytes(payload);

							buffer.put(payload);
							out.write(buffer.array());
							out.flush();

							messsagesSent.put(msgNr, payload);

							return payload;

						}

					} catch (IOException e) {
						e.printStackTrace();
						assertTrue(false);
					}

					return null;

				}
			};

			writerFutures.add(writerExecutor.submit(writerCallable));
			readerFutures.add(readerExecutor.submit(readerCallable));

		}

		log.debug("Waiting for writers to complete...");
		for (Future future : writerFutures) {
			assertNotNull(future.get());
		}
		log.debug("Writers completed!");

		log.debug("Waiting for readers to complete...");
		for (Future future : readerFutures) {
			assertNotNull(future.get());
		}
		log.debug("Readers completed!");

		for (Map.Entry<Integer, byte[]> entry : messsagesSent.entrySet()) {

			byte[] sent = entry.getValue();
			byte[] received = messsagesReceived.get(entry.getKey());

			assertNotNull(sent);
			assertNotNull(received);
			assertArrayEquals(sent, received);

		}

	}

	@Test
	public void testProtobufDelimited() throws Exception {

		final int cnt = 1000;

		// need to use an array, so it can be final to be able to reference it from the listener below (whacky hacky)
		final Connection[] gw2connection = new Connection[1];
		sc2.addListener(new ServerConnectionListener() {
			@Override
			public void serverConnectionOpened(ServerConnection serverConnection) {
			}

			@Override
			public void serverConnectionClosed(ServerConnection serverConnection) {
			}

			@Override
			public void connectionEstablished(ServerConnection serverConnection, Connection connection) {
				gw2connection[0] = connection;
			}
		}
		);

		final Connection gw1connection = cs1.getConnection("gw2");

		final Map<String, Messages.Msg> messagesSent =
				Collections.synchronizedMap(new HashMap<String, Messages.Msg>(cnt));
		final Map<String, Messages.Msg> messagesReceived =
				Collections.synchronizedMap(new HashMap<String, Messages.Msg>(cnt));

		ExecutorService readerExecutor = Executors.newFixedThreadPool(5);
		ExecutorService writerExecutor = Executors.newFixedThreadPool(5);

		List<Future> readerFutures = new LinkedList<Future>();
		List<Future> writerFutures = new LinkedList<Future>();

		synchronized (this) {
			Thread.sleep(100);
		}

		assertNotNull(gw2connection[0]);

		Callable<Void> readerCallable = new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				try {

					synchronized (gw2connection) {

						log.debug("Getting input stream for connection");
						InputStream in = gw2connection[0].getInputStream();
						int received = 0;

						while (!Thread.interrupted() && gw2connection[0].isConnected() && received < cnt) {
							received++;
							Messages.Msg.Builder builder = Messages.Msg.newBuilder();
							builder.mergeDelimitedFrom(in);
							Messages.Msg msg = builder.build();
							messagesReceived.put((String) MessageTools.getPayloadSerializable(msg), msg);
							log.debug("Received message #{}", MessageTools.getPayloadSerializable(msg));
						}

					}

				} catch (IOException e) {
					e.printStackTrace();
					assertTrue(false);
				}

				return null;

			}
		};


		for (int i = 0; i < cnt; i++) {

			final int msgNr = i;

			Callable<Messages.Msg> writerCallable = new Callable<Messages.Msg>() {
				@Override
				public Messages.Msg call() {

					try {

						synchronized (gw1connection) {


							OutputStream out = gw1connection.getOutputStream();

							Messages.Msg msg = MessageTools.buildMessage(
									"gw1",
									"gw2",
									"test",
									"" + msgNr,
									UnreliableMessagingService.PRIORITY_NORMAL
							);

							msg.writeDelimitedTo(out);
							log.debug("Wrote message #{}", msgNr);

							messagesSent.put((String) MessageTools.getPayloadSerializable(msg), msg);

							return msg;

						}

					} catch (IOException e) {
						e.printStackTrace();
						assertTrue(false);
					}

					return null;

				}
			};

			writerFutures.add(writerExecutor.submit(writerCallable));

		}

		readerFutures.add(readerExecutor.submit(readerCallable));

		log.debug("Waiting for writers to complete...");
		for (Future future : writerFutures) {
			assertNotNull(future.get());
		}
		log.debug("Writers completed!");

		log.debug("Waiting for readers to complete...");
		for (Future future : readerFutures) {
			assertNull(future.get());
		}
		log.debug("Readers completed!");

		for (Map.Entry<String, Messages.Msg> entry : messagesSent.entrySet()) {

			Messages.Msg sent = entry.getValue();
			Messages.Msg received = messagesReceived.get(entry.getKey());

			assertNotNull(sent);
			assertNotNull(received);

			assertEquals(sent.getFrom(), received.getFrom());
			assertEquals(sent.getPayload(), received.getPayload());
			assertEquals(sent.getMsgType(), received.getMsgType());
			assertEquals(sent.getPriority(), received.getPriority());
			assertEquals(sent.getReplyTo(), received.getReplyTo());
			assertEquals(sent.getReplyWith(), received.getReplyWith());
			assertEquals(sent.getTo(), received.getTo());

		}

	}
}
