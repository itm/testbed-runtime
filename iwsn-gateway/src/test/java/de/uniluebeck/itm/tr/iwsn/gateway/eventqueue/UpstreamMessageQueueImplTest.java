package de.uniluebeck.itm.tr.iwsn.gateway.eventqueue;

import com.google.common.util.concurrent.SettableFuture;
import com.leansoft.bigqueue.IBigQueue;
import de.uniluebeck.itm.tr.common.IdProvider;
import de.uniluebeck.itm.tr.common.IncrementalIdProvider;
import de.uniluebeck.itm.tr.common.UnixTimestampProvider;
import de.uniluebeck.itm.tr.iwsn.common.MessageWrapper;
import de.uniluebeck.itm.tr.iwsn.gateway.GatewayConfig;
import de.uniluebeck.itm.tr.iwsn.gateway.GatewayEventBus;
import de.uniluebeck.itm.tr.iwsn.messages.*;
import de.uniluebeck.itm.util.scheduler.SchedulerService;
import de.uniluebeck.itm.util.serialization.MultiClassSerializationHelper;
import eu.wisebed.api.v3.common.NodeUrn;
import org.jboss.netty.channel.Channel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import sun.net.www.MessageHeader;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class UpstreamMessageQueueImplTest {

	UpstreamMessageQueueImpl eventQueue;

	@Mock
	UpstreamMessageQueueHelper eventQueueHelper;

	@Mock
	MultiClassSerializationHelper<Message> serializationHelper;

	@Mock
	GatewayConfig config;

	@Mock
	GatewayEventBus gatewayEventBus;

	@Mock
	IBigQueue bigQueue;

	@Mock
	IdProvider idProvider;

	@Mock
	private SchedulerService schedulerService;
	private MessageFactory messageFactory;

	@Before
	public void setup() throws Exception {

		messageFactory = new MessageFactoryImpl(new IncrementalIdProvider(), new UnixTimestampProvider());

		when(config.getEventQueuePath()).thenReturn("./event-queue");
		when(eventQueueHelper.configureEventSerializationHelper()).thenReturn(serializationHelper);
		when(eventQueueHelper.createAndConfigureQueue()).thenReturn(bigQueue);

		eventQueue = new UpstreamMessageQueueImpl(eventQueueHelper, gatewayEventBus, schedulerService, messageFactory);
		eventQueue.startAsync().awaitRunning();
	}

	@Test
	public void testIfEventsNotDequeuedIfChannelIsNotConnected() throws Exception {
		byte[] serialization = new byte[]{1, 2, 3};
		when(idProvider.get()).thenReturn(Long.parseLong("1"));

		NotificationEvent event = messageFactory.notificationEvent(
				of(newArrayList(new NodeUrn("urn:unit:test:0x1"))),
				empty(),
				"blablabla"
		);

		Message message = MessageWrapper.wrap(event);

		when(serializationHelper.deserialize(new byte[]{1, 2, 3})).thenReturn(message);
		when(serializationHelper.serialize((Message) notNull())).thenReturn(serialization);

		eventQueue.onEvent(event);

		verify(bigQueue).enqueue(serialization);
		verify(bigQueue, never()).dequeue();
		verify(bigQueue, never()).dequeueAsync();

		Channel channel = mock(Channel.class);
		SettableFuture<byte[]> future = SettableFuture.create();
		future.set(serialization);
		when(bigQueue.dequeueAsync()).thenReturn(future);
		eventQueue.channelConnected(channel);
		verify(bigQueue).dequeueAsync();
	}
}
