package de.uniluebeck.itm.tr.iwsn.gateway.eventqueue;

import de.uniluebeck.itm.tr.iwsn.gateway.GatewayConfig;
import de.uniluebeck.itm.tr.iwsn.messages.DevicesAttachedEvent;
import de.uniluebeck.itm.tr.iwsn.messages.Event;
import de.uniluebeck.itm.tr.iwsn.messages.Message;
import de.uniluebeck.itm.util.serialization.MultiClassSerializationHelper;
import eu.wisebed.api.v3.common.NodeUrn;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static de.uniluebeck.itm.tr.iwsn.messages.MessageFactory.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UpstreamMessageQueueHelperImplTest {

    @Mock
    private GatewayConfig config;
    private UpstreamMessageQueueHelperImpl helper;
    private MultiClassSerializationHelper<Message> serializer;

    @Before
    public void setUp() throws Exception {

        when(config.getEventQueuePath()).thenReturn("./event-queue");

        helper = new UpstreamMessageQueueHelperImpl(config);
        serializer = helper.configureEventSerializationHelper();
    }

    @Test
    public void testSerializationHelper() throws Exception {

        NodeUrn nodeUrn = new NodeUrn("urn:unit:test:0x1");
        long timestamp = DateTime.now().getMillis();
        DevicesAttachedEvent devicesAttachedEvent = newDevicesAttachedEvent(timestamp, nodeUrn);
        Event event = newEvent(123, devicesAttachedEvent);
        Message message = newMessage(event);

        byte[] serialized = serializer.serialize(message);

        Message deserialized = serializer.deserialize(serialized);

        assertEquals(message, deserialized);
    }
}
