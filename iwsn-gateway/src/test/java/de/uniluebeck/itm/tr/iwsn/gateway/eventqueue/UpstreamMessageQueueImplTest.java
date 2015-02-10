package de.uniluebeck.itm.tr.iwsn.gateway.eventqueue;

import com.google.common.util.concurrent.SettableFuture;
import com.google.protobuf.MessageLite;
import com.leansoft.bigqueue.IBigQueue;
import de.uniluebeck.itm.tr.common.IdProvider;
import de.uniluebeck.itm.tr.iwsn.gateway.GatewayConfig;
import de.uniluebeck.itm.tr.iwsn.gateway.GatewayEventBus;
import de.uniluebeck.itm.tr.iwsn.messages.DevicesAttachedEvent;
import de.uniluebeck.itm.tr.iwsn.messages.Message;
import de.uniluebeck.itm.util.scheduler.SchedulerService;
import de.uniluebeck.itm.util.serialization.MultiClassSerializationHelper;
import org.jboss.netty.channel.Channel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class UpstreamMessageQueueImplTest {

    UpstreamMessageQueueImpl eventQueue;

    @Mock
    UpstreamMessageQueueHelper eventQueueHelper;

    @Mock
    MultiClassSerializationHelper<MessageLite> serializationHelper;

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

    @Before
    public void setup() throws Exception {

        when(config.getEventQueuePath()).thenReturn("./event-queue");
        when(eventQueueHelper.configureEventSerializationHelper()).thenReturn(serializationHelper);
        when(eventQueueHelper.createAndConfigureQueue()).thenReturn(bigQueue);

        eventQueue = new UpstreamMessageQueueImpl(eventQueueHelper, gatewayEventBus, idProvider, schedulerService);
        eventQueue.startAndWait();
    }


    @Test
    public void testIfEventsNotDequeuedIfChannelIsNotConnected() throws Exception {
        byte[] serialization = new byte[]{1, 2, 3};
        when(idProvider.get()).thenReturn(Long.parseLong("1"));

        DevicesAttachedEvent evt = DevicesAttachedEvent.newBuilder().setTimestamp(1).build();
        when(serializationHelper.deserialize(new byte[]{1, 2, 3})).thenReturn(evt);
        when(serializationHelper.serialize((Message) notNull())).thenReturn(serialization);

        eventQueue.onDevicesAttachedEvent(evt);
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
