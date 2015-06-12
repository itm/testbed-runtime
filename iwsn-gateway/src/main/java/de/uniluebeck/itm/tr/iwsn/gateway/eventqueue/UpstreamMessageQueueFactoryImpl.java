package de.uniluebeck.itm.tr.iwsn.gateway.eventqueue;

import com.google.inject.Inject;
import com.leansoft.bigqueue.BigQueueImpl;
import com.leansoft.bigqueue.IBigQueue;
import de.uniluebeck.itm.tr.iwsn.gateway.GatewayConfig;

import java.io.IOException;

public class UpstreamMessageQueueFactoryImpl implements UpstreamMessageQueueFactory {

	private final GatewayConfig gatewayConfig;

	@Inject
	public UpstreamMessageQueueFactoryImpl(final GatewayConfig gatewayConfig) {
		this.gatewayConfig = gatewayConfig;
	}

	@Override
	public IBigQueue create(String queueName) throws IOException {
		return new BigQueueImpl(gatewayConfig.getEventQueuePath(), queueName);
	}
}
