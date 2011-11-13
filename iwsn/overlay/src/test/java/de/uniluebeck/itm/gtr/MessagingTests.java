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
import de.uniluebeck.itm.gtr.messaging.MessageTools;
import de.uniluebeck.itm.gtr.messaging.Messages;
import de.uniluebeck.itm.gtr.messaging.event.MessageEventAdapter;
import de.uniluebeck.itm.gtr.messaging.event.MessageEventListener;
import de.uniluebeck.itm.gtr.messaging.event.MessageEventService;
import de.uniluebeck.itm.gtr.messaging.reliable.ReliableMessagingService;
import de.uniluebeck.itm.gtr.messaging.reliable.ReliableMessagingTimeoutException;
import de.uniluebeck.itm.gtr.messaging.unreliable.UnreliableMessagingService;
import de.uniluebeck.itm.gtr.naming.NamingEntry;
import de.uniluebeck.itm.gtr.naming.NamingInterface;
import de.uniluebeck.itm.tr.util.ExecutorUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.Assert.*;


public class MessagingTests {

	private static final Logger log = LoggerFactory.getLogger(MessagingTests.class);

	private TestbedRuntime gw1;

	private TestbedRuntime gw2;

	private UnreliableMessagingService ums1;

	private UnreliableMessagingService ums2;

	private ReliableMessagingService rms1;

	private MessageEventService mes1;

	private MessageEventService mes2;

	private ExecutorService executorService;

	private ScheduledExecutorService scheduledExecutorService;

	private static class UnreliableMessageRunnable implements Runnable {

		private UnreliableMessagingService messageService;

		private Messages.Msg message;

		private UnreliableMessageRunnable(UnreliableMessagingService messageService, Messages.Msg msg) {
			this.messageService = messageService;
			this.message = msg;
		}

		@Override
		public void run() {
			try {
				messageService.sendAsync(this.message);
			} catch (Exception e) {
				e.printStackTrace();
				assertTrue(false);
			}
		}
	}

	@Before
	public void setUp() throws Exception {

		log.debug("================== Setting up... ==================");

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

		gw1.getMessageServerService().addMessageServer("tcp", "localhost:1110");
		gw2.getMessageServerService().addMessageServer("tcp", "localhost:2220");

		// start both nodes' services
		gw1.start();
		gw2.start();

		ums1 = gw1.getUnreliableMessagingService();
		ums2 = gw2.getUnreliableMessagingService();

		rms1 = gw1.getReliableMessagingService();

		mes1 = gw1.getMessageEventService();
		mes2 = gw2.getMessageEventService();

		MessageEventListener messageEventListener = new MessageEventListener() {
			@Override
			public void messageSent(Messages.Msg msg) {
				log.info("SENT: {} -> {}: {}",
						new Object[]{msg.getFrom(), msg.getTo(), MessageTools.getPayloadSerializable(msg)}
				);
			}

			@Override
			public void messageDropped(Messages.Msg msg) {
				log.info("DROP: {} -> {}: {}",
						new Object[]{msg.getFrom(), msg.getTo(), MessageTools.getPayloadSerializable(msg)}
				);
			}

			@Override
			public void messageReceived(Messages.Msg msg) {
				log.info("RECV: {} -> {}: {}",
						new Object[]{msg.getFrom(), msg.getTo(), MessageTools.getPayloadSerializable(msg)}
				);
			}
		};

		mes1.addListener(messageEventListener);
		mes2.addListener(messageEventListener);

	}

	@After
	public void tearDown() {

		gw1.stop();
		gw2.stop();

		executorService = null;
		gw1 = gw2 = null;
		mes1 = mes2 = null;
		rms1 = null;
		ums1 = ums2 = null;

		ExecutorUtils.shutdown(scheduledExecutorService, 0, TimeUnit.SECONDS);

	}

	@Test
	public void testSingleThreadUnreliableSend() throws InterruptedException, ExecutionException {
		testUnreliableInternal(1);
	}

	@Test
	public void testMultiThreadUnreliableSend() throws InterruptedException, ExecutionException {
		testUnreliableInternal(5);
	}

	private void testUnreliableInternal(int threadCnt) throws InterruptedException, ExecutionException {

		executorService = Executors.newFixedThreadPool(threadCnt);

		int cnt = 500;

		// payload (msg-nr) -> msg
		final Map<String, Messages.Msg> gw1MessagesSent = new HashMap<String, Messages.Msg>(cnt);
		final Map<String, Messages.Msg> gw2MessagesReceived = new HashMap<String, Messages.Msg>(cnt);

		mes1.addListener(new MessageEventAdapter() {
			@Override
			public void messageSent(Messages.Msg msg) {
				gw1MessagesSent.put((String) MessageTools.getPayloadSerializable(msg), msg);
			}
		}
		);

		mes2.addListener(new MessageEventAdapter() {
			@Override
			public void messageReceived(Messages.Msg msg) {
				gw2MessagesReceived.put((String) MessageTools.getPayloadSerializable(msg), msg);
			}
		}
		);

		// fork processes to do the messaging work
		List<Future> futures = new LinkedList<Future>();
		for (int i = 0; i < cnt; i++) {
			String payload = "" + i;
			Messages.Msg msg =
					MessageTools.buildMessage("gw1", "gw2", "test", payload, 1, System.currentTimeMillis() + 5000);
			futures.add(executorService.submit(new UnreliableMessageRunnable(ums1, msg)));
		}

		// join process to be sure work is done
		for (Future future : futures) {
			assertNull(future.get());
		}

		// wait some time for messages to be asynchronously delivered
		Thread.sleep(cnt * 10);

		for (Map.Entry<String, Messages.Msg> entry : gw1MessagesSent.entrySet()) {

			Messages.Msg sent = entry.getValue();
			Messages.Msg recv = gw2MessagesReceived.get(entry.getKey());

			assertNotNull("Message " + MessageTools.getPayloadSerializable(sent) + " was not received!", recv);

			assertEquals(sent.getFrom(), recv.getFrom());
			assertEquals(sent.getPayload(), recv.getPayload());
			assertEquals(sent.getMsgType(), recv.getMsgType());
			assertEquals(sent.getPriority(), recv.getPriority());
			assertEquals(sent.getReplyTo(), recv.getReplyTo());
			assertEquals(sent.getReplyWith(), recv.getReplyWith());
			assertEquals(sent.getTo(), recv.getTo());
			assertEquals(sent.getValidUntil(), recv.getValidUntil());
		}

		assertSame(0, executorService.shutdownNow().size());

	}

	private static class SenderReceiverMessageEventListener implements MessageEventListener {

		Map<String, Messages.Msg> sent;

		Map<String, Messages.Msg> received;

		Map<String, Messages.Msg> dropped;

		private SenderReceiverMessageEventListener() {
			this.sent = new HashMap<String, Messages.Msg>();
			this.received = new HashMap<String, Messages.Msg>();
			this.dropped = new HashMap<String, Messages.Msg>();
		}

		@Override
		public void messageSent(Messages.Msg msg) {
			sent.put((String) MessageTools.getPayloadSerializable(msg), msg);
		}

		@Override
		public void messageDropped(Messages.Msg msg) {
			dropped.put((String) MessageTools.getPayloadSerializable(msg), msg);
		}

		@Override
		public void messageReceived(Messages.Msg msg) {
			received.put((String) MessageTools.getPayloadSerializable(msg), msg);
		}
	}

	@Test
	public void testSingleThreadReliableSend()
			throws InterruptedException, ReliableMessagingTimeoutException, ExecutionException {
		testReliableSendInternal(1);
	}

	@Test
	public void testMultiThreadReliableSend()
			throws InterruptedException, ReliableMessagingTimeoutException, ExecutionException {
		testReliableSendInternal(5);
	}

	private void testReliableSendInternal(int threadCnt) throws ExecutionException, InterruptedException {

		executorService = Executors.newFixedThreadPool(threadCnt);

		int cnt = 500;

		SenderReceiverMessageEventListener gw1Listener = new SenderReceiverMessageEventListener();
		SenderReceiverMessageEventListener gw2Listener = new SenderReceiverMessageEventListener();

		mes1.addListener(gw1Listener);
		mes2.addListener(gw2Listener);

		mes2.addListener(new MessageEventAdapter() {
			@Override
			public void messageReceived(Messages.Msg msg) {
				if (msg.hasReplyWith()) {
					ums2.sendAsync(
							MessageTools.buildReply(msg, msg.getMsgType(), MessageTools.getPayloadSerializable(msg))
					);
				}
			}
		}
		);

		// maps callable to expected return value of the message exchange
		Map<Future, String> futures = new HashMap<Future, String>();

		// fork some processes to do parallel work
		for (int i = 0; i < cnt; i++) {
			String payload = "" + i;
			ReliableMessageCallable callable = new ReliableMessageCallable("gw1", "gw2", rms1, payload);
			futures.put(executorService.submit(callable), payload);
		}

		// join them and check if the results are as expected
		for (Future future : futures.keySet()) {
			assertEquals(future.get(), futures.get(future));
		}

		compareSentReceived(gw1Listener.sent, gw2Listener.received);
		compareSentReceived(gw2Listener.received, gw1Listener.sent);

		assertSame(0, executorService.shutdownNow().size());

	}

	@Test
	public void testSingleThreadReliableAsync() throws ExecutionException, InterruptedException {
		testReliableAsyncInternal(1);
	}

	@Test
	public void testMultiThreadReliableAsync() throws ExecutionException, InterruptedException {
		testReliableAsyncInternal(5);
	}

	private void testReliableAsyncInternal(int threadCnt) throws ExecutionException, InterruptedException {

		executorService = Executors.newFixedThreadPool(threadCnt);

		int cnt = 500;

		SenderReceiverMessageEventListener gw1Listener = new SenderReceiverMessageEventListener();
		SenderReceiverMessageEventListener gw2Listener = new SenderReceiverMessageEventListener();

		mes1.addListener(gw1Listener);
		mes2.addListener(gw2Listener);

		mes2.addListener(new MessageEventAdapter() {
			@Override
			public void messageReceived(Messages.Msg msg) {
				if (msg.hasReplyWith()) {
					ums2.sendAsync(
							MessageTools.buildReply(msg, msg.getMsgType(), MessageTools.getPayloadSerializable(msg))
					);
				}
			}
		}
		);

		// maps callable to expected return value of the message exchange
		final List<Future> futures = new ArrayList<Future>(cnt);
		final Set<Integer> receivedReplies = new HashSet<Integer>();

		// fork some processes to do parallel work
		for (int i = 0; i < cnt; i++) {
			final int msgNr = i;
			final String payload = "" + msgNr;
			ReliableAsyncMessageCallable runnable = new ReliableAsyncMessageCallable("gw1", "gw2", rms1, payload,
					new ReliableMessagingService.AsyncCallback() {
						@Override
						public void success(byte[] reply) {
							receivedReplies.add(msgNr);
							assertEquals(payload, MessageTools.getSerializable(reply));
						}

						@Override
						public void failure(Exception exception) {
							receivedReplies.add(msgNr);
							fail();
						}
					}
			);
			futures.add(executorService.submit(runnable));
		}

		// join them and check if the results are as expected
		for (Future future : futures) {
			future.get();
		}

		// wait until all asynchronous callback received some kind of notification (either failure or success)
		while (receivedReplies.size() < cnt) {
			Thread.sleep(5);
		}

		compareSentReceived(gw1Listener.sent, gw2Listener.received);
		compareSentReceived(gw2Listener.received, gw1Listener.sent);

		assertSame(0, executorService.shutdownNow().size());

	}

	private class ReliableAsyncMessageCallable implements Runnable {

		private String from;

		private String to;

		private ReliableMessagingService reliableMessagingService;

		private String payload;

		private ReliableMessagingService.AsyncCallback callback;

		private ReliableAsyncMessageCallable(String from, String to, ReliableMessagingService reliableMessagingService,
											 String payload, ReliableMessagingService.AsyncCallback callback) {
			this.from = from;
			this.to = to;
			this.reliableMessagingService = reliableMessagingService;
			this.payload = payload;
			this.callback = callback;
		}

		@Override
		public void run() {
			Messages.Msg msg = MessageTools.buildReliableTransportMessage(
					from,
					to,
					"test",
					payload,
					1,
					System.currentTimeMillis() + 60000
			);
			reliableMessagingService.sendAsync(msg, callback);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}

			ReliableMessageCallable callable = (ReliableMessageCallable) o;

			return from.equals(callable.from) && payload.equals(callable.payload) && to.equals(callable.to);

		}

		@Override
		public int hashCode() {
			int result = from.hashCode();
			result = 31 * result + to.hashCode();
			result = 31 * result + payload.hashCode();
			return result;
		}
	}

	private class ReliableMessageCallable implements Callable<Serializable> {

		private String from;

		private String to;

		private ReliableMessagingService reliableMessagingService;

		private String payload;

		private ReliableMessageCallable(String from, String to, ReliableMessagingService reliableMessagingService,
										String payload) {
			this.from = from;
			this.to = to;
			this.reliableMessagingService = reliableMessagingService;
			this.payload = payload;
		}

		@Override
		public Serializable call() throws Exception {
			Messages.Msg msg = MessageTools.buildReliableTransportMessage(
					from,
					to,
					"test",
					payload,
					1,
					System.currentTimeMillis() + 5000
			);
			return MessageTools.getSerializable(reliableMessagingService.send(msg));
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}

			ReliableMessageCallable callable = (ReliableMessageCallable) o;

			return from.equals(callable.from) && payload.equals(callable.payload) && to.equals(callable.to);

		}

		@Override
		public int hashCode() {
			int result = from.hashCode();
			result = 31 * result + to.hashCode();
			result = 31 * result + payload.hashCode();
			return result;
		}
	}

	private void compareSentReceived(Map<String, Messages.Msg> senderMap, Map<String, Messages.Msg> receiverMap) {

		for (Map.Entry<String, Messages.Msg> entry : senderMap.entrySet()) {

			Messages.Msg sent = entry.getValue();
			Messages.Msg recv = receiverMap.get(entry.getKey());

			assertNotNull("Message " + MessageTools.getPayloadSerializable(sent) + " was not received!", recv);

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

}
