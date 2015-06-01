package de.uniluebeck.itm.tr.iwsn.gateway.eventqueue;

import de.uniluebeck.itm.tr.common.IncrementalIdProvider;
import de.uniluebeck.itm.tr.common.UnixTimestampProvider;
import de.uniluebeck.itm.tr.iwsn.common.MessageWrapper;
import de.uniluebeck.itm.tr.iwsn.gateway.GatewayConfig;
import de.uniluebeck.itm.tr.iwsn.messages.DevicesAttachedEvent;
import de.uniluebeck.itm.tr.iwsn.messages.Message;
import de.uniluebeck.itm.tr.iwsn.messages.MessageFactory;
import de.uniluebeck.itm.tr.iwsn.messages.MessageFactoryImpl;
import de.uniluebeck.itm.util.serialization.MultiClassSerializationHelper;
import eu.wisebed.api.v3.common.NodeUrn;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Optional;

import static java.util.Optional.of;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UpstreamMessageQueueHelperImplTest {

	private static final NodeUrn NODE_URN = new NodeUrn("urn:unit:test:0x1");

	private static final Optional<Long> NOW = of(DateTime.now().getMillis());

	private static final MessageFactory MESSAGE_FACTORY = new MessageFactoryImpl(
			new IncrementalIdProvider(), new UnixTimestampProvider()
	);

	@Mock
	private GatewayConfig config;

	private MultiClassSerializationHelper<Message> serializer;

	@Before
	public void setUp() throws Exception {
		when(config.getEventQueuePath()).thenReturn("./event-queue");
		UpstreamMessageQueueHelperImpl helper = new UpstreamMessageQueueHelperImpl(config);
		serializer = helper.configureEventSerializationHelper();
	}

	@Test
	public void testSerializationHelper() throws Exception {
		DevicesAttachedEvent devicesAttachedEvent = MESSAGE_FACTORY.devicesAttachedEvent(NOW, NODE_URN);
		Message message = MessageWrapper.WRAP_FUNCTION.apply(devicesAttachedEvent);
		assertEquals(message, serializer.deserialize(serializer.serialize(message)));
	}
}
