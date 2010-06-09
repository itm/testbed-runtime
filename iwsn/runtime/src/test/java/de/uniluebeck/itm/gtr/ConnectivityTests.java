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

package de.uniluebeck.itm.gtr;

import com.google.inject.Guice;
import com.google.inject.Injector;
import de.uniluebeck.itm.gtr.common.CommonModule;
import de.uniluebeck.itm.gtr.common.SchedulerService;
import de.uniluebeck.itm.gtr.connection.*;
import de.uniluebeck.itm.gtr.messaging.MessageTools;
import de.uniluebeck.itm.gtr.messaging.Messages;
import de.uniluebeck.itm.gtr.naming.NamingEntry;
import de.uniluebeck.itm.gtr.naming.NamingInterface;
import de.uniluebeck.itm.gtr.naming.NamingModule;
import de.uniluebeck.itm.gtr.naming.NamingService;
import de.uniluebeck.itm.gtr.routing.RoutingModule;
import de.uniluebeck.itm.gtr.routing.RoutingTableService;
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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.*;


public class ConnectivityTests {

	private static final Logger log = LoggerFactory.getLogger(ConnectivityTests.class);

	private static final Random random = new Random();

	private ExecutorService executorService;

	private ConnectionService cs1;

	private ConnectionService cs2;

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

		Injector gw1Injector = Guice.createInjector(
				new CommonModule(),
				new RoutingModule(),
				new NamingModule(),
				new ConnectionModule(),
				new TestbedRuntimeModule(
						new String[]{"gw1"},
						SchedulerService.class,
						RoutingTableService.class,
						NamingService.class,
						ConnectionService.class
				)
		);
		gw1 = gw1Injector.getInstance(TestbedRuntime.class);

		Injector gw2Injector = Guice.createInjector(
				new CommonModule(),
				new RoutingModule(),
				new NamingModule(),
				new ConnectionModule(),
				new TestbedRuntimeModule(
						new String[]{"gw2"},
						SchedulerService.class,
						RoutingTableService.class,
						NamingService.class,
						ConnectionService.class
				)
		);
		gw2 = gw2Injector.getInstance(TestbedRuntime.class);

		// configure topology on both nodes
		gw1.getRoutingTableService().setNextHop("gw2", "gw2");
		gw1.getNamingService().addEntry(new NamingEntry("gw2", new NamingInterface("tcp", "localhost:2220"), 1));

		gw2.getRoutingTableService().setNextHop("gw1", "gw1");
		gw2.getNamingService().addEntry(new NamingEntry("gw1", new NamingInterface("tcp", "localhost:1110"), 1));

		// start both nodes' stack
		gw1.startServices();
		gw2.startServices();

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

		gw1.stopServices();
		gw2.stopServices();

	}

	@Test
	public void testConcurrentInstableConnectivity() throws Exception {

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

		final Map<String, Messages.Msg> messsagesSent =
				Collections.synchronizedMap(new HashMap<String, Messages.Msg>(cnt));
		final Map<String, Messages.Msg> messsagesReceived =
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
							messsagesReceived.put((String) MessageTools.getPayloadSerializable(msg), msg);
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

							Messages.Msg msg = MessageTools.buildMessage("gw1", "gw2", "test", "" + msgNr, 1,
									System.currentTimeMillis() + 5000
							);
							msg.writeDelimitedTo(out);
							log.debug("Wrote message #{}", msgNr);

							messsagesSent.put((String) MessageTools.getPayloadSerializable(msg), msg);

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

		for (Map.Entry<String, Messages.Msg> entry : messsagesSent.entrySet()) {

			Messages.Msg sent = entry.getValue();
			Messages.Msg recv = messsagesReceived.get(entry.getKey());

			assertNotNull(sent);
			assertNotNull(recv);

			assertEquals(sent.getFrom(), recv.getFrom());
			assertEquals(sent.getPayload(), recv.getPayload());
			assertEquals(sent.getMsgType(), recv.getMsgType());
			assertEquals(sent.getPriority(), recv.getPriority());
			assertEquals(sent.getReplyTo(), recv.getReplyTo());
			assertEquals(sent.getReplyWith(), recv.getReplyWith());
			assertEquals(sent.getTo(), recv.getTo());
			assertEquals(sent.getValidUntil(), recv.getValidUntil());

		}

	}

//	protected static final byte DLE = 0x10;
//
//	protected static final byte STX = 0x02;
//
//	protected static final byte ETX = 0x03;
//
//	protected static final byte[] DLE_STX = new byte[]{DLE, STX};
//
//	protected static final byte[] DLE_ETX = new byte[]{DLE, ETX};
//
//	@Test
//	public void testDataStreamDLESTXETX() throws Exception {
//
//		int cnt = 100;
//
//		// need to use an array, so it can be final to be able to reference it from the listener below (whacky hacky)
//		final Connection[] gw2connection = new Connection[1];
//		sc2.addListener(new ServerConnectionListener() {
//			@Override
//			public void serverConnectionOpened(ServerConnection serverConnection) {
//			}
//
//			@Override
//			public void serverConnectionClosed(ServerConnection serverConnection) {
//			}
//
//			@Override
//			public void connectionEstablished(ServerConnection serverConnection, Connection connection) {
//				gw2connection[0] = connection;
//			}
//		}
//		);
//
//		final Connection gw1connection = cs1.getConnection("gw2");
//		final Random random = new Random();
//
//		final Map<Integer, byte[]> messsagesSent = Collections.synchronizedMap(new HashMap<Integer, byte[]>(cnt));
//		final Map<Integer, byte[]> messsagesReceived = Collections.synchronizedMap(new HashMap<Integer, byte[]>(cnt));
//
//		ExecutorService readerExecutor = Executors.newFixedThreadPool(5);
//		ExecutorService writerExecutor = Executors.newFixedThreadPool(5);
//
//		List<Future> readerFutures = new LinkedList<Future>();
//		List<Future> writerFutures = new LinkedList<Future>();
//
//		synchronized (this) {
//			Thread.sleep(100);
//		}
//
//		Runnable readerRunnable = new Runnable() {
//
//			private boolean foundDLE;
//
//			private boolean foundPacket;
//
//			private byte[] packet = new byte[2048];
//
//			private int packetLength = 0;
//
//			@Override
//			@SuppressWarnings({"ConstantConditions"})
//			public void run() {
//
//				log.debug("Reader started");
//
//				synchronized (gw2connection[0]) {
//
//
//					try {
//
//						InputStream in = gw2connection[0].getInputStream();
//
//						log.debug("Got InputStream, starting to read...");
//
//						while (in != null && in.available() > 0 && !Thread.interrupted()) {
//
//							byte c = (byte) (0xff & in.read());
//
//							// Check if DLE was found
//							if (foundDLE) {
//								foundDLE = false;
//
//								if ((c == STX) && !this.foundPacket) {
//
//									log.trace("STX received in DLE mode");
//									foundPacket = true;
//
//								} else if ((c == ETX) && this.foundPacket) {
//
//									log.trace("ETX received in DLE mode");
//
//									byte[] payload = new byte[packetLength];
//									System.arraycopy(packet, 0, payload, 0, packetLength);
//
//									ByteBuffer msgNrBuffer = ByteBuffer.allocate(4);
//									System.arraycopy(payload, 0, msgNrBuffer.array(), 0, 3);
//									int msgNr = msgNrBuffer.getInt();
//
//									ByteBuffer msgBuffer = ByteBuffer.allocate(payload.length - 4);
//									System.arraycopy(payload, 4, msgBuffer.array(), 0, payload.length - 4);
//
//									log.debug("= Reading message #{} of length {}", msgNr, payload.length - 4);
//
//									messsagesReceived.put(msgNr, msgBuffer.array());
//
//									clearPacket();
//
//								} else if ((c == DLE) && this.foundPacket) {
//
//									// Stuffed DLE found
//									log.trace("Stuffed DLE received in DLE mode");
//
//									ensureBufferSize();
//									this.packet[this.packetLength++] = DLE;
//
//								} else {
//									log.trace("Incomplete packet received: " + packet.toString());
//									clearPacket();
//								}
//
//							} else {
//								if (c == DLE) {
//									log.trace("Plain DLE received");
//									foundDLE = true;
//								} else if (this.foundPacket) {
//									this.packet[this.packetLength++] = c;
//								}
//							}
//						}
//
//					} catch (final IOException error) {
//						log.trace("Error on rx (Retry in 1s): " + error);
//					}
//
//				}
//
//				log.debug("Reader stopped");
//
//			}
//
//			protected void ensureBufferSize() {
//				if (this.packetLength + 1 >= this.packet.length) {
//					final byte tmp[] = new byte[this.packetLength + 100];
//					System.arraycopy(this.packet, 0, tmp, 0, this.packetLength);
//					this.packet = tmp;
//				}
//			}
//
//			protected void clearPacket() {
//				this.packetLength = 0;
//				this.foundDLE = false;
//				this.foundPacket = false;
//			}
//
//		};
//
//		for (int i = 0; i < cnt; i++) {
//
//			final int msgNr = i;
//
//			Callable<byte[]> writerCallable = new Callable<byte[]>() {
//				@Override
//				public byte[] call() {
//
//					try {
//
//						synchronized (gw1connection) {
//
//							OutputStream out = gw1connection.getOutputStream();
//
//							out.write(DLE_STX);
//
//							int msgLength = random.nextInt(123);
//
//							ByteBuffer buffer = ByteBuffer.allocate(4);
//							buffer.putInt(msgNr);
//							out.write(buffer.array());
//
//							byte[] payload = new byte[msgLength];
//							random.nextBytes(payload);
//							out.write(payload);
//
//							out.write(DLE_ETX);
//
//							log.debug("* Writing message #{} of length {}", msgNr, msgLength);
//
//							messsagesSent.put(msgNr, payload);
//
//							return payload;
//
//						}
//
//					} catch (IOException e) {
//						e.printStackTrace();
//						assertTrue(false);
//					}
//
//					return null;
//
//				}
//			};
//
//			writerFutures.add(writerExecutor.submit(writerCallable));
//
//		}
//
//		readerFutures.add(readerExecutor.submit(readerRunnable));
//
//		log.debug("Waiting for writers to complete...");
//		for (Future future : writerFutures) {
//			assertNotNull(future.get());
//		}
//		log.debug("Writers completed!");
//
//		log.debug("Waiting for readers to complete...");
//		for (Future future : readerFutures) {
//			assertNull(future.get());
//		}
//		log.debug("Readers completed!");
//
//		for (Map.Entry<Integer, byte[]> entry : messsagesSent.entrySet()) {
//
//			byte[] sent = entry.getValue();
//			byte[] received = messsagesReceived.get(entry.getKey());
//
//			assertNotNull(sent);
//			assertNotNull(received);
//			assertArrayEquals(sent, received);
//
//		}
//
//	}

}
