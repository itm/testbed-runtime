package de.uniluebeck.itm.tr.federator.iwsn;

import eu.wisebed.api.v3.sm.ChannelHandlerDescription;
import eu.wisebed.api.v3.sm.SessionManagement;

import java.util.List;
import java.util.concurrent.Callable;

public class GetSupportedChannelHandlersCallable implements Callable<List<ChannelHandlerDescription>> {

	private final SessionManagement endpoint;

	public GetSupportedChannelHandlersCallable(final SessionManagement endpoint) {
		this.endpoint = endpoint;
	}

	@Override
	public List<ChannelHandlerDescription> call() throws Exception {
		return endpoint.getSupportedChannelHandlers();
	}

}
